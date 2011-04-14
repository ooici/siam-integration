package net.ooici.siamci.impl.reqproc;

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

}
