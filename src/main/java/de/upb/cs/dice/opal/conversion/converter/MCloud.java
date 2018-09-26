package de.upb.cs.dice.opal.conversion.converter;
import org.apache.jena.rdf.model.* ;

public class MCloud {
    private static final Model m_model = ModelFactory.createDefaultModel();

    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://mcloud.projekt-opal.de/"; //todo appropriate NS should be chosen

    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return NS;}

    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );

    /** <p>A summary of the resource.</p> */
    public static final Resource climateAndWeather = m_model.createResource( "http://mcloud.projeckt-opal.de/climateAndWeather" );
    public static final Resource bahn = m_model.createResource( "http://mcloud.projeckt-opal.de/bahn" );
    public static final Resource street = m_model.createResource( "http://mcloud.projeckt-opal.de/street" );
    public static final Resource waterwaysAndWaters = m_model.createResource( "http://mcloud.projeckt-opal.de/waterwaysAndWaters" );
    public static final Resource aerospace = m_model.createResource( "http://mcloud.projeckt-opal.de/aerospace" );
    public static final Resource infrastructure = m_model.createResource( "http://mcloud.projeckt-opal.de/infrastructure" );

    public static Resource getResourceOfUri(String uri) {
        switch (uri) {
            case "http://mcloud.projeckt-opal.de/climateAndWeather": return climateAndWeather;
            case "http://mcloud.projeckt-opal.de/bahn": return bahn;
            case "http://mcloud.projeckt-opal.de/street": return street;
            case "http://mcloud.projeckt-opal.de/waterwaysAndWaters": return waterwaysAndWaters;
            case "http://mcloud.projeckt-opal.de/aerospace": return aerospace;
            case "http://mcloud.projeckt-opal.de/infrastructure": return infrastructure;
        }
        throw new StringIndexOutOfBoundsException();
    }

}
