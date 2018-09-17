package org.aksw.conversiontool.converter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class MCloudConfig {
    @Id
    @GeneratedValue
    private Long id;

    @Column
    private String key;

    @Column
    private String resourceUri;

    public MCloudConfig() {
    }

    public MCloudConfig(String key, String resourceUri) {
        this.key = key;
        this.resourceUri = resourceUri;
    }

    public Long getId() {
        return id;
    }

    public MCloudConfig setId(Long id) {
        this.id = id;
        return this;
    }

    public String getKey() {
        return key;
    }

    public MCloudConfig setKey(String key) {
        this.key = key;
        return this;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public MCloudConfig setResource(String resourceUri) {
        this.resourceUri = resourceUri;
        return this;
    }
}
