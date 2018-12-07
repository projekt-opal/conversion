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
        try {

            ResIterator resIterator = model.listResourcesWithProperty(RDF.type, DCAT.Dataset);
            if (resIterator.hasNext()) {
                Resource dataSet = resIterator.nextResource();
                String portal = getPortal(dataSet, model);
                makeOpalConfirmedUri(model, dataSet, DCAT.Dataset, null, "dataset");
                makeOpalConfirmedUri(model, dataSet, DCAT.Distribution, DCAT.distribution, "distribution");
                ResIterator opalConfirmedIterator = model.listResourcesWithProperty(RDF.type, DCAT.Dataset);
                Resource dataSetOpalConfirmed = opalConfirmedIterator.nextResource();// TODO: 07.12.18 Check for Exception (".nextResource()")
                addDatasetToCatalog(dataSetOpalConfirmed, portal, model);
            }

            model.write(new FileOutputStream("/home/afshin/files/opal.ttl", true));
        } catch (Exception e) {
            logger.error("An error occured in saving th model, {}", e);
        }

    }

    private void addDatasetToCatalog(Resource dataSet, String portal, Model model) {

        String baseUriCatalog = "http://projekt-opal.de/catalog/" + portal.replace('.', '_'); // TODO: 07.12.18 Make URL static final

        model.add(ResourceFactory.createResource(baseUriCatalog), RDF.type, DCAT.Catalog);
        model.add(ResourceFactory.createResource(baseUriCatalog), DCAT.dataset, dataSet);
    }

    private String getPortal(Resource dataSet, Model model) {

        StmtIterator stmtIterator = model.listStatements(dataSet, ResourceFactory.createProperty("http://www.w3.org/ns/dcat#catalog"), (RDFNode) null);
        if (stmtIterator.hasNext()) {
            Statement statement = stmtIterator.nextStatement();
            String portal = statement.getObject().asLiteral().getString();
            stmtIterator = model.listStatements(dataSet, ResourceFactory.createProperty("http://www.w3.org/ns/dcat#catalog"), (RDFNode) null);
            model.remove(stmtIterator);
            return portal;
        } else {
            return "govdata.de"; // TODO: 07.12.18 In the future query TS from Crawler to make sure dataset exists in the govData CATALOG.
            // Note : Also if dataSets from additional sources are added, this logic needs to be changed
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
