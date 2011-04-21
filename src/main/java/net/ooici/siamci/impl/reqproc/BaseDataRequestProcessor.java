package net.ooici.siamci.impl.reqproc;

import java.util.Map;

import org.mbari.siam.core.BaseInstrumentService;

import net.ooici.siamci.IDataManagers;
import net.ooici.siamci.IDataRequestProcessor;

/**
 * Base class for processors that need to access a data manager.
 * 
 * @author carueda
 */
public abstract class BaseDataRequestProcessor extends BaseRequestProcessor
        implements IDataRequestProcessor {

    protected IDataManagers dataManagers;

    public void setDataManagers(IDataManagers dataManagers) {
        this.dataManagers = dataManagers;
    }

    /**
     * Gets the full name for the requested channel in a form compliant with the
     * "turbineName" used by SIAM. See {@link BaseInstrumentService#run()}. This
     * name is composed as follows: <br/> {@code serviceName.replace(' ', '_') + "-"
     * + isiID} <br/>
     * where serviceName and isiID are the value of the corresponding properties
     * 'serviceName' and 'isiID'.
     * 
     * @param props
     *            the properties obtained from the intrument.
     * @return the "dataTurbine" name
     * @throws Exception
     *             if any of the required properties is missing.
     */
    protected static String _getTurbineName(Map<String, String> props,
            String channelName) throws Exception {
        String serviceName = props.get("serviceName");
        String isiID = props.get("isiID");

        if (serviceName == null || serviceName.trim().length() == 0) {
            throw new Exception("'serviceName' property no associated with instrument");
        }
        if (isiID == null || isiID.trim().length() == 0) {
            throw new Exception("'isiID' property no associated with instrument");
        }
        serviceName = serviceName.trim();
        isiID = isiID.trim();

        String turbineName = serviceName.replace(' ', '_') + "-" + isiID + "/"
                + channelName;
        return turbineName;

    }

}
