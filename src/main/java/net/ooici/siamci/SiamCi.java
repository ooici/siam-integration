package net.ooici.siamci;

import net.ooici.siamci.impl.SiamCiFactoryImpl;
import siam.IAsyncSiam;
import siam.ISiam;

/**
 * The central place where the main objects are created and cached for the
 * application. It uses the default factory {@link SiamCiFactoryImpl}.
 * 
 * @author carueda
 */
public class SiamCi {

    /**
     * Parameters for the application.
     * 
     * @author carueda
     */
    static class SiamCiParameters {
        final String siamHost;
        final String brokerHost;
        final int brokerPort;
        final String queueName;
        final String exchangeName;

        SiamCiParameters(String siamHost, String brokerHost, int brokerPort,
                String queueName, String exchangeName) {
            super();
            this.siamHost = siamHost;
            this.brokerHost = brokerHost;
            this.brokerPort = brokerPort;
            this.queueName = queueName;
            this.exchangeName = exchangeName;
        }

        public String toString() {
            return String.format("siamHost='%s', broker='%s:%s', queueName='%s', exchangeName='%s'",
                    siamHost,
                    brokerHost,
                    brokerPort,
                    queueName,
                    exchangeName);
        }
    }

    private static SiamCi instance = null;

    /**
     * Creates the instance of this class.
     * 
     * @param params
     * @throws Exception
     */
    static void createInstance(SiamCiParameters params) throws Exception {
        if (instance != null) {
            throw new IllegalStateException("instance already created");
        }
        instance = new SiamCi(params);
    }

    /**
     * Gets the instance of this class.
     * 
     * @return the instance of this class.
     */
    public static SiamCi instance() {
        if (instance == null) {
            throw new IllegalStateException("instance has not been created yet");
        }

        return instance;
    }

    // /////////////////////////////////////
    // instance
    // /////////////////////////////////////

    private final ISiamCiFactory siamCiFactory;

    private final ISiam siam;
    private final IAsyncSiam asyncSiam;
    private final IRequestProcessors requestProcessors;
    private final ISiamCiAdapter siamCiAdapter;

    private SiamCi(SiamCiParameters params) throws Exception {

        siamCiFactory = new SiamCiFactoryImpl();
        siam = siamCiFactory.createSiam(params.siamHost);
        asyncSiam = siamCiFactory.createAsyncSiam(siam);

        requestProcessors = siamCiFactory.createRequestProcessors(siam);
        requestProcessors.setAsyncSiam(asyncSiam);
        requestProcessors.setDataManagers(siamCiFactory.getDataManagers());

        siamCiAdapter = siamCiFactory.createSiamCiAdapter(params.brokerHost,
                params.brokerPort,
                params.queueName,
                params.exchangeName,
                requestProcessors);
    }

    /**
     * Returns the factory for the application.
     * 
     * @return the factory for the application.
     */
    ISiamCiFactory getSiamCiFactory() {
        return siamCiFactory;
    }

    /**
     * Gets the SIAM hight-level interface implementation
     */
    ISiam getSiam() {
        return siam;
    }

    /**
     * Gets the adapter service.
     */
    ISiamCiAdapter getSiamCiAdapter() {
        return siamCiAdapter;
    }

    /**
     * Gets the main publisher of the application.
     * 
     * <p>
     * NOTE: Should be called after starting the adapter service, otherwise null
     * may be returned.
     * 
     * @return the main publisher of the application.
     */
    public IPublisher getPublisher() {
        return getSiamCiAdapter().getPublisher();
    }
}
