#!/usr/bin/env python

from distutils.core import setup

setup(name='PyBoof',
      version='0.19-SNAPSHOT',
      description='Py4J Python wrapper for BoofCV',
      package_dir={'pyboof': 'python/pyboof'},
      packages=['pyboof']
      )