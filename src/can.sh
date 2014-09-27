#! /bin/bash/

javac BootStrapServer.java
echo Main-Class: BootStrapServer > MANIFEST.MF
jar -cvmf MANIFEST.MF bootstrap.jar BootStrapServer.class WorkerThread.class
