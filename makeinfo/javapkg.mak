#Include this piece of makefile to set up tags for building one package
#directory in a java hierarchy.

#Pick up definitions of general stuff
include $(MAKEINFODIR)/javadefs.mak

#Allow local overrides from TOPDIR
-include $(TOPDIR)/localdefs.mak

#Required variables:
#TOPDIR= the top-level directory, containing the src directory and jar file,
#         typically ../../.. or something similar.

ifdef JUNITTEST
DESTDIR = $(TOPDIR)/testobj
SOURCEPATH = $(TOPDIR)/test:$(TOPDIR)/src
CLASSPATH = $(DESTDIR):$(TOPDIR)/jiviewer.jar:$(JUNITJAR)
else
DESTDIR = $(TOPDIR)/obj
SOURCEPATH = $(TOPDIR)/src
CLASSPATH = $(DESTDIR):$(JUNITJAR)
endif

SRCS	= *.java

default:	classes jarfile

classes:;	@echo $(JAVAC) $(JAVAC_DEBUG_OPTS) -d $(DESTDIR) \
			-classpath $(CLASSPATH) \
			-sourcepath $(SOURCEPATH) $(SRCS)
		@$(JAVAC) $(JAVAC_DEBUG_OPTS) -d $(DESTDIR) \
			-classpath $(CLASSPATH) \
			-sourcepath $(SOURCEPATH) $(SRCS)

jarfile:;	(cd $(TOPDIR); $(MAKE) jaronly)

runtest:;	$(JAVA) -classpath $(CLASSPATH) \
			junit.textui.TestRunner $(JUNITTEST)

guitest:;	$(JAVA) -classpath $(CLASSPATH) \
			junit.swingui.TestRunner $(JUNITTEST)

clean:		cleanclasses $(MORECLEANTARGETS)

cleanclasses:;		rm -f *.class
