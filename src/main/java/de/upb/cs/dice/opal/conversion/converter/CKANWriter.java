package de.upb.cs.dice.opal.conversion.converter;


import com.google.gson.*;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.jena.vocabulary.DCAT.keyword;

@Component
public class CKANWriter {

    private static final Logger logger = LoggerFactory.getLogger(CKANWriter.class);

    private static final int EMPTY_JSON_LENGTH = 2;

    @Value("${ckan.url}")
    public String CKAN_URL;
    @Value("${ckan.api_key}")
    private String API_KEY;

    //cache variables
    private String vocabulary_id = null;
    private Map<String, String> tags = new ConcurrentHashMap<>();
    private Map<String, String> groups = new ConcurrentHashMap<>();
    private Map<String, String> license_uri2id = new ConcurrentHashMap<>();
    private Map<String, String> license_title2id = new ConcurrentHashMap<>();

    private static final Property ADMS_IDENTIFIER_PROPERTY = ResourceFactory.createProperty("http://www.w3.org/ns/adms#identifier");
    private static final Property ADMS_VERSIONNOTES_PROPERTY = ResourceFactory.createProperty("http://www.w3.org/ns/adms#versionNotes");
    private static final Property ADMS_SAMPLE_PROPERTY = ResourceFactory.createProperty("http://www.w3.org/ns/adms#sample");
    private static final Property ADMS_CONTACTPOINT_PROPERTY = ResourceFactory.createProperty("http://www.w3.org/ns/adms#contactPoint");
    private static final Property ADMS_STATUS_PROPERTY = ResourceFactory.createProperty("http://www.w3.org/ns/adms#status");

    private static final Property SCHEMA_STARTDATE_PROPERTY = ResourceFactory.createProperty("http://schema.org/startDate");
    private static final Property SCHEMA_ENDDATE_PROPERTY = ResourceFactory.createProperty("http://schema.org/endDate");

    private static final Property TIME_HASBEGINNING_PROPERTY = ResourceFactory.createProperty("http://www.w3.org/2006/time/hasBeginning");
    private static final Property TIME_HASEND_PROPERTY = ResourceFactory.createProperty("http://www.w3.org/2006/time/hasEnd");
    private static final Property TIME_INXSDDATETIME_PROPERTY = ResourceFactory.createProperty("http://www.w3.org/2006/time/inXSDDateTime");


    private static final Property SPDX_CHECKSUM_PROPERTY = ResourceFactory.createProperty("http://spdx.org/rdf/terms#checksum");
    private static final Property SPDX_ALGORITHM_PROPERTY = ResourceFactory.createProperty("http://spdx.org/rdf/terms#algorithm");
    private static final Property SPDX_CHECKSUMVALUE_PROPERTY = ResourceFactory.createProperty("http://spdx.org/rdf/terms#checksumValue");


    @JmsListener(destination = "ckanQueue", containerFactory = "messageFactory")
    public void dump(byte[] bytes) {
        try {
            if (bytes == null) return;
            Model model = RDFUtility.deserialize(bytes);
//            try (OutputStream outputStream = new FileOutputStream("/home/afshin/Desktop/c.ttl")){
//                model.write(outputStream, "turtle");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
            String json = getModelJson(model);
            String url = CKAN_URL + "/api/3/action/package_create";
            fireAndForgetCallPostCKAN(url, json);
        } catch (Exception e) {
            logger.error("An error occurred in dumping model", e);
        }
    }


    public String getModelJson(Model model) {

//        Map<String, String> json = new HashMap<>();
        StringBuilder json = new StringBuilder("{");
//        Map<String, String> extras = new HashMap<>();
        StringBuilder extras = new StringBuilder("[");

        ResIterator resIterator = model.listResourcesWithProperty(RDF.type, DCAT.Dataset);
        if (!resIterator.hasNext()) return null;
        Resource dataset = resIterator.nextResource();

        //region json_String
        setStringValue(model, dataset, json, DCTerms.title, "title"); //dct:title =>	title
        setStringValue(model, dataset, json, DCTerms.description, "notes"); //dct:description => notes
        setStringValue(model, dataset, json, OWL.versionInfo, "version"); //owl:versionInfo => version
        setStringValue(model, dataset, json, DCAT.landingPage, "url"); //dcat:landingPage => url
        //endregion

        //region json_List
        //endregion

        //region extras_String
        setExtrasStringValue(model, dataset, extras, DCTerms.identifier, "identifier"); //dct:identifier => extra:identifier
        setExtrasStringValue(model, dataset, extras, ADMS_IDENTIFIER_PROPERTY, "alternate_identifier"); //adms:identifier	extra:alternate_identifier
        setExtrasStringValue(model, dataset, extras, DCTerms.issued, "issued"); //dct:issued => extra:issued (Note in CKAN when retrieving the value of Date will make it readable
        setExtrasStringValue(model, dataset, extras, DCTerms.modified, "modified"); //dct:modified => extra:modified (Note in CKAN when retrieving the value of Date will make it readable
        setExtrasStringValue(model, dataset, extras, ADMS_VERSIONNOTES_PROPERTY, "version_notes"); //adms:versionNotes => extra:version_notes
        setExtrasStringValue(model, dataset, extras, DCTerms.accrualPeriodicity, "frequency"); //dct:accrualPeriodicity => extra:frequency
        setExtrasStringValue(model, dataset, extras, DCTerms.accessRights, "access_rights"); //dct:accessRights => extra:access_rights
        setExtrasStringValue(model, dataset, extras, DCTerms.provenance, "provenance"); //dct:provenance => extra:provenance
        setExtrasStringValue(model, dataset, extras, DCTerms.type, "dcat_type"); //dct:type => extra:dcat_type
        // TODO: 04.04.19 dct:spatial	extra:spatial_uri later
        //endregion

        //region extras_List
        setExtrasListValues(model, dataset, extras, DCTerms.conformsTo, "conforms_to"); //dct:conformsTo => extra:conforms_to
        setExtrasListValues(model, dataset, extras, DCTerms.language, "language"); //dct:language => extra:language
        setExtrasListValues(model, dataset, extras, FOAF.page, "documentation"); //foaf:page => extra:documentation
        setExtrasListValues(model, dataset, extras, DCTerms.hasVersion, "has_version"); //dct:hasVersion => extra:has_version
        setExtrasListValues(model, dataset, extras, DCTerms.isVersionOf, "is_version_of"); //dct:isVersionOf => extra:is_version_of
        setExtrasListValues(model, dataset, extras, DCTerms.source, "source"); //dct:source => extra:source
        setExtrasListValues(model, dataset, extras, ADMS_SAMPLE_PROPERTY, "sample"); //adms:sample => extra:sample
        //endregion

        //region special_cases
        json.append(String.format("%s\"%s\":\"%s\"", json.length() > 1 ? "," : "", "owner_org", "diceupb"));
        setName(model, dataset, json); //name mst be less than 100 and all lower-case, the rest 2 characters are left for incremental the same dataset name
        setKeywords(model, dataset, json); //dcat:keyword => tags
        setThemes(model, dataset, json); //dcat:theme => extra:theme, but groups is used anywhere
        setTemporals(model, dataset, extras); //dct:temporal => extra:temporal_start + extra:temporal_end
        // dct:publisher => extra:publisher_uri , foaf:name	extra:publisher_name, foaf:mbox	extra:publisher_email
        setPublisherInfos(model, dataset, extras); //foaf:homepage	extra:publisher_url, dct:type	extra:publisher_type
        setContact(model, dataset, extras); //dcat:contactPoint	extra:contact_uri, vcard:fn	extra:contact_name, vcard:hasEmail	extra:contact_email
        setDistributions(model, dataset, json); //dcat:distribution => resources
        //endregion


        extras.append("]");
        json.append(String.format(",\"%s\":%s", "extras", extras));

        json.append("}");
        return json.toString();

    }


    @PostConstruct
    private void licenseRegister() {
        try {
            String apiUrl = CKAN_URL + "/api/3/action/license_list";
            URL obj = new URL(apiUrl);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'GET' request to URL : " + apiUrl);
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
            String jsonLine = response.toString();
            System.out.println(jsonLine);

            JsonElement jelement = new JsonParser().parse(jsonLine);
            JsonObject jobject = jelement.getAsJsonObject();
            if (jobject.get("success").getAsBoolean()) {
                JsonArray result = jobject.getAsJsonArray("result");
                for (JsonElement x : result) {
                    license_uri2id.put(((JsonObject) x).get("url").getAsString(), ((JsonObject) x).get("id").getAsString());
                    license_title2id.put(((JsonObject) x).get("title").getAsString(), ((JsonObject) x).get("id").getAsString());
                }
            } else {
                throw new Exception("success is false");
            }

        } catch (Exception ex) {
            logger.error("Error in fetching licenses ", ex);
        }
    }

    private void setDistributions(Model model, Resource dataset, StringBuilder json) {
        boolean licenseIsSet = false;
        List<String> resources = new ArrayList<>();
        NodeIterator nodeIterator = model.listObjectsOfProperty(dataset, DCAT.distribution);
        while (nodeIterator.hasNext()) {
            RDFNode rdfNode = nodeIterator.nextNode();
            if (rdfNode.isResource()) { //a distribution
                Resource distribution = rdfNode.asResource();
                StringBuilder dict = new StringBuilder();
                dict.append("{");
                setStringValue(model, distribution, dict, DCTerms.title, "name");
                setStringValue(model, distribution, dict, DCTerms.description, "description");
                setStringValue(model, distribution, dict, DCAT.accessURL, "access_url");
                setStringValue(model, distribution, dict, DCAT.downloadURL, "download_url");
                setStringValue(model, distribution, dict, DCTerms.issued, "issued");
                setStringValue(model, distribution, dict, DCTerms.modified, "modified");
                setStringValue(model, distribution, dict, ADMS_STATUS_PROPERTY, "status");
                setStringValue(model, distribution, dict, DCTerms.rights, "rights");
//                setStringValue(model, distribution, dict, DCTerms.license, "license");
                if (!licenseIsSet) {
                    NodeIterator licenseIterator = model.listObjectsOfProperty(distribution, DCTerms.license);
                    if (licenseIterator.hasNext()) {
                        RDFNode licenseNode = licenseIterator.nextNode();
                        if (licenseNode.isResource()) {
                            Resource license = licenseNode.asResource();
                            dict.append(String.format(",\"%s\":\"%s\"", "license", license.getURI()));
                            String license_id = license_uri2id.get(license.getURI());
                            if (license_id == null) {
                                String licenseTitle = getObjectValue(model, license, DCTerms.title);
                                if (licenseTitle != null)
                                    license_id = license_title2id.get(licenseTitle);
                            }
                            if (license_id != null) {
                                licenseIsSet = true;
                                json.append(String.format(",\"%s\":\"%s\"", "license_id", license_id));
                            }

                        } else if (licenseNode.isLiteral()) {
                            String value = licenseNode.asLiteral().getString();
                            dict.append(String.format(",\"%s\":\"%s\"", "license", value));
                        }
                    }
                } else {
                    setStringValue(model, distribution, dict, DCTerms.license, "license");
                }
                setListValues(model, distribution, dict, DCTerms.language, "language");
                setListValues(model, distribution, dict, FOAF.page, "documentation");
                setListValues(model, distribution, dict, DCTerms.conformsTo, "conforms_to");

                String downloadUrl = getObjectValue(model, distribution, DCAT.downloadURL);
                String accessUrl = getObjectValue(model, distribution, DCAT.accessURL);

                if (downloadUrl != null) dict.append(String.format(",\"%s\":\"%s\"", "url", downloadUrl));
                else if (accessUrl != null) dict.append(String.format(",\"%s\":\"%s\"", "url", accessUrl));

                setFileFormat(model, distribution, dict);
                setStringValue(model, distribution, dict, DCAT.byteSize, "size");
                setChecksum(model, distribution, dict);
                dict.append(String.format("%s\"%s\":\"%s\"", dict.length() > EMPTY_JSON_LENGTH ? "," : "", "uri", distribution.getURI()));
                dict.append("}");

                resources.add(dict.toString());
            }
        }
        if (resources.size() > 0)
            json.append(String.format("%s\"%s\":%s", json.length() > EMPTY_JSON_LENGTH ? "," : "", "resources", getArrayJsonValue(resources)));
    }

    private void setChecksum(Model model, Resource distribution, StringBuilder dict) {
        NodeIterator spdxChecksumIterator = model.listObjectsOfProperty(distribution, SPDX_CHECKSUM_PROPERTY);
        if (spdxChecksumIterator.hasNext()) {
            RDFNode nextNode = spdxChecksumIterator.nextNode();
            if (nextNode.isResource()) {
                Resource checksum = nextNode.asResource();
                setStringValue(model, checksum, dict, SPDX_ALGORITHM_PROPERTY, "hash_algorithm");
                setStringValue(model, checksum, dict, SPDX_CHECKSUMVALUE_PROPERTY, "hash");
            }
        }
    }

    private void setFileFormat(Model model, Resource distribution, StringBuilder dict) {
        FileFormat fileFormat = _distribution_format(model, distribution);
        if (fileFormat.getImt() != null && !fileFormat.getImt().isEmpty()) {
            dict.append(String.format("%s\"%s\":\"%s\"", dict.length() > EMPTY_JSON_LENGTH ? "," : "", "mimetype", fileFormat.getImt()));
        }
        if (fileFormat.getLabel() != null && !fileFormat.getLabel().isEmpty())
            dict.append(String.format("%s\"%s\":\"%s\"", dict.length() > EMPTY_JSON_LENGTH ? "," : "", "format", fileFormat.getLabel()));
        else if (fileFormat.getImt() != null && !fileFormat.getImt().isEmpty())
            dict.append(String.format("%s\"%s\":\"%s\"", dict.length() > EMPTY_JSON_LENGTH ? "," : "", "format", fileFormat.getLabel()));
    }


    private FileFormat _distribution_format(Model model, Resource distribution) { // boolean normalize_ckan_format is considered to be always true
        String imt = null;
        String label = null;

        imt = getObjectValue(model, distribution, DCAT.mediaType);
        NodeIterator nodeIterator = model.listObjectsOfProperty(distribution, DCTerms.format);
        if (nodeIterator.hasNext()) {
            RDFNode rdfNode = nodeIterator.nextNode();
            if (rdfNode.isLiteral()) {
                if ((imt == null || imt.isEmpty()) && rdfNode.asLiteral().getString().contains("/"))
                    imt = rdfNode.asLiteral().getString();
                else
                    label = rdfNode.asLiteral().getString();
            } else {
                NodeIterator typeIterator = model.listObjectsOfProperty(rdfNode.asResource(), RDF.type);
                if (typeIterator.hasNext() && typeIterator.nextNode().asResource().equals(ResourceFactory.createResource("http://purl.org/dc/terms/IMT"))) {
                    if (imt == null || imt.isEmpty())
                        imt = getObjectValue(model, rdfNode.asResource(), RDF.value);
                    label = getObjectValue(model, rdfNode.asResource(), RDFS.label);
                } else if (rdfNode instanceof Resource) {
//                # If the URIRef does not reference a BNode, it could reference an IANA type.
//                # Otherwise, use it as label.
                    String format_uri = rdfNode.toString(); // TODO: 29.03.19 Check for Correctness
                    if (format_uri.contains("iana.org/assignments/media-types") && (imt == null || imt.isEmpty()))
                        imt = format_uri;
                    else
                        label = format_uri;
                }
            }
        }
        return new FileFormat(imt, label);
    }


    private void setName(Model model, Resource dataset, StringBuilder json) {
        String title = getObjectValue(model, dataset, DCTerms.title);
        if (title != null) {
            String name = title.substring(0, Math.min(98, title.length())).toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
            json.append(String.format("%s\"%s\":\"%s\"", json.length() > EMPTY_JSON_LENGTH ? "," : "", "name", name));
        }
    }

    private void setTemporals(Model model, Resource dataset, StringBuilder extras) {
        String[] temporals = time_interval(model, dataset);
        if (temporals[0] != null)
            extras.append(String.format("%s{\"key\":\"%s\", \"value\":\"%s\"}",
                    extras.length() > EMPTY_JSON_LENGTH ? "," : "", "temporal_start", temporals[0]));
        if (temporals[1] != null)
            extras.append(String.format("%s{\"key\":\"%s\", \"value\":\"%s\"}",
                    extras.length() > EMPTY_JSON_LENGTH ? "," : "", "temporal_end", temporals[1]));
    }

    private void setThemes(Model model, Resource dataset, StringBuilder json) {
        List<String> themes = getThemes(model, dataset);
        if (themes.size() > 0) {
            List<String> groupsJson = new ArrayList<>();
            for (String theme : themes) groupsJson.add(getOrCreateGroupJson(theme));
            json.append(String.format("%s\"%s\":%s", json.length() > EMPTY_JSON_LENGTH ? "," : "", "groups", getArrayJsonValue(groupsJson)));
        }
    }

    private void setKeywords(Model model, Resource dataset, StringBuilder json) {
        List<String> keywords = getKeywords(model, dataset);
        if (keywords.size() > 0) {
            List<String> tagsJson = new ArrayList<>();
            if (vocabulary_id == null) {
                try {
                    createVocabularyId();
                } catch (Exception e) {
                    logger.error("Error in creating Vocabulary ID", e);
                }
            }
            for (String keyword : keywords) tagsJson.add(getOrCreateTagJson(keyword));
            json.append(String.format("\"%s\":%s,", "tags", getArrayJsonValue(tagsJson)));
        }
    }

    private StringBuilder getArrayJsonValue(List<String> jsonValues) {
        StringBuilder arrayJasonValue = new StringBuilder();
        arrayJasonValue.append("[");
        for (int i = 0; i < jsonValues.size(); i++) {
            if (i > 0) arrayJasonValue.append(",");
            arrayJasonValue.append(jsonValues.get(i));
        }
        arrayJasonValue.append("]");
        return arrayJasonValue;
    }

    private void setContact(Model model, Resource dataset, StringBuilder extras) {
        NodeIterator nodeIterator = model.listObjectsOfProperty(dataset, DCAT.contactPoint);
        if (!nodeIterator.hasNext()) nodeIterator = model.listObjectsOfProperty(dataset, ADMS_CONTACTPOINT_PROPERTY);
        if (nodeIterator.hasNext()) {
            RDFNode rdfNode = nodeIterator.nextNode();
            if (rdfNode.isResource()) {
                Resource contact = rdfNode.asResource();
                extras.append(String.format("%s{\"key\":\"%s\", \"value\":\"%s\"}",
                        extras.length() > EMPTY_JSON_LENGTH ? "," : "", "contact_uri", contact.getURI()));
                String name = getObjectValue(model, contact, VCARD4.fn);
                if (name != null)
                    extras.append(String.format("%s{\"key\":\"%s\", \"value\":\"%s\"}",
                            extras.length() > EMPTY_JSON_LENGTH ? "," : "", "contact_name", name));
                String email = getObjectValue(model, contact, VCARD4.hasEmail);
                if (email != null) {
                    if (email.startsWith("mailto:"))
                        email = email.substring("mailto:".length());
                    extras.append(String.format("%s{\"key\":\"%s\", \"value\":\"%s\"}",
                            extras.length() > EMPTY_JSON_LENGTH ? "," : "", "contact_email", email));
                }
            }
        }
    }

    private void setPublisherInfos(Model model, Resource dataset, StringBuilder extras) {
        NodeIterator nodeIterator = model.listObjectsOfProperty(dataset, DCTerms.publisher);
        if (nodeIterator.hasNext()) {
            RDFNode rdfNode = nodeIterator.nextNode();
            if (rdfNode.isResource()) {
                Resource agent = rdfNode.asResource();
                extras.append(String.format("%s{\"key\":\"%s\", \"value\":\"%s\"}",
                        extras.length() > EMPTY_JSON_LENGTH ? "," : "", "publisher_uri", agent.getURI()));
                String foafName = getObjectValue(model, agent, FOAF.name);
                if (foafName != null)
                    extras.append(String.format("%s{\"key\":\"%s\", \"value\":\"%s\"}",
                            extras.length() > EMPTY_JSON_LENGTH ? "," : "", "publisher_name", foafName));
                String foafMbox = getObjectValue(model, agent, FOAF.mbox);
                if (foafMbox != null)
                    extras.append(String.format("%s{\"key\":\"%s\", \"value\":\"%s\"}",
                            extras.length() > EMPTY_JSON_LENGTH ? "," : "", "publisher_email", foafMbox));
                String foafHomePage = getObjectValue(model, agent, FOAF.homepage);
                if (foafHomePage != null)
                    extras.append(String.format("%s{\"key\":\"%s\", \"value\":\"%s\"}",
                            extras.length() > EMPTY_JSON_LENGTH ? "," : "", "publisher_url", foafHomePage));
                String dctType = getObjectValue(model, agent, DCTerms.type);
                if (dctType != null)
                    extras.append(String.format("%s{\"key\":\"%s\", \"value\":\"%s\"}",
                            extras.length() > EMPTY_JSON_LENGTH ? "," : "", "publisher_type", dctType));
            }
        }
    }

    private String[] time_interval(Model model, Resource dataset) {
        String[] ret = new String[2];
        NodeIterator nodeIterator = model.listObjectsOfProperty(dataset, DCTerms.temporal);
        if (nodeIterator.hasNext()) {
            RDFNode rdfNode = nodeIterator.nextNode();
            if (rdfNode.isResource()) {
                Resource interval = rdfNode.asResource();
                NodeIterator startDateIterator = model.listObjectsOfProperty(interval, SCHEMA_STARTDATE_PROPERTY);
                NodeIterator endDateIterator = model.listObjectsOfProperty(interval, SCHEMA_ENDDATE_PROPERTY);
                if (startDateIterator.hasNext() || endDateIterator.hasNext()) {
                    if (startDateIterator.hasNext()) {
                        RDFNode rdfNodeStartDate = startDateIterator.nextNode();
                        if (rdfNodeStartDate.isLiteral()) ret[0] = rdfNodeStartDate.asLiteral().getString();
                        else ret[0] = rdfNodeStartDate.asResource().getURI();
                    }
                    if (endDateIterator.hasNext()) {
                        RDFNode rdfNodeEndDate = endDateIterator.nextNode();
                        if (rdfNodeEndDate.isLiteral()) ret[1] = rdfNodeEndDate.asLiteral().getString();
                        else ret[1] = rdfNodeEndDate.asResource().getURI();
                    }
                } else { //If no luck, try the w3 time way
                    startDateIterator = model.listObjectsOfProperty(interval, TIME_HASBEGINNING_PROPERTY);
                    endDateIterator = model.listObjectsOfProperty(interval, TIME_HASEND_PROPERTY);
                    if (startDateIterator.hasNext()) {
                        RDFNode rdfNodeStartDate = startDateIterator.nextNode();
                        if (rdfNodeStartDate.isResource())
                            ret[0] = getObjectValue(model, dataset, TIME_INXSDDATETIME_PROPERTY);
                    }
                    if (endDateIterator.hasNext()) {
                        RDFNode rdfNodeEndDate = endDateIterator.nextNode();
                        if (rdfNodeEndDate.isResource())
                            ret[1] = getObjectValue(model, dataset, TIME_INXSDDATETIME_PROPERTY);
                    }
                }
            }
        }
        return ret;
    }

    private void setExtrasListValues(Model model, Resource subject, StringBuilder extras, Property property, String key) {
        List<String> values = getObjectValueList(model, subject, property);
        if (values.size() > 0)
            extras.append(String.format("%s{\"key\":\"%s\", \"value\":%s}",
                    extras.length() > EMPTY_JSON_LENGTH ? "," : "", key, getArrayJsonValue(values)));
    }

    private void setListValues(Model model, Resource subject, StringBuilder json, Property property, String key) {
        List<String> values = getObjectValueList(model, subject, property);
        if (values.size() > 0)
            json.append(String.format("%s\"%s\":%s}",
                    json.length() > EMPTY_JSON_LENGTH ? "," : "", key, getArrayJsonValue(values)));
    }

    private void setStringValue(Model model, Resource subject, StringBuilder json, Property property, String key) {
        String value = getObjectValue(model, subject, property);
        if (value != null)
            json.append(String.format("%s\"%s\":\"%s\"", json.length() > EMPTY_JSON_LENGTH ? "," : "", key, value));
    }

    private void setExtrasStringValue(Model model, Resource subject, StringBuilder extras, Property property, String key) {
        String value = getObjectValue(model, subject, property);
        if (value != null) extras.append(String.format("%s{\"key\":\"%s\", \"value\":\"%s\"}",
                extras.length() > EMPTY_JSON_LENGTH ? "," : "", key, value));
    }

    private String getOrCreateGroupJson(String theme) {
        if (this.groups.size() == 0) syncGroupsCache();
        if (!this.groups.containsKey(theme)) createGroup(theme);
        return groups.get(theme);
    }

    private void createGroup(String theme) {
        String apiUrl = CKAN_URL + "/api/3/action/group_create";
        List<NameValuePair> params = new ArrayList<>();
        String groupName = theme.substring(0, Math.min(100, theme.length())).toLowerCase();
        params.add(new BasicNameValuePair("name", groupName));
        params.add(new BasicNameValuePair("display_name", theme));
        params.add(new BasicNameValuePair("title", theme));
        try {
            JsonObject jobject = callPostCKAN(apiUrl, params);
            if (jobject.get("success").getAsBoolean()) {
                Map<String, String> tmp = new HashMap<>();
                tmp.put("name", groupName);
                this.tags.put(theme, new Gson().toJson(tmp));
            } else {
                throw new Exception("success is false");
            }
        } catch (Exception ex) {
            logger.error("Exception in createGroup", ex);
        }
    }

    private void syncGroupsCache() {
        try {
            String apiUrl = CKAN_URL + "/api/3/action/group_list";
            JsonObject jobject = getJsonObjectFromAPI(apiUrl);
            if (jobject.get("success").getAsBoolean()) {
                JsonArray result = jobject.getAsJsonArray("result");
                for (JsonElement group : result) {
                    String groupString = group.getAsString();
                    String groupDisplayName = getGroupDisplayName(groupString);
                    if (groupDisplayName != null) {
                        Map<String, String> tmp = new HashMap<>();
                        tmp.put("name", groupString);
                        this.groups.put(groupDisplayName, new Gson().toJson(tmp));
                    }
                }
            } else {
                throw new Exception("success is false");
            }
        } catch (Exception ex) {
            logger.error("Error in fetching licenses ", ex);
        }
    }

    private String getGroupDisplayName(String groupString) throws IOException {
        String apiUrl = CKAN_URL + "/api/3/action/group_show?id=" + groupString;
        JsonObject jobject = getJsonObjectFromAPI(apiUrl);
        if (jobject.get("success").getAsBoolean()) {
            return jobject.getAsJsonObject("result").get("display_name").getAsString();
        }
        return null;
    }

    private List<String> getThemes(Model model, Resource dataset) {
        List<String> ret = new ArrayList<>();
        NodeIterator nodeIterator = model.listObjectsOfProperty(dataset, DCAT.theme);
        while (nodeIterator.hasNext()) {
            RDFNode rdfNode = nodeIterator.nextNode();
            if (rdfNode.isResource()) {
                String[] split = rdfNode.asResource().getURI().split("/");
                ret.add(split[split.length - 1]);
            } else
                ret.add(rdfNode.asLiteral().getString());
        }
        return ret;
    }

    private synchronized String getOrCreateTagJson(String keyword) {
        if (this.tags.size() == 0) syncTagsCache();
        if (!this.tags.containsKey(keyword)) createTag(keyword);
        return tags.get(keyword);
    }

    private void syncTagsCache() {
        try {
            String apiUrl = CKAN_URL + "/api/3/action/tag_list?vocabulary_id=" + this.vocabulary_id;
            JsonObject jobject = getJsonObjectFromAPI(apiUrl);
            if (jobject.get("success").getAsBoolean()) {
                JsonArray result = jobject.getAsJsonArray("result");
                for (JsonElement tag : result) {
                    String tagString = tag.getAsString();
                    String displayName = getTagDisplayName(tagString);
                    if (displayName != null) {
                        Map<String, String> tmp = new HashMap<>();
                        tmp.put("name", tagString);
                        tmp.put("vocabulary_id", this.vocabulary_id);
                        this.tags.put(displayName, new Gson().toJson(tmp));
                    }
                }
            } else {
                throw new Exception("success is false");
            }
        } catch (Exception ex) {
            logger.error("Error in fetching licenses ", ex);
        }
    }

    private String getTagDisplayName(String tag) throws Exception {
        String apiUrl = CKAN_URL + "/api/3/action/tag_show?id=" + tag
                + "&vocabulary_id=" + this.vocabulary_id;
        JsonObject jobject = getJsonObjectFromAPI(apiUrl);
        if (jobject.get("success").getAsBoolean()) {
            return jobject.getAsJsonObject("result").get("display_name").getAsString();
        }
        return null;
    }

    private void createTag(String keyword) {
        String apiUrl = CKAN_URL + "/api/3/action/tag_create";
        List<NameValuePair> params = new ArrayList<>();
        String tagName = keyword.substring(0, Math.min(100, keyword.length())).toLowerCase();
        params.add(new BasicNameValuePair("name", tagName));
        params.add(new BasicNameValuePair("display_name", keyword));
        params.add(new BasicNameValuePair("vocabulary_id", this.vocabulary_id));

        String json = null;
        try {
            JsonObject jobject = callPostCKAN(apiUrl, params);
            if (jobject.get("success").getAsBoolean()) {
                Map<String, String> tmp = new HashMap<>();
                tmp.put("name", tagName);
                tmp.put("vocabulary_id", this.vocabulary_id);
                this.tags.put(keyword, new Gson().toJson(tmp));
            } else {
                throw new Exception("success is false");
            }
        } catch (Exception ex) {
            logger.error("Exception in createTag", ex);
        }
    }

    private JsonObject callPostCKAN(String apiUrl, List<NameValuePair> params) throws Exception {
        JsonObject jobject;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(apiUrl);
            httpPost.setHeader("Authorization", API_KEY);
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            CloseableHttpResponse response = client.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) throw new Exception("status code is not 200, status code = " + statusCode);
            InputStream content = response.getEntity().getContent();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(content));
            StringBuilder jsonLine = new StringBuilder();
            String inputLine;
            while ((inputLine = bufferedReader.readLine()) != null) jsonLine.append(inputLine);
            JsonElement jelement = new JsonParser().parse(jsonLine.toString());
            jobject = jelement.getAsJsonObject();
        }
        return jobject;
    }

    private void fireAndForgetCallPostCKAN(String apiUrl, String json) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(apiUrl);
            httpPost.setHeader("Authorization", API_KEY);
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Accept", "*/*");
            httpPost.setHeader("User-Agent", "Mozilla/5.0");
            httpPost.setHeader("cache-control","no-cache");
            httpPost.setEntity(new StringEntity(json, "UTF-8"));
            CloseableHttpResponse response = client.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200)
                logger.error("status code is not 200, status code = " + statusCode);
        } catch (IOException e) {
            logger.error("Error in calling CKAN POST CALL", e);
        }
    }

    private synchronized void createVocabularyId() throws Exception {

        String vocabularyId = getVocabularyId();
        if (vocabularyId != null) {
            vocabulary_id = vocabularyId;
            return;
        }

        String apiUrl = CKAN_URL + "/api/3/action/vocabulary_create";
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("name", "opalVocabulary"));

        JsonObject jobject = callPostCKAN(apiUrl, params);

        if (jobject.get("success").getAsBoolean()) {
            this.vocabulary_id = jobject.get("result").getAsJsonObject().get("id").getAsString();
        } else {
            throw new Exception("success is false");
        }
    }

    private String getVocabularyId() {
        try {
            String apiUrl = CKAN_URL + "/api/3/action/vocabulary_list";
            JsonObject jobject = getJsonObjectFromAPI(apiUrl);
            if (jobject.get("success").getAsBoolean()) {
                JsonArray result = jobject.getAsJsonArray("result");
                for (JsonElement x : result) {
                    if (((JsonObject) x).get("name").getAsString().equals("opalVocabulary"))
                        return ((JsonObject) x).get("id").getAsString();
                }
            } else {
                throw new Exception("success is false");
            }
        } catch (Exception ex) {
            logger.error("Error in fetching licenses ", ex);
        }
        return null;
    }

    private JsonObject getJsonObjectFromAPI(String apiUrl) throws IOException {
        URL obj = new URL(apiUrl);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.getResponseCode();

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) response.append(inputLine);
        in.close();

        String jsonLine = response.toString();

        JsonElement jelement = new JsonParser().parse(jsonLine);
        return jelement.getAsJsonObject();
    }

    private List<String> getKeywords(Model model, Resource dataset) {
        List<String> ret = new ArrayList<>();
        NodeIterator nodeIterator = model.listObjectsOfProperty(dataset, keyword);
        while (nodeIterator.hasNext())
            ret.add(nodeIterator.nextNode().asLiteral().getString());
        return ret;
    }

    private String getObjectValue(Model model, Resource subject, Property property) {
        NodeIterator nodeIterator = model.listObjectsOfProperty(subject, property);
        if (nodeIterator.hasNext()) {
            RDFNode rdfNode = nodeIterator.nextNode();
            if (rdfNode.isLiteral()) return rdfNode.asLiteral().getString();
            return rdfNode.asResource().getURI();
        }
        return null;
    }

    private List<String> getObjectValueList(Model model, Resource subject, Property property) {
        List<String> ret = new ArrayList<>();
        NodeIterator nodeIterator = model.listObjectsOfProperty(subject, property);
        while (nodeIterator.hasNext()) {
            RDFNode rdfNode = nodeIterator.nextNode();
            if (rdfNode.isLiteral()) ret.add(rdfNode.asLiteral().getString());
            ret.add(rdfNode.asResource().getURI());
        }
        return ret;
    }

    public static void main(String[] args) {
        try {
            Model model = RDFDataMgr.loadModel("/home/afshin/Desktop/c.ttl");
            CKANWriter ckanWriter = new CKANWriter();
            ckanWriter.CKAN_URL= "http://localhost:5000";
            ckanWriter.API_KEY = "bd79d579-290d-4f72-9d74-d81aa41f4243";
            ckanWriter.licenseRegister();
            String json = ckanWriter.getModelJson(model);
            String url =  "http://localhost:5000" + "/api/3/action/package_create";
            ckanWriter.fireAndForgetCallPostCKAN(url, json);
        } catch (Exception e) {
            logger.error("An error occurred in dumping model", e);
        }
    }
}
