package de.upb.cs.dice.opal.conversion.converter;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class LicenseUtility {

    private static final Logger logger = LoggerFactory.getLogger(LicenseUtility.class);

    private List<Model> models;

    private long cnt = 0;

    private File localTripleStore;

    public LicenseUtility() throws IOException {
        models = new ArrayList<>();
        models.add(RDFDataMgr.loadModel(new ClassPathResource("static/data/licenses_20180514.ttl").getFile().getAbsolutePath()));
        localTripleStore = new ClassPathResource("static/data/opalDefinedLicenses.ttl").getFile();
        models.add(RDFDataMgr.loadModel(localTripleStore.getAbsolutePath()));
        cnt = models.get(models.size() - 1).listResourcesWithProperty(RDF.type, SKOS.Concept).toList().size();
    }

    public Resource getLicense(String url) {
        Resource license = ResourceFactory.createResource(url);
        for (Model model : models) {
            ResIterator resIterator = model.listResourcesWithProperty(FOAF.homepage, license);
            if (resIterator.hasNext()) {
                return resIterator.nextResource();
            }
        }

        Resource newLicense = ResourceFactory.createResource("http://projekt-opal.de/def/licenses/license_" + cnt++);
        models.get(models.size() - 1).add(newLicense, RDF.type, SKOS.Concept)
                .add(newLicense, SKOS.topConceptOf, ResourceFactory.createResource("http://projekt-opal.de/def/licenses"))
                .add(newLicense, SKOS.inScheme, ResourceFactory.createResource("http://projekt-opal.de/def/licenses"))
                .add(newLicense, DCTerms.references, license)
                .add(newLicense, FOAF.homepage, license);

        try (FileWriter out = new FileWriter(localTripleStore.getAbsolutePath())) {
            models.get(models.size() - 1).write(out, "TURTLE");
        } catch (IOException closeException) {
            logger.error("An error occurred in saving new Model, {}", closeException);
        }
        return newLicense; // TODO: 14.09.18 Needs to decide what should we do
    }
}
