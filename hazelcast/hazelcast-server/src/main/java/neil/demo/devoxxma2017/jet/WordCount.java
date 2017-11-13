package neil.demo.devoxxma2017.jet;

import java.util.regex.Pattern;

import com.hazelcast.jet.Pipeline;
import com.hazelcast.jet.Sinks;
import com.hazelcast.jet.Sources;
import com.hazelcast.jet.Traversers;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.function.DistributedFunctions;

import neil.demo.devoxxma2017.Constants;

/**
 * <p>A Jet "<i>Pipeline</i>" is a higher level framework for constructing
 * <b>D</b>istributed <b>A</b>cyclic <b>G</b>raph.
 * </p>
 * <p>Jet compiles the "<i>Pipeline</i>" into the "<i>DAG</i>", so it's
 * more of a convenience if there's no need for fine-grained control of
 * partitioning, edges, etc
 * </p>
 */
public class WordCount {

	private static final Pattern WORDS_PATTERN   = Pattern.compile("\\W+");
	
	/**
	 * <p>The ubiquitous <b>Word Count</b>
	 * <p>Count the words in Hamlet's soliloquoy, "<i>To be, or not to be....</i>"
	 * </p>
	 * <p>Five stages of processing, conceptually from top to bottom:
	 * </p>
	 * <pre>
	 *              +----------+
	 *              |1  IMap   |
	 *              | "hamlet" |
	 *              |  Source  |
	 *              +----------+
	 *                    |
	 *                    |
	 *                    |
	 *              +----------+
	 *              |2 Lines   |
	 *              |  into    |
	 *              |  Words   |
	 *              +----------+
	 *                    |
	 *                    |
	 *                    |
	 *              +----------+
	 *              |3         |
	 *              | Lowercase|
	 *              |          |
	 *              +----------+
	 *                    |
	 *                    |
	 *                    |
	 *              +----------+
	 *              |4         |
	 *              | Aggregate|
	 *              |          |
	 *              +----------+
	 *                    |
	 *                    |
	 *                    |
	 *              +----------+
	 *              |5  IMap   |
	 *              |  "words" |
	 *              |   Sink   |
	 *              +----------+
	 * </pre>
	 * <p>The stages are:
	 * </p>
	 * <ol>
	 * <li><b>Step 1</b>
	 * <p>This is a "<i>source</i>" stage. Lines of text are read
	 * from an {@link com.hazelcast.core.IMap IMap} as key-value
	 * pairs in the form key "{@code 0}" value "{@code To be or not to be}"
	 * </p>
	 * <p>Note this read occurs in parallel if there are multiple JVMs
	 * in the cluster. Entry "{@code 0}" may be read by one JVM and
	 * entry "{@code 1}" by another.
	 * </p>
	 * </li>
	 * <li><b>Step 2</b>
	 * <p>This stage takes the entry and ignores the key (the line
	 * number in the original text). The value is a single line
	 * of text. The traverser takes this line and breaks it into words.
	 * </p>
	 * <p>So for each map entry provided to this stage, multiple
	 * strings are output.
	 * </p>
	 * <p>The entry "{@code (0,'To be or not to be')}" as input
	 * results in six outputs : "{@code To}", "{@code be}", "{@code or}",
	 * "{@code not}", "{@code to}", and "{@code be}".
	 * </p>
	 * </li>
	 * <li><b>Step 3</b>
	 * <p>This stage takes the words from step 2 and forces them
	 * into lower case.
	 * </p>
	 * </li>
	 * <li><b>Step 4</b>
	 * <p>This stage aggregates the incoming stream of words by
	 * the whole item (the word) producing the count as a total
	 * accumulated for each input.
	 * </p>
	 * </li>
	 * <li><b>Step 5</b>
	 * <p>This is a "<i>sink</i>" stage. The words and their
	 * accumulated counts are written to a 
	 * {@link com.hazelcast.core.IMap IMap} as key-value pairs
	 * in the form key "{@code word}" value "{@code count}",
	 * so "{@code ('be',5)}".
	 * </p>
	 * </li>
	 * </ol>
	 * 
	 * <p><b>Notes:</b></p>
	 * <p>Although logically this is a sequence, the Jet job will run in
	 * parallel, at least one job instance on each JVM. And, the source
	 * and sink {@code com.hazelcast.core.IMap IMap} structures are
	 * partitioned. Each Jet job instance will start by reading from
	 * it's share of the source {@code com.hazelcast.core.IMap IMap} 
	 * and end by writing to it's share of the sink
	 * {@code com.hazelcast.core.IMap IMap}.
	 * Best performance comes from minimising the JVM to JVM transfer.
	 * </p>
	 * 
	 * @return
	 */
	public static Pipeline build() {

		Pipeline pipeline = Pipeline.create();
		
		pipeline.drawFrom(Sources.<Integer, String>map(Constants.IMAP_NAME_HAMLET))
		.flatMap(entry -> Traversers.traverseArray(WORDS_PATTERN.split(entry.getValue())))
		.map(String::toLowerCase)
		.groupBy(DistributedFunctions.wholeItem(), AggregateOperations.counting())
		.drainTo(Sinks.map(Constants.IMAP_NAME_WORDS));
		
		return pipeline;
	}

}
