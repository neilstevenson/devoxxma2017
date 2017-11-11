package neil.demo.devoxxma2017;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p>Run this process using Spring Boot to control
 * the lifecycle.
 * </p>
 */
@SpringBootApplication
public class Application {

	/**
	 * <p>For testing, limit Hazelcast partitioning to 3.
	 * </p>
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.setProperty("hazelcast.partition.count", String.valueOf(Constants.TOPIC_NAME_GPX_PARTITION_COUNT));
		
		SpringApplication.run(Application.class, args);
	}

}
