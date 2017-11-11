package neil.demo.devoxxma2017.jet;

import java.util.Map;

import com.hazelcast.jet.aggregate.AggregateOperation;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.core.Edge;
import com.hazelcast.jet.core.TimestampKind;
import com.hazelcast.jet.core.Vertex;
import com.hazelcast.jet.core.WatermarkEmissionPolicy;
import com.hazelcast.jet.core.WatermarkPolicies;
import com.hazelcast.jet.core.WindowDefinition;
import com.hazelcast.jet.core.processor.Processors;
import com.hazelcast.jet.core.processor.SinkProcessors;
import com.hazelcast.jet.core.processor.SourceProcessors;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import com.hazelcast.jet.function.DistributedFunction;
import com.hazelcast.jet.function.DistributedFunctions;
import com.hazelcast.jet.function.DistributedPredicate;
import com.hazelcast.jet.function.DistributedToLongFunction;
import com.hazelcast.map.journal.EventJournalMapEvent;

import neil.demo.devoxxma2017.Constants;
import neil.demo.devoxxma2017.Gpx.TrkPt;
import neil.demo.devoxxma2017.Speed;

/**
 * <p>A Jet "<i>DAG</i>" is a <b>D</b>istributed <b>A</b>cyclic <b>G</b>raph,
 * a set of processing nodes (a <i>vertex</i>) and the plumbing (<i>edge</i>)
 * that moves the data through the graph.
 * </p>
 */
public class Speedo {

	private static final boolean START_FROM_OLDEST = false;
	private static final DistributedPredicate<EventJournalMapEvent<String, TrkPt>> NO_SELECTION_FILTER = null;
	private static final DistributedFunction<EventJournalMapEvent<String, TrkPt>, ?> NO_PROJECTION_FILTER = null;
    private static final int ONE_MINUTE_IN_MS = 60 * 1000;
    private static final int FIVE_MINUTES_IN_MS = 5 * ONE_MINUTE_IN_MS;

	/**
	 * <p>What this job does is analyse a stream of positions to produce
	 * a stream of speeds. Each speed is essentially calculated from a
	 * pair of timestamped position - the distance between two points
	 * divided by the time between those two measurements.
	 * </p>
	 * <p>The big complication here is although the input stream is
	 * a sequence of points (for each multiple people), these points
	 * come from the outside world across flaky networks. This is
	 * typical for Internet Of Things (IOT) applications, and means
	 * we have to deal with missing records, records out of order
	 * and sometimes very late. 
	 * </p>
	 * <p>The processing graph looks conceptually like this, from top to bottom:
	 * </p>
	 * <pre>
	 *              +----------+
	 *              |1  IMap   |
	 *              |"position"|
	 *              |  Journal |
	 *              +----------+
	 *                    |
	 *                    |
	 *                    |
	 *              +----------+
	 *              |2 Project |
	 *              |  Journal |
	 *              | to Entry |
	 *              +----------+
	 *                    |
	 *                    |
	 *                    |
	 *              +----------+
	 *              |3         |
	 *              | Punctuate|
	 *              |          |
	 *              +----------+
	 *                    |
	 *                    |
	 *                    |
	 *              +----------+
	 *              |4         |
	 *              | Calculate|
	 *              |   Speed  |
	 *              +----------+
	 *                    |
	 *                    |
	 *                    |
	 *              +----------+
	 *              |5 Project |
	 *              |  Entry   |
	 *              | to Value |
	 *              +----------+
	 *                    |
	 *                    |
	 *                    |
	 *              +----------+
	 *              |6 Remove  |
	 *              |   Not    |
	 *              |  Moving  |
	 *              +----------+
	 *                    |
	 *                    |
	 *                    |
	 *              +----------+
	 *              |7  Print  |
	 *              |   Out    |
	 *              |          |
	 *              +----------+
	 *                    |
	 *                    |
	 *                    |
	 *              +----------+
	 *              |8  IMap   |
	 *              |  "speed" |
	 *              |   Sink   |
	 *              +----------+
	 * </pre>
	 * <p>The stages are:
	 * </p>
	 * <ol>
	 * <li><b>Step 1</b>
	 * <p>Step 1 is a "<i>source</i>", it reads from Hazelcast's internal journal of
	 * changes to the "{@code position}" {@link com.hazelcast.core.IMap IMap} and
	 * injects these records into the graph. The records are of type
	 * {@link com.hazelcast.map.journal.EventJournalMapEvent EvenetJournalMapEvent}
	 * as the journal records events on a map ; there is an equivalent for cache
	 * events.
	 * </p>
	 * <p>A "{@code null}" selection and a "{@code null}" projection argument are shown here.
	 * You can omit them, there is a 2 argument version of this source that defaults them
	 * to "{@code null}").
	 * </p>
	 * <p>Ordinarily these would be useful to reduce the number of records passed on to
	 * the next stage or to reduce their size. The sooner you can reduce the data volume
	 * the faster everything runs.
	 * </p>
	 * </li>
	 * <li><b>Step 2</b>
	 * <p>Step 2 takes the output from step 1 as input, reformats each data record from a
	 * {@link com.hazelcast.map.journal.EventJournalMapEvent EvenetJournalMapEvent} to a
	 * {@link com.hazelcast.jet.datamodel.TimestampedEntry TimestampedEntry}
	 * to pass on to step 3. As above, this would be more efficient to build this projection
	 * into step 1 logic but less obvious.
	 * </p>.
	 * <p>A {@link com.hazelcast.jet.datamodel.TimestampedEntry TimestampedEntry} is just
	 * like a {@link java.util.Map.Entry Map.Entry}, it has a key and a value, but also
	 * an accessible timestamp which we use later for windowing.
	 * </p>
	 * </li>
	 * <li><b>Step 3</b>
	 * <p>Step 3 is a "<i>marking</i>" step. Meta-data (timestamps) is inserted periodically,
	 * into the event stream to enable the next stage to determine the next stage to
	 * discard events that don't belong in the window and would perturb the
	 * calculation.
	 * </p>
	 * </li>
	 * <li><b>Step 4</b>
	 * <p>This is a "<i>reduction</i>" stage, a window's worth of data records
	 * is passed into {@link SpeedoAggregation} which produces a single data.
	 * Refer to this class for the details, but it's the core of the business
	 * logic, calculate one speed based on some points.
	 * </p>
	 * <p>A window is a view onto a continuous stream, unlike a batch or micro-batch
	 * which is a discrete block of data. Events that are visible in a window may
	 * be visible in other windows, if the windows are overlapping.
	 * </p>
	 * <p>What goes into this stage is windows of map entries of a certain type
	 * ("{code <String, TrkPt>}") and what comes out is a map entry of a different
	 * time ("{code <String, Entry<String,Speed>>}").
	 * </p>
	 * </li>
	 * <li><b>Step 5</b>
	 * <p>This is another "<i>projection</i>" taking "{code <String, Entry<String,Speed>>}"
	 * from step 4 and putting out "{code Entry<String,Speed>}"
	 * </p>
	 * </li>
	 * <li><b>Step 6</b>
	 * <p>This is a "<i>filter</i>" stage operating on the value part of
	 * the map entry produced by step 5 (a {@link neil.demo.devoxxma2017.Speed Speed}
	 * record). Speed records which aren't based on sufficient tracking points
	 * are discarded.
	 * </p>
	 * </li>
	 * <li><b>Step 6</b>
	 * <p>This is a "<i>processor</i>" stage, where the processing consists of
	 * logging the input and copying it to output.
	 * </p>
	 * <p>This is done here to demonstrate how to write your own. Unlike {@link ReadKafka}
	 * which uses a terminal processor (or "<i>sink</i>") in parallel to another terminal
	 * processor doing the save to the {@link com.hazelcast.core.IMap IMap}, this approach
	 * is an intermediate processor passing output to the terminal processor that saves
	 * to the {@link com.hazelcast.core.IMap IMap}.
	 * </p>
	 * <p>There are advantages and disadvantages to each approach. It's fractionally harder
	 * to remove the intermediate processor from the graph than to do so for an extra
	 * terminal processor. Conversely, this gives sequentiality, we can see the data before
	 * it goes to the {@link com.hazelcast.core.IMap IMap}.
	 * </p>
	 * </li>
	 * <li><b>Step 8</b>
	 * <p>Step 8 is a "<i>sink</i>", the entries produced by step 5 that
	 * haven't been discarded by step 6 are saved to a {@link com.hazelcast.core.IMap IMap}
	 * named "{@code speed}".
	 * </p>
	 * </li>
	 * </ol>
	 * 
	 * @return
	 */
	public static DAG build() {

		/* Create a processing graph
		 */
		DAG dag = new DAG();

		/* Define a time based data window - 5 minutes long and advancing to a new window every
		 * minute. Therefore overlapping, some data in consecutive windows.
		 */
        WindowDefinition windowDefinition = WindowDefinition.slidingWindowDef(FIVE_MINUTES_IN_MS, ONE_MINUTE_IN_MS);

        /* Define an aggregator on a single stream, type <Input, Aggregator, Output>, that takes a series of
         * positions and "aggregates" these together reducing them to a speed.
         */
        AggregateOperation1<TimestampedEntry<String,TrkPt>, SpeedoAggregation, Map.Entry<String, Speed>> speedoAggregation 
        			= AggregateOperation
                .withCreate(SpeedoAggregation::new)
                .andAccumulate(SpeedoAggregation::accumulate)
                .andCombine(SpeedoAggregation::combine)
                .andFinish(SpeedoAggregation::finish);
        
		/* Define the steps of processing, see diagram above
		 */
		Vertex step1 = dag.newVertex("eventJournal", 
				SourceProcessors.streamMapP(Constants.IMAP_NAME_POSITION, NO_SELECTION_FILTER, NO_PROJECTION_FILTER, START_FROM_OLDEST));

        Vertex step2 = dag.newVertex("projection",
                Processors.mapP((EventJournalMapEvent<String,TrkPt> event) 
                		-> new TimestampedEntry<>(event.getNewValue().getDate().getTime(), event.getKey(), event.getNewValue()
                )));

		Vertex step3 = dag.newVertex("punctuation",
                Processors.insertWatermarksP(
                        		(DistributedToLongFunction<TimestampedEntry<String,TrkPt>>) TimestampedEntry::getTimestamp,
                				WatermarkPolicies.withFixedLag(0), 
                             WatermarkEmissionPolicy.emitByFrame(windowDefinition)));
		
        Vertex step4 = dag.newVertex("aggregate",
        		Processors.aggregateToSlidingWindowP(
        				DistributedFunctions.entryKey(),
                		(DistributedToLongFunction<TimestampedEntry<?, ?>>) TimestampedEntry::getTimestamp,
        				TimestampKind.EVENT, 
        				windowDefinition,
        				speedoAggregation)
        		);

        Vertex step5 = dag.newVertex("projection2",
                Processors.mapP((TimestampedEntry<String,Map.Entry<String,Speed>> entry) 
                		-> entry.getValue()
                ));

        Vertex step6 = dag.newVertex("removeStationary",
        		Processors.filterP((Map.Entry<String,Speed> entry) -> entry.getValue().getMetresPerSecond() > 0));

        Vertex step7 = dag.newVertex("logger", SpeedoLogger::new);

        Vertex step8 = dag.newVertex("mapSink", SinkProcessors.writeMapP(Constants.IMAP_NAME_SPEED));

        /* Connect the steps together, in a simple chain, the output of one becomes
		 * the input to the next.
		 */
        dag.edge(Edge.between(step1, step2));
        dag.edge(Edge.between(step2, step3));
		dag.edge(Edge.between(step3, step4));
		dag.edge(Edge.between(step4, step5));
		dag.edge(Edge.between(step5, step6));
		dag.edge(Edge.between(step6, step7));
		dag.edge(Edge.between(step7, step8));
		
		return dag;
	}

}
