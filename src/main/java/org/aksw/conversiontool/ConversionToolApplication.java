package org.aksw.conversiontool;

import org.aksw.conversiontool.converter.HtmlToRdf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

@SpringBootApplication
public class ConversionToolApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ConversionToolApplication.class, args);
    }

    @Autowired
    private HtmlToRdf htmlToRdf;

    @Override
    public void run(String... args) {
        try {
            String[] names = new String[]{"133","209","245","292","378","49","600","606","84"};
            for(String x: names) {
                File file = new ClassPathResource(String.format("static/data/%s/Suche - mCLOUD.html", x)).getFile();
                htmlToRdf.convert(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
