package de.upb.cs.dice.opal.conversion.converter;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Component
@EnableScheduling
public class FileDumper {

    private static final Logger logger = LoggerFactory.getLogger(FileDumper.class);

    private ArrayBlockingQueue<Model> queue = new ArrayBlockingQueue<>(200000);
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private static AtomicLong counter = new AtomicLong();

    @Value("${info.outputFolder}")
    private String outputPath;

    @JmsListener(destination = "fileQueue", containerFactory = "messageFactory")
    public void dump(byte[] bytes) {
        try {
            if (bytes.length == 0) return;
            Model model = RDFUtility.deserialize(bytes);
//            while (queue.remainingCapacity() == 0) {}
            queue.add(model);
        } catch (Exception e) {
            logger.error("An error occurred in dumping model", e);
        }
    }

    @Scheduled(fixedDelay = 100000)
    public void intervalWrite() {
        int size = queue.size();
        logger.debug("intervalWrite, {}", size);
        if (size > 0) {
            int len = size;
            final Model batchModel = ModelFactory.createDefaultModel();
            while (len-- > 0) {
                Model model = queue.poll();
                batchModel.add(model);
            }
            String fileName = "model" + counter.incrementAndGet() + ".ttl";
            Runnable runnable = () -> writeToFile(batchModel, fileName);
            executorService.submit(runnable);
        }
        logger.debug("finished intervalWrite, {}", size);
    }

    private void writeToFile(Model model, String fileName) {
        try (FileOutputStream outputStream = new FileOutputStream(outputPath + "/" + fileName)) {
            model.write(outputStream, "TURTLE");
        } catch (IOException e) {
            logger.error("An error occurred in dumping model to file ", fileName, e);
        }
    }

}
