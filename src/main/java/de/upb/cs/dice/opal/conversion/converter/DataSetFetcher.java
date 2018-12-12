package de.upb.cs.dice.opal.conversion.converter;

import com.google.common.collect.ImmutableMap;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.retry.core.QueryExecutionFactoryRetry;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataSetFetcher implements CredentialsProvider {
    private static final Logger logger = LoggerFactory.getLogger(DataSetFetcher.class);


    private org.aksw.jena_sparql_api.core.QueryExecutionFactory qef;
    @Value("${tripleStore.url}")
    private String tripleStoreURL;
    @Value("${tripleStore.username}")
    private String tripleStoreUsername;
    @Value("${tripleStore.password}")
    private String tripleStorePassword;

    @Value("${info.portals}")
    private String[] portals;

    private final Converter converter;

    private org.apache.http.auth.Credentials credentials;

    private static final int PAGE_SIZE = 100;

    private static final ImmutableMap<String, String> PREFIXES = ImmutableMap.<String, String>builder()
            .put("dcat", "http://www.w3.org/ns/dcat#")
            .put("dct", "http://purl.org/dc/terms/")
            .build();

    @Autowired
    public DataSetFetcher(Converter converter) {
        this.converter = converter;
    }

    private void initialQueryExecutionFactory(String portal) {
        credentials = new UsernamePasswordCredentials(tripleStoreUsername, tripleStorePassword);

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setDefaultCredentialsProvider(this);
        org.apache.http.impl.client.CloseableHttpClient client = clientBuilder.build();


        qef = new QueryExecutionFactoryHttp(
                String.format(tripleStoreURL, portal),
                new org.apache.jena.sparql.core.DatasetDescription(), client);
        qef = new QueryExecutionFactoryRetry(qef, 5, 1000);

    }

    @Scheduled(cron = "${info.dumper.scheduler}")
    public void scheduledDumping() {
        try {
            fetch();
        } catch (Exception e) {
            logger.error("{}", e);
        }
    }

    public void fetch() throws Exception {
        logger.info("Fetching started");

//        List<String> portals = new ArrayList<>();
//        portals.add("mcloud");
//        portals.add("govdata");
//        portals.add("europeandataportal");

        for (String portal : portals)
            startFetchingOnePortal(portal);
        logger.info("Fetching finished");
    }

    private void startFetchingOnePortal(String portalName) throws Exception {
        logger.info("Starting fetching portal {}", portalName);
        initialQueryExecutionFactory(portalName);

        Resource portalResource = ResourceFactory.createResource("http://projekt-opal.de/catalog/" + portalName);

        long totalNumberOfDataSets = getTotalNumberOfDataSets();
        logger.debug("Total number of datasets is {}", totalNumberOfDataSets);
        if (totalNumberOfDataSets == -1) {
            throw new Exception("Cannot Query the TripleStore");
        }

        for (int idx = 0; idx < totalNumberOfDataSets; idx += PAGE_SIZE) {
            logger.trace("Getting list datasets  {} : {}", idx, idx + PAGE_SIZE);
            List<Resource> listOfDataSets = getListOfDataSets(idx);
            for (Resource dataSet : listOfDataSets) {
                logger.trace("Getting graph of {}", dataSet);
                Model dataSetGraph = getAllPredicatesObjectsPublisherDistributions(dataSet);
                converter.convert(dataSetGraph, portalResource);
            }
        }
        logger.info("fetching portal {} finished", portalName);
    }

    /**
     * @return -1 => something went wrong, o.w. the number of distinct dataSets are return
     */
    private long getTotalNumberOfDataSets() {
        long cnt;
        ParameterizedSparqlString pss = new ParameterizedSparqlString("" +
                "SELECT (COUNT(DISTINCT ?dataSet) AS ?num)\n" +
                "WHERE { \n" +
                "  GRAPH ?g {\n" +
                "    ?dataSet a dcat:Dataset.\n" +
                "    FILTER(EXISTS{?dataSet dct:title ?title.})\n" +
                "  }\n" +
                "}");

        pss.setNsPrefixes(PREFIXES);

        cnt = getCount(pss);
        return cnt;
    }


    private List<Resource> getListOfDataSets(int idx) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString("" +
                "SELECT DISTINCT ?dataSet\n" +
                "WHERE { \n" +
                "  GRAPH ?g {\n" +
                "    ?dataSet a dcat:Dataset.\n" +
                "    FILTER(EXISTS{?dataSet dct:title ?title.})\n" +
                "  }\n" +
                "}\n" +
                "ORDER BY ?dataSet\n" +
                "OFFSET \n" + idx +
                "LIMIT " + (idx + PAGE_SIZE)
        );

        pss.setNsPrefixes(PREFIXES);

        return getResources(pss);
    }

    private Model getAllPredicatesObjectsPublisherDistributions(Resource dataSet) {
        Model model;


        ParameterizedSparqlString pss = new ParameterizedSparqlString("" +
                "CONSTRUCT { " + "?dataSet ?predicate ?object .\n" +
                "\t?object ?p2 ?o2}\n" +
                "WHERE { \n" +
                "  GRAPH ?g {\n" +
                "    ?dataSet ?predicate ?object.\n" +
                "    OPTIONAL { ?object ?p2 ?o2 }\n" +
                "  }\n" +
                "}");

        pss.setNsPrefixes(PREFIXES);
        pss.setParam("dataSet", dataSet);

        model = executeConstruct(pss);

        return model;
    }


    private Model executeConstruct(ParameterizedSparqlString pss) {
        Model model = null;
        try (QueryExecution queryExecution = qef.createQueryExecution(pss.asQuery())) {
            model = queryExecution.execConstruct();
        } catch (Exception ex) {
            logger.error("An error occurred in executing construct, {}", ex);
        }
        return model;
    }

    private List<Resource> getResources(ParameterizedSparqlString pss) {
        List<Resource> ret = new ArrayList<>();
        try (QueryExecution queryExecution = qef.createQueryExecution(pss.asQuery())) {
            ResultSet resultSet = queryExecution.execSelect();
            while (resultSet.hasNext()) {
                QuerySolution solution = resultSet.nextSolution();
                Resource dataSet = solution.getResource("dataSet");
                ret.add(dataSet);
                logger.trace("getResource: {}", dataSet);
            }
        } catch (Exception ex) {
            logger.error("An error occurred in getting resources, {}", ex);
        }
        return ret;
    }


    private long getCount(ParameterizedSparqlString pss) {
        long cnt = -1;
        try (QueryExecution queryExecution = qef.createQueryExecution(pss.asQuery())) {
            ResultSet resultSet = queryExecution.execSelect();
            while (resultSet.hasNext()) {
                QuerySolution solution = resultSet.nextSolution();
                RDFNode num = solution.get("num");
                cnt = num.asLiteral().getLong();
            }
        } catch (Exception ex) {
            logger.error("An error occurred in getting Count, {}", ex);
        }
        return cnt;
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
