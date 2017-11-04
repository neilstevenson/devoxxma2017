package neil.demo.devoxxma2017;

import java.util.HashMap;

import java.util.Map;

import neil.demo.devoxxma2017.Gpx.TrkPt;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

/**
 * <p>Addition Spring beans to make XML reading and Kafka writing
 * simpler.
 * </p>
 */
@Configuration
public class ApplicationConfig {

    /**
     * <p>Used by {@link GpxWriter} to parse the XML
     * test data files</p>
     *     
     * @return
     */
	@Bean
	public Jaxb2Marshaller jaxb2Marshaller() {
		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		jaxb2Marshaller.setClassesToBeBound(Gpx.class);
		return jaxb2Marshaller;
	}

    /**
     * <p>Used by {@link GpxWriter} to write objects to a Kafka
     * topic, one at a time.
     * </p>
     *     
     * @return
     */
	@Bean
	public KafkaTemplate<String, TrkPt> kafkaTemplate(@Value("${bootstrap-servers}") String bootstrapServers) {
		Map<String, Object> producerConfigs = new HashMap<>();
		
		producerConfigs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		producerConfigs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		producerConfigs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

		ProducerFactory<String, TrkPt> producerFactory = new DefaultKafkaProducerFactory<>(producerConfigs);

		KafkaTemplate<String, TrkPt> kafkaTemplate = new KafkaTemplate<>(producerFactory);
				
		kafkaTemplate.setDefaultTopic(Constants.TOPIC_NAME_GPX);
		
		return kafkaTemplate;
	}
}