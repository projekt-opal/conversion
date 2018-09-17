package org.aksw.conversiontool.converter;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Scanner;

@Component
public class HtmlToRdf {

    private static final Logger logger = LoggerFactory.getLogger(HtmlToRdf.class);

    private final LicenseUtility licenseUtility;
    private final MCloudService mCloudService;


    @Autowired
    public HtmlToRdf(LicenseUtility licenseUtility, MCloudService mCloudService) {
        this.licenseUtility = licenseUtility;
        this.mCloudService = mCloudService;
    }

    private static Record[] records = new Record[]{
            new Record("#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-18.columns > div > h3"
                    ,DCTerms.title),
            new Record("#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-18.columns > div > p"
                    ,DCTerms.description),
            new Record("#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-18.columns > div > div > div > div > div.small-20.columns > a > span.link-download"
                    ,DCAT.downloadURL),
            new Record("#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-18.columns > div > div > div > div > div.small-4.columns > span"
                    ,DC_10.format), //todo DCTems.format also exist
            new Record("#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-6.columns > div > p:nth-child(2) > span > a"
                    ,DCTerms.publisher),
            new Record("#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-6.columns > div > div > span.tag-theme-text"
                    ,DCAT.theme),
            new Record("#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-6.columns > div > p:nth-child(6) > span"
                    , DCTerms.temporal),
            new Record("#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-6.columns > div > p:nth-child(8) > span"
                    ,DCTerms.modified), // TODO: 14.09.18 Modification is for distribution not the metaData
            new Record("#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-6.columns > div > p:nth-child(10) > span:nth-child(2)"
                    , RDFS.label),
            new Record("#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-6.columns > div > p:nth-child(12) > a",
                    DCTerms.license)
    };

    public void convert(File file, String subject) {
        try {
            Document document = Jsoup.parse(new Scanner(file).useDelimiter("\\A").next());
            for(Record record : records) {
                Elements elements = document.select(record.getSelector());
                if(record.getProperty().equals(DCTerms.license)) {
                    String href = elements.first().attr("href");
                    Resource license = licenseUtility.getLicense(href);
                    System.out.println(subject + "," + DCTerms.license + "," + license );// TODO: 14.09.18 In Some cases license is NULL
                } else if(record.getProperty().equals(DCAT.theme)) {
                    Resource resource = null;
                    try {
                        String s = elements.first().text();
                        MCloudConfig mCloudConfig = mCloudService.findByKey(s);
                        resource = MCloud.getResourceOfUri(mCloudConfig.getResourceUri());
                    } catch (Exception e) {
                        logger.error("Error {}", e);
                    }
                    System.out.println(subject + "," + DCAT.theme + "," + resource);
                } else
                    System.out.println(subject + "," + record.getProperty() + "," + elements);
            }
        } catch (Exception ex) {
            logger.error("Error {}", ex);
        }
    }
}
