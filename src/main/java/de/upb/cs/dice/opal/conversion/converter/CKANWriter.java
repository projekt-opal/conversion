package de.upb.cs.dice.opal.conversion.converter;


import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.AbstractMap;

@Component
public class CKANWriter {

    private static final Logger logger = LoggerFactory.getLogger(CKANWriter.class);

    private JenaModelToDcatJsonConverter jenaModelToDcatJsonConverter;

    @Value("${ckan.url}")
    private String CKAN_URL;
    @Value("${ckan.api_key}")
    private String API_KEY;
    @Value("${duplicateName.appendNumber}")
    private boolean appendNumber;

    @PostConstruct
    public void initialize() {
        jenaModelToDcatJsonConverter = new JenaModelToDcatJsonConverter(CKAN_URL, API_KEY, appendNumber);
    }


    @JmsListener(destination = "ckanQueue", containerFactory = "messageFactory")
    public void dump(byte[] bytes) {
        try {
            if (bytes == null) return;
            Model model = RDFUtility.deserialize(bytes);
            AbstractMap.SimpleEntry<StringBuilder, StringBuilder> modelJson = jenaModelToDcatJsonConverter.getModelJson(model);
            StringBuilder json = modelJson.getKey();
            StringBuilder extras = modelJson.getValue();

            json.append(String.format(",\"%s\":[%s]", "extras", extras));
            String payload = String.format("{%s}", json);


            String url = CKAN_URL + "/api/3/action/package_create";
            jenaModelToDcatJsonConverter.fireAndForgetCallPostCKAN(url, payload);
        } catch (Exception e) {
            logger.error("An error occurred in dumping model", e);
        }
    }


}
