package org.aksw.conversiontool.converter;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MCloudService extends CrudRepository<MCloudConfig, Long> {
    MCloudConfig findByKey(String key);
}
