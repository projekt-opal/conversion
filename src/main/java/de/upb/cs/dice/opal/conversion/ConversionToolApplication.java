package de.upb.cs.dice.opal.conversion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

@SpringBootApplication
@EnableScheduling
public class ConversionToolApplication /*implements CommandLineRunner*/ {

    public static void main(String[] args) {
        SpringApplication.run(ConversionToolApplication.class, args);
    }

//    @Autowired
//    private JmsTemplate jmsTemplate;

//    public void test() {
//        Model model = RDFDataMgr.loadModel("/home/afshin/Desktop/b.ttl", Lang.TURTLE);
//        byte[] message = RDFUtility.serialize(model);
//        jmsTemplate.convertAndSend("conversionQueue", message);
//    }

    @Bean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler(); //single threaded by default
    }

//    @Override
//    public void run(String... args) throws Exception {
//        test();
//    }
}
