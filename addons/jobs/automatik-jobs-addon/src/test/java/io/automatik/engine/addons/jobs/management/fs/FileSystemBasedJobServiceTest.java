package io.automatik.engine.addons.jobs.management.fs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.automatik.engine.api.Application;
import io.automatik.engine.api.jobs.DurationExpirationTime;
import io.automatik.engine.api.jobs.ProcessInstanceJobDescription;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.Processes;
import io.automatik.engine.services.uow.CollectingUnitOfWorkFactory;
import io.automatik.engine.services.uow.DefaultUnitOfWorkManager;

@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
public class FileSystemBasedJobServiceTest {

	@Mock
	Processes processes;

	@Mock
	Application application;

	@Mock
	Process process;

	@Test
	public void testScheduleJobsForProcessInstance() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		when(application.unitOfWorkManager())
				.thenReturn(new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory()));
		when(processes.processById("processId")).then(i -> {
			latch.countDown();
			return process;
		});

		FileSystemBasedJobService jobs = new FileSystemBasedJobService("target/jobs", 1, processes, application);

		ProcessInstanceJobDescription processInstanceJobDescription = ProcessInstanceJobDescription.of(123,
				DurationExpirationTime.after(500), "processInstanceId", "processId");

		jobs.scheduleProcessInstanceJob(processInstanceJobDescription);

		boolean achieved = latch.await(2, TimeUnit.SECONDS);
		assertThat(achieved).isTrue();
		verify(processes, times(1)).processById(eq("processId"));
	}

	@Test
	public void testScheduleJobsForProcessInstanceReload() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		when(application.unitOfWorkManager())
				.thenReturn(new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory()));
		when(processes.processById("processId")).then(i -> {
			latch.countDown();
			return process;
		});

		FileSystemBasedJobService jobs = new FileSystemBasedJobService("target/jobs", 1, processes, application);

		ProcessInstanceJobDescription processInstanceJobDescription = ProcessInstanceJobDescription.of(123,
				DurationExpirationTime.after(100), "processInstanceId", "processId");

		jobs.scheduleProcessInstanceJob(processInstanceJobDescription);

		jobs.shutown(null);

		jobs = new FileSystemBasedJobService("target/jobs", 1, processes, application);
		jobs.scheduleOnLoad(null);

		boolean achieved = latch.await(2, TimeUnit.SECONDS);
		assertThat(achieved).isTrue();
		verify(processes, times(1)).processById(eq("processId"));
	}

	@Test
	public void testScheduleJobsForProcessInstanceAndCance() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		when(application.unitOfWorkManager())
				.thenReturn(new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory()));
		lenient().when(processes.processById("processId")).then(i -> {
			latch.countDown();
			return process;
		});

		FileSystemBasedJobService jobs = new FileSystemBasedJobService("target/jobs", 1, processes, application);

		ProcessInstanceJobDescription processInstanceJobDescription = ProcessInstanceJobDescription.of(123,
				DurationExpirationTime.after(500), "processInstanceId", "processId");

		jobs.scheduleProcessInstanceJob(processInstanceJobDescription);

		jobs.cancelJob(processInstanceJobDescription.id());

		boolean achieved = latch.await(1, TimeUnit.SECONDS);
		assertThat(achieved).isFalse();
		verify(processes, times(0)).processById(eq("processId"));
	}
}
