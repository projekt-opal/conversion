package de.upb.cs.dice.opal.conversion.repository;

import de.upb.cs.dice.opal.conversion.model.Ckan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CkanRepository extends CrudRepository<Ckan, Integer> {
}
