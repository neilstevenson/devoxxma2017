package neil.demo.devoxxma2017;

import java.util.Collections;
import java.util.Map.Entry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.spi.properties.GroupProperty;
import com.hazelcast.zookeeper.ZookeeperDiscoveryProperties;
import com.hazelcast.zookeeper.ZookeeperDiscoveryStrategyFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Additional Spring beans to configure the application.
 * </p>
 */
@Configuration
@Slf4j
public class ApplicationConfig {

	/**
	 * <p>Create IMDG client configuration from a file.
	 * </p>
	 */
    @Bean
    public ClientConfig clientConfig(Environment environment) throws Exception {
            ClientConfig clientConfig = new XmlClientConfigBuilder("hazelcast-client.xml").build();

    		/*
    		 * If in Docker, turn off TCP in favour of on Zookeeper discovery.
    		 * Mostly preset except Zookeeper IP
    		 */
    		String zookeeper = environment.getProperty(Constants.ZOOKEEPER_IP_PROPERTY_NAME);
    		if (zookeeper!=null) {
    			clientConfig.getNetworkConfig().setAddresses(Collections.emptyList());

    			clientConfig.setProperty(GroupProperty.DISCOVERY_SPI_ENABLED.toString(), "true");

    			DiscoveryStrategyConfig discoveryStrategyConfig = new DiscoveryStrategyConfig(
    					new ZookeeperDiscoveryStrategyFactory());
    			discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.ZOOKEEPER_URL.key(),
    					zookeeper + ":2181");
    			discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.ZOOKEEPER_PATH.key(),
    					Constants.ZOOKEEPER_HAZELCAST_PATH);
    			discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.GROUP.key(), clientConfig.getGroupConfig().getName());

    			for (Entry<String, ?> entry: discoveryStrategyConfig.getProperties().entrySet()) {
    				log.info("Discovery property '{}'=='{}'", entry.getKey(), entry.getValue());
    			}
    			
    			clientConfig.getNetworkConfig().getDiscoveryConfig().addDiscoveryStrategyConfig(discoveryStrategyConfig);
    		}
    		
    		return clientConfig;
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
