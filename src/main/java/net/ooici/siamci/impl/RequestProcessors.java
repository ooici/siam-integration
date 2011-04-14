package net.ooici.siamci.impl;

import net.ooici.play.InstrDriverInterface.Command;
import net.ooici.siamci.IDataManagers;
import net.ooici.siamci.IDataRequestProcessor;
import net.ooici.siamci.IPublisher;
import net.ooici.siamci.IRequestProcessor;
import net.ooici.siamci.IRequestProcessors;
import net.ooici.siamci.impl.reqproc.BaseRequestProcessor;
import net.ooici.siamci.impl.reqproc.EchoRequestProcessor;
import net.ooici.siamci.impl.reqproc.FetchParamsRequestProcessor;
import net.ooici.siamci.impl.reqproc.GetLastSampleRequestProcessor;
import net.ooici.siamci.impl.reqproc.GetStatusRequestProcessor;
import net.ooici.siamci.impl.reqproc.ListPortsRequestProcessor;
import net.ooici.siamci.impl.reqproc.SetParamsRequestProcessor;
import net.ooici.siamci.utils.ScUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import siam.IAsyncSiam;
import siam.ISiam;

import com.google.protobuf.GeneratedMessage;

/**
 * Provides the {@link IRequestProcessor} instance for each type of request
 * supported by the SIAM-CI integration prototype.
 * 
 * @author carueda
 */
class RequestProcessors implements IRequestProcessors {

    private static final Logger log = LoggerFactory
            .getLogger(RequestProcessors.class);

    /**
     * The request processors are gathered in this enumeration.
     */
    private enum RP {

        echo(new EchoRequestProcessor()),

        list_ports(new ListPortsRequestProcessor()),

        get_status(new GetStatusRequestProcessor()),

        get_last_sample(new GetLastSampleRequestProcessor()),

        fetch_params(new FetchParamsRequestProcessor()),

        set_params(new SetParamsRequestProcessor()),

        ;

        private BaseRequestProcessor reqProc;

        RP(BaseRequestProcessor reqProc) {
            this.reqProc = reqProc;
        }

    }

    RequestProcessors(ISiam siam) {
        for (RP rp : RP.values()) {
            rp.reqProc.setSiam(siam);
        }
        log.debug("instance created.");
    }

    public void setAsyncSiam(IAsyncSiam asyncSiam) {
        for (RP rp : RP.values()) {
            rp.reqProc.setAsyncSiam(asyncSiam);
        }
    }

    public void setPublisher(IPublisher publisher) {
        for (RP rp : RP.values()) {
            rp.reqProc.setPublisher(publisher);
        }
    }

    public void setDataManagers(IDataManagers dataManagers) {
        for (RP rp : RP.values()) {
            if (rp.reqProc instanceof IDataRequestProcessor) {
                ((IDataRequestProcessor) rp.reqProc)
                        .setDataManagers(dataManagers);
            }
        }
    }

    /**
     * Returns the request processor for the given request ID
     * 
     * @param id
     *            ID of the desired processor
     * @return the desired processor, or a special processor if the ID is not
     *         recognized
     */
    public IRequestProcessor getRequestProcessor(String id) {
        try {
            RP rp = RP.valueOf(id);
            return rp.reqProc;
        }
        catch (IllegalArgumentException e) {
            return _unrecognizedRequestProcessor;
        }
    }

    private static BaseRequestProcessor _unrecognizedRequestProcessor = new BaseRequestProcessor() {
        public GeneratedMessage processRequest(int reqId, Command cmd) {
            String cmdName = cmd.getCommand();
            String description = _rid(reqId) + "Command '" + cmdName
                    + "' not recognized";
            log.debug(description);
            return ScUtils.createFailResponse(description);
        }
    };

}
