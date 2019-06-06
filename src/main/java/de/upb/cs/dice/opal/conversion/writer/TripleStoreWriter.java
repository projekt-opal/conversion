package de.upb.cs.dice.opal.conversion.writer;

import de.upb.cs.dice.opal.conversion.utility.RDFUtility;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@EnableScheduling
@EnableRetry
public class TripleStoreWriter implements CredentialsProvider {

    private static final Logger logger = LoggerFactory.getLogger(TripleStoreWriter.class);


    @Value("${info.opal.tripleStore.url}")
    private String tripleStoreURL;
    @Value("${info.opal.tripleStore.username}")
    private String tripleStoreUsername;
    @Value("${info.opal.tripleStore.password}")
    private String tripleStorePassword;

    private org.apache.http.impl.client.CloseableHttpClient client;
    private org.apache.http.auth.Credentials credentials;

    private ExecutorService executorService = Executors.newFixedThreadPool(20);

    @PostConstruct
    public void initialize() {
        this.credentials = new UsernamePasswordCredentials(tripleStoreUsername, tripleStorePassword);
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setDefaultCredentialsProvider(this);
        client = clientBuilder.build();
    }

    @RabbitListener(queues = "#{writerQueueTripleStore}")
    public void writeToTripleStore(byte[] bytes) {

        // TODO: 12.12.18 find better way to get toString of RdfNodes
        if (bytes == null) return;
        Runnable runnable = () -> writeModel(bytes);
        executorService.submit(runnable);

    }

    private void writeModel(byte[] bytes) {
        Model model = RDFUtility.deserialize(bytes);
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


    private boolean runWriteQuery(StringBuilder triples, QuerySolutionMap mp) {
        try {
            ParameterizedSparqlString pss = new ParameterizedSparqlString("INSERT DATA { GRAPH <http://projekt-opal.de> {" + triples + "} }");
            pss.setParams(mp);

            String query = pss.toString();
            query = new String(query.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8); // TODO: 17.04.19 check to make sure that it is OK
            logger.debug("writing query is: {}", query);
            runInsertQuery(query);
            return true;
        } catch (Exception e) {
            logger.error("An error occurred in writing to TripleStore ", e);
        }
        return false;
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    private void runInsertQuery(String query) {
        UpdateRequest request = UpdateFactory.create(query);
        UpdateProcessor proc = UpdateExecutionFactory.createRemoteForm(request, tripleStoreURL, client);
        proc.execute();
    }

    @Override
    public void setCredentials(AuthScope authScope, Credentials credentials) {

    }

    @Override
    public Credentials getCredentials(AuthScope authScope) {
        return credentials;
    }

    @Override
    public void clear() {

    }
}
