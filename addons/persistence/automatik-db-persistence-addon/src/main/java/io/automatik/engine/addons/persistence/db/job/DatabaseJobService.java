package io.automatik.engine.addons.persistence.db.job;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.addons.persistence.db.model.JobInstanceEntity;
import io.automatik.engine.addons.persistence.db.model.JobInstanceEntity.JobStatus;
import io.automatik.engine.api.Application;
import io.automatik.engine.api.Model;
import io.automatik.engine.api.auth.IdentityProvider;
import io.automatik.engine.api.auth.TrustedIdentityProvider;
import io.automatik.engine.api.jobs.JobsService;
import io.automatik.engine.api.jobs.ProcessInstanceJobDescription;
import io.automatik.engine.api.jobs.ProcessJobDescription;
import io.automatik.engine.api.uow.UnitOfWorkManager;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.Processes;
import io.automatik.engine.services.time.TimerInstance;
import io.automatik.engine.services.uow.UnitOfWorkExecutor;
import io.automatik.engine.workflow.Sig;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class DatabaseJobService implements JobsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseJobService.class);

    protected final Long interval;

    protected final UnitOfWorkManager unitOfWorkManager;

    protected final ScheduledThreadPoolExecutor scheduler;

    protected final ScheduledThreadPoolExecutor loadScheduler;

    protected ManagedExecutor exec;

    protected Map<String, Process<? extends Model>> mappedProcesses = new HashMap<>();
    protected ConcurrentHashMap<String, ScheduledFuture<?>> scheduledJobs = new ConcurrentHashMap<>();

    @Inject
    public DatabaseJobService(ManagedExecutor exec,
            @ConfigProperty(name = "quarkus.automatik.jobs.db.interval", defaultValue = "10") Long interval,
            @ConfigProperty(name = "quarkus.automatik.jobs.db.threads", defaultValue = "1") int threads,
            Processes processes, Application application) {
        this.exec = exec;
        this.interval = interval;
        processes.processIds().forEach(id -> mappedProcesses.put(id, processes.processById(id)));

        this.unitOfWorkManager = application.unitOfWorkManager();

        this.scheduler = new ScheduledThreadPoolExecutor(threads, r -> new Thread(r, "automatik-jobs-executor"));
        this.loadScheduler = new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "automatik-jobs-loader"));
    }

    public void start(@Observes StartupEvent event) {
        loadScheduler.scheduleAtFixedRate(() -> {
            UnitOfWorkExecutor.executeInUnitOfWork(unitOfWorkManager, () -> {
                LocalDateTime next = LocalDateTime.now().plus(Duration.ofMinutes(interval));
                List<JobInstanceEntity> jobs = JobInstanceEntity.loadJobs(next);
                LOGGER.debug("Loaded jobs ({}) to be executed before {}", jobs.size(), next);
                for (JobInstanceEntity job : jobs) {

                    if (job.ownerInstanceId == null) {
                        scheduledJobs.computeIfAbsent(job.id, k -> {
                            return log(job.id, scheduler.schedule(new StartProcessOnExpiredTimer(job.id,
                                    job.ownerDefinitionId, -1),
                                    Duration.between(LocalDateTime.now(), job.expirationTime).toMillis(),
                                    TimeUnit.MILLISECONDS));
                        });
                    } else {
                        scheduledJobs.computeIfAbsent(job.id, k -> {
                            return log(job.id, scheduler.schedule(
                                    new SignalProcessInstanceOnExpiredTimer(job.id, job.triggerType,
                                            job.ownerDefinitionId,
                                            job.ownerInstanceId, job.limit),
                                    Duration.between(LocalDateTime.now(), job.expirationTime).toMillis(),
                                    TimeUnit.MILLISECONDS));
                        });
                    }
                }
                return null;
            });

        }, 1, interval * 60, TimeUnit.SECONDS);
    }

    public void shutdown(@Observes ShutdownEvent event) {
        this.loadScheduler.shutdownNow();

        this.scheduler.shutdown();
    }

    @Override
    public String scheduleProcessJob(ProcessJobDescription description) {
        LOGGER.debug("ScheduleProcessJob: {}", description);
        JobInstanceEntity scheduledJob = null;
        if (description.expirationTime().repeatInterval() != null) {

            scheduledJob = new JobInstanceEntity(description.id(),
                    description.process().id(),
                    JobInstanceEntity.JobStatus.SCHEDULED,
                    description.expirationTime().get().toLocalDateTime(),
                    description.expirationTime().repeatLimit(), description.expirationTime().repeatInterval());
        } else {

            scheduledJob = new JobInstanceEntity(description.id(),
                    description.process().id(),
                    JobInstanceEntity.JobStatus.SCHEDULED,
                    description.expirationTime().get().toLocalDateTime(),
                    -1, null);
        }
        JobInstanceEntity persist = scheduledJob;
        UnitOfWorkExecutor.executeInUnitOfWork(unitOfWorkManager, () -> {
            if (JobInstanceEntity.findById(persist.id) == null) {

                JobInstanceEntity.persist(persist);
            }
            return null;
        });
        if (description.expirationTime().get().toLocalDateTime().isBefore(LocalDateTime.now().plusMinutes(interval))) {

            scheduledJobs.computeIfAbsent(description.id(), k -> {
                return scheduler.schedule(processJobByDescription(description),
                        calculateDelay(description.expirationTime().get()), TimeUnit.MILLISECONDS);
            });
        }

        return description.id();
    }

    @Override
    public String scheduleProcessInstanceJob(ProcessInstanceJobDescription description) {

        JobInstanceEntity scheduledJob = null;
        if (description.expirationTime().repeatInterval() != null) {

            scheduledJob = new JobInstanceEntity(description.id(), description.triggerType(),
                    description.processId() + version(description.processVersion()),
                    description.processInstanceId(),
                    JobInstanceEntity.JobStatus.SCHEDULED,
                    description.expirationTime().get().toLocalDateTime(),
                    description.expirationTime().repeatLimit(), description.expirationTime().repeatInterval());
        } else {
            scheduledJob = new JobInstanceEntity(description.id(), description.triggerType(),
                    description.processId() + version(description.processVersion()), description.processInstanceId(),
                    JobInstanceEntity.JobStatus.SCHEDULED,
                    description.expirationTime().get().toLocalDateTime(),
                    -1, null);
        }
        JobInstanceEntity.persist(scheduledJob);

        if (description.expirationTime().get().toLocalDateTime().isBefore(LocalDateTime.now().plusMinutes(interval))) {

            scheduledJobs.computeIfAbsent(description.id(), k -> {
                return log(description.id(), scheduler.schedule(
                        new SignalProcessInstanceOnExpiredTimer(description.id(), description.triggerType(),
                                description.processId() + version(description.processVersion()),
                                description.processInstanceId(), description.expirationTime().repeatLimit()),
                        calculateDelay(description.expirationTime().get()),
                        TimeUnit.MILLISECONDS));
            });
        }

        return description.id();
    }

    @Override
    public boolean cancelJob(String id) {

        return JobInstanceEntity.deleteById(id);
    }

    @Override
    public ZonedDateTime getScheduledTime(String id) {
        JobInstanceEntity found = JobInstanceEntity.findById(id);

        if (found == null) {

        }
        return ZonedDateTime.of(found.expirationTime, ZoneId.systemDefault());
    }

    protected long calculateDelay(ZonedDateTime expirationDate) {
        return Duration.between(ZonedDateTime.now(), expirationDate).toMillis();
    }

    protected Runnable processJobByDescription(ProcessJobDescription description) {
        return new StartProcessOnExpiredTimer(description.id(),
                description.process().id(), description.expirationTime().repeatLimit());

    }

    protected String version(String version) {
        if (version != null && !version.trim().isEmpty()) {
            return "_" + version.replaceAll("\\.", "_");
        }
        return "";
    }

    protected void removeScheduledJob(String id) {
        JobInstanceEntity.deleteById(id);
    }

    protected void updateRepeatableJob(String id) {
        JobInstanceEntity job = JobInstanceEntity.findById(id);

        job.limit = job.limit - 1;
        job.expirationTime = job.expirationTime.plus(job.repeatInterval, ChronoUnit.MILLIS);
        job.status = JobStatus.SCHEDULED;
        JobInstanceEntity.persist(job);

        if (job.ownerInstanceId == null) {
            scheduledJobs.computeIfAbsent(job.id, k -> {
                return log(job.id, scheduler.schedule(new StartProcessOnExpiredTimer(job.id,
                        job.ownerDefinitionId, job.limit),
                        Duration.between(LocalDateTime.now(), job.expirationTime).toMillis(),
                        TimeUnit.MILLISECONDS));
            });
        } else {
            scheduledJobs.computeIfAbsent(job.id, k -> {
                return log(job.id, scheduler.scheduleAtFixedRate(
                        new SignalProcessInstanceOnExpiredTimer(job.id,
                                job.triggerType,
                                job.ownerDefinitionId,
                                job.ownerInstanceId, job.limit),
                        Duration.between(LocalDateTime.now(), job.expirationTime).toMillis(), job.repeatInterval,
                        TimeUnit.MILLISECONDS));
            });
        }
    }

    protected ScheduledFuture<?> log(String jobId, ScheduledFuture<?> future) {
        LOGGER.debug("Next fire of job {} is in {} seconds ", jobId, future.getDelay(TimeUnit.SECONDS));

        return future;
    }

    private class SignalProcessInstanceOnExpiredTimer implements Runnable {

        private final String id;
        private final String processId;
        private String processInstanceId;

        private final String trigger;
        private Integer limit;

        private SignalProcessInstanceOnExpiredTimer(String id, String trigger, String processId, String processInstanceId,
                Integer limit) {
            this.id = id;
            this.processId = processId;
            this.processInstanceId = processInstanceId;
            this.trigger = trigger;
            this.limit = limit;
        }

        @Override
        public void run() {
            LOGGER.debug("Job {} started", id);
            boolean acquired = UnitOfWorkExecutor.executeInUnitOfWork(unitOfWorkManager, () -> {

                JobInstanceEntity job = JobInstanceEntity.acquireJob(id);
                if (job == null || job.status != JobStatus.SCHEDULED) {
                    return false;
                }
                job.status = JobStatus.TAKEN;

                JobInstanceEntity.persist(job);

                return true;

            });

            if (!acquired) {
                scheduledJobs.remove(id).cancel(true);

                return;
            }
            Process<?> process = mappedProcesses.get(processId);
            if (process == null) {
                LOGGER.warn("No process found for process id {}", processId);
                return;
            }
            IdentityProvider.set(new TrustedIdentityProvider("System<timer>"));
            UnitOfWorkExecutor.executeInUnitOfWork(unitOfWorkManager, () -> {
                Optional<? extends ProcessInstance<?>> processInstanceFound = process.instances()
                        .findById(processInstanceId);
                if (processInstanceFound.isPresent()) {
                    ProcessInstance<?> processInstance = processInstanceFound.get();
                    String[] ids = id.split("_");
                    processInstance
                            .send(Sig.of(trigger, TimerInstance.with(Long.parseLong(ids[1]), id, limit)));
                    scheduledJobs.remove(id).cancel(false);
                    if (limit > 0) {
                        updateRepeatableJob(id);
                    }
                } else {
                    // since owning process instance does not exist cancel timers
                    scheduledJobs.remove(id).cancel(false);
                    removeScheduledJob(id);
                }

                return null;
            });
            LOGGER.debug("Job {} completed", id);

        }
    }

    private class StartProcessOnExpiredTimer implements Runnable {

        private final String id;
        private final String processId;

        private Integer limit;

        private StartProcessOnExpiredTimer(String id, String processId, Integer limit) {
            this.id = id;
            this.processId = processId;
            this.limit = limit;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public void run() {
            LOGGER.debug("Job {} started", id);
            boolean acquired = UnitOfWorkExecutor.executeInUnitOfWork(unitOfWorkManager, () -> {

                JobInstanceEntity job = JobInstanceEntity.acquireJob(id);
                if (job == null) {
                    return false;
                }
                job.status = JobStatus.TAKEN;

                JobInstanceEntity.persist(job);

                return true;

            });

            if (!acquired) {
                scheduledJobs.remove(id).cancel(true);

                return;
            }
            Process process = mappedProcesses.get(processId);
            if (process == null) {
                LOGGER.warn("No process found for process id {}", processId);
                return;
            }
            IdentityProvider.set(new TrustedIdentityProvider("System<timer>"));
            UnitOfWorkExecutor.executeInUnitOfWork(unitOfWorkManager, () -> {
                ProcessInstance<?> pi = process.createInstance(process.createModel());
                if (pi != null) {
                    pi.start("timer", null, null);
                }
                scheduledJobs.remove(id).cancel(false);
                limit--;
                if (limit > 0) {
                    updateRepeatableJob(id);
                }
                return null;
            });

            LOGGER.debug("Job {} completed", id);
        }
    }

}
