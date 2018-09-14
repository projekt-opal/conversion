package org.aksw.conversiontool.converter;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LicenseUtility {
    private Model model;

    public LicenseUtility() throws IOException {
        model = RDFDataMgr.loadModel(new ClassPathResource("static/data/licenses.ttl").getFile().getAbsolutePath());
    }

    public Resource getLicense(String url) {
        ResIterator resIterator = model.listResourcesWithProperty(FOAF.homepage, ResourceFactory.createResource(url));
        if(resIterator.hasNext()) {
            Resource resource = resIterator.nextResource();
            return resource;
        }
        return null; // TODO: 14.09.18 Needs to decide what should we do
    }
}
