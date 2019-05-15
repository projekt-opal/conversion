package de.upb.cs.dice.opal.conversion.converter;

import org.springframework.stereotype.Component;

@Component
public class CKAN_Config {

    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public CKAN_Config setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }
}
