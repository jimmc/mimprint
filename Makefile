#Makefile for jiviewer

PROJECT      := jiviewer
PKGS         := jimmc.swing jimmc.util jimmc.jiviewer
MAINPKG      := jimmc.jiviewer

PKGDIRS      := $(subst .,/,$(PKGS))
MAINPKGDIR   := $(subst .,/,$(MAINPKG))
BASENAME     := $(PROJECT)
TOPDIR       := .
ARCHDIR      := Arch
MAKEINFODIR  := makeinfo
include $(MAKEINFODIR)/javadefs.mak
-include $(TOPDIR)/localdefs.mak

CLASSPATH      = obj
SOURCEPATH     = src

#MAIN refers to the main java sources, not including test sources
MAIN_SRCS    := $(wildcard $(PKGDIRS:%=src/%/*.java))
MAIN_OBJS    := $(MAIN_SRCS:src/%.java=obj/%.class)
MAIN_PROPS   := $(wildcard $(PKGDIRS:%=src/%/*.properties))
MAIN_MAKEFILES := $(PKGDIRS:%=src/%/Makefile)

#TEST refers to the java sources for unit testing
TEST_SRCS    := $(wildcard $(PKGDIRS:%=test/%/*.java))
TEST_OBJS    := $(TEST_SRCS:test/%.java=testobj/%.class)
TEST_PROPS   := $(wildcard $(PKGDIRS:%=test/%/*.properties))
TEST_MAKEFILES := $(PKGDIRS:%=test/%/Makefile)

#SRCS is all java source files, both regular and test
SRCS         := $(MAIN_SRCS) $(TEST_SRCS) $(JARINST_SRCS)
OBJS	     := $(MAIN_OBJS) $(TEST_OBJS)
PROPS	     := $(MAIN_PROPS) $(TEST_PROPS)
MAKEFILES    := $(MAIN_MAKEFILES) $(TEST_MAKEFILES)
SRCHTMLS     := $(wildcard $(PKGDIRS:%=src/%/*.html))

VERSION      := $(shell cat VERSION | awk '{print $$2}')
_VERSION     := $(shell cat VERSION | awk '{print $$2}' | \
			sed -e 's/\./_/g' -e 's/v//')
VDATE        := $(shell cat VERSION | awk '{print $$3, $$4, $$5}')
JARFILE      := $(BASENAME).jar
JARMANIFEST  := misc/manifest.mf
JAROBJS      := $(OBJS)
PROPFILE     := src/jimmc/jiviewer/Resources.properties
PROPS        := $(wildcard $(PKGDIRS:%=src/%/*.props))

JAR_LIST_OBJS := $(PKGDIRS:%=%/*.class)
JAR_LIST_PROPS:= $(MAIN_PROPS:src/%=%)

RELDIR       := $(BASENAME)-$(_VERSION)
RELBINFILES  := README VERSION $(JARFILE)
RELSRCS      := Makefile makeinfo/*.mak $(SRCS)

default:	objdir jar

all:		objdir jar doc

objdir:;	[ -d obj ] || mkdir obj
		[ -d testobj ] || mkdir testobj

classfiles:	$(JAROBJS)

jar:		$(JARFILE)

$(JARFILE):	$(JAROBJS) $(JARMANIFEST) propfile jaronly

propfile:	$(PROPFILE)

$(PROPFILE):	$(PROPS) VERSION
		rm -f $(PROPFILE)
		echo "#This file is automatically created from the *.props files" > $(PROPFILE)
		echo "#in various source directories." >> $(PROPFILE)
		for f in $(PROPS); do \
			echo "" >> $(PROPFILE); \
			echo "#===== $$f =====" >> $(PROPFILE); \
			cat $$f | sed -e 's/%VERSION%/$(VERSION)/g' \
			  -e 's/%VDATE%/$(VDATE)/g' >> $(PROPFILE); \
		done

jaronly:;	cd obj && $(JAR) -cmf ../$(JARMANIFEST) ../$(JARFILE) \
			$(JAR_LIST_OBJS)
		cd src && $(JAR) -uf ../$(JARFILE) $(JAR_LIST_PROPS)
		#cd testobj && $(JAR) -uf ../$(JARFILE) $(JAR_LIST_OBJS)
		#cd test && $(JAR) -uf ../$(JARFILE) $(JAR_LIST_PROPS)

$(MAIN_OBJS):	javacmain

$(TEST_OBJS):	javactest

#javac:		javacmain javactest
javac:		javacmain

javacmain:;	@echo Building main classes
		@echo $(JAVAC) $(JAVAC_DEBUG_OPTS) \
			-classpath $(CLASSPATH) -d obj \
			-sourcepath $(SOURCEPATH) $(MAIN_SRCS)
		@$(JAVAC) $(JAVAC_DEBUG_OPTS) \
			-classpath $(CLASSPATH) -d obj \
			-sourcepath $(SOURCEPATH) $(MAIN_SRCS)

javactest:;	@echo Building test classes
		@echo $(JAVAC) $(JAVAC_DEBUG_OPTS) \
			-classpath $(TESTCLASSPATH) -d testobj \
			-sourcepath $(TESTSOURCEPATH) $(TEST_SRCS)
		@$(JAVAC) $(JAVAC_DEBUG_OPTS) \
			-classpath $(TESTCLASSPATH) -d testobj \
			-sourcepath $(TESTSOURCEPATH) $(TEST_SRCS)

runtest:;	cd test/jimmc/jiviewer && $(MAKE) alltest

doc:		docdir jiviewer_doc

docdir:;	[ -d doc/api ] || mkdir doc/api

jiviewer_doc:;	$(JAVADOC) -J-mx50m -d doc/api -classpath $(CLASSPATH) \
			-sourcepath $(SOURCEPATH) \
			-overview misc/overview.html \
			$(PKGS)

#Make the release directory, including source and docs
rel:		relbin relsrc

#Make the release directory with just the binary-kit files
relbin:;	mkdir $(RELDIR)
		cp -p $(RELBINFILES) $(RELDIR)

relsrc:;	zip -r $(RELDIR)/src.zip $(RELSRCS)

#After making the release directory, make a zip file for it
relzip:;	zip -r $(RELDIR).zip $(RELDIR)

#After making the release directory, make a gzipped tar file for it
relgz:;		tar czf $(RELDIR).tgz $(RELDIR)

#After making relgz and relzip, copy those files and the README file into
#the archive directory with the appropriate names.
arch:;		cp -p $(RELDIR).zip $(ARCHDIR)
		cp -p $(RELDIR).tgz $(ARCHDIR)
		cp -p README.html $(ARCHDIR)/README-$(_VERSION).html

#Set the CVS tag for the current release
cvstag:;	cvs rtag $(BASENAME)-$(_VERSION) $(PROJECT)

#Clean does not remove doc/api; use docclean for that
clean:;		rm -f $(JARFILE)
		rm -rf obj/* testobj/*

docclean:;	rm -rf doc/api

wc:;		wc $(SRCS)

wcmain:;	wc $(MAIN_SRCS)

wctest:;	wc $(TEST_SRCS)
