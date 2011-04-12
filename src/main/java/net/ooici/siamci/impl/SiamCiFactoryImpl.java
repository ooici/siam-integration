package net.ooici.siamci.impl;

import net.ooici.siamci.IRequestProcessors;
import net.ooici.siamci.ISiamCiAdapter;
import net.ooici.siamci.ISiamCiFactory;
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

	/**
	 * true: to use implementation based on ION messaging APIs. false: to use
	 * alternative implementation that manipulates the GPB messages directly.
	 * 
	 * As of 3/7/11 set to true.
	 */
	private static final boolean USE_ION_MESSAGING = true;

	public ISiam createSiam(String host) throws Exception {
		return new Siam(host);
	}

	public IAsyncSiam createAsyncSiam(ISiam siam) throws Exception {
		return new AsyncSiam(siam);
	}

	public IRequestProcessors createRequestProcessors(ISiam siam) {
		return new RequestProcessors(siam);
	}

	public ISiamCiAdapter createSiamCiAdapter(String brokerHost,
			int brokerPort, String queueName,
			IRequestProcessors requestProcessors) {
		if (USE_ION_MESSAGING) {
			return new SiamCiAdapterIonMsg(brokerHost, brokerPort, queueName,
					requestProcessors);
		}
		else {
			// Old code, not maintained
			return null;
		}
	}

}
