package de.upb.cs.dice.opal.conversion.converter;

import org.apache.jena.graph.impl.LiteralLabelFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Component
public class AgentUtility {

    private static final Logger logger = LoggerFactory.getLogger(AgentUtility.class);

    private File localTripleStore;

    private Model model;

    private long cnt;

    public AgentUtility() throws IOException {
        localTripleStore = new ClassPathResource("static/data/agents.ttl").getFile();
        model = RDFDataMgr.loadModel(localTripleStore.getAbsolutePath());
        cnt = model.listResourcesWithProperty( RDF.type, FOAF.Agent).toList().size();
    }

    public Resource getAgent(String uri, String name, String languageTag) {
        Resource agentUrl = ResourceFactory.createResource(uri);
        ResIterator resIterator = model.listResourcesWithProperty(FOAF.homepage, agentUrl);
        if (resIterator.hasNext())
            return resIterator.nextResource();

        Resource agent = ResourceFactory.createResource("http://agent.projekt-opal.de/def/agents/agent_" + cnt++);
        model.add(agent, RDF.type, FOAF.Agent)
            .add(agent, FOAF.homepage, agentUrl)
            .add(agent, FOAF.name, ResourceFactory.createLangLiteral(name, languageTag));
        try (FileWriter out = new FileWriter(localTripleStore.getAbsolutePath())) {
            model.write(out, "TURTLE");
        } catch (IOException closeException) {
            logger.error("An error occurred in saving new Model, {}", closeException);
        }
        return agent;
    }
}
