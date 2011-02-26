package net.ooici.siamci.impl;

import net.ooici.siamci.ISiamCiAdapter;
import net.ooici.siamci.ISiamCiFactory;
import net.ooici.siamci.impl.gpb.SiamCiAdapterGpb;
import net.ooici.siamci.impl.ionmsg.SiamCiAdapterIonMsg;

/**
 * Default ISiamCiFactory implementation.
 * It provides the ISiamCiAdapter implementation.
 *  
 * @author carueda
 */
public class SiamCiFactoryImpl implements ISiamCiFactory {

	/** Currently set to false as the implementation using the ION messaging APIs is incomplete.
	 *  The alternative implementation manipulates the GPB messages directly. 
	 */
	private static final boolean USE_ION_MESSAGING = false;

	public ISiamCiAdapter createSiamCiAdapter() {
		if ( USE_ION_MESSAGING ) {
			return new SiamCiAdapterIonMsg();
		}
		else {
			return new SiamCiAdapterGpb();
		}
	}

}
