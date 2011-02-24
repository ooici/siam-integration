package net.ooici.siamci.impl;

import net.ooici.siamci.ISiamCiAdapter;
import net.ooici.siamci.ISiamCiFactory;

/**
 * Deafult ISiamCiFactory implementation
 * @author carueda
 *
 */
public class SiamCiFactoryImpl implements ISiamCiFactory {

	public ISiamCiAdapter createSiamCiAdapter() {
		// TODO
		return new SiamCiAdapterImpl();
	}

}
