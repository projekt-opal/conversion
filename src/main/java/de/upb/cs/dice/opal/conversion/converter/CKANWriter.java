package de.upb.cs.dice.opal.conversion.converter;


import org.apache.jena.rdf.model.*;
import org.dice_research.opal.common.vocabulary.Dqv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.AbstractMap;

@Component
public class CKANWriter {

    private static final Logger logger = LoggerFactory.getLogger(CKANWriter.class);

    private JenaModelToDcatJsonConverter jenaModelToDcatJsonConverter;

    private final QualityMetricsConfiguration qualityMetricsConfiguration;
    private final CKAN_Config ckanConfig;

    @Value("${info.ckan.url}")
    private String CKAN_URL;
//    @Value("${ckan.api_key}")
//    private String API_KEY;
    @Value("${info.duplicateName.appendNumber}")
    private boolean appendNumber;

    @Autowired
    public CKANWriter(QualityMetricsConfiguration qualityMetricsConfiguration, CKAN_Config ckanConfig) {
        this.qualityMetricsConfiguration = qualityMetricsConfiguration;
        this.ckanConfig = ckanConfig;
    }

//    @PostConstruct
//    public void initialize() {
//        jenaModelToDcatJsonConverter = new JenaModelToDcatJsonConverter(CKAN_URL, ckanConfig.getApiKey(), appendNumber);
//    }


    @JmsListener(destination = "ckanQueue", containerFactory = "messageFactory")
    public void dump(byte[] bytes) {
        try {
            if (bytes == null) return;
            Model model = RDFUtility.deserialize(bytes);

            if(jenaModelToDcatJsonConverter == null) {
                synchronized (this) {
                    if(jenaModelToDcatJsonConverter == null)
                        jenaModelToDcatJsonConverter = new JenaModelToDcatJsonConverter(CKAN_URL, ckanConfig.getApiKey(), appendNumber);
                }
            }

            AbstractMap.SimpleEntry<StringBuilder, StringBuilder> modelJson = jenaModelToDcatJsonConverter.getModelJson(model);
            StringBuilder json = modelJson.getKey();
            StringBuilder extras = modelJson.getValue();

            addQualityMetrics(extras, model);

            json.append(String.format(",\"%s\":[%s]", "extras", extras));
            String payload = String.format("{%s}", json);


            String url = CKAN_URL + "/api/3/action/package_create";
            jenaModelToDcatJsonConverter.fireAndForgetCallPostCKAN(url, payload);
        } catch (Exception e) {
            logger.error("An error occurred in dumping model", e);
        }
    }

    private void addQualityMetrics(StringBuilder extras, Model model) {
        qualityMetricsConfiguration.getMeasurementResource().forEach((key, resource) -> {
//            NodeIterator nodeIterator = model.listObjectsOfProperty(measurementProperty);
            ResIterator resIterator =
                    model.listResourcesWithProperty(Dqv.IS_MEASUREMENT_OF, ResourceFactory.createResource(resource));
            if (resIterator.hasNext()) {
                Resource measurement = resIterator.nextResource();
                NodeIterator nodeIterator = model.listObjectsOfProperty(measurement, Dqv.HAS_VALUE);
                if (nodeIterator.hasNext()) {
                    RDFNode rdfNode = nodeIterator.nextNode();
                    if (rdfNode.isLiteral()) {
                        try {
                            extras.append(String.format("%s{\"key\":\"%s\", \"value\":\"%s\"}",
                                    extras.length() > 0 ? "," : "",
                                    qualityMetricsConfiguration.getMeasurementName().get(key),
                                    rdfNode.asLiteral().getString()));
                        } catch (Exception e) {
                            //ignore
                        }
                    }
                }
            }
        });
    }

}
