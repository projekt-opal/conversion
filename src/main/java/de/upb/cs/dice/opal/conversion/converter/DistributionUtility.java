package de.upb.cs.dice.opal.conversion.converter;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Component
public class DistributionUtility {

    private static final Logger logger = LoggerFactory.getLogger(DistributionUtility.class);

    private File localTripleStore;

    private Model model;

    private long cnt;

    public DistributionUtility() throws IOException {
        localTripleStore = new ClassPathResource("static/data/distribution.ttl").getFile();
        model = RDFDataMgr.loadModel(localTripleStore.getAbsolutePath());
        cnt = model.listResourcesWithProperty(RDF.type, DCAT.distribution).toList().size();
    }

    public Resource getDistribution(String url) {
        Resource resource = ResourceFactory.createResource(url);
        ResIterator resIterator = model.listResourcesWithProperty(DCAT.accessURL, resource);
        if (resIterator.hasNext())
            return resIterator.nextResource();
        resIterator = model.listResourcesWithProperty(DCAT.downloadURL, resource);
        if (resIterator.hasNext())
            return resIterator.nextResource();

        Resource distribution = ResourceFactory.createResource("http://distribution.projekt-opal.de/distribution_" + cnt++);
        String[] split = url.split("/");
        Property property = DCAT.accessURL;
        if(split[split.length - 1].contains("."))
            property = DCAT.downloadURL;
        model.add(distribution, RDF.type, DCAT.distribution)
            .add(distribution, property, resource);
        try (FileWriter out = new FileWriter(localTripleStore.getAbsolutePath())) {
            model.write(out, "TURTLE");
        } catch (IOException closeException) {
            logger.error("An error occurred in saving new Model, {}", closeException);
        }
        return distribution;
    }
}
