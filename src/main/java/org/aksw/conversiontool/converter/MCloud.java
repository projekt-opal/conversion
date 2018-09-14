package org.aksw.conversiontool.converter;
import org.apache.jena.rdf.model.* ;

public class MCloud {
    private static final Model m_model = ModelFactory.createDefaultModel();

    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://mcloud.de/";

    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return NS;}

    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );

    /** <p>A summary of the resource.</p> */
    public static final Property climateAndWeather = m_model.createProperty( "http://mcloud.de/climateAndWeather" );
    public static final Property bahn = m_model.createProperty( "http://mcloud.de/bahn" );
    public static final Property street = m_model.createProperty( "http://mcloud.de/street" );
    public static final Property waterwaysAndWaters = m_model.createProperty( "http://mcloud.de/waterwaysAndWaters" );
    public static final Property aerospace = m_model.createProperty( "http://mcloud.de/aerospace" );
    public static final Property infrastructure = m_model.createProperty( "http://mcloud.de/infrastructure" );

    public static Property getPropertyOfUri(String uri) {
        switch (uri) {
            case "http://mcloud.de/climateAndWeather": return climateAndWeather;
            case "http://mcloud.de/bahn": return bahn;
            case "http://mcloud.de/street": return street;
            case "http://mcloud.de/waterwaysAndWaters": return waterwaysAndWaters;
            case "http://mcloud.de/aerospace": return aerospace;
            case "http://mcloud.de/infrastructure": return infrastructure;
        }
        throw new StringIndexOutOfBoundsException();
    }

}
