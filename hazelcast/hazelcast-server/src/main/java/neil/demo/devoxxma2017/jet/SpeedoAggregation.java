package neil.demo.devoxxma2017.jet;

import java.io.Serializable;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;

import com.hazelcast.jet.datamodel.TimestampedEntry;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import neil.demo.devoxxma2017.Speed;
import neil.demo.devoxxma2017.Gpx.TrkPt;

/**
 * <p>This class implements a <i>reduction</i>,
 * reducing an input collection to an output singleton.
 * </p>
 * <p>Specifically here, the input is a series of
 * tracking points for a key within a time window.
 * {@code Jet} looks after providing the necessary
 * points. The code collates these points, and
 * when signalled produces the output value.
 * </p>
 * <p>What we are capturing as input are GPS points.
 * What we output is a speed.
 * </p>
 * <p>The speed is calculated from the distance from
 * the first point to the last point. We msotly
 * ignore the intermediate points, but we count how
 * many there are as this gives a vague indication
 * as to accuracy, the more points in the window the
 * better.
 * </p>
 * <p>There are three methods:
 * </p>
 * <ul>
 * <li><p>{@link #accumulate()} Captures provided points
 * </p>
 * </li>
 * <li><p>{@link #combine()} Merges points into this object instance
 * produced by another object instance, necessary as processing can
 * be parallel across the grid.
 * </p>
 * </li>
 * <li><p>{@link #finish()} Calculates the resulting speed.
 * </p>
 * </li>
 * </ul>
 * <p><b>Note:</b> The calculation of the speed in {@link #finish()}
 * is very poor, taking no account of rounding error, curvature
 * of the Earth or crossing east/west.
 * </p>
 */
@Data
@Slf4j
@SuppressWarnings("serial")
public class SpeedoAggregation implements Serializable {
	private static final int RADIUS_OF_THE_EARTH_M = 6_371_000;

	private String key = null;
	private TrkPt first = null;
	private TrkPt last = null;
	private int points = 0;

	/**
	 * <p>Capture a tracking point in the current time window. This may
	 * be the first, and may be the only point in a time window.
	 * </p>
	 * 
	 * @param input A point in the current window
	 * @return
	 */
	public SpeedoAggregation accumulate(TimestampedEntry<String, TrkPt> input) {

		if (this.key==null) {
			// Initial point
			this.key = input.getKey();
			this.points = 1;
			this.first = this.last = input.getValue();
		} else {
			// Augmenting point, possibly change earliest or latest stashed
			this.points+=1;
			if (this.first.getDate().getTime() > input.getValue().getDate().getTime()) {
				this.first = input.getValue();
			}
			if (this.last.getDate().getTime() < input.getValue().getDate().getTime()) {
				this.last = input.getValue();
			}
		}
		
		return this;
	}

	/**
	 * <p>Combine the points from another aggregator instance for the
	 * same person, prior to calculating the speed.
	 * </p>
	 * 
	 * @param that Another aggregator instance, perhaps from another JVM
	 * @return
	 */
	public SpeedoAggregation combine(SpeedoAggregation that) {
		this.points += that.getPoints();
		
		if (this.key==null) {
			// Merge destination empty
			this.key = that.getKey();
			this.first = that.getFirst();
			this.last = that.getLast();
		} else {
			if (that.key!=null) {
				if (this.first.getDate().getTime() > that.getFirst().getDate().getTime()) {
					this.first = that.getFirst();
				}
				if (this.last.getDate().getTime() > that.getLast().getDate().getTime()) {
					this.last = that.getLast();
				}
			}
		}
		
		return this;
	}

	/**
	 * <p>Create a map entry holding the speed for the
	 * person involved, calculated from the first and
	 * last locations captured.
	 * </p>
	 * <p>If there is no first and last location, or these
	 * are the same, then the speed is zero. This could happen
	 * if there is insufficient data in the window because the
	 * window size is too small (a configuration error) or
	 * some GPS points didn't arrive (something to expect).
	 * </p>
	 * <p>The speed calculation is flawed in at least a few
	 * minor ways, which don't particularly matter for the example
	 * but are worth noting:
	 * </p>
	 * <ul>
	 * <li><b>Rounding</b>
	 * <p>Java {@code double} is used for the calculation which has
	 * rounding errors. {@code BigDecimal} would be more accurate.
	 * </p>
	 * </li>
	 * <li><b>Elevation</b>
	 * <p>Changes in height are ignored, the distance is calculated as if on
	 * a flat surface.
	 * </p>
	 * </li>
	 * <li><b>Haversine</b>
	 * <p>The speed itself is calculated using
	 * <a href="https://en.wikipedia.org/wiki/Haversine_formula">The Haversine Formula</a>
	 * from points on the surface of a sphere. The Earth is not truly round, it bulges
	 * more at the Equator and less at the poles.
	 * </li>
	 * </ul>
	 *
	 * @return An entry for the "{@code speed}" map, speed in metres per second
	 */
	public Map.Entry<String, Speed> finish() {
		Speed value = new Speed();
		
		if (this.points > 1) {
			value.setTime(this.last.getDate().getTime());
			
			try {
				// Speed needs movement
				if ((this.last.getLatitude() != this.first.getLatitude())
					|| (this.last.getLongitude() != this.first.getLongitude())) {
					
			        double lat1 = Math.toRadians(this.last.getLatitude());
			        double lat2 = Math.toRadians(this.first.getLatitude());
			        double long1 = Math.toRadians(this.last.getLongitude());
			        double long2 = Math.toRadians(this.first.getLongitude());

			        double latDiff = lat1 - lat2;
			        double longDiff = long1 - long2;

			        double distance = Math.pow(Math.sin(latDiff / 2), 2)
			                + Math.pow(Math.sin(longDiff / 2), 2)
			                * Math.cos(lat1)
			                * Math.cos(lat2);

			        double metres = 2 * RADIUS_OF_THE_EARTH_M * Math.asin(Math.sqrt(distance));			        
			        
			        double seconds = (this.last.getDate().getTime() - this.first.getDate().getTime()) / 1000;

					value.setMetresPerSecond(metres / seconds);
				}
			} catch (Exception e) {
				log.error("finish()", e);
			}
		}

		return new SimpleImmutableEntry<>(this.key, value);
	}
}