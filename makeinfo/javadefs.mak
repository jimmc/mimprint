#Include this piece of makefile to set up defs for java stuff

#Change these definitions to point to the location on your machine
#containing these files
JVER          = 1.4.1
JDK           = /u/java/j2sdk$(JVER)
JRE           = $(JDK)/jre

HOSTNAME = $(shell uname -n)

#This section allows for development on another machine with some files in
#different places.  As long as your machine doesn't have the same name as
#ALTHOST, then you don't need to worry about it.
ALTHOST = paqnote
ifeq ($(ALTHOST),$(HOSTNAME))
JDK           = /p/java/jdk$(JVER)
BSHDIR        = /p/net/bsh-1.0
JUNITDIR      = /p/java/junit3.2
endif

#A second alternate host
ALTHOST = paqnotew
ifeq ($(ALTHOST),$(HOSTNAME))
JDK           = C:/jdk$(JVER)
endif

#These definitions are built on the previous definitions.
#You should not need to change them.
JAVA          = $(JDK)/bin/java
JAVAC         = $(JDK)/bin/javac
JAR           = $(JDK)/bin/jar
JAVADOC       = $(JDK)/bin/javadoc

JUNITJAR      = $(JUNITDIR)/junit.jar
