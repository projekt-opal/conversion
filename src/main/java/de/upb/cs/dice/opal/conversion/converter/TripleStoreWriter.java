package de.upb.cs.dice.opal.conversion.converter;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@EnableScheduling
public class TripleStoreWriter {

    private static final Logger logger = LoggerFactory.getLogger(TripleStoreWriter.class);

    private ExecutorService executorService = Executors.newCachedThreadPool();

    @Value("${info.outputFolder}")
    private String outputFolder;

    private AtomicInteger cnt = new AtomicInteger(0);

    private Queue<Model> queue = new ConcurrentLinkedQueue<>();

    public void write(Model model) {
        logger.trace("adding a model to queue");
        queue.add(model);
    }

    @Scheduled(fixedDelay = 10000)
    public void intervalWrite() {
        int size = queue.size();
        logger.debug("intervalWrite, {}", size);
        if (queue.size() > 0) {
            int len = queue.size();
            final Model batchModel = ModelFactory.createDefaultModel();
            while (len-- > 0) {
                Model model = queue.poll();
                batchModel.add(model);
            }
            Runnable runnable = () -> writeToFile(batchModel, cnt.getAndAdd(1));
            executorService.submit(runnable);
        }
        logger.debug("finished intervalWrite, {}", size);

    }

    private void writeToFile(Model batchModel, int cnt) {
        try(FileOutputStream fileOutputStream =
                    new FileOutputStream(String.format("%s/model_%d.ttl", outputFolder, cnt))) {
            batchModel.write(fileOutputStream, "TURTLE");
        } catch (Exception e) {
            logger.error("An error occurred in writing to file, {}", e);
        }
    }

}
