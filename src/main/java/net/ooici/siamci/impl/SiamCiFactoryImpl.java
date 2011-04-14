package net.ooici.siamci.impl;

import net.ooici.siamci.IDataManagers;
import net.ooici.siamci.IRequestProcessors;
import net.ooici.siamci.ISiamCiAdapter;
import net.ooici.siamci.ISiamCiFactory;
import net.ooici.siamci.impl.data.DataManagers;
import net.ooici.siamci.impl.ionmsg.SiamCiAdapterIonMsg;
import siam.AsyncSiam;
import siam.IAsyncSiam;
import siam.ISiam;
import siam.Siam;

/**
 * Default ISiamCiFactory implementation. It provides the implementation of
 * central interfaces.
 * 
 * @author carueda
 */
public class SiamCiFactoryImpl implements ISiamCiFactory {

    private final IDataManagers dataManagers = new DataManagers();

    public ISiam createSiam(String host) throws Exception {
        return new Siam(host);
    }

    public IAsyncSiam createAsyncSiam(ISiam siam) throws Exception {
        return new AsyncSiam(siam);
    }

    public IRequestProcessors createRequestProcessors(ISiam siam) {
        return new RequestProcessors(siam);
    }

    public IDataManagers getDataManagers() {
        return dataManagers;
    }

    public ISiamCiAdapter createSiamCiAdapter(String brokerHost,
            int brokerPort, String queueName, String exchangeName,
            IRequestProcessors requestProcessors) {

        return new SiamCiAdapterIonMsg(brokerHost, brokerPort, queueName,
                exchangeName, requestProcessors);
    }

}
