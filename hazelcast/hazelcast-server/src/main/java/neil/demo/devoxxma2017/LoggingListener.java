package neil.demo.devoxxma2017;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>A map listener that captures data creation and data
 * update events on any map, and prints them out.
 * </p>
 * <p>This is purely for diagnostics. You could chose for
 * this to be local (only printing the events that occur on its
 * JVM) or global (on any JVM). For a grid of 3 nodes, the
 * local setting would see each update logged once by one JVM,
 * and the global setting would see each update logged once
 * each by all three JVMs.
 * </p>
 * <p>There are other events we could log here, such as "{@code EntryExpiredListener}"
 * or "{@code EntryEvictedListener}". This code example doens't use eviction or
 * expiry, so the events wouldn't be created and so not logged.
 * </p>
 */
@SuppressWarnings("rawtypes")
@Slf4j
public class LoggingListener implements EntryAddedListener, EntryUpdatedListener {

	@Override
	public void entryUpdated(EntryEvent arg0) {
		this.log(arg0);
	}
	@Override
	public void entryAdded(EntryEvent arg0) {
		this.log(arg0);
	}

	/**
	 * <p>Generic logging of {@link com.hazelcast.core.IMap IMap} data
	 * change event.
	 * </p>
	 * <p>An event has a value and a previous value, which we don't
	 * use. For creates the previous value is null. For deletes
	 * the new value is null.
	 * </p>
	 * 
	 * @param entryEvent A key, a value and more
	 * @throws Exception
	 */
	private void log(EntryEvent entryEvent) {
		log.info("{} event, map '{}', key '{}', new value '{}'",
				entryEvent.getEventType(),
				entryEvent.getName(),
				entryEvent.getKey(),
				entryEvent.getValue());
	}

}
