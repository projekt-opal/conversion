package de.upb.cs.dice.opal.conversion.converter;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
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
import org.springframework.stereotype.Component;

@Component
public class TripleStoreWriter implements CredentialsProvider {

    private static final Logger logger = LoggerFactory.getLogger(TripleStoreWriter.class);


    @Value("${civetTripleStore.url}")
    private String tripleStoreURL;
//    @Value("${civetTripleStore.username}")
//    private String tripleStoreUsername;
//    @Value("${civetTripleStore.password}")
//    private String tripleStorePassword;

    private org.apache.http.auth.Credentials credentials;


    public void write(Model model) {

        // TODO: 12.12.18 find better way to get toString of RdfNodes

        StmtIterator stmtIterator = model.listStatements();
        StringBuilder insertQuery = new StringBuilder("INSERT DATA { ");
        QuerySolutionMap mp = new QuerySolutionMap();
        int cnt = 0;
        while (stmtIterator.hasNext()) {
            Statement statement = stmtIterator.nextStatement();

            String s = "?s" + cnt;
            String p = "?p" + cnt;
            String o = "?o" + cnt;

            cnt++;

            mp.add(s, statement.getSubject());
            mp.add(p, statement.getPredicate());
            mp.add(o, statement.getObject());

            insertQuery
                    .append(s).append(' ')
                    .append(p).append(' ')
                    .append(o).append(" .\n");
        }
        insertQuery.append("}");
        ParameterizedSparqlString pss = new ParameterizedSparqlString(insertQuery.toString());
        pss.setParams(mp);


        try {
            UpdateRequest request = UpdateFactory.create(pss.toString());
            UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, tripleStoreURL);
            try {
                proc.execute();
            } catch (Exception e) {
                logger.error("An error occurred in writing to TripleStore, {}", e);
            }
        } catch (Exception e) {
            logger.error("An error occurred in writing to TripleStore, {}", e);
        }
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
