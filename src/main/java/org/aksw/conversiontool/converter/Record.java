package org.aksw.conversiontool.converter;

import org.apache.jena.rdf.model.Property;

public class Record {
    private String selector;
    private String value;
    private String subject;
    private Property property;
    private String object;

    public Record() {
    }

    public Record(String selector, Property property) {
        this.selector = selector;
        this.property = property;
    }

    public String getSelector() {
        return selector;
    }

    public Record setSelector(String selector) {
        this.selector = selector;
        return this;
    }

    public String getValue() {
        return value;
    }

    public Record setValue(String value) {
        this.value = value;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public Record setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public Property getProperty() {
        return property;
    }

    public Record setProperty(Property property) {
        this.property = property;
        return this;
    }

    public String getObject() {
        return object;
    }

    public Record setObject(String object) {
        this.object = object;
        return this;
    }
}
