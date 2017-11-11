package neil.demo.devoxxma2017.jet;

import java.util.Properties;
import java.util.UUID;

import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.core.Edge;
import com.hazelcast.jet.core.Vertex;
import com.hazelcast.jet.core.processor.DiagnosticProcessors;
import com.hazelcast.jet.core.processor.KafkaProcessors;
import com.hazelcast.jet.core.processor.SinkProcessors;

import neil.demo.devoxxma2017.Constants;
import neil.demo.devoxxma2017.TrkPtDeserializer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * <p>A Jet "<i>DAG</i>" is a <b>D</b>istributed <b>A</b>cyclic <b>G</b>raph,
 * a set of processing nodes (a <i>vertex</i>) and the plumbing (<i>edge</i>)
 * that moves the data through the graph.
 * </p>
 */
public class ReadKafka {

	private static final String PREFIX = ReadKafka.class.getSimpleName() + " ";
	
	/**
	 * <p>Define a three stage processing graph to upload Kafka data
	 * into Hazelcast.
	 * </p>
	 * <p>When executed, at least one instance will run on each
	 * server JVM in the cluster. The number running on each
	 * JVM depends on the number of CPUs or any override.
	 * For example, 3 JVMs in a cluster on 2-CPU machines
	 * mean 6 instances of the graph constituting the processing
	 * job.
	 * </p>
	 * <p>The processing graph looks conceptually like this, from top to bottom:
	 * </p>
	 * <pre>
	 *              +----------+
	 *              |1 Kafka   |
	 *              |  Topic   |
	 *              |  "gpx"   |
	 *              +----------+
	 *             /            \
	 *            0              1
	 *           /                \
	 *     +----------+       +----------+
	 *     |2a IMap   |       |2b  Print |
	 *     |"position"|       |    Out   |
	 *     |   Save   |       |          |
	 *     +----------+       +----------+
	 * </pre>
	 * 
	 * <p>The three stages are:
	 * </p>
	 * <ol>
	 * <li><b>Step 1</b>
	 * <p>Step 1 is a "<i>source</i>", a way to get data into the processing
	 * graph. In this case, it is a Kafka source.
	 * </p>
	 * <p>A Kafka source needs a number of Kafka specific properties. Here
	 * we have pre-determined most of them, such as the topic name and the
	 * data class being read. The Kafka brokers list is provided as a
	 * parameter.
	 * </p>
	 * <p>We don't need to write the Kafka read logic ourselves, as Jet
	 * does this as a predefined type.
	 * <p>
	 * <P>There are two output channels "<i>edges</i>" from step 1,
	 * numbered 0 (the default) and 1. Each item read by Jet is written
	 * one to each edge.
	 * </li>
	 * <li><b>Step 2a</b>
	 * <p>Step 2a is a "<i>sink</i>", a point in the processing graph
	 * where data progresses no further. In this case it is saved
	 * to an {@link com.hazelcast.core.IMap IMap}.
	 * <p>
	 * <p>In the "<i>edge</i>" definition, we see that the first ("<i>0</i>")
	 * output from step 1 is connected to the first ("<i>0</i>") input for
	 * step 2a.</p>
	 * <p>Collectively step 1 and 2a do the work here, data is read from Kafka
	 * by the source stage and saved to Hazelcast by the sink stage.
	 * <p>
	 * </li>
	 * <li><b>Step 2b</b>
	 * <p>Step 2b is also a "<i>sink</i>" stage. The data fed into that stage doesn't
	 * progress any further through the graph, and in this case is just printed
	 * for diagnostic logging.
	 * </p>
	 * <p>In the "<i>edge</i>" definition, we see that the second ("<i>1</i>")
	 * output from step 1 is connected to the first ("<i>0</i>") input for
	 * step 2b.
	 * </p>
	 * </li>
	 * </ol>
	 *
	 * <p><b>Note 1:</b> This is a graph not a sequence. Output from
	 * the Kafka load step 1 goes both to the Map save step 2a and
	 * the logging step 2b. Kafka data does not pass through the
	 * logging stage to the IMap. We could code it this way,
	 * which would make it more of a data pipeline.
	 * </p>
	 * <p>The logger of course contributes nothing to the end
	 * result, so could be removed.
	 * </p>
	 * <p><b>Note 2:</b> This graph runs in parallel as part
	 * of a Jet job on all nodes in the Hazelcast grid. Kafka
	 * is partitioned and Hazelcast is partitioned. Specifically
	 * Kafka is here partitioned into 3 and Hazelcast into 3
	 * also. In general don't expect Kafka and Hazelcast to
	 * have the same number of partitions.
	 * </p>
	 * <p>So the instance of the graph that reads from Kafka
	 * partition 0 might be on one JVM, <i>and</i> sends
	 * data to the writer to the IMap partition 0 which is
	 * on one grid JVM, ideally same same on to save network
	 * transfer. Kafka then is a parallel source going to a
	 * parallel sink of Hazelcast map.
	 * </p>
	 * <p><b>Note 3:</b> We use the "{@code .localParallelism(1)}"
	 * flag so each JVM only takes one of the Kafka partitions
	 * to read from, and this makes it clearer by the log output
	 * that the read is occurring in parallel across the grid.
	 * </p>
	 * <p><b>Note 4:</b> We use "{@code dag.edge(Edge.from(step1,0).to(step2a));}" to
	 * indicate the default first output from step 1 goes into step 2a. As a shorthand
	 * we could code this as "{@code dag.edge(Edge.between(kafkaSource, mapSink));}".
	 * </p>
	 *
	 * @param bootstrapServers Kafka servers list
	 * @return
	 */
	public static DAG build(String bootstrapServers) {

		/* Connection properties for Kafka. There isn't a constant
		 * for "earliest" in 1.0.0.
		 */
		Properties properties = new Properties();
		properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
		properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, TrkPtDeserializer.class.getCanonicalName());
		properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

		/* Create a processing graph
		 */
		DAG dag = new DAG();

		/* Define three steps of processing, see diagram above
		 */
		Vertex step1 = dag.newVertex("kafkaSource",
				KafkaProcessors.streamKafkaP(properties, Constants.TOPIC_NAME_GPX))
				.localParallelism(1)
				;
		
		Vertex step2a = dag.newVertex("mapSink", SinkProcessors.writeMapP(Constants.IMAP_NAME_POSITION));
		Vertex step2b = dag.newVertex("logSink", DiagnosticProcessors.writeLoggerP(o -> new String(PREFIX + o)));
		
		/* Connect the three steps together, not linearly
		 */
		dag.edge(Edge.from(step1,0).to(step2a,0));
        dag.edge(Edge.from(step1,1).to(step2b,0));
		
		return dag;
	}

}
