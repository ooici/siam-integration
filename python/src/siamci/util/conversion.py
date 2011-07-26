#!/usr/bin/env python

"""
@file siamci/util/conversion.py
@author Carlos Rueda
@brief Some conversion utilities
"""

from ion.core.object.object_utils import create_type_identifier

Command_type = create_type_identifier(object_id=20034, version=1)
ChannelParameterPair_type = create_type_identifier(object_id=20035, version=1)
SuccessFail_type = create_type_identifier(object_id=20036, version=1)


def get_python_content(content):
    """
    Converts content received from the java side to convenient python type
    """
    
#    from IPython.Shell import IPShellEmbed; (IPShellEmbed())()
    
    if isinstance(content, dict):
        # this is the case when op_acceptResponse is called from python code
        return content
    
    ret = {}
    
    if SuccessFail_type == content.MessageType:
        obj = content.MessageObject
        
        ret['result'] = obj.result
        strs = []
        pairs = {}
        for item in obj.item:
            if item.type == 1:   # STR
                strs.append(item.str)
                
            elif item.type == 2: # PAIR
                pairs[item.pair.first] = item.pair.second
                
            elif item.type == 3: # PAIR_AND_VALUE
                pass  # TODO
            
            else: raise Exception("Unexpected item type: " + str(item.type))
            
        #
        # TODO probably check whether there's a mix of strings and pairs
        #
        
        if len(pairs) > 0:
            ret['item'] = pairs
        else:
            ret['item'] = strs


    elif ChannelParameterPair_type == content.MessageType:
        
        # TODO this has NOT been tested
        
        obj = content.MessageObject
        
        ret['channel'] = obj.channel
        ret['parameter'] = obj.parameter

    
    elif Command_type == content.MessageType:
        
        # TODO this has NOT been tested
        
        obj = content.MessageObject
        
        ret['command'] = obj.command
        ret['args'] = {}
        for args in obj.args:
            ret['args'][arg.pair.first] = args.pair.second
        ret['publish_stream'] = obj.publish_stream

    
    else: raise Exception("Unexpected message type: " + str(content.MessageType))
    
    
    return ret
