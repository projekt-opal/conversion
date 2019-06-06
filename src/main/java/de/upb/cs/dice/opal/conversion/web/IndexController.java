package de.upb.cs.dice.opal.conversion.web;

import de.upb.cs.dice.opal.conversion.converter.DataSetFetcher;
import de.upb.cs.dice.opal.conversion.config.CKAN_Config;
import de.upb.cs.dice.opal.conversion.model.Portal;
import de.upb.cs.dice.opal.conversion.repository.PortalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class IndexController {

    private final DataSetFetcher dataSetFetcher;
    private final CKAN_Config ckanConfig;
    private final PortalRepository portalRepository;

//    @Value("#{'/home/${user}/apiKey.txt'}")
//    private String keyWritePath;

    @Autowired
    public IndexController(DataSetFetcher dataSetFetcher, CKAN_Config ckanConfig, PortalRepository portalRepository) {
        this.dataSetFetcher = dataSetFetcher;
        this.ckanConfig = ckanConfig;
        this.portalRepository = portalRepository;
    }

    @GetMapping("/convert")
    public String convert(@RequestParam(name = "API_KEY", required = true) String apiKey) {
        ckanConfig.setApiKey(apiKey);

//        try (OutputStream out = new FileOutputStream(keyWritePath)){
//            out.write(apiKey.getBytes());
//        } catch(Exception ignored){}

        dataSetFetcher.fetch();
        return "index";
    }

    @GetMapping("/")
    public String index(Model model) {
        Iterable<Portal> portals = portalRepository.findAll();
        model.addAttribute("portals", portals);
        return "index";
    }

}
