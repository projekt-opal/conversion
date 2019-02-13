package de.upb.cs.dice.opal.conversion.converter;

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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@EnableScheduling
@EnableRetry
public class TripleStoreWriter {

    private static final Logger logger = LoggerFactory.getLogger(TripleStoreWriter.class);

    private ExecutorService executorService = Executors.newCachedThreadPool();

    @Value("${civetTripleStore.url}")
    private String tripleStoreURL;

    private Queue<Model> queue = new ConcurrentLinkedQueue<>();

    public void write(Model model) {
        logger.trace("adding a model to queue");
        queue.add(model);
    }

    // TODO: 19.12.18 Remove scheduling because 50Triples per write is the best option

    @Scheduled(fixedDelay = 1000)
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
            Runnable runnable = () -> writeToTripleStore(batchModel);
            executorService.submit(runnable);
        }
        logger.debug("finished intervalWrite, {}", size);

    }

    private void writeToTripleStore(Model model) {

        // TODO: 12.12.18 find better way to get toString of RdfNodes

        StmtIterator stmtIterator = model.listStatements();
        QuerySolutionMap mp = new QuerySolutionMap();
        int cnt = 0;
        StringBuilder triples = new StringBuilder();
        while (stmtIterator.hasNext()) {
            if (cnt > 50) {
                runWriteQuery(triples, mp);
                triples = new StringBuilder();
                mp = new QuerySolutionMap();
                cnt = 0;
            }
            Statement statement = stmtIterator.nextStatement();

            String s = "?s" + cnt;
            String p = "?p" + cnt;
            String o = "?o" + cnt;

            cnt++;

            mp.add(s, statement.getSubject());
            mp.add(p, statement.getPredicate());
            mp.add(o, statement.getObject());

            triples
                    .append(s).append(' ')
                    .append(p).append(' ')
                    .append(o).append(" . ");
        }

        runWriteQuery(triples, mp);
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2))
    private void runWriteQuery(StringBuilder triples, QuerySolutionMap mp) {
        try {

            ParameterizedSparqlString pss = new ParameterizedSparqlString("INSERT DATA { graph <http://projekt-opal.de> {" + triples + "}}");
            pss.setParams(mp);


            String query = pss.toString();
            logger.debug("writing query is: {}", query);
            UpdateRequest request = UpdateFactory.create(query);
            UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, tripleStoreURL);
            try {
                proc.execute();
            } catch (Exception e) {
                logger.error("An error occurred in writing to TripleStore ", e);
            }
        } catch (Exception e) {
            logger.error("An error occurred in writing to TripleStore ", e);
        }
    }

}
