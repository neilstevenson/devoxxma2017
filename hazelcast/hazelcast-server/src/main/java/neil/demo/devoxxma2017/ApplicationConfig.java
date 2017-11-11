package neil.demo.devoxxma2017;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JetConfig;

/**
 * <p>Additional Spring beans to configure the application.
 * </p>
 */
@Configuration
public class ApplicationConfig {

	/**
	 * <p>Create IMDG server configuration from a file.
	 * </p>
	 */
	@Bean
	public Config config() {
		return new ClasspathXmlConfig("hazelcast.xml");
	}

	/**
	 * <p>A Jet engine instance embeds an IMDG instance to provide
	 * it's infrastructure -- discovery etc. It will usually also
	 * use this IMDG instance as a data source or sink, but if
	 * you'd prefer you can use external IMDG instances for this.
	 * </p>
	 * <p>Provide Jet with IMDG configuration for the IMDG that
	 * Jet instance includes.
	 * </p>
	 * 
	 * @param config IMDG config created above
	 */
	@Bean
	public JetInstance jetInstance(Config config) {
		JetConfig jetConfig = new JetConfig().setHazelcastConfig(config);

		return Jet.newJetInstance(jetConfig);
	}

	/**
	 * <p>Expose the IMDG instance embedded in the Jet instance
	 * as a separate {@code @Bean}.
	 * </p>
	 * <p>Add a listener to react to commands written to the
	 * "command" map.
	 * </p>
	 * 
	 * @param commandListener An instance of {@link CommandListener}
	 * @param jetInstance Jet server containing IMDG server created above
	 */
	@Bean
	public HazelcastInstance hazelcastInstance(CommandListener commandListener, JetInstance jetInstance) {
		HazelcastInstance hazelcastInstance = jetInstance.getHazelcastInstance();

		// React to map changes
		IMap<?, ?> commandMap = hazelcastInstance.getMap(Constants.IMAP_NAME_COMMAND);
        commandMap.addLocalEntryListener(commandListener);		

		// Add in, if we want to trace map changes
		//IMap<?, ?> positionMap = hazelcastInstance.getMap(Constants.IMAP_NAME_POSITION);
		//positionMap.addLocalEntryListener(new LoggingListener());        
        
        return hazelcastInstance;
	}
}
