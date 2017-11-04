package neil.demo.devoxxma2017;

/**
 * <p>Constants to share across the applications.
 * </p>
 */
public class Constants {

	// GPX file names, in src/main/resources
	public static final String[] FILE_NAMES_GPX = { "fuad", "neil" };

	// Hazelcast
	public static final String IMAP_NAME_COMMAND = "command";

	// Kafka, partition count must match "print-topic.sh" script. Would be better to derive
	public static final int TOPIC_NAME_GPX_PARTITION_COUNT = 3;
	public static final String TOPIC_NAME_GPX = "gpx";
	
}
