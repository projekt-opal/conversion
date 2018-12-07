package de.upb.cs.dice.opal.conversion.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    private final DataSetFetcher dataSetFetcher;

    @Autowired
    public IndexController(DataSetFetcher dataSetFetcher) {
        this.dataSetFetcher = dataSetFetcher;
    }

    @GetMapping("/convert")
    public String convert() {

        try {
            dataSetFetcher.fetch();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "index";
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

}
