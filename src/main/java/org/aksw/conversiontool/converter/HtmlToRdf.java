package org.aksw.conversiontool.converter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Scanner;

@Component
public class HtmlToRdf {

    private static final Logger logger = LoggerFactory.getLogger(HtmlToRdf.class);

    private static String[] cssSelectors = new String[]{
            "#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-18.columns > div > h3\n",
            "#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-18.columns > div > p\n",
            "#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-18.columns > div > div > div > div > div.small-20.columns > a > span.link-download\n",
            "#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-18.columns > div > div > div > div > div.small-4.columns > span\n",
            "#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-6.columns > div > p:nth-child(2) > span > a\n",
            "#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-6.columns > div > div > span.tag-theme-text\n",
            "#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-6.columns > div > p:nth-child(6) > span\n",
            "#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-6.columns > div > p:nth-child(8) > span\n",
            "#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-6.columns > div > p:nth-child(10) > span:nth-child(2)\n",
            "#portlet_mcloudsearchportlet > div > div > div > div.content-page.datail-page > div.row > div.small-24.xlarge-6.columns > div > p:nth-child(12) > a"
    };

    public void convert(File file) {
        try {
            Document document = Jsoup.parse(new Scanner(file).useDelimiter("\\A").next());
            for(String cssQuery : cssSelectors) {
                Elements elements = document
                        .select(cssQuery);
                logger.info("Elements \n{}", elements);
            }
        } catch (Exception ex) {
            logger.error("Error {}", ex);
        }
    }
}
