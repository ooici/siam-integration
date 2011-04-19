import siam.IAsyncSiam;
import siam.ISiam;
import net.ooici.siamci.IDataManagers;
import net.ooici.siamci.IPublisher;
import net.ooici.siamci.IRequestProcessor;
import net.ooici.siamci.IRequestProcessors;
import net.ooici.siamci.ISiamCiAdapter;



/**
 * @__opt attributes
 * @__opt operations
 * @hidden
 */
class UMLOptions {}


/**
    @navassoc - <uses> - SiamCiFactoryImpl
    @navassoc - <starts> - ISiamCiAdapter
*/
 class SiamCiMain {}

///**
//    @navassoc - <creates> - ISiam
//    @navassoc - <creates> - IAsyncSiam
//    @navassoc - <creates> - IRequestProcessors
//    @navassoc - <creates> - ISiamCiAdapter
// */
/**
 * @opt operations
 */
interface ISiamCiFactory {
    public ISiam createSiam(String host) throws Exception;

    public IAsyncSiam createAsyncSiam(ISiam siam) throws Exception;

    public IRequestProcessors createRequestProcessors(ISiam siam);


    public ISiamCiAdapter createSiamCiAdapter(String brokerHost,
            int brokerPort, String queueName, String exchangeName,
            IRequestProcessors requestProcessors);
    
}

    
 class SiamCiFactoryImpl implements ISiamCiFactory {}

 interface ISiamCiFactory {}
 interface ISiamCiAdapter {}
 
 /**
 @__navassoc - <gets> - IRequestProcessor
 @opt operations
 */
interface IRequestProcessors {
    public IRequestProcessor getRequestProcessor(String id);
}

/**
@navassoc - <generates> - GeneratedMessage
 */
 interface IRequestProcessor {}


interface IAsyncSiam{}
interface ISiam{}



/**
@navassoc - <creates> - SiamCiServerIonMsg
*/
class SiamCiAdapterIonMsg implements ISiamCiAdapter{}

/**
@navassoc - <uses> - IRequestProcessors
*/
class SiamCiServerIonMsg implements IPublisher, Runnable {}



class GeneratedMessage{}

/**
@navassoc - <publishes> - GeneratedMessage
*/
interface IPublisher {}


abstract class BaseRequestProcessor implements IRequestProcessor {}

