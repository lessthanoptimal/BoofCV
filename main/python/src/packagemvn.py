#!/usr/bin/python
# -*- coding: utf-8 -*-

import os
import fnmatch

__author__ = 'pja'

modules = ["calibration","feature","geo","io","ip","recognition","sfm","visualize"]

# sanity check to see if it is in the correct directory
for m in modules:
    if not os.path.isdir(m):
        print "Can't find module "+m
        exit()

os.system("mvn clean")
os.system("mvn package")
os.system("mvn javadoc:jar")
os.system("mvn source:jar")

def formatMvn(module):
    # get the jar which contains the java byte code.  it's format is known
    binjar = fnmatch.filter(os.listdir('target'),"*[0-9].jar")[0]

    # extract the version string
    version = binjar[len(module)+1:-4]

    # construct names of other jars of interest
    docjar = module+'-'+version+'-javadoc.jar'
    srcjar = module+'-'+version+'-sources.jar'

    r = 'mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=pom.xml '
    r = r + '-Dfile=target/'+binjar+' '
    r = r + '-Dfiles=target/'+docjar+',target/'+srcjar+' -Dclassifiers=javadoc,sources -Dtypes=jar,jar'
    return r

for m in modules:
    os.chdir(m)

    mvncmd = formatMvn(m)
    os.system(mvncmd)

    os.chdir("..")

print 'Then go to https://oss.sonatype.org, click Staging Upload in the left column. In the Staging Upload panel, ' \
      'select Artifact Bundle as Upload Mode and select the bundle you just created:'




