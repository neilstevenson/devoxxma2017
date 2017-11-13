package neil.demo.devoxxma2017;

/**
 * <p>Constants to share across the applications.
 * </p>
 */
public class Constants {

	// GPX file names, in src/main/resources
	public static final String[] FILE_NAMES_GPX = { "fuad", "neil" };

	// Hazelcast
	public static final String COMMAND_NOUN_KAFKA = "kafka";
	public static final String COMMAND_NOUN_SPEEDO = "speedo";
	public static final String COMMAND_NOUN_WORDCOUNT = "wordcount";
	public static final String COMMAND_VERB_START = "start";
	public static final String COMMAND_VERB_STOP = "stop";
	public static final String IMAP_NAME_COMMAND = "command";
	public static final String IMAP_NAME_HAMLET = "hamlet";
	public static final String IMAP_NAME_POSITION = "position";
	public static final String IMAP_NAME_SPEED = "speed";
	public static final String IMAP_NAME_WORDS = "words";
	
	public static final String[] IMAP_NAMES = {
		IMAP_NAME_COMMAND, IMAP_NAME_HAMLET, IMAP_NAME_POSITION, IMAP_NAME_SPEED, IMAP_NAME_WORDS
	};
	

	// JFreechart
	public static final String SPEEDO_PANEL_TITLE = "Speedometer";
	public static final String SPEEDO_PANEL_X_AXIS = "Time";
	public static final String SPEEDO_PANEL_Y_AXIS = "Speed (m/s)";

	// Kafka, partition count must match "print-topic.sh" script. Would be better to derive
	public static final int TOPIC_NAME_GPX_PARTITION_COUNT = 3;
	public static final String TOPIC_NAME_GPX = "gpx";

	// Zookeeper, if used for Hazelcast discovery from Docker
	public static final String ZOOKEEPER_HAZELCAST_PATH = "/discovery/hazelcast";
	public static final String ZOOKEEPER_IP_PROPERTY_NAME = "zkip";

}
