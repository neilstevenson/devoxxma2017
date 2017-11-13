package neil.demo.devoxxma2017;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Inject some test data into the cluster if none already
 * exists, so only the first server in the cluster does this
 * activity.
 * </p>
 */
@Component
@Order(Integer.MIN_VALUE)
@Slf4j
public class ApplicationInitializer implements CommandLineRunner {

	@Autowired
	private HazelcastInstance hazelcastInstance;

	@Override
	public void run(String... arg0) throws Exception {
		
		// Force create all maps
		for (String mapName : Constants.IMAP_NAMES) {
			this.hazelcastInstance.getMap(mapName);
		}

		IMap<Integer, String> hamletMap = this.hazelcastInstance.getMap(Constants.IMAP_NAME_HAMLET);

		if (!hamletMap.isEmpty()) {
			log.info("Skip loading '{}', not empty", hamletMap.getName());
		} else {

			AtomicInteger count = new AtomicInteger(0);

			Arrays.stream(TestData.HAMLET).forEach((Object[] datum) -> {
				String line = (String) datum[0];

				hamletMap.set(count.incrementAndGet(), line);
			});

			log.info("Loaded {} into '{}'", count, hamletMap.getName());
		}

	}

}