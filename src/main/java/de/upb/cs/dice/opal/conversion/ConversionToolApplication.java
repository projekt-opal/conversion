package de.upb.cs.dice.opal.conversion;

import de.upb.cs.dice.opal.conversion.converter.HtmlToRdf;
import de.upb.cs.dice.opal.conversion.converter.MCloud;
import de.upb.cs.dice.opal.conversion.converter.MCloudConfig;
import de.upb.cs.dice.opal.conversion.converter.MCloudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

@SpringBootApplication
public class ConversionToolApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ConversionToolApplication.class, args);
    }

    @Autowired
    private HtmlToRdf htmlToRdf;

    @Autowired
    private MCloudService mCloudService;

    @Override
    public void run(String... args) {
        try {
            String[] names = new String[]{"133","209","245","292","378","49","600","606","84"};
            for(String x: names) {
                File file = new ClassPathResource(String.format("static/data/%s/Suche - mCLOUD.html", x)).getFile();
                htmlToRdf.convert(file, String.format("opal:ds_%s",x));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @PostConstruct
    public void initialDB() {
        mCloudService.save(new MCloudConfig("Klima und Wetter", MCloud.climateAndWeather.getURI()));
        mCloudService.save(new MCloudConfig("Bahn", MCloud.bahn.getURI()));
        mCloudService.save(new MCloudConfig("Straßen", MCloud.street.getURI()));
        mCloudService.save(new MCloudConfig("Wasserstraßen und Gewässer", MCloud.waterwaysAndWaters.getURI()));
        mCloudService.save(new MCloudConfig("Luft- und Raumfahrt", MCloud.aerospace.getURI()));
        mCloudService.save(new MCloudConfig("Infrastruktur", MCloud.infrastructure.getURI()));
    }
}
