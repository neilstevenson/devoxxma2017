package neil.demo.devoxxma2017;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;

import lombok.extern.slf4j.Slf4j;
import neil.demo.devoxxma2017.jet.ReadKafka;
import neil.demo.devoxxma2017.jet.Speedo;

/**
 * <p>A map listener that responds to events in a map named "{@code command}".
 * Entries are written to the map in the form (<b>verb</b>,<b>noun</b>).
 * For example "{@code start,kafka}" means a request to start the Kafka
 * job.
 * </p>
 * <p>Note these commands are requests. The listener can chose to ignore
 * them, for example if a request is made to start a job that is already
 * running.
 * </p>
 * <p>Note also this listener should be defined as local not global, meaning
 * it is only told of map events occuring on it's own JVM. If set for
 * global, one map event on one JVM would trigger this listener on
 * all servers in the grid, so the command would be processed more
 * than once.
 * </p>
 */
@Slf4j
@Component
public class CommandListener implements EntryAddedListener<String, String[]>, EntryUpdatedListener<String, String[]> {

	@Autowired
	private JetInstance jetInstance;

	private Job kafka = null;
	private Job speedo = null;
	
	@Override
	public void entryUpdated(EntryEvent<String, String[]> arg0) {
		try {
			this.handle(arg0);
		} catch (Exception e) {
			log.error("entryUpdated", e);
		}
	}
	@Override
	public void entryAdded(EntryEvent<String, String[]> arg0) {
		try {
			this.handle(arg0);
		} catch (Exception e) {
			log.error("entryAdded", e);
		}
	}

	/**
	 * <p>Process the key verb of the command, start or stop.
	 * </p>
	 * 
	 * @param arg0 A noun and verb params
	 * @throws Exception
	 */
	private void handle(EntryEvent<String, String[]> arg0) throws Exception {
		String verb = arg0.getKey();
		String[] params = arg0.getValue();

		log.info("'{}' '{}'", verb, params);
		
		if (verb.equalsIgnoreCase(Constants.COMMAND_VERB_START)) {
			this.handleStart(params);
		} else {
			if (verb.equals(Constants.COMMAND_VERB_STOP)) {
				this.handleStop(params);
			} else {
				log.error("Unknown command verb '{}'", verb);
			}
		}
	}

	
	/**
	 * <p>Start a job, if not running.
	 * </p>
	 *
	 * @param params Job name and any required params
	 */
	private void handleStart(String[] params) {
		if (params[0].equalsIgnoreCase(Constants.COMMAND_NOUN_KAFKA)) {
			if (this.kafka == null) {
				DAG dag = ReadKafka.build(params[1]);
				this.kafka = this.jetInstance.newJob(dag);
				log.info("Started Kafka Reader, job id {}", this.kafka.getJobId());
			} else {
				log.info("Ignoring start request, Kakfa Reader job id {} already running", this.kafka.getJobId());
			}
		} else {
			if (params[0].equalsIgnoreCase(Constants.COMMAND_NOUN_SPEEDO)) {
				if (this.speedo == null) {
					DAG dag = Speedo.build();
					this.speedo = this.jetInstance.newJob(dag);
					log.info("Started Speedo, job id {}", this.speedo.getJobId());
				} else {
					log.info("Ignoring start request, Speedo job id {} already running", this.speedo.getJobId());
				}
			} else {
				log.error("Unknown command noun '{}'", params[0]);
			}
		}
	}
	
	
	/**
	 * <p>Stop a job, if running.
	 * </p>
	 *
	 * @param params Job name
	 */
	private void handleStop(String[] params) {
		if (params[0].equalsIgnoreCase(Constants.COMMAND_NOUN_KAFKA)) {
			if (this.kafka != null) {
				log.info("Stopping Kafka Reader, job id {}", this.kafka.getJobId());
				this.kafka = null;
			} else {
				log.info("Ignoring stop request, Kakfa Reader job is not running");
			}
		} else {
			if (params[0].equalsIgnoreCase(Constants.COMMAND_NOUN_SPEEDO)) {
				if (this.speedo != null) {
					log.info("Stopping Speedo, job id {}", this.speedo.getJobId());
					this.speedo = null;
				} else {
					log.info("Ignoring stop request, Speedo job is not running");
				}
			} else {
				log.error("Unknown command noun '{}'", params[0]);
			}
		}
	}

}
