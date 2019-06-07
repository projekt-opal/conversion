package de.upb.cs.dice.opal.conversion.web;

import de.upb.cs.dice.opal.conversion.converter.DataSetFetcher;
import de.upb.cs.dice.opal.conversion.converter.FetchersForPortals;
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

    private final CkanRepository ckanRepository;
    private final PortalRepository portalRepository;
    private final FetchersForPortals fetchersForPortals;

    @Autowired
    public IndexController(CkanRepository ckanRepository, PortalRepository portalRepository, FetchersForPortals fetchersForPortals) {
        this.ckanRepository = ckanRepository;
        this.portalRepository = portalRepository;
        this.fetchersForPortals = fetchersForPortals;
    }

    @GetMapping("/convert")
    public String convert(
            @RequestParam(name = "portalName", required = false) String portalName,
            @RequestParam(name = "lnf", defaultValue = "0") String lnf,
            @RequestParam(name = "high", defaultValue = "-1") String high,
            @RequestParam(name = "apiKey", required = false) String apiKey) {
        Iterable<Ckan> all = ckanRepository.findAll();
        if (all.iterator().hasNext()) {
            Ckan ckan = all.iterator().next();
            if(!ckan.getApiKey().equals(apiKey)) {
                ckan.setApiKey(apiKey);
                ckanRepository.save(ckan);
            }
        } else ckanRepository.save(new Ckan().setApiKey(apiKey));

        System.out.println(portalName + "," + lnf + "," + high + "," + apiKey);

        if(portalName != null && !portalName.isEmpty()) {
            DataSetFetcher fetcher = fetchersForPortals.getFetcher(portalName);
            fetcher.fetch(portalName, Integer.valueOf(lnf), Integer.valueOf(high));
        }
        return "redirect:/";
    }

    @GetMapping("/")
    public String index(Model model) {
        Iterable<Portal> portals = portalRepository.findAll();
        model.addAttribute("portals", portals);
        Iterable<Ckan> all = ckanRepository.findAll();
        if (all.iterator().hasNext()) {
            Ckan ckan = all.iterator().next();
            model.addAttribute("apiKey", ckan.getApiKey());
        }
        return "index";
    }

}
