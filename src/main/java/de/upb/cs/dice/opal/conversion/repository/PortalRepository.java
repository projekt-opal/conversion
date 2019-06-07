package de.upb.cs.dice.opal.conversion.repository;

import de.upb.cs.dice.opal.conversion.model.Portal;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortalRepository extends CrudRepository<Portal, Integer> {
    Portal findByName(String portalName);
}
