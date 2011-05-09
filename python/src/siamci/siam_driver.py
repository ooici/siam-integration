#!/usr/bin/env python

"""
@file siamci/siam_driver.py
@author Carlos Rueda
@brief Driver code for SIAM
"""
import ion.util.ionlog
log = ion.util.ionlog.getLogger(__name__)
from twisted.internet import defer  #, reactor



from ion.agents.instrumentagents.instrument_agent import InstrumentDriver
from ion.agents.instrumentagents.instrument_agent import InstrumentDriverClient
#from ion.agents.instrumentagents.instrument_agent import publish_msg_type

from ion.core.process.process import ProcessFactory

from siamci.siamci_proxy import SiamCiAdapterProxy
from siamci.util.tcolor import red, blue


from net.ooici.play.instr_driver_interface_pb2 import Command, SuccessFail, OK, ERROR

from ion.agents.instrumentagents.instrument_constants import DriverCommand
from ion.agents.instrumentagents.instrument_constants import DriverState
from ion.agents.instrumentagents.instrument_constants import DriverEvent
from ion.agents.instrumentagents.instrument_constants import DriverAnnouncement
from ion.agents.instrumentagents.instrument_constants import DriverChannel
from ion.agents.instrumentagents.instrument_constants import BaseEnum
from ion.agents.instrumentagents.instrument_constants import InstErrorCode

from ion.agents.instrumentagents.instrument_fsm import InstrumentFSM

from ion.agents.instrumentagents.instrument_constants import InstErrorCode



# Device states.
class SiamDriverState(DriverState):
    """
    Add siam driver specific states here.
    """
    pass

# Device events.
class SiamDriverEvent(DriverEvent):
    """
    Add siam driver specific events here.
    """
    pass

# Device commands.
class SiamDriverCommand(DriverCommand):
    """
    Add siam driver specific commands here.
    """
    GET_CHANNELS = 'DRIVER_CMD_GET_CHANNELS'
    GET_LAST_SAMPLE = 'DRIVER_CMD_GET_LAST_SAMPLE'


# Device channels / transducers.
class SiamDriverChannel(DriverChannel):
    """
    siam driver channels.
    """
    pass

class SiamDriverAnnouncement(DriverAnnouncement):
    """
    Add siam driver specific announcements here.
    """
    pass




       
class SiamInstrumentDriver(InstrumentDriver):
    """
    Instrument driver interface to a SIAM enabled instrument.
    Main operations are supported by the core class SiamCiAdapterProxy.
    
    @todo: do translation of GPBs to the native python structures proposed by the 
           Instrument Driver Interface page. At this point, I'm still focusing on
           the core needed functionality and using the GPBs directly.
    
    @todo: NOT all operations are instrument-specific; there are some that are associated
           with the SIAM node in general, for example, to retrieve all the instruments that
           are deployed on that node.  The TODO is about separating this more generic 
           functionality and use relevant ION mechanisms (eg., resource registries) for
           those purposes.
    """

    def __init__(self, *args, **kwargs):
        
        
        """
        Creates an instance of the driver.
        """
        
        InstrumentDriver.__init__(self, *args, **kwargs)
        log.debug("\nSiamDriver __init__: spawn_args = " +str(self.spawn_args))
        
        
        """
        A flag indicating whether notifications to self.proc_supid should be sent
        when entering states. It is assigned the value returned by self.spawn_args.get('notify_agent', False).
        @NOTE:
            This is to avoid "ERROR:Process does not define op=driver_event_occurred" messages
            when self.proc_supid does not correspond to an instrument agent.
            There might be a more appropriate way to accomplish this behavior. I'm using this
            mechanism in test_siam_agent.py to include the corresponding spawn arg.
        """
        self.notify_agent = self.spawn_args.get('notify_agent', False)
         
        
        
        """
        used to connect to the SIAM-CI adapter service.
        """
        self.pid = None
        
        
        """
        Instrument port
        """
        self.port = None
        
            
        """
        Will be a SiamCiAdapterProxy(pid, port) instance upon configuration
        """
        self.siamci = None



        """
        Instrument state handlers
        """
        self.state_handlers = {
            SiamDriverState.UNCONFIGURED : self.state_handler_unconfigured,
            SiamDriverState.DISCONNECTED : self.state_handler_disconnected,
            SiamDriverState.CONNECTING : self.state_handler_connecting,
            SiamDriverState.DISCONNECTING : self.state_handler_disconnecting,
            SiamDriverState.CONNECTED : self.state_handler_connected,
#            SiamDriverState.ACQUIRE_SAMPLE : self.state_handler_acquire_sample,
#            SiamDriverState.UPDATE_PARAMS : self.state_handler_update_params,
#            SiamDriverState.SET : self.state_handler_set,
#            SiamDriverState.AUTOSAMPLE : self.state_handler_autosample
        }
        
        """
        Instrument state machine.
        """
        self.fsm = InstrumentFSM(SiamDriverState, SiamDriverEvent, self.state_handlers,
                                 SiamDriverEvent.ENTER, SiamDriverEvent.EXIT)
        
        
        
        
    ###########################################################################
    # <state handlers>
    
    def state_handler_unconfigured(self,event,params):
        """
        Event handler for STATE_UNCONFIGURED.
        Events handled:
        EVENT_ENTER: Reset communication parameters to null values.
        EVENT_EXIT: Pass.
        EVENT_CONFIGURE: Set communication parameters and switch to
                STATE_DISCONNECTED if successful.
        EVENT_INITIALIZE: Reset communication parameters to null values.
        """
        
        log.debug("state_handler_unconfigured: event = " +str(event) + "\n\t\t params = " + str(params))

        success = InstErrorCode.OK
        next_state = None
        
        if event == SiamDriverEvent.ENTER:
            
            if self.notify_agent:
                # Announce the state change to agent.                        
                content = {'type':SiamDriverAnnouncement.STATE_CHANGE,'transducer':SiamDriverChannel.INSTRUMENT,
                           'value':SiamDriverState.UNCONFIGURED}
                self.send(self.proc_supid,'driver_event_occurred',content)

            self._initialize()

        elif event == SiamDriverEvent.EXIT:
            pass
        
        elif event == SiamDriverEvent.INITIALIZE:
            self._initialize()
            
        elif event == SiamDriverEvent.CONFIGURE:
            if self._configure(params):
                next_state = SiamDriverState.DISCONNECTED
        
        else:
            success = InstErrorCode.INCORRECT_STATE
            
#        log.debug("UUUUUUU NEXT STATE: " +str(next_state))
        return (success,next_state)



    def state_handler_disconnected(self,event,params):
        """
        Event handler for STATE_DISCONNECTED.
        Events handled:
        EVENT_ENTER: Pass.
        EVENT_EXIT: Pass.        
        EVENT_INITIALIZE: Switch to STATE_UNCONFIGURED.
        EVENT_CONNECT: Switch to STATE_CONNECTING.
        """
        
        log.debug("state_handler_disconnected: event = " +str(event) + "\n\t\t params = " + str(params))

        success = InstErrorCode.OK
        next_state = None
        
        if event == SiamDriverEvent.ENTER:
            
            if self.notify_agent:
                # Announce the state change to agent.            
                content = {'type':SiamDriverAnnouncement.STATE_CHANGE,'transducer':SiamDriverChannel.INSTRUMENT,
                           'value':SiamDriverState.DISCONNECTED}
                self.send(self.proc_supid,'driver_event_occurred',content)
            
#            # If we enter a disconnect state with the connection complete
#            # defered defined, then we are entering from a previous connection
#            # in response to a disconnect comment. Fire the deferred with
#            # reply to indicate successful disconnect.
#            if self._connection_complete_deferred:
#                d,self._connection_complete_deferred = self._connection_complete_deferred,None
#                reply = {'success':InstErrorCode.OK,'result':None}
#                d.callback(reply)
            
        elif event == SiamDriverEvent.EXIT:
            pass

        elif event == SiamDriverEvent.INITIALIZE:
            next_state = SiamDriverState.UNCONFIGURED         
                    
        elif event == SiamDriverEvent.CONNECT:
            next_state = SiamDriverState.CONNECTING         
                
        # not in sbe37    
        elif event == SiamDriverEvent.DISCONNECT_COMPLETE:
            pass   
                    
        else:
            success = InstErrorCode.INCORRECT_STATE
            
        return (success,next_state)
    
    
    
    def state_handler_connecting(self,event,params):
        """
        Event handler for STATE_CONNECTING.
        Events handled:
        EVENT_ENTER: Attemmpt to establish connection.
        EVENT_EXIT: Pass.        
        EVENT_CONNECTION_COMPLETE: Switch to SiamDriverState.CONNECTED   (STATE_UPDATE_PARAMS in SBE37)
        EVENT_CONNECTION_FAILED: Switch to STATE_DISCONNECTED.
        """
        
        log.debug("state_handler_connecting: event = " +str(event) + "\n\t\t params = " + str(params))
        
        success = InstErrorCode.OK
        next_state = None
        
        if event == SiamDriverEvent.ENTER:
            
            if self.notify_agent:
                # Announce the state change to agent.            
                content = {'type':SiamDriverAnnouncement.STATE_CHANGE,'transducer':SiamDriverChannel.INSTRUMENT,
                           'value':SiamDriverState.CONNECTING}
                self.send(self.proc_supid,'driver_event_occurred',content)

            
        elif event == SiamDriverEvent.EXIT:
            pass
                    
        elif event == SiamDriverEvent.CONNECTION_COMPLETE:
#            next_state = SiamDriverState.UPDATE_PARAMS
            next_state = SiamDriverState.CONNECTED
                    
        elif event == SiamDriverEvent.CONNECTION_FAILED:
            # Error message to agent here.
            next_state = SiamDriverState.DISCONNECTED

        else:
            success = InstErrorCode.INCORRECT_STATE

        return (success,next_state)



    def state_handler_connected(self,event,params):
        """
        Event handler for STATE_CONNECTED.
        EVENT_ENTER: Notifies agent if instructed so
        CONNECTION_COMPLETE: pass
        EVENT_EXIT: Pass.        
        EVENT_DISCONNECT: Switch to STATE_DISCONNECTING.
        EVENT_COMMAND_RECEIVED: If a command is queued, switch to command
        specific state for handling.
        EVENT_DATA_RECEIVED: Pass.
        """
        
        log.debug("state_handler_connected: event = " +str(event) + "\n\t\t params = " + str(params))
        
        success = InstErrorCode.OK
        next_state = None

        if event == SiamDriverEvent.ENTER:
            
            if self.notify_agent:
                # Announce the state change to agent.            
                content = {'type':SiamDriverAnnouncement.STATE_CHANGE,'transducer':SiamDriverChannel.INSTRUMENT,
                           'value':SiamDriverState.CONNECTED}
                self.send(self.proc_supid,'driver_event_occurred',content)            
            
#            # If we enter connected with the connection complete deferred
#            # defined we are establishing the initial connection in response
#            # to a connect command. Send the reply to indicate successful
#            # connection.
#            if self._connection_complete_deferred:
#                d,self._connection_complete_deferred = self._connection_complete_deferred,None
#                reply = {'success':InstErrorCode.OK,'result':None}
#                d.callback(reply)
            
        elif event == SiamDriverEvent.CONNECTION_COMPLETE:
            pass

        elif event == SiamDriverEvent.EXIT:
            pass
            
        elif event == SiamDriverEvent.DISCONNECT:
            next_state = SiamDriverState.DISCONNECTING
                    
        elif event == SiamDriverEvent.SET:
            next_state = SiamDriverState.SET
            
        elif event == SiamDriverEvent.ACQUIRE_SAMPLE:
            next_state = SiamDriverState.ACQUIRE_SAMPLE
            
        elif event == SiamDriverEvent.START_AUTOSAMPLE:
            next_state = SiamDriverState.AUTOSAMPLE
            
        elif event == SiamDriverEvent.TEST:
            next_state = SiamDriverState.TEST
            
        elif event == SiamDriverEvent.CALIBRATE:
            next_state = SiamDriverState.CALIBRATE
            
        elif event == SiamDriverEvent.RESET:
            next_state = SiamDriverState.RESET
            
        elif event == SiamDriverEvent.DATA_RECEIVED:
            pass
                
        else:
            success = InstErrorCode.INCORRECT_STATE

        return (success,next_state)

    
       
    def state_handler_disconnecting(self,event,params):
        """
        Event handler for STATE_DISCONNECTING.
        Events handled:
        EVENT_ENTER: Attempt to close connection to instrument.
        EVENT_EXIT: Pass.        
        EVENT_DISCONNECT_COMPLETE: Switch to STATE_DISCONNECTED.
        """
        
        success = InstErrorCode.OK
        next_state = None
            
        if event == SiamDriverEvent.ENTER:
            
            if self.notify_agent:
                # Announce the state change to agent.            
                content = {'type':SiamDriverAnnouncement.STATE_CHANGE,'transducer':SiamDriverChannel.INSTRUMENT,
                           'value':SiamDriverState.DISCONNECTED}
                self.send(self.proc_supid,'driver_event_occurred',content)            
            
            
        elif event == SiamDriverEvent.EXIT:
            pass
                    
        elif event == SiamDriverEvent.DISCONNECT_COMPLETE:
            next_state = SiamDriverState.DISCONNECTED

        else:
            success = InstErrorCode.INCORRECT_STATE

        return (success,next_state)
       
       
       
        
    # </state handlers>
    ###########################################################################
    
    
    def _initialize(self):
        """
        Set the configuration to an initialized, unconfigured state.
        """
        self.pid = None
        self.port = None
    

    
    ###########################################################################
    # <Process lifecycle methods>
    
    @defer.inlineCallbacks
    def plc_init(self):
        """
        Process lifecycle initialization.
        """
        yield
        
        # Set initial state.
        self.fsm.start(SiamDriverState.UNCONFIGURED)
        log.debug("SiamDriver plc_init: FSM started with state UNCONFIGURED")
        
                

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
        cur_state = self.fsm.current_state
        yield self.reply_ok(msg, cur_state)



    @defer.inlineCallbacks
    def op_initialize(self, content, headers, msg):
        """
        Restore driver to a default, unconfigured state.
        @param content A dict with optional timeout: {'timeout':timeout}.
        @retval A reply message with a dict {'success':success,'result':None}.
        """
        
        # Timeout not implemented for this op.
        timeout = content.get('timeout',None)
        if timeout != None:
            assert(isinstance(timeout,int)), 'Expected integer timeout'
            assert(timeout>0), 'Expected positive timeout'
            pass

        # Set up the reply and fire an EVENT_INITIALIZE.
        reply = {'success':None,'result':None}         
        success = self.fsm.on_event(SiamDriverEvent.INITIALIZE)
        
        # Set success and send reply. Unsuccessful initialize means the
        # event is not handled in the current state.
        if not success:
            reply['success'] = InstErrorCode.INCORRECT_STATE

        else:
            reply['success'] = InstErrorCode.OK
 
        yield self.reply_ok(msg, reply)
        
        
    @defer.inlineCallbacks
    def op_configure(self, content, headers, msg):
        
        assert(isinstance(content, dict)), 'Expected dict content.'
        params = content.get('params', None)
        assert(isinstance(params, dict)), 'Expected dict params.'
        
        # Timeout not implemented for this op.
        timeout = content.get('timeout', None)
        if timeout != None:
            assert(isinstance(timeout, int)), 'Expected integer timeout'
            assert(timeout > 0), 'Expected positive timeout'
            pass

        # Set up the reply message and validate the configuration parameters.
        # Reply with the error message if the parameters not valid.
        reply = {'success':None, 'result':params}
        reply['success'] = self._validate_configuration(params)
        
        if InstErrorCode.is_error(reply['success']):
            yield self.reply_ok(msg, reply)
            return
        
        # Fire EVENT_CONFIGURE with the validated configuration parameters.
        # Set the error message if the event is not handled in the current
        # state.
        reply['success'] = self.fsm.on_event(SiamDriverEvent.CONFIGURE,params)

        yield self.reply_ok(msg, reply)
        
                
                

    def _configure(self, params):
        
        # Validate configuration.
        success = self._validate_configuration(params)
        if InstErrorCode.is_error(success):
            return False

        # Set configuration parameters.
        self.pid = params['pid']
        self.port = params['port']

        log.debug("SiamInstrumentDriver _configure: pid = '" + str(self.pid) + "' port = '" + str(self.port) + "'")
        self.siamci = SiamCiAdapterProxy(self.pid, self.port)
        
        return True
    
            
    def _validate_configuration(self, params):
        
        # Get required parameters.
        pid = params.get('pid', None)
        port = params.get('port', None)

        # fail if missing a required parameter.
        if not pid or not port:
            return InstErrorCode.REQUIRED_PARAMETER
        
        # NOTE: port is actually optional, but force it here as the
        # general case is that the driver is for a concrete instrument (which 
        # is identified by this port. 

        return InstErrorCode.OK

        
      
        
        
    @defer.inlineCallbacks
    def op_connect(self, content, headers, msg):
        
        # Timeout not implemented for this op.
        timeout = content.get('timeout',None)
        if timeout != None:
            assert(isinstance(timeout,int)), 'Expected integer timeout'
            assert(timeout>0), 'Expected positive timeout'
            pass


        success = self.fsm.on_event(SiamDriverEvent.CONNECT) 
        reply = {'success':success,'result':None}
        if InstErrorCode.is_error(reply['success']):
            yield self.reply_ok(msg, reply)
            return
        
        success = self.fsm.on_event(SiamDriverEvent.CONNECTION_COMPLETE)
        reply['success'] = success
        
        """
        There is no actual "connect"/"disconnect" as the driver interacts 
        via messaging with the SIAM-CI adapter service. 
        We just start our proxy:
        """
        yield self.siamci.start()
        
        yield self.reply_ok(msg, reply)
        
       
        


                
               
    @defer.inlineCallbacks
    def op_disconnect(self, content, headers, msg):
        
        # Timeout not implemented for this op.
        timeout = content.get('timeout',None)
        if timeout != None:
            assert(isinstance(timeout,int)), 'Expected integer timeout'
            assert(timeout>0), 'Expected positive timeout'
            pass

        success = self.fsm.on_event(SiamDriverEvent.DISCONNECT) 
        reply = {'success':success,'result':None}
        
        if InstErrorCode.is_error(reply['success']):
            yield self.reply_ok(msg, reply)
            return
        

        success = self.fsm.on_event(SiamDriverEvent.DISCONNECT_COMPLETE)
        reply['success'] = success
        
        """
        @TODO: why trying to stop (terminate) the SiamCiProxy process causes error?
        """
#        yield self.siamci.stop()

        yield self.reply_ok(msg, reply)               
                
                
                
                
                
                

    @defer.inlineCallbacks
    def op_get_status(self, content, headers, msg):
        log.debug('In SiamDriver op_get_status')
        
        
        assert(isinstance(content,dict)), 'Expected dict content.'
        params = content.get('params',None)
        """
        For 'params', how is that a list, eg., [('all','all)], passed from
        the client gets converted to a tuple here, eg.,  (('all', 'all'),) ?
        """
#        self._debug("op_get_status params " +str(params))
        
        assert(isinstance(params,(list,tuple))), 'Expected list or tuple params in op_get_status'
        assert(all(map(lambda x:isinstance(x,tuple),params))), \
            'Expected tuple elements in params list'
        
        # Timeout not implemented for this op.
        timeout = content.get('timeout',None)
        if timeout != None:
            assert(isinstance(timeout,int)), 'Expected integer timeout'
            assert(timeout>0), 'Expected positive timeout'
            pass
        
        # @todo: Do something with the new possible argument 'timeout'


        response = yield self.siamci.get_status(params=params)
#        self._debug("op_get_status response " +str(response))
        result = response.result
        reply = {'success':InstErrorCode.OK,'result':result}
#        self._debug("op_get_status reply " +str(reply))

        yield self.reply_ok(msg, reply)



    @defer.inlineCallbacks
    def op_get(self, content, headers, msg):
        
        assert(isinstance(content,dict)),'Expected dict content.'
        params = content.get('params',None)
        assert(isinstance(params,(list,tuple))),'Expected list or tuple params.'

        # Timeout not implemented for this op.
        timeout = content.get('timeout',None)
        if timeout != None:
            assert(isinstance(timeout,int)), 'Expected integer timeout'
            assert(timeout>0), 'Expected positive timeout'
            pass
        
        # publish_stream
        publish_stream = content.get('publish_stream',None)
        
        log.debug('In SiamDriver op_get: params = ' +str(params)+ "  publish_stream=" +str(publish_stream))
        
        if publish_stream is None: 
            successFail = yield self.siamci.fetch_params(params)
        else:
            successFail = yield self.siamci.fetch_params(params, publish_stream=publish_stream)
            
        log.debug('In SiamDriver op_get successFail --> ' + str(successFail))
        
        # initialize reply assuming OK
        reply = {'success':InstErrorCode.OK, 'result':None}
        
        if successFail.result != OK:
            reply['success'] = InstErrorCode.GET_DEVICE_ERR
            yield self.reply_ok(msg,reply)
            return
                
        result = {}
        for it in successFail.item:
            log.debug('In SiamDriver op_get item --> ' + str(it))
            chName = it.pair.first
            value = it.pair.second
            key = ('instrument', chName)
            result[key] = (InstErrorCode.OK, value)
            
            
        log.debug('In SiamDriver op_get result --> ' + str(result))

        reply['result'] = result
        
        yield self.reply_ok(msg,reply)        
        
        
      
    @defer.inlineCallbacks
    def op_execute(self, content, headers, msg):
        """
        Execute a driver command. Commands may be
        common or specific to the device, with specific commands known through
        knowledge of the device or a previous get_capabilities query.
        @param content A dict with channels and command lists and optional
            timeout:
            {'channels':[chan_arg,...,chan_arg],
            'command':[command,arg,...,argN]),
            'timeout':timeout}.
        @retval A reply message with a dict
            {'success':success,
            'result':{chan_arg:(success,command_specific_values),...,
            chan_arg:(success,command_specific_values)}}. 
        """
        
        assert(isinstance(content,dict)), 'Expected dict content.'

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
            reply['success'] = InstErrorCode.ERROR
            reply['result'] = "Error retrieving channels"
            yield self.reply_ok(msg,reply)
            return
        instrument_channels = [it.str for it in successFail.item]
        
        #
        # NOTE: special channel name SiamDriverChannel.INSTRUMENT only accepted in a singleton channels list.
        #
        
        if len(channels) == 0 or len(channels) == 1 and SiamDriverChannel.INSTRUMENT == channels[0]:
            # ok, this means all channels
            pass
        else:
            # verify the explicit requested channels are valid
            for chan in channels:
                if SiamDriverChannel.INSTRUMENT == chan:
                    reply['success'] = InstErrorCode.INVALID_CHANNEL
                    reply['result'] = "Can only use 'instrument' for a singleton channels list"
                    yield self.reply_ok(msg,reply)
                    return
                    
                if not chan in instrument_channels:
                    reply['success'] = InstErrorCode.UNKNOWN_CHANNEL
                    reply['result'] = "instrument does not have channel named '" +str(chan)+ "'"
                    yield self.reply_ok(msg,reply)
                    return

        drv_cmd = command[0]
      
        
        # get channels
        if drv_cmd == SiamDriverCommand.GET_CHANNELS:
            #
            # we already have them from the general preparation above
            #
            reply['success'] = InstErrorCode.OK
            reply['result'] = instrument_channels
            yield self.reply_ok(msg,reply)
            return
        
        # get last sample
        if drv_cmd == SiamDriverCommand.GET_LAST_SAMPLE:
            yield self.__get_last_sample(channels, reply)
            yield self.reply_ok(msg,reply)
            return
        
        #
        # Else: INVALID_COMMAND
        reply['success'] = InstErrorCode.INVALID_COMMAND
        yield self.reply_ok(msg,reply)
        return
        
       
       
      
      
    @defer.inlineCallbacks
    def __get_last_sample(self, channels, reply):
        log.debug('In SiamDriver __get_last_sample')
        
        response = yield self.siamci.get_last_sample()
        log.debug('In SiamDriver __get_last_sample --> ' + str(response))  
        
        if response.result != OK:
            reply['success'] = InstErrorCode.ERROR
            yield self.reply_ok(msg, reply)
            return
        
        result = {}
        for it in response.item:
            ch = it.pair.first
            val = it.pair.second
            result[ch] = val
            
        reply['success'] = InstErrorCode.OK
        reply['result'] = result
        
      
      
      
       
       
        

    @defer.inlineCallbacks
    def op_set_params(self, content, headers, msg):
        log.debug('In SiamDriver op_set_params')
        
        response = yield self.siamci.fetch_params(content)
        log.debug('In SiamDriver op_set_params --> ' + str(response))    
        yield self.reply_ok(msg, response)



    def _debug(self, msg):
        print(blue(msg))
#        log.debug(red(msg))


class SiamInstrumentDriverClient(InstrumentDriverClient):
    """
    The client class for the instrument driver. This is the client that the
    instrument agent can use for communicating with the driver.
    """
    
    """
    In this case, this is the driver interface to a SIAM enabled instrument.
    Operations are mainly supported by the SiamCiAdapterProxy class.
    """
    

    """
    @TODO: Remove this set_params once the 'get' operation is completed        
    """        
    @defer.inlineCallbacks
    def set_params(self, param_dict):
        """
        @todo: we override the method in the superclass because that method expects the
        resulting content to be a dict: assert(isinstance(content, dict))
        In my current design, we work with the GPBs directly.
        """
        
        log.debug("SiamInstrumentDriverClient set_params " + str(param_dict))
        (content, headers, message) = yield self.rpc_send('set_params',
                                                          param_dict)
        
        # @todo: conversion to python type 
        
        defer.returnValue(content)
        
        

# Spawn of the process using the module name
factory = ProcessFactory(SiamInstrumentDriver)
