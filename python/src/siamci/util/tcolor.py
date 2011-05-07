#!/usr/bin/env python

"""
@file siamci/util/tcolor.py
@author Carlos Rueda
@brief Simple utility to generate colored output with sequences understood by
        typical terminals.
"""


RED = u"\u001b[0;31m"
GREEN = u"\u001b[0;32m";
BLUE = u"\u001b[0;34m"
DEFAULT = u"\u001b[1;00m"

def red(msg):
    return RED + msg + DEFAULT

def green(msg):
    return GREEN + msg + DEFAULT

def blue(msg):
    return BLUE + msg + DEFAULT


