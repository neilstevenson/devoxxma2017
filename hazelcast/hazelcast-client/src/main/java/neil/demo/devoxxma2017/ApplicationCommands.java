package neil.demo.devoxxma2017;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

/**
 * <p>Additional commands that the Hazelcast client provides
 * to the Spring Shell CLI.
 * </p>
 * <p>Mainly these commands are requests to control Jet jobs,
 * in (<b>noun</b>,<b>verb</b>) format. For example,
 * "{@code kafka,start}" and "{@code kafka,stop}".
 * </p>
 * <p>We request these rather than initiate them directly,
 * as it's easier for the command listener to track only
 * one instance of the job runs than to do it here.
 * </p>
 * <p>There's a bit of repetition in the coding for starting
 * and stopping the different job kinds, easily re-factored.
 * </p>
 */
@Component
public class ApplicationCommands implements CommandMarker {

	private static final String DISTRIBUTED_OBJECT_INTERNAL_PREFIX = "__";
	
	private static final String KAFKA_START
		= Constants.COMMAND_NOUN_KAFKA + "-" + Constants.COMMAND_VERB_START;
	private static final String KAFKA_STOP
		= Constants.COMMAND_NOUN_KAFKA + "-" + Constants.COMMAND_VERB_STOP;
	private static final String SPEEDO_START
		= Constants.COMMAND_NOUN_SPEEDO + "-" + Constants.COMMAND_VERB_START;
	private static final String SPEEDO_STOP
		= Constants.COMMAND_NOUN_SPEEDO + "-" + Constants.COMMAND_VERB_STOP;
	// Wordcount is not a continuous job, ends when input exhausted
	private static final String WORDCOUNT_START
		= Constants.COMMAND_NOUN_WORDCOUNT + "-" + Constants.COMMAND_VERB_START;
	
	private ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private HazelcastInstance hazelcastInstance;
	@Value("${bootstrap-servers}")
	private String bootstrapServers;

	/**
	 * <p>Request the Kafka stream reader be started.
	 * </p>
	 * 
	 * @param kafka (Optional) Kafka servers to use instead of {@code "${bootstrap-servers}"
	 */
	@CliCommand(value = KAFKA_START,
				help = "Request initiation of the Kafka Reader")
	public String startKafka(
			
            @CliOption(key = {"kafka"}
            , mandatory = false
            , help = "Optionally '--kafka 127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094' to specify Kafka"
            		)
            final String kafka

			) {
		
		IMap<String, String[]> commandMap = this.hazelcastInstance.getMap(Constants.IMAP_NAME_COMMAND);

		String[] params = new String[2];
		params[0] = Constants.COMMAND_VERB_START;
		if (kafka==null || kafka.length()==0) {
			params[1] = this.bootstrapServers;
		} else {
			params[1] = kafka;
		}
				
		commandMap.put(Constants.COMMAND_NOUN_KAFKA, params);
		
		return String.format("Requested %s job '%s' with %s", Constants.COMMAND_VERB_START, Constants.COMMAND_NOUN_KAFKA, params[1]);
	}

	
	/**
	 * <p>Request the Kafka stream reader be stopped.
	 * </p>
	 */
	@CliCommand(value = KAFKA_STOP,
				help = "Request halt for the Kafka Reader")
	public String stopKafka() {
		
		IMap<String, String[]> commandMap = this.hazelcastInstance.getMap(Constants.IMAP_NAME_COMMAND);

		String[] params = new String[1];
		params[0] = Constants.COMMAND_VERB_STOP;
		
		commandMap.put(Constants.COMMAND_NOUN_KAFKA, params);
		
		return String.format("Requested %s job '%s'", Constants.COMMAND_VERB_STOP, Constants.COMMAND_NOUN_KAFKA);
	}
	

	/**
	 * <p>Request the speed stream reader be started.
	 * </p>
	 */
	@CliCommand(value = SPEEDO_START,
				help = "Request initiation of the Speedometer")
	public String startSpeedo() {
		
		IMap<String, String[]> commandMap = this.hazelcastInstance.getMap(Constants.IMAP_NAME_COMMAND);

		String[] params = new String[1];
		params[0] = Constants.COMMAND_VERB_START;
		
		commandMap.put(Constants.COMMAND_NOUN_SPEEDO, params);
		
		return String.format("Requested %s job '%s'", Constants.COMMAND_VERB_START, Constants.COMMAND_NOUN_SPEEDO);
	}

	
	/**
	 * <p>Request the speed stream reader be stopped.
	 * </p>
	 */
	@CliCommand(value = SPEEDO_STOP,
				help = "Request halt for the Speedometer")
	public String stopSpeedo() {
		
		IMap<String, String[]> commandMap = this.hazelcastInstance.getMap(Constants.IMAP_NAME_COMMAND);

		String[] params = new String[1];
		params[0] = Constants.COMMAND_VERB_STOP;
		
		commandMap.put(Constants.COMMAND_NOUN_SPEEDO, params);
		
		return String.format("Requested %s job '%s'", Constants.COMMAND_VERB_STOP, Constants.COMMAND_NOUN_SPEEDO);
	}
	

	/**
	 * <p>Request the word count job be run. There is no STOP
	 * needed as this is not a continuous job.
	 * </p>
	 */
	@CliCommand(value = WORDCOUNT_START,
				help = "Request Wordcount runs to completion")
	public String wordcount() {
		
		IMap<String, String[]> commandMap = this.hazelcastInstance.getMap(Constants.IMAP_NAME_COMMAND);

		String[] params = new String[1];
		params[0] = Constants.COMMAND_VERB_START;
		
		commandMap.put(Constants.COMMAND_NOUN_WORDCOUNT, params);
		
		return String.format("Requested %s job '%s'", Constants.COMMAND_VERB_START, Constants.COMMAND_NOUN_WORDCOUNT);
	}

	
	// -- Helpful commands below
	
	/**
	 * <p>Diagnostic command: Map, etc content.
	 * </p>
	 * 
	 * @throws JsonProcessingException If {@code ObjectMapper} fails on pretty print
	 */
	@SuppressWarnings("rawtypes")
	@CliCommand(value = "data",
			help = "List the IMDG data contents")
	public String data() throws JsonProcessingException {		
		
		Collection<DistributedObject> distributedObjects = this.hazelcastInstance.getDistributedObjects();
	
		StringBuilder result = new StringBuilder(String.format("=============================================%n"));
		
		int count = 0;
	
		// Exclude syastem objects, with "__" prefix on their name
		for (DistributedObject distributedObject : distributedObjects) {
			if (!distributedObject.getName().startsWith(DISTRIBUTED_OBJECT_INTERNAL_PREFIX)) {
				count++;
				if (distributedObject instanceof IMap) {
					IMap<?, ?> iMap = (IMap) distributedObject;
					result.append(String.format("IMap '%s', size '%d'%n",
							iMap.getName(), iMap.size()));
					
					// Not sorted, can't assume comparable
					for (Object key : iMap.keySet()) {
						Object value = iMap.get(key);
						result.append(String.format("  -> '%s', '%s'%n", 
								this.objectMapper.writeValueAsString(key),
								this.objectMapper.writeValueAsString(value)
								));
					}
				} else {
					result.append(String.format("Object '%s', class '%s'%n", 
							distributedObject.getName(), distributedObject.getClass().getName()));
				}
			}
		}

		result.append(String.format("[%d object%s]%n", count, (count==1 ? "" : "s")));
		result.append(String.format("=============================================%n"));
	
		return result.toString();
	}

}
