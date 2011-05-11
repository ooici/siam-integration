#!/usr/bin/env python

"""
@file setup.py
@author Carlos Rueda
@brief setup file for SIAM-CI
@see http://peak.telecommunity.com/DevCenter/setuptools
"""

try:
    from setuptools import setup, find_packages
except ImportError:
    from distutils.core import setup

setup(
    name = 'siamci',
    version = '0.0.1', 
    description = 'ION SIAM-CI integration',
    url = 'https://confluence.oceanobservatories.org/display/CIDev/SIAM-CI+Integration+Prototype',
    download_url = 'http://ooici.net/packages',    
    dependency_links = [ 'http://ooici.net/releases' ],
    license = 'Apache 2.0',
    author = 'Carlos Rueda', 
    author_email = 'carueda@mbari.org',
    keywords = [
        'ooici', 
        'siam',
        'integration'
               ],
    classifiers = [
        'Development Status :: 3 - Alpha',
        'Environment :: Console',
        'Intended Audience :: Developers',
        'License :: OSI Approved :: Apache Software License',
        'Operating System :: OS Independent',
        'Programming Language :: Python',
        'Topic :: Scientific/Engineering'
                  ],
    #packages=['ion'],
    packages = find_packages('src'),
    package_dir = {'': 'src'},

    install_requires = [
        'ioncore>=0.4.13'                
                       ],

    include_package_data = True
     )
