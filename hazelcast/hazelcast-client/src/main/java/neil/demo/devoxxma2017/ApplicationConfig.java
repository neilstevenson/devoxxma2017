package neil.demo.devoxxma2017;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

/**
 * <p>Additional Spring beans to configure the application.
 * </p>
 */
@Configuration
public class ApplicationConfig {

	/**
	 * <p>Create IMDG client configuration from a file.
	 * </p>
	 */
    @Bean
    public ClientConfig clientConfig() throws Exception {
            return new XmlClientConfigBuilder("hazelcast-client.xml").build();
    }

    /**
     * <p>Create an IMDG client instance using the IMDG client connection
     * configuration.
     * </p>
     * <p>The client listens for events on the server to display a speed
     * panel, for entries being updated in the speed map.
     * 
     * 
     * @param clientConfig The {@code @Bean} created above
     * @return
     */
	@Bean
	public HazelcastInstance hazelcastInstance(ClientConfig clientConfig) {
		boolean INCLUDE_VALUE = true;
		
		HazelcastInstance hazelcastInstance = HazelcastClient.newHazelcastClient(clientConfig);
		
		IMap<?, ?> speedMap = hazelcastInstance.getMap(Constants.IMAP_NAME_SPEED);
		speedMap.addEntryListener(new SpeedPanelListener(), INCLUDE_VALUE);
		
		return hazelcastInstance;
	}
	
}
