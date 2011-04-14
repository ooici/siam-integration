package net.ooici.siamci;


/**
 * Interface for processors that need to access a data manager.
 * 
 * @author carueda
 */
public interface IDataRequestProcessor extends IRequestProcessor {

    public void setDataManagers(IDataManagers dataManagers);
}
