package neil.demo.devoxxma2017;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.shell.Bootstrap;

/**
 * <p>Run this process using Spring Shell to control
 * the lifecycle.
 * </p>
 */
@SpringBootApplication
@PropertySource(value="classpath:/application.properties")
public class Application {

	public static void main(String[] args) throws Exception {
        System.setProperty("hazelcast.logging.type", "slf4j");
        System.setProperty("java.awt.headless", "false");
        
        Bootstrap.main(args);
    }

}
