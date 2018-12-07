package de.upb.cs.dice.opal.conversion.converter;

import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.SelectorImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class Converter {

    private static final Logger logger = LoggerFactory.getLogger(Converter.class);

    public void convert(Model model) {

        ResIterator resIterator = model.listResourcesWithProperty(RDF.type, DCAT.Dataset);
        if (resIterator.hasNext()) {
            Resource dataSet = resIterator.nextResource();
            makeOpalConfirmedUri(model, dataSet, DCAT.Dataset, null, "dataset");
            makeOpalConfirmedUri(model, dataSet, DCAT.Distribution, DCAT.distribution, "distribution");
        }

        try {
            model.write(new FileOutputStream("/home/afshin/files/opal.ttl", true));
        } catch (Exception e) {
            logger.error("An error occured in saving th model, {}", e);
        }

    }


    private boolean isNotOpalConfirmed(String uri) {
        return !uri.startsWith("http://projekt-opal.de/");
    }

    private void makeOpalConfirmedUri(Model model, Resource dataSet, Resource classType, Property propertyType, String typeName) {
        ResIterator resIterator = model.listResourcesWithProperty(RDF.type, classType);
        while (resIterator.hasNext()) {
            Resource oldResource = resIterator.nextResource();
            if (isNotOpalConfirmed(oldResource.getURI())) {
                Resource newResource = generateOpalConfirmedUrl(oldResource, typeName);

                StmtIterator oldIterator = model.listStatements(new SelectorImpl(oldResource, null, (RDFNode) null));
                List<Statement> newResourceStatements = new ArrayList<>();
                while (oldIterator.hasNext()) {
                    Statement statement = oldIterator.nextStatement();
                    newResourceStatements.add(new StatementImpl(newResource, statement.getPredicate(), statement.getObject()));
                }
                oldIterator = model.listStatements(new SelectorImpl(oldResource, null, (RDFNode) null));
                model.remove(oldIterator);
                model.add(newResourceStatements);

                if (propertyType != null) {
                    model.remove(dataSet, propertyType, oldResource);
                    model.add(dataSet, propertyType, newResource);
                }
            }
        }
    }

    private Resource generateOpalConfirmedUrl(Resource resource, String type) {
        String uri = resource.getURI();
        String pattern = "[^a-zA-Z0-9]";
        String s = uri.replaceAll(pattern, "_");
        return ResourceFactory.createResource("http://projekt-opal.de/" + type + "/" + s);
    }

}
