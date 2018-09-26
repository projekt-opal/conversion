package de.upb.cs.dice.opal.conversion.converter;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class LicenseUtility {
    private List<Model> models;

    public LicenseUtility() throws IOException {
        models = new ArrayList<>();
        models.add(RDFDataMgr.loadModel(new ClassPathResource("static/data/licenses_20180514.ttl").getFile().getAbsolutePath()));
    }

    public Resource getLicense(String url) {
        for(Model model : models) {
            ResIterator resIterator = model.listResourcesWithProperty(FOAF.homepage, ResourceFactory.createResource(url));
            if (resIterator.hasNext()) {
                return resIterator.nextResource();
            }
        }
        return null; // TODO: 14.09.18 Needs to decide what should we do
    }
}
