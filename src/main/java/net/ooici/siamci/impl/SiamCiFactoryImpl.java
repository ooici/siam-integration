package net.ooici.siamci.impl;

import net.ooici.siamci.IRequestProcessor;
import net.ooici.siamci.ISiam;
import net.ooici.siamci.ISiamCiAdapter;
import net.ooici.siamci.ISiamCiFactory;
import net.ooici.siamci.impl.gpb.SiamCiAdapterGpb;
import net.ooici.siamci.impl.ionmsg.SiamCiAdapterIonMsg;
import siam.Siam;

/**
 * Default ISiamCiFactory implementation.
 * It provides the implementation of central interfaces.
 *  
 * @author carueda
 */
public class SiamCiFactoryImpl implements ISiamCiFactory {

	/** 
	 * true: to use implementation based on ION messaging APIs.
	 * false: to use alternative implementation that manipulates the GPB messages directly.
	 * 
	 * As of 3/7/11 set to true.
	 */
	private static final boolean USE_ION_MESSAGING = true;
	
	
	public ISiam createSiam(String host) throws Exception {
		return new Siam(host);
	}

	public IRequestProcessor createRequestProcessor(ISiam siam) {
		return new RequestProcessor(siam);
	}

	public ISiamCiAdapter createSiamCiAdapter(IRequestProcessor requestProcessor) {
		if ( USE_ION_MESSAGING ) {
			return new SiamCiAdapterIonMsg(requestProcessor);
		}
		else {
			return new SiamCiAdapterGpb(requestProcessor);
		}
	}

}
