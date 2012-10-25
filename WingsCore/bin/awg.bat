@echo off

rem Generate Workflow Instances

set SCRIPTS_HOME=%~dp0
set WINGS_HOME=%~dp0/..
call "%SCRIPTS_HOME%setenv.bat"

shift
%java% "-Dwings.home='%WINGS_HOME%'" "-Dlog4j.configuration=%AWG_CONF%" "-Dpellet.configuration=%PELLET_CONF%" -Xmx1024M edu.isi.ikcap.wings.AWG %*
pause
