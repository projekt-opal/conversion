package de.upb.cs.dice.opal.conversion.config;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class CKAN_Config {

    private String apiKey;

//    @Value("#{'/home/${user}/apiKey.txt'}")
//    private String keyWritePath;

    @PostConstruct
    public void init() {
//        if(Files.exists(Paths.get(keyWritePath))) { //if the key is already available it will use it
//            try {
//                apiKey = Files.readAllLines(Paths.get(keyWritePath)).get(0);
//            } catch (IOException ignored) {}
//        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public CKAN_Config setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }
}
