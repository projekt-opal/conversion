package de.upb.cs.dice.opal.conversion.config;

import de.upb.cs.dice.opal.conversion.model.Portal;
import de.upb.cs.dice.opal.conversion.repository.PortalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class DB {

    private final PortalRepository portalRepository;

    @Autowired
    public DB(PortalRepository portalRepository) {
        this.portalRepository = portalRepository;
    }

    @PostConstruct
    public void initDB() {
        Iterable<Portal> portals = portalRepository.findAll();
        if(!portals.iterator().hasNext()) {
            portalRepository.save(new Portal().setName("mcloud").setLastNotFetched(0).setHigh(-1));
            portalRepository.save(new Portal().setName("govdata").setLastNotFetched(0).setHigh(-1));
            portalRepository.save(new Portal().setName("europeandataportal").setLastNotFetched(0).setHigh(-1));
        }
    }
}
