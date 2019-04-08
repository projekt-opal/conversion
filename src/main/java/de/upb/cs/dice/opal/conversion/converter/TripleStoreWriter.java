package de.upb.cs.dice.opal.conversion.converter;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@EnableRetry
public class TripleStoreWriter implements CredentialsProvider {

    private static final Logger logger = LoggerFactory.getLogger(TripleStoreWriter.class);

    @Value("${civetTripleStore.url}")
    private String tripleStoreURL;

    org.apache.http.impl.client.CloseableHttpClient client;


    public TripleStoreWriter() {
        this.credentials = new UsernamePasswordCredentials("dba", "dba");
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setDefaultCredentialsProvider(this);
        client = clientBuilder.build();
    }

    @JmsListener(destination = "writerQueue", containerFactory = "messageFactory")
    @SendTo("ckanQueue")
    public byte[] writeToTripleStore(byte[] bytes) {

        // TODO: 12.12.18 find better way to get toString of RdfNodes
        if (bytes.length == 0) return new byte[0];

        Model model = RDFUtility.deserialize(bytes);
        StmtIterator stmtIterator = model.listStatements();
        QuerySolutionMap mp = new QuerySolutionMap();
        int cnt = 0;
        StringBuilder triples = new StringBuilder();
        while (stmtIterator.hasNext()) {
            if (cnt > 20) {
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

        if (runWriteQuery(triples, mp))
            return bytes;
        return new byte[0];
    }

    private org.apache.http.auth.Credentials credentials;

    private boolean runWriteQuery(StringBuilder triples, QuerySolutionMap mp) {
        try {
            ParameterizedSparqlString pss = new ParameterizedSparqlString("INSERT DATA { GRAPH <http://projekt-opal.de> {" + triples + "} }");
            pss.setParams(mp);

            String query = pss.toString();
            logger.debug("writing query is: {}", query);
            runInsertQuery(query);
            return true;
        } catch (Exception e) {
            logger.error("An error occurred in writing to TripleStore ", e);
        }
        return false;
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200))
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
