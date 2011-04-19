package net.ooici.siamci.impl.data;

import java.util.HashMap;
import java.util.Map;

import net.ooici.siamci.IDataManager;
import net.ooici.siamci.IDataManagers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides data manager instances.
 * 
 * @author carueda
 */
public class DataManagers implements IDataManagers {

    private static final Logger log = LoggerFactory
            .getLogger(DataManagers.class);

    private Map<String, IDataManager> dataManagers = new HashMap<String, IDataManager>();

    public IDataManager getDataManager(String rbnbHost, String baseClientName) {
        //
        // this key is internal; any combination will do.
        final String key = rbnbHost + "$$" + baseClientName;
        
        IDataManager dataManager = dataManagers.get(key);
        if (dataManager == null) {
            synchronized (dataManagers) {
                dataManager = dataManagers.get(key);
                if (dataManager == null) {
                    dataManager = new DataManager(rbnbHost, baseClientName);
                    dataManagers.put(key, dataManager);
                }
            }
        }
        log.info("Returning dataManager (" +dataManager.getClass().getName()+ ")");
        return dataManager;
    }
}
