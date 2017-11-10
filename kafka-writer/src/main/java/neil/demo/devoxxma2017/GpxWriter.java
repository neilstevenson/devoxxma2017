package neil.demo.devoxxma2017;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

import neil.demo.devoxxma2017.Gpx.TrkPt;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * <p>This class is a {@link org.springframework.boot.CommandLineRunner CommandLineRunner}
 * so Spring will invoke the {@link #run()} method once everything is set-up.
 * </p>
 */
@Component
@Slf4j
public class GpxWriter implements CommandLineRunner {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private Jaxb2Marshaller jaxb2Marshaller;
    @Autowired
    private KafkaTemplate<String, TrkPt> kafkaTemplate;

    /**
     * <p>Interleave the track points and write them to the Kafka topic.
     * So, point 0 from file A, point 0 from file B, point 1 from file A, point
     * 1 from file B, point 2 from file A, point 2 from file B, and so on.
     * <p>
     * <p>The partition count for the topic is expressed as a constant.
     * It would be better to obtain the partition count from the topic.
     * </p>
     * 
     * @param arg0 From command line, ignored
     */
    @Override
    public void run(String... arg0) throws Exception {
    			AtomicLong onFailureCount = new AtomicLong(0);
    			AtomicLong[] onSuccessCount = new AtomicLong[Constants.TOPIC_NAME_GPX_PARTITION_COUNT];
    			for (int i=0 ; i < Constants.TOPIC_NAME_GPX_PARTITION_COUNT ; i++) {
    				onSuccessCount[i] = new AtomicLong(0);
    			}
    			
    			Map<String, List<TrkPt>> testData = this.loadTestData();
                 
            // Find info on list of tracking points
            int max = 0;
            int total = 0;
            for (List<TrkPt> list : testData.values()) {
            		if (list.size() > max) {
            			max = list.size();
            		}
            		total += list.size(); 
            }
            
            // Wait for all callbacks
			CountDownLatch countDownLatch = new CountDownLatch(total);
			
            for (int i = 0 ; i < max ; i++) {
            	
            		// Space out the writes so 3600 take 30 seconds 
            		TimeUnit.MILLISECONDS.sleep(5L);
            	
            		for (String key : testData.keySet()) {
            			List<TrkPt> value = testData.get(key);
            			if (value.size() > i) {
            				TrkPt trkPt = value.get(i);
            		
            				int partition = key.hashCode() % Constants.TOPIC_NAME_GPX_PARTITION_COUNT;
            				
            				ListenableFuture<SendResult<String, TrkPt>> sendResult =
                                this.kafkaTemplate.sendDefault(partition, key, trkPt);
            				
            				sendResult.addCallback(
                                new ListenableFutureCallback<SendResult<String, TrkPt>>() {
                                       @Override
                                       public void onSuccess(SendResult<String, TrkPt> sendResult) {
                                                ProducerRecord<String, TrkPt> producerRecord = sendResult.getProducerRecord();
                                                RecordMetadata recordMetadata = sendResult.getRecordMetadata();
                                                log.info("onSuccess(), offset {} partition {} timestamp {} for '{}'",
                                                recordMetadata.offset(), recordMetadata.partition(),
                                                recordMetadata.timestamp(), producerRecord.value());   
                                                onSuccessCount[recordMetadata.partition()].incrementAndGet();
                                                countDownLatch.countDown();
                                       }

                                       @Override
                                       public void onFailure(Throwable t) {
                                               log.error("onFailure()", t);
                                               onFailureCount.incrementAndGet();
                                               countDownLatch.countDown();
                                       }
                                }
                                );
            				
            			}
            		}
            }

            // Await callbacks
            countDownLatch.await();
            
            if (onFailureCount.get() > 0) {
            		throw new RuntimeException(onFailureCount.get() + " failures writing to Kafka");
            } else {
            		for (int i = 0; i < Constants.TOPIC_NAME_GPX_PARTITION_COUNT ; i++) {
            			log.info("Wrote {} tracking points to partition {}", onSuccessCount[i].get(), i);
            		}
            		log.info("=> Total written successfully {}", total);
            }
    }

    /**
     * <p>Read the file contents into memory.
     * </p>
     * <p>For filename "{@code hello}" we expect to find "{@code hello.gpx}".
     * Test data files are stored in the <i>common</i> module in
     * the {@code src/main/resources} folder, and the build embeds them
     * in the jar file produced.
     * </p>
     * 
     * @return One list of points per file successfully read
     */
	private Map<String, List<TrkPt>> loadTestData() {
		
		Map<String, List<TrkPt>> result = new TreeMap<>();
		
		for (String fileName : Constants.FILE_NAMES_GPX) {
			
			String resourceName = fileName + ".gpx";
			
			try {
				Resource resource = this.applicationContext.getResource("classpath:" + resourceName);
				
				try (InputStream inputStream = resource.getInputStream();) {

	                StreamSource streamSource = new StreamSource(inputStream);
	                Gpx gpx = (Gpx) this.jaxb2Marshaller.unmarshal(streamSource);
	                
	                List<TrkPt> trkPts = gpx.getTrk().getTrkseg().getTrkpt();
					
	                result.put(fileName, trkPts);
	                log.info("Read {} points from '{}'", trkPts.size(), resource.getURL().toString());
				}
				
			} catch (Exception e) {
				log.error("Problem reading '" + resourceName + "'", e);
			}
			
		}
		
		return result;
	}
}
