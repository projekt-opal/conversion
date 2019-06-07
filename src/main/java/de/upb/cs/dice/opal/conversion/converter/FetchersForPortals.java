package de.upb.cs.dice.opal.conversion.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FetchersForPortals {

    private final ApplicationContext context;
    private Map<String, DataSetFetcher> fetcherMap = new ConcurrentHashMap<>();

    @Autowired
    public FetchersForPortals(ApplicationContext context) {
        this.context = context;
    }

    public DataSetFetcher getFetcher(String portalName) {
        if(!fetcherMap.containsKey(portalName)) {
            DataSetFetcher dataSetFetcher = context.getBean(DataSetFetcher.class);
            fetcherMap.put(portalName, dataSetFetcher);
        }
        return fetcherMap.get(portalName);
    }


}
