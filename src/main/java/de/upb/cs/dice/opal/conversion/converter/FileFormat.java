package de.upb.cs.dice.opal.conversion.converter;

public class FileFormat {
    private String imt;
    private String label;

    public FileFormat() {
    }

    public FileFormat(String imt, String label) {
        this.imt = imt;
        this.label = label;
    }

    public String getImt() {
        return imt;
    }

    public FileFormat setImt(String imt) {
        this.imt = imt;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public FileFormat setLabel(String label) {
        this.label = label;
        return this;
    }
}
