package de.upb.cs.dice.opal.conversion.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Map;

@Configuration
@PropertySource("classpath:qualityMeasurement.properties")
@ConfigurationProperties(prefix = "quality")
public class QualityMetricsConfiguration {

    private Map<String, String> measurementResource;
    private Map<String, String> measurementName;

    public Map<String, String> getMeasurementResource() {
        return measurementResource;
    }

    public QualityMetricsConfiguration setMeasurementResource(Map<String, String> measurementResource) {
        this.measurementResource = measurementResource;
        return this;
    }

    public Map<String, String> getMeasurementName() {
        return measurementName;
    }

    public QualityMetricsConfiguration setMeasurementName(Map<String, String> measurementName) {
        this.measurementName = measurementName;
        return this;
    }
}
