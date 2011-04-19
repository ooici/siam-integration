

/**
 * @__opt attributes
 * @__opt operations
 * @hidden
 */
class UMLOptions {}


/**
@navassoc - <uses> - MessageClient
*/
class SiamCiAdapterProxy {}

/**
    @navassoc - <uses> - SiamCiAdapterProxy
 */
class SiamInstrumentDriver extends InstrumentDriver {}


/**
@navassoc - <client-of> - SiamInstrumentDriver
*/
class SiamInstrumentDriverClient extends InstrumentDriverClient{}

class SiamCiReceiverService extends ServiceProcess{}

/**
@navassoc - <client-of> - SiamCiReceiverService
*/
class SiamCiReceiverServiceClient extends ServiceClient{}


 