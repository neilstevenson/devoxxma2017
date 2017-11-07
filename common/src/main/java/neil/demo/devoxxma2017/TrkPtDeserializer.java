package neil.demo.devoxxma2017;

import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import neil.demo.devoxxma2017.Gpx.TrkPt;

/**
 * <p>Deserializer for the {@link Gps.Trkpt} class.
 * </p>
 */
public class TrkPtDeserializer implements Deserializer<TrkPt> {

	private ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void close() {
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void configure(Map arg0, boolean arg1) {
	}

	@Override
	public TrkPt deserialize(String topic, byte[] bytes) {
		TrkPt trkPt = null;
		try {
			trkPt = this.objectMapper.readValue(bytes, TrkPt.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return trkPt;
	}

}