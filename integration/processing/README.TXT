Gradle will ignore this package unless 'core.jar' exists in this directory.  That jar can be found in your Processing installation at process-XXX/core/library/core.jar, where XXX is installed version of processing.  Unfortunately there is no officially supported version of Processing on Maven central so it cannot be automatically installed.  Which is why you have to do this manual hackery.

Testing has been done using Processing 2.X

-------

To create the zip package for Processing type 'gradle processingZip' and after a little bit boofcv_processing.zip should appear in integration/processing directory.