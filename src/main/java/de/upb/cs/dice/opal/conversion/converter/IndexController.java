package de.upb.cs.dice.opal.conversion.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class IndexController {

    private final DataSetFetcher dataSetFetcher;
    private final CKAN_Config ckanConfig;

    @Autowired
    public IndexController(DataSetFetcher dataSetFetcher, CKAN_Config ckanConfig) {
        this.dataSetFetcher = dataSetFetcher;
        this.ckanConfig = ckanConfig;
    }

    @GetMapping("/convert")
    public String convert(@RequestParam(name = "API_KEY", required = true) String apiKey) {
        ckanConfig.setApiKey(apiKey);
        dataSetFetcher.fetch();
        return "index";
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

}
