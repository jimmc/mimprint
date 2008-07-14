@echo off
REM You must install the scala jar files in order to use this app.
set scomp=scala-compiler.jar
set slib=scala-library.jar
set mlib=mimprint.jar
set classpath=%scomp%;%slib%;%mlib%
set smain=scala.tools.nsc.MainGenericRunner
set mmain=net.jimmc.mimprint.AppStart
java -cp %classpath% %smain% %mmain% %1%
