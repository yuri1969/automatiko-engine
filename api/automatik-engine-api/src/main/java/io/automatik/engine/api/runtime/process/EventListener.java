
package io.automatik.engine.api.runtime.process;

import java.util.Collections;
import java.util.Set;

import io.automatik.engine.api.workflow.EventDescription;

/**
 * An interface that represents an element that is listening for specific types
 * of events.
 */
public interface EventListener {

	/**
	 * Signals that an event has occurred. The type parameter defines which type of
	 * event and the event parameter can contain additional information related to
	 * the event.
	 *
	 * @param type  the type of event
	 * @param event the data associated with this event
	 */
	void signalEvent(String type, Object event);

	/**
	 * Returns the event types this event listener is interested in. May return
	 * <code>null</code> if the event types are unknown.
	 */
	String[] getEventTypes();

	/**
	 * Returns unique set of event descriptions that this event listener is
	 * interested in.
	 * 
	 * @return returns set of event definitions awaiting or empty set
	 */
	default Set<EventDescription<?>> getEventDescriptions() {
		return Collections.emptySet();
	}

}
