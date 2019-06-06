package de.upb.cs.dice.opal.conversion.web;

import de.upb.cs.dice.opal.conversion.converter.DataSetFetcher;
import de.upb.cs.dice.opal.conversion.model.Ckan;
import de.upb.cs.dice.opal.conversion.model.Portal;
import de.upb.cs.dice.opal.conversion.repository.CkanRepository;
import de.upb.cs.dice.opal.conversion.repository.PortalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class IndexController {

    private final DataSetFetcher dataSetFetcher;
    private final CkanRepository ckanRepository;
    private final PortalRepository portalRepository;

    @Autowired
    public IndexController(DataSetFetcher dataSetFetcher, CkanRepository ckanRepository, PortalRepository portalRepository) {
        this.dataSetFetcher = dataSetFetcher;
        this.ckanRepository = ckanRepository;
        this.portalRepository = portalRepository;
    }

    @GetMapping("/convert")
    public String convert(@RequestParam(name = "API_KEY") String apiKey) {
        Iterable<Ckan> all = ckanRepository.findAll();
        if (all.iterator().hasNext()) {
            Ckan ckan = all.iterator().next();
            ckan.setApiKey(apiKey);
            ckanRepository.save(ckan);
        } else ckanRepository.save(new Ckan().setApiKey(apiKey));

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
