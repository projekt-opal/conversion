package de.upb.cs.dice.opal.conversion.converter;

import com.google.common.collect.ImmutableMap;
import de.upb.cs.dice.opal.conversion.model.Portal;
import de.upb.cs.dice.opal.conversion.repository.PortalRepository;
import de.upb.cs.dice.opal.conversion.utility.RDFUtility;
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
import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
@EnableRetry
public class DataSetFetcher implements CredentialsProvider, Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DataSetFetcher.class);

    static final Property opalTemporalCatalogProperty =
            ResourceFactory.createProperty("http://projekt-opal.de/catalog");
    private org.aksw.jena_sparql_api.core.QueryExecutionFactory qef;
    private boolean isCanceled;

    @Value("${info.crawler.tripleStore.url}")
    private String tripleStoreURL;
    @Value("${info.crawler.tripleStore.username}")
    private String tripleStoreUsername;
    @Value("${info.crawler.tripleStore.password}")
    private String tripleStorePassword;

    private org.apache.http.auth.Credentials credentials;

    private static final int PAGE_SIZE = 100;

    private static final ImmutableMap<String, String> PREFIXES = ImmutableMap.<String, String>builder()
            .put("dcat", "http://www.w3.org/ns/dcat#")
            .put("dct", "http://purl.org/dc/terms/")
            .build();


    private final RabbitTemplate rabbitTemplate;
    private final PortalRepository portalRepository;
    private String portalName;

    @Autowired
    public DataSetFetcher(RabbitTemplate rabbitTemplate, PortalRepository portalRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.portalRepository = portalRepository;
    }

    public void run() {
        try {
            logger.info("Starting fetching portal {}", portalName);
            initialQueryExecutionFactory(portalName);

            Resource portalResource = ResourceFactory.createResource("http://projekt-opal.de/catalog/" + portalName);

            int totalNumberOfDataSets = getTotalNumberOfDataSets();
            logger.debug("Total number of datasets is {}", totalNumberOfDataSets);
            if (totalNumberOfDataSets == -1) {
                throw new Exception("Cannot Query the TripleStore");
            }
            Portal portal = portalRepository.findByName(portalName);
            int high = portal.getHigh();
            int lnf = portal.getLastNotFetched();

            if(high == -1 || high > totalNumberOfDataSets) {
                high = totalNumberOfDataSets;
            }

            portal.setHigh(high);
            portalRepository.save(portal);

            for (int idx = lnf; idx < high; idx += PAGE_SIZE) {
                if(isCanceled) {
                    logger.info("fetching portal {} is cancelled", portalName);
                    return;
                }
                if(idx > 0 && idx % 100000 == 0) {
                    try {
                        Thread.sleep(1000000); //manual waits for 1000 seconds for not reaching memory problem
                    } catch (InterruptedException ignored) {}
                }
                portal.setLastNotFetched(idx);
                portalRepository.save(portal);

                int min = Math.min(PAGE_SIZE, high - idx);
                logger.info("Getting list datasets  {} : {}", idx, idx + min);
                List<Resource> listOfDataSets = getListOfDataSets(idx, min);
                listOfDataSets
//                        .subList(1,2) //only for debug
                        .parallelStream()
                        .forEach(dataSet -> {
                            logger.trace("Getting graph of {}", dataSet);
                            try {
                                getGraphAndConvert(portalResource, dataSet);
                            } catch (Exception e) {
                                logger.error("An error occurred in getting graph and converting of it for {} {}", dataSet, e);
                            }
                        });
            }
            portal.setLastNotFetched(high);
            portalRepository.save(portal);


            logger.info("fetching portal {} finished", portalName);
        } catch (Exception e) {
            logger.error("An Error occurred in converting portal {}, {}", portalName, e);
        }
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

    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 1000))
    private void getGraphAndConvert(Resource portalResource, Resource dataSet) {
        Model dataSetGraph = getAllPredicatesObjectsPublisherDistributions(dataSet);
        dataSetGraph.add(dataSet, opalTemporalCatalogProperty, portalResource);
        byte[] bytes = RDFUtility.serialize(dataSetGraph);
        //enqueue charge response to activemq to
        rabbitTemplate.convertAndSend("conversionQueue", bytes);
    }

    /**
     * @return -1 => something went wrong, o.w. the number of distinct dataSets are return
     */
    private int getTotalNumberOfDataSets() {
        int cnt;
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


    private List<Resource> getListOfDataSets(int idx, int limit) {

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
                "LIMIT " + limit
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


    public DataSetFetcher setPortalName(String portalName) {
        this.portalName = portalName;
        return this;
    }

    private int getCount(ParameterizedSparqlString pss) {
        int cnt = -1;
        try (QueryExecution queryExecution = qef.createQueryExecution(pss.asQuery())) {
            ResultSet resultSet = queryExecution.execSelect();
            while (resultSet.hasNext()) {
                QuerySolution solution = resultSet.nextSolution();
                RDFNode num = solution.get("num");
                cnt = num.asLiteral().getInt();
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

    public DataSetFetcher setCanceled(boolean canceled) {
        isCanceled = canceled;
        return this;
    }
}
