package de.upb.cs.dice.opal.conversion.converter;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MCloudService extends CrudRepository<MCloudConfig, Long> {
    Optional<MCloudConfig> findByKey(String key);
}
