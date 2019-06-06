package de.upb.cs.dice.opal.conversion.model;

import javax.persistence.*;

@Entity
public class Ckan {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column
    private String apiKey;

    public int getId() {
        return id;
    }

    public Ckan setId(int id) {
        this.id = id;
        return this;
    }

    public String getApiKey() {
        return apiKey;
    }

    public Ckan setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }
}
