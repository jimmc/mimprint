#Makefile for jiviewer

PROJECT = jiviewer
BASENAME = jiviewer
TOPDIR = .
ARCHDIR = Arch
MAKEINFODIR = makeinfo
include $(MAKEINFODIR)/javadefs.mak
-include $(TOPDIR)/localdefs.mak

CLASSPATH      = obj
SOURCEPATH     = src

#MAIN refers to the main java sources, not including test sources
MAIN_SRCS = $(wildcard \
		src/jimmc/swing/*.java \
		src/jimmc/jiviewer/*.java )
MAIN_OBJS = $(MAIN_SRCS:src/%.java=obj/%.class)
MAIN_PROPS    = src/jimmc/swing/*.properties \
		src/jimmc/jiviewer/*.properties
MAIN_MAKEFILES = src/jimmc/swing/Makefile \
		src/jimmc/jiviewer/Makefile

#TEST refers to the java sources for unit testing
TEST_SRCS = $(wildcard \
		test/jimmc/swing/*.java \
		test/jimmc/jiviewer/*.java )
TEST_OBJS = $(TEST_SRCS:test/%.java=testobj/%.class)
TEST_PROPS    = test/jimmc/swing/*.properties \
		test/jimmc/jiviewer/*.properties
TEST_MAKEFILES = test/jimmc/swing/Makefile \
		test/jimmc/jiviewer/Makefile

#SRCS is all java source files, both regular and test
SRCS    = $(MAIN_SRCS) $(TEST_SRCS)
OBJS	= $(MAIN_OBJS) $(TEST_OBJS)
PROPS	= $(MAIN_PROPS) $(TEST_PROPS)
MAKEFILES = $(MAIN_MAKEFILES) $(TEST_MAKEFILES)

PKGS          = jimmc.swing jimmc.jiviewer

VERSION       = $(shell cat misc/Version | awk '{print $$2}')
_VERSION      = $(shell cat misc/Version | awk '{print $$2}' | \
			sed -e 's/\./_/g' -e 's/v//')
RELDIR        = $(BASENAME)-$(_VERSION)
RELFILES      = README README.build README.html $(JARFILE) \
		misc/Version misc/COPYING misc/COPYRIGHT misc/HISTORY
RELDATFILES   = dat/format.dat dat/kingmacbeth.dat
KITMISC       = README README.build README.html \
		misc/Version misc/COPYING misc/COPYRIGHT misc/HISTORY \
		misc/manifest.mf \
		makeinfo/*.mak doc/*.html
KITSRCS       = $(KITMISC) Makefile $(MAKEFILES) $(SRCS)

JARFILE       = $(BASENAME).jar
JARMANIFEST   = misc/manifest.mf
JAROBJS       = $(OBJS)
JARPROPS      = $(PROPS)
JAR_LIST_OBJS = jimmc/swing/*.class \
		jimmc/jiviewer/*.class
JAR_LIST_PROPS = jimmc/swing/*.properties \
		jimmc/jiviewer/*.properties

default:	objdir jar

all:		objdir jar doc

objdir:;	[ -d obj ] || mkdir obj
		[ -d testobj ] || mkdir testobj

classfiles:	$(JAROBJS)

jar:		$(JARFILE)

$(JARFILE):	$(JAROBJS) $(JARMANIFEST) jaronly

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
rel:;		mkdir $(RELDIR)
		cp -p $(RELFILES) $(RELDIR)
		mkdir $(RELDIR)/dat
		cp -p $(RELDATFILES) $(RELDIR)/dat
		mkdir $(RELDIR)/src
		tar cf - $(KITSRCS) | (cd $(RELDIR)/src && tar xf -)
		mkdir $(RELDIR)/doc
		find doc \
		    -name \*.html -print -o \
		    -name \*.css -print -o \
		    -name package-list -print | \
			tar cf - --files-from - | \
			(cd $(RELDIR) && tar xf - )

rel_examples:
		mkdir $(RELDIR)/examples
		find examples -name CVS -prune -o \
			-name '*.jpg' -print | \
			tar cf - --files-from - | \
			(cd $(RELDIR) && tar xf - )

#After making the release directory, make a zip file for it
relzip:;	zip -r $(RELDIR).zip $(RELDIR)

#After making the release directory, make a gzipped tar file for it
relgz:;		tar czf $(RELDIR).tgz $(RELDIR)

#Make a kit directory to distribute the source
kit:;		mkdir kit
		tar cf - $(KITSRCS) | (cd kit && tar xf -)

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
