package neil.demo.devoxxma2017;

import java.util.Map.Entry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.spi.properties.GroupProperty;
import com.hazelcast.zookeeper.ZookeeperDiscoveryProperties;
import com.hazelcast.zookeeper.ZookeeperDiscoveryStrategyFactory;

/**
 * <p>
 * Additional Spring beans to configure the application.
 * </p>
 */
@Configuration
public class ApplicationConfig {

	/**
	 * <p>
	 * Create IMDG server configuration from a file.
	 * </p>
	 * 
	 * @param environment
	 *            From Spring
	 */
	@Bean
	public Config config(Environment environment) {
		Config config = new ClasspathXmlConfig("hazelcast.xml");

		/*
		 * If in Docker, turn off TCP in favour of on Zookeeper discovery.
		 * Mostly preset except Zookeeper IP. System.out is better for Docker explorer
		 */
		String zookeeper = environment.getProperty(Constants.ZOOKEEPER_IP_PROPERTY_NAME);
		System.out.println(Constants.ZOOKEEPER_IP_PROPERTY_NAME + "==" + zookeeper);
		if (zookeeper!=null) {
			config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);

			config.setProperty(GroupProperty.DISCOVERY_SPI_ENABLED.toString(), "true");

			DiscoveryStrategyConfig discoveryStrategyConfig = new DiscoveryStrategyConfig(
					new ZookeeperDiscoveryStrategyFactory());
			discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.ZOOKEEPER_URL.key(),
					zookeeper + ":2181");
			discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.ZOOKEEPER_PATH.key(),
					Constants.ZOOKEEPER_HAZELCAST_PATH);
			discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.GROUP.key(), config.getGroupConfig().getName());

			for (Entry<String, ?> entry: discoveryStrategyConfig.getProperties().entrySet()) {
				System.out.printf("Discovery property '%s'=='%s'%n", entry.getKey(), entry.getValue());
			}
			
			config.getNetworkConfig().getJoin().getDiscoveryConfig()
					.addDiscoveryStrategyConfig(discoveryStrategyConfig);
			
			// For proof, Management Centre on same host as Zookeeper
			config.getManagementCenterConfig().setEnabled(true).setUrl("http://" + zookeeper + ":8080/mancenter");
		} else {
			config.getManagementCenterConfig().setEnabled(true).setUrl("http://127.0.0.1:8080/mancenter");
		}

		return config;
	}

	/**
	 * <p>
	 * A Jet engine instance embeds an IMDG instance to provide it's infrastructure
	 * -- discovery etc. It will usually also use this IMDG instance as a data
	 * source or sink, but if you'd prefer you can use external IMDG instances for
	 * this.
	 * </p>
	 * <p>
	 * Provide Jet with IMDG configuration for the IMDG that Jet instance includes.
	 * </p>
	 * 
	 * @param config
	 *            IMDG config created above
	 */
	@Bean
	public JetInstance jetInstance(Config config) {
		JetConfig jetConfig = new JetConfig().setHazelcastConfig(config);

		return Jet.newJetInstance(jetConfig);
	}

	/**
	 * <p>
	 * Expose the IMDG instance embedded in the Jet instance as a separate
	 * {@code @Bean}.
	 * </p>
	 * <p>
	 * Add a listener to react to commands written to the "command" map.
	 * </p>
	 * 
	 * @param commandListener
	 *            An instance of {@link CommandListener}
	 * @param jetInstance
	 *            Jet server containing IMDG server created above
	 */
	@Bean
	public HazelcastInstance hazelcastInstance(CommandListener commandListener, JetInstance jetInstance) {
		HazelcastInstance hazelcastInstance = jetInstance.getHazelcastInstance();

		// React to map changes
		IMap<?, ?> commandMap = hazelcastInstance.getMap(Constants.IMAP_NAME_COMMAND);
		commandMap.addLocalEntryListener(commandListener);

		// Add in, if we want to trace map changes
		// IMap<?, ?> positionMap =
		// hazelcastInstance.getMap(Constants.IMAP_NAME_POSITION);
		// positionMap.addLocalEntryListener(new LoggingListener());

		return hazelcastInstance;
	}
}
