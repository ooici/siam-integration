#!/usr/bin/env python

"""
@file siamci/siam_driver.py
@author Carlos Rueda
@brief Driver code for SIAM
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)
import logging

from twisted.internet import defer  #, reactor

from ion.core.process.process import ProcessFactory

from ion.agents.instrumentagents.instrument_driver import InstrumentDriver
from ion.agents.instrumentagents.instrument_driver import InstrumentDriverClient
from ion.agents.instrumentagents.instrument_constants import InstErrorCode
from ion.agents.instrumentagents.instrument_constants import ObservatoryState

from siamci.siamci_constants import SiamDriverEvent
from siamci.siamci_constants import SiamDriverState
from siamci.siamci_constants import SiamDriverChannel
from siamci.siamci_constants import SiamDriverCommand
from siamci.siamci_constants import SiamDriverAnnouncement
from siamci.siamci_constants import SiamDriverCapability
from siamci.siamci_constants import SiamDriverMetadataParameter
from siamci.siamci_constants import SiamDriverStatus

from siamci.siamci_proxy import SiamCiAdapterProxy

from siamci.util.conversion import get_python_content
from siamci.util.tcolor import red, blue

from net.ooici.play.instr_driver_interface_pb2 import Command, SuccessFail, OK, ERROR


       
class SiamInstrumentDriver(InstrumentDriver):
    """
    Instrument driver interface to a SIAM enabled instrument.
    Main operations are supported by the core class SiamCiAdapterProxy.
    """

    def __init__(self, *args, **kwargs):
        
        """
        Creates an instance of the driver. This instance will be initially
        unconfigured (ie., with no specific instrument associated) until
        the configure operation is called and completed.
        """
        
        InstrumentDriver.__init__(self, *args, **kwargs)
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug("\nSiamDriver __init__: spawn_args = " +str(self.spawn_args))
        
        
        """
        A flag indicating whether notifications to self.proc_supid should be sent
        when entering states. It is assigned the value returned by 
        self.spawn_args.get('notify_agent', False).
        @NOTE:
            This is to avoid "ERROR:Process does not define op=driver_event_occurred" messages
            when self.proc_supid does not correspond to an instrument agent.
            There might be a more appropriate way to accomplish this behavior. I'm using this
            mechanism in test_siam_agent.py to include the corresponding spawn arg.
        """
        self.notify_agent = self.spawn_args.get('notify_agent', False)
         
        
        
        """
        Used to connect to the SIAM-CI adapter service. More specifically, this is
        the routing key (queue) where the SIAM-CI adapter service (java) is listening
        for requests.
        """
        self.pid = None
        
        
        """
        Instrument port. A concrete instrument in the SIAM node is identified by its
        corresponding port.
        """
        self.port = None
        
            
        """
        Will be a SiamCiAdapterProxy(pid, port) instance upon configuration
        """
        self.siamci = None


        """
        Used in certain non-data operations to enable handling of asynchronous 
        notifications from the SIAM-CI adapter service
        """
        self.publish_stream = None

        """
        Used in data related operations to enable handling of asynchronous 
        notifications from the SIAM-CI adapter service
        """
        self.data_publish_stream = str(self.id)

        self._initialize()


        
        
    def _initialize(self):
        """
        Set the configuration to an initialized, unconfigured state.
        """
        self.pid = None
        self.port = None
        self.current_state = SiamDriverState.UNCONFIGURED
    

    
    ###########################################################################
    # <Process lifecycle methods>
    
    @defer.inlineCallbacks
    def plc_init(self):
        """
        Process lifecycle initialization.
        """
        yield
        
        # Set initial state.
        self.current_state = SiamDriverState.UNCONFIGURED
        log.debug("SiamDriver plc_init: self.current_state = " +str(self.current_state))
        
                

    @defer.inlineCallbacks
    def plc_terminate(self):
        log.debug("SiamDriver plc_terminate")
        """
        Process lifecycle termination.
        """
        yield

    # </Process lifecycle methods>
    ###########################################################################



    @defer.inlineCallbacks
    def op_get_state(self, content, headers, msg):
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('op_get_state: called. current_state = ' + str(self.current_state))
        
        cur_state = self.current_state
        yield self.reply_ok(msg, cur_state)


       
    @defer.inlineCallbacks
    def op_configure(self, content, headers, msg):
        
        assert(isinstance(content, dict)), 'Expected dict content.'
        params = content.get('params', None)
        assert(isinstance(params, dict)), 'Expected dict params.'
        
        # Set up the reply message and validate the configuration parameters.
        # Reply with the error message if the parameters not valid.
        reply = {'success':None, 'result':params}
        
        if self.current_state != SiamDriverState.UNCONFIGURED:
            reply['success'] = InstErrorCode.INCORRECT_STATE
            log.error('op_configure: incorrect state for this operation.  current_state = ' + str(self.current_state))
            yield self.reply_ok(msg, reply)
            return
        
        reply['success'] = self._configure(params);
        
        if InstErrorCode.is_error(reply['success']):
            yield self.reply_ok(msg, reply)
            return
        
        self.current_state = SiamDriverState.DISCONNECTED
        
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('op_configure: complete. success = %s' % (reply['success']))
            
        yield self.reply_ok(msg, reply)
        
                
                

    def _configure(self, params):
        
        # Validate configuration.
        success = self._validate_configuration(params)
        if InstErrorCode.is_error(success):
            return success

        # Set configuration parameters.
        self.pid = params['pid']
        self.port = params['port']

        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug("_configure: pid = '" + \
                      str(self.pid) + "' port = '" + str(self.port) + "'")
            
        self.siamci = SiamCiAdapterProxy(self.pid, self.port)
        
        return InstErrorCode.OK
    
            
    def _validate_configuration(self, params):
        
        # Get required parameters.
        pid = params.get('pid', None)
        port = params.get('port', None)

        # fail if missing a required parameter.
        if not pid or not port:
            return InstErrorCode.REQUIRED_PARAMETER
        
        return InstErrorCode.OK




    @defer.inlineCallbacks
    def op_initialize(self, content, headers, msg):
        """
        Restore driver to a default, unconfigured state.
        @param content A dict with optional timeout: {'timeout':timeout}.
        @retval A reply message with a dict {'success':success,'result':None}.
        """
        
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('op_initialize: called.  current_state = ' + str(self.current_state))
        
        # Set up the reply and fire an EVENT_INITIALIZE.
        reply = {'success':None,'result':None}    
        
        if self.current_state != SiamDriverState.DISCONNECTED:
            reply['success'] = InstErrorCode.INCORRECT_STATE
            log.error('op_initialize: incorrect state for this operation.  current_state = ' + str(self.current_state))
            yield self.reply_ok(msg, reply)
            return
        
        self._initialize()
        reply['success'] = InstErrorCode.OK
 
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('op_initialize: reply = %s' % (str(reply)))

        yield self.reply_ok(msg, reply)
        
         
        
        
    @defer.inlineCallbacks
    def op_connect(self, content, headers, msg):
        
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('op_connect: called.  current_state = ' + str(self.current_state))
        
        # Set up the reply and fire an EVENT_INITIALIZE.
        reply = {'success':None,'result':None}    
        
        if self.current_state != SiamDriverState.DISCONNECTED:
            reply['success'] = InstErrorCode.INCORRECT_STATE
            log.error('op_connect: incorrect state for this operation.  current_state = ' + str(self.current_state))
            yield self.reply_ok(msg, reply)
            return
        
        self.current_state = SiamDriverState.CONNECTING
        
        if self.notify_agent:
            # Announce the state change to agent.            
            content = {'type':SiamDriverAnnouncement.STATE_CHANGE,'transducer':SiamDriverChannel.INSTRUMENT,
                       'value':SiamDriverState.CONNECTING}
            yield self.send(self.proc_supid,'driver_event_occurred',content)
        
        """
        Start our proxy:
        """
        yield self.siamci.start()
        
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('op_connect: SiamCiProxy started')
        
        self.current_state = SiamDriverState.CONNECTED
        
        if self.notify_agent:
            # Announce the state change to agent.            
            content = {'type':SiamDriverAnnouncement.STATE_CHANGE,'transducer':SiamDriverChannel.INSTRUMENT,
                       'value':SiamDriverState.CONNECTED}
            yield self.send(self.proc_supid,'driver_event_occurred',content)            

        reply['success'] = InstErrorCode.OK
        
        yield self.reply_ok(msg, reply)



    @defer.inlineCallbacks
    def op_disconnect(self, content, headers, msg):
        
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('op_disconnect: called.  current_state = ' + str(self.current_state))
        
        # Set up the reply and fire an EVENT_INITIALIZE.
        reply = {'success':None,'result':None}    
        
        if self.current_state != SiamDriverState.CONNECTED:
            reply['success'] = InstErrorCode.INCORRECT_STATE
            log.error('op_disconnect: incorrect state for this operation.  current_state = ' + str(self.current_state))
            yield self.reply_ok(msg, reply)
            return
        
        self.current_state = SiamDriverState.DISCONNECTING
        
        if self.notify_agent:
            # Announce the state change to agent.            
            content = {'type':SiamDriverAnnouncement.STATE_CHANGE,'transducer':SiamDriverChannel.INSTRUMENT,
                       'value':SiamDriverState.DISCONNECTING}
            yield self.send(self.proc_supid,'driver_event_occurred',content)
        
        """
        @TODO: why calling ''yield self.siamci.stop()'' to stop (terminate) the 
        SiamCiProxy process causes errors?  Here are some of the errors if this
        call is included when running test_siam_driver.py: 
            [process        :780] WARNING:Process bootstrap RPC conv-id=carueda_46740.7#29 timed out!
            [state_object   :113] ERROR:ERROR in StateObject process(event=deactivate)
            [receiver       :169] ERROR:Receiver error: Illegal state change
            [state_object   :132] ERROR:Subsequent ERROR in StateObject error(), ND-ND
        """
#        yield self.siamci.stop()
        
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('op_disconnect: SiamCiProxy stopped -- NOT REALLY -- check source code')
        
        self.current_state = SiamDriverState.DISCONNECTED
        
        if self.notify_agent:
            # Announce the state change to agent.            
            content = {'type':SiamDriverAnnouncement.STATE_CHANGE,'transducer':SiamDriverChannel.INSTRUMENT,
                       'value':SiamDriverState.DISCONNECTED}
            yield self.send(self.proc_supid,'driver_event_occurred',content)            

        reply['success'] = InstErrorCode.OK
        
        yield self.reply_ok(msg, reply)


    @defer.inlineCallbacks
    def op_get_status(self, content, headers, msg):
        
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('op_get_status: called.  current_state = ' + str(self.current_state))
        
        
        assert(isinstance(content,dict)), 'Expected dict content.'
        params = content.get('params',None)
        """
        For 'params', how is that a list, eg., [('all','all)], passed from
        the client gets converted to a tuple here, eg.,  (('all', 'all'),) ?
        """
        
        assert(isinstance(params,(list,tuple))), \
            'Expected list or tuple params in op_get_status'
        assert(all(map(lambda x:isinstance(x,tuple),params))), \
            'Expected tuple elements in params list'
        
        
        reply = self._get_status(params)
        
        yield self.reply_ok(msg, reply)



    def _get_status(self,params):
        """
        Get the instrument and channel status.
        @params A list of (channel,status) keys.
        @retval A dict containing success and status results:
            {'success':success,'result':
            {(chan,arg):(success,val),...,(chan,arg):(success,val)}}
        """
        
        ###############################################################
        ### TODO Proper dispatch of the requested status.
        ### Here is the initial code:      
#        response = yield self.siamci.get_status(params=params)
#        result = response.result
#        reply = {'success':InstErrorCode.OK,'result':result}
        ### But now we are dispaching part of the logic only on the 
        ### python side.
        ###############################################################
        
        
        # Set up the reply message.
        reply = {'success':None,'result':None}
        result = {}
        get_errors = False

        for (chan,arg) in params:
            if SiamDriverChannel.has(chan) and SiamDriverStatus.has(arg):
                # If instrument channel or all.
                if chan == SiamDriverChannel.INSTRUMENT or chan == SiamDriverChannel.ALL:
                    if arg == SiamDriverStatus.DRIVER_STATE or \
                            arg == SiamDriverStatus.ALL:
                        result[(SiamDriverChannel.INSTRUMENT,
                                SiamDriverStatus.DRIVER_STATE)] = \
                            (InstErrorCode.OK, self.current_state)
                    
                    if arg == SiamDriverStatus.OBSERVATORY_STATE or \
                            arg == SiamDriverStatus.ALL:
                        result[(SiamDriverChannel.INSTRUMENT,
                                SiamDriverStatus.OBSERVATORY_STATE)] = \
                            (InstErrorCode.OK,self._get_observatory_state())
                    
                        #
                        # TODO  OTHERS ......
                        #
                        
            # Status or channel key or both invalid.       
            else:
                result[(chan,arg)] = (InstErrorCode.INVALID_STATUS,None)
                get_errors = True

        reply['result'] = result
            
        # Set the overall error state.
        if get_errors:
            reply['success'] = InstErrorCode.GET_DEVICE_ERR

        else:
            reply['success'] = InstErrorCode.OK
            
        return reply


    def _get_observatory_state(self):
        """
        Return the observatory state of the instrument.
        """
        
        curstate = self.current_state
        if curstate == SiamDriverState.DISCONNECTED:
            return ObservatoryState.NONE
        
        elif curstate == SiamDriverState.CONNECTING:
            return ObservatoryState.NONE

        elif curstate == SiamDriverState.DISCONNECTING:
            return ObservatoryState.NONE

        elif curstate == SiamDriverState.ACQUIRE_SAMPLE:
            return ObservatoryState.ACQUIRING

        elif curstate == SiamDriverState.CONNECTED:
            return ObservatoryState.STANDBY

        elif curstate == SiamDriverState.CALIBRATE:
            return ObservatoryState.CALIBRATING

        elif curstate == SiamDriverState.AUTOSAMPLE:
            return ObservatoryState.STREAMING

        elif curstate == SiamDriverState.SET:
            return ObservatoryState.UPDATING

        elif curstate == SiamDriverState.TEST:
            return ObservatoryState.TESTING

        elif curstate == SiamDriverState.UNCONFIGURED:
            return ObservatoryState.NONE

        elif curstate == SiamDriverState.UPDATE_PARAMS:
            return ObservatoryState.UPDATING

        else:
            return ObservatoryState.UNKNOWN





    @defer.inlineCallbacks
    def op_get_capabilities(self, content, headers, msg):
        assert(isinstance(content,dict)), 'Expected dict content.'
        params = content.get('params',None)
        assert(isinstance(params,(list,tuple))), 'Expected list or tuple params.'

        reply = self._get_capabilities(params)
        yield self.reply_ok(msg, reply)

    def _get_capabilities(self,params):

        # Set up the reply message.
        reply = {'success':None,'result':None}
        result = {}
        get_errors = False

        for arg in params:
            if SiamDriverCapability.has(arg):
                
                if arg == SiamDriverCapability.DEVICE_COMMANDS or \
                        arg == SiamDriverCapability.DEVICE_ALL:
                    result[SiamDriverCapability.DEVICE_COMMANDS] = \
                        (InstErrorCode.OK,SiamDriverCommand.list())
                
                if arg == SiamDriverCapability.DEVICE_METADATA or \
                        arg == SiamDriverCapability.DEVICE_ALL:
                    result[SiamDriverCapability.DEVICE_METADATA] = \
                        (InstErrorCode.OK,SiamDriverMetadataParameter.list())
                
#                if arg == SiamDriverCapability.DEVICE_PARAMS or \
#                        arg == SiamDriverCapability.DEVICE_ALL:
#                    result[SiamDriverCapability.DEVICE_PARAMS] = \
#                        (InstErrorCode.OK,self.parameters.keys())
                
                if arg == SiamDriverCapability.DEVICE_STATUSES or \
                        arg == SiamDriverCapability.DEVICE_ALL:
                    result[SiamDriverCapability.DEVICE_STATUSES] = \
                        (InstErrorCode.OK,SiamDriverStatus.list())
            
                if arg == SiamDriverCapability.DEVICE_CHANNELS or \
                        arg == SiamDriverCapability.DEVICE_ALL:
                    result[SiamDriverCapability.DEVICE_CHANNELS] = \
                        (InstErrorCode.OK,SiamDriverChannel.list())

            else:
                result[arg] = (InstErrorCode.INVALID_CAPABILITY,None)
                get_errors = True

        if get_errors:
            reply['success'] = InstErrorCode.GET_DEVICE_ERR
        else:
            reply['success'] = InstErrorCode.OK
        reply['result'] = result
    
        log.debug("_get_capabilities: returning reply: " + str(reply))       
        return reply
    


    @defer.inlineCallbacks
    def op_get(self, content, headers, msg):
        
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('op_get: called.  current_state = ' + str(self.current_state))
            
        assert(isinstance(content,dict)),'Expected dict content.'
        params = content.get('params',None)
        assert(isinstance(params,(list,tuple))),'Expected list or tuple params.'

        
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('op_get: params = ' +\
                      str(params)+ "  publish_stream=" +str(self.publish_stream))
        
        if self.current_state != SiamDriverState.CONNECTED:
            reply['success'] = InstErrorCode.INCORRECT_STATE
            log.error('op_get: incorrect state for this operation.  current_state = ' + str(self.current_state))
            yield self.reply_ok(msg, reply)
            return
        
        if self.publish_stream is None: 
            successFail = yield self.siamci.fetch_params(params)
        else:
            successFail = yield self.siamci.fetch_params(params, publish_stream=self.publish_stream)
            
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('op_get successFail --> ' + str(successFail))
        
        # initialize reply assuming OK
        reply = {'success':InstErrorCode.OK, 'result':None}
        
        if successFail.result != OK:
            reply['success'] = InstErrorCode.GET_DEVICE_ERR
            yield self.reply_ok(msg,reply)
            return
                
        result = {}
        for it in successFail.item:
            # logging does not have a TRACE level!
#            if log.getEffectiveLevel() <= logging.TRACE:
#                log.trace('In SiamDriver op_get item --> ' + str(it))
            chName = it.pair.first
            value = it.pair.second
            key = (SiamDriverChannel.INSTRUMENT, chName)
            result[key] = (InstErrorCode.OK, value)
            
            
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('In SiamDriver op_get result --> ' + str(result))

        reply['result'] = result
        
        yield self.reply_ok(msg,reply)        
        
        

    @defer.inlineCallbacks
    def op_set(self, content, headers, msg):

        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('op_set: called.  current_state = ' + str(self.current_state))
            
        
        assert(isinstance(content,dict)), 'Expected dict content.'
        
        #
        # params should be of the form:
        #    {(chan_arg,param_arg):value,...,(chan_arg,param_arg):value}
        #
        params = content.get('params',None)
        assert(isinstance(params,dict)), 'Expected dict params.'

        assert(all(map(lambda x: isinstance(x,(list,tuple)),
                       params.keys()))), 'Expected list or tuple dict keys.'
        assert(all(map(lambda x: isinstance(x,str),
                       params.values()))), 'Expected string dict values.'
        
         
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('op_set: params = ' +\
                      str(params)+ "  publish_stream=" +str(self.publish_stream))
        
        if self.current_state != SiamDriverState.CONNECTED:
            reply['success'] = InstErrorCode.INCORRECT_STATE
            log.error('op_set: incorrect state for this operation.  current_state = ' + str(self.current_state))
            yield self.reply_ok(msg, reply)
            return
        
         
        reply = {'success':None,'result':None}
        
        #
        # TODO accept the format of the params as indicated above. For the
        # moment, we convert the input:
        #     {(chan_arg,param_arg):value,...,(chan_arg,param_arg):value}
        # into
        #     {param_arg:value,..., param_arg:value}
        # while checking that all chan_arg in the input are equal to
        # SiamDriverChannel.INSTRUMENT.
        
        params_for_proxy = {}
        for (chan,param) in params.keys():
            if SiamDriverChannel.INSTRUMENT != chan:
                reply['success'] = InstErrorCode.INVALID_CHANNEL
                errmsg = "Only " +str(SiamDriverChannel.INSTRUMENT) + \
                        " accepted for channel; given: " + chan
                reply['result'] = errmsg
                log.warning("op_set: " +errmsg)
                yield self.reply_ok(msg,reply)
                return
            
            val = params[(chan,param)]
            params_for_proxy[param] = val
            
        
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug("op_set: params_for_proxy = " + str(params_for_proxy) + \
                      "  **** siamci = " + str(self.siamci))
            
        response = yield self.siamci.set_params(params_for_proxy)
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('op_set: set_params response --> ' + str(response))
                
        if response.result == OK:
            reply['success'] = InstErrorCode.OK
            reply['result'] = params_for_proxy    # just informative
        else:
            reply['success'] = InstErrorCode.SET_DEVICE_ERR
            
        yield self.reply_ok(msg, reply)
        
 



    @defer.inlineCallbacks
    def op_execute(self, content, headers, msg):

        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('op_execute: called.  current_state = ' + str(self.current_state))
            

        assert(isinstance(content,dict)), 'Expected dict content.'

        if self.current_state != SiamDriverState.CONNECTED:
            reply['success'] = InstErrorCode.INCORRECT_STATE
            log.error('op_execute: incorrect state for this operation.  current_state = ' + str(self.current_state))
            yield self.reply_ok(msg, reply)
            return
        
        # Set up reply dict, get required parameters from message content.
        reply = {'success':None,'result':None}
        command = content.get('command',None)
        channels = content.get('channels',None)
        timeout = content.get('timeout',None)

        # Fail if required parameters absent.
        if not command:
            reply['success'] = InstErrorCode.REQUIRED_PARAMETER
            yield self.reply_ok(msg,reply)
            return
        if not channels:
            reply['success'] = InstErrorCode.REQUIRED_PARAMETER
            yield self.reply_ok(msg,reply)
            return

        assert(isinstance(command,(list,tuple)))        
        assert(all(map(lambda x:isinstance(x,str),command)))
        assert(isinstance(channels,(list,tuple)))        
        assert(all(map(lambda x:isinstance(x,str),channels)))
        if timeout != None:
            assert(isinstance(timeout,int)), 'Expected integer timeout'
            assert(timeout>0), 'Expected positive timeout'
            pass
        
        # Fail if command or channels not valid for siam driver.
        if not SiamDriverCommand.has(command[0]):
            reply['success'] = InstErrorCode.UNKNOWN_COMMAND
            yield self.reply_ok(msg,reply)
            return
        
        # get the actual instrument channels:
        successFail = yield self.siamci.get_channels()
        if successFail.result != OK:
            reply['success'] = InstErrorCode.EXE_DEVICE_ERR
            errmsg = "Error retrieving channels"
            reply['result'] = errmsg
            log.warning("op_execute: " +errmsg)
            yield self.reply_ok(msg,reply)
            return
        instrument_channels = [it.str for it in successFail.item]
        
        #
        # NOTE: special channel name SiamDriverChannel.INSTRUMENT only 
        # accepted in a singleton channels list.
        #
        
        if len(channels) == 0 or (len(channels) == 1 and  
            SiamDriverChannel.INSTRUMENT == channels[0]):
            # ok, this means all channels for various operations.
            pass
        else:
            # verify the explicit requested channels are valid
            for chan in channels:
                if SiamDriverChannel.INSTRUMENT == chan:
                    reply['success'] = InstErrorCode.INVALID_CHANNEL
                    errmsg = "Can only use '" + \
                            str(SiamDriverChannel.INSTRUMENT) + \
                            "' for a singleton channels list"
                    reply['result'] = errmsg
                    log.warning("op_execute: " +errmsg)
                    yield self.reply_ok(msg,reply)
                    return
                    
                if not chan in instrument_channels:
                    reply['success'] = InstErrorCode.UNKNOWN_CHANNEL
                    errmsg = "instrument does not have channel named '" +str(chan)+ "'"
                    reply['result'] = errmsg
                    log.warning("op_execute: " +errmsg)
                    yield self.reply_ok(msg,reply)
                    return

        drv_cmd = command[0]
      
        #############################
        # dispatch the given command:
        #############################
        
        # GET_CHANNELS ##############################################
        if drv_cmd == SiamDriverCommand.GET_CHANNELS:
            #
            # we already have the channels from the general preparation above
            #
            reply['success'] = InstErrorCode.OK
            reply['result'] = instrument_channels
            if log.getEffectiveLevel() <= logging.DEBUG:
                log.debug("op_execute GET_CHANNELS to reply: " + str(reply))
            yield self.reply_ok(msg,reply)
            return
        
        # GET_LAST_SAMPLE ##############################################
        if drv_cmd == SiamDriverCommand.GET_LAST_SAMPLE:
            yield self.__get_last_sample(channels, reply)
            if log.getEffectiveLevel() <= logging.DEBUG:
                log.debug("op_execute GET_LAST_SAMPLE to reply: " + str(reply))
            yield self.reply_ok(msg,reply)
            return
        
        # START_AUTO_SAMPLING ##############################################
        if drv_cmd == SiamDriverCommand.START_AUTO_SAMPLING:
            yield self.__start_sampling(channels, reply)
            if log.getEffectiveLevel() <= logging.DEBUG:
                log.debug("op_execute START_AUTO_SAMPLING to reply: " + str(reply))
            yield self.reply_ok(msg,reply)
            return

        # STOP_AUTO_SAMPLING ##############################################
        if drv_cmd == SiamDriverCommand.STOP_AUTO_SAMPLING:
            yield self.__stop_sampling(channels, reply)
            if log.getEffectiveLevel() <= logging.DEBUG:
                log.debug("op_execute STOP_AUTO_SAMPLING to reply: " + str(reply))
            yield self.reply_ok(msg,reply)
            return

        
        #
        # Else: INVALID_COMMAND
        #
        reply['success'] = InstErrorCode.INVALID_COMMAND
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug("op_execute INVALID_COMMAND to reply: " + str(reply))
        yield self.reply_ok(msg,reply)
        return
        
       
  
  
      
    @defer.inlineCallbacks
    def op_acceptResponse(self, content, headers, msg):
        """
        This operation is called by the java side.
        """
        
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug("op_acceptResponse: called.")
            
        publish_id = headers['publish_id']
        if not publish_id:
            log.warn('op_acceptResponse: publish_id not given')
            yield self.reply_err(msg, "op_acceptResponse : WARNING: publish_id not given")
            return

        # reply to client. TODO note that this still uses an RPC style so we
        # should reply something, but a future change is to use just a 
        # one-directional notification.        
        yield self.reply_ok(msg, {'op_acceptResponse' : "OK: response for publish_id='" +str(publish_id)+ "' accepted"})
        
        #
        # now, let's dispatch the notification:
        #
        
        content = get_python_content(content)
        
        channel = content['item']['channel']
        value = float(content['item']['value'])
        
        
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug("op_acceptResponse: publish_id = " +str(publish_id) +
                      " content = " +str(content))
        
        if self.notify_agent:
            sample_data = {}
            sample_data[channel] = value
            samples = [sample_data]
            notify_content = {'type':SiamDriverAnnouncement.DATA_RECEIVED,
                       'transducer':channel,'value':samples}
            
            
            if log.getEffectiveLevel() <= logging.DEBUG:
                log.debug("****** notifying agent: " +str(notify_content))
            
            
            yield self.send(self.proc_supid, 'driver_event_occurred', notify_content)                                                
                
      
      
      
    @defer.inlineCallbacks
    def __get_last_sample(self, channels, reply):
        log.debug('In SiamDriver __get_last_sample')
        
        response = yield self.siamci.get_last_sample()
        
        if response.result != OK:
            # TODO: some more appropriate error code
            reply['success'] = InstErrorCode.EXE_DEVICE_ERR
            return
        
        result = {}
        for it in response.item:
            ch = it.pair.first
            val = it.pair.second
            result[ch] = val
            
        reply['success'] = InstErrorCode.OK
        reply['result'] = result
        
      
    @defer.inlineCallbacks
    def __start_sampling(self, channels, reply):
        """
        If successful, 
            reply['success'] = InstErrorCode.OK
            reply['result'] = {'channel':channel, 'publish_stream':self.data_publish_stream }
            
        where channel is the channel in the singleton channels list

        """
        
        if log.getEffectiveLevel() <= logging.DEBUG:
            log.debug('__start_sampling channels = ' +str(channels) + \
                      " notify_agent = " + str(self.notify_agent) + \
                      " data_publish_stream = " + str(self.data_publish_stream))
        
        if len(channels) != 1:
            reply['success'] = InstErrorCode.INVALID_CHANNEL
            errmsg = "Can only be one channel for the START_AUTO_SAMPLING operation"
            reply['result'] = errmsg
            log.warning("__start_sampling: " +errmsg)
            return
                
        # the actual channel we will be sampling on
        channel = channels[0]
        if SiamDriverChannel.INSTRUMENT == channel:
            reply['success'] = InstErrorCode.INVALID_CHANNEL
            errmsg = "Has to be a specific channel, not '" + \
                    str(SiamDriverChannel.INSTRUMENT) + "'"
            reply['result'] = errmsg
            log.warning("__start_sampling: " +errmsg)
            return
        
        
        if self.data_publish_stream is not None:
            response = yield self.siamci.execute_StartAcquisition(channel, self.data_publish_stream)
            if response.result != OK:
                log.warning("execute_StartAcquisition failed: " +str(response))
                # TODO: some more appropriate error code
                reply['success'] = InstErrorCode.EXE_DEVICE_ERR
                return
            
            reply['success'] = InstErrorCode.OK
            reply['result'] = {'channel':channel, 'publish_stream':self.data_publish_stream }
            return
        
          
        # TODO: perhaps a more appropriate error code for this situation
        reply['success'] = InstErrorCode.EXE_DEVICE_ERR
        errmsg = "associated data_publish_stream required for this operation"
        reply['result'] = errmsg
        log.warning("__start_sampling: " +errmsg)
        
        
      
    @defer.inlineCallbacks
    def __stop_sampling(self, channels, reply):
        """
        Request to stop sampling
        """
        
        if len(channels) != 1:
            reply['success'] = InstErrorCode.INVALID_CHANNEL
            errmsg = "Can only be one channel for the START_AUTO_SAMPLING operation"
            reply['result'] = errmsg
            log.warning("__stop_sampling: " +errmsg)
            return
                
        # the actual channel we will be sampling on
        channel = channels[0]
        if SiamDriverChannel.INSTRUMENT == channel:
            reply['success'] = InstErrorCode.INVALID_CHANNEL
            errmsg = "Has to be a specific channel, not '" + \
                    str(SiamDriverChannel.INSTRUMENT) + "'"
            reply['result'] = errmsg
            log.warning("__stop_sampling: " +errmsg)
            return
        

        if self.data_publish_stream is not None:
            response = yield self.siamci.execute_StopAcquisition(channel, self.data_publish_stream)
            if log.getEffectiveLevel() <= logging.DEBUG:
                log.debug('In SiamDriver __stop_sampling --> ' + str(response))  
            
            if response.result != OK:
                # TODO: some more appropriate error code
                reply['success'] = InstErrorCode.EXE_DEVICE_ERR
                return
            
            reply['success'] = InstErrorCode.OK
            reply['result'] = {'channel':channel, 'publish_stream':self.data_publish_stream }
            return
            
            
        # TODO: perhaps a more appropriate error code for this situation
        reply['success'] = InstErrorCode.EXE_DEVICE_ERR
        errmsg = "associated data_publish_stream required for this operation"
        reply['result'] = errmsg
        log.warning("__stop_sampling: " +errmsg)
        
        

class SiamInstrumentDriverClient(InstrumentDriverClient):
    """
    The client class for the instrument driver. This is the client that the
    instrument agent can use for communicating with the driver.
    
    In this case, this is the driver interface to a SIAM enabled instrument.
    Operations are mainly supported by the SiamCiAdapterProxy class.
    """
    
        
        
# Spawn of the process using the module name
factory = ProcessFactory(SiamInstrumentDriver)
