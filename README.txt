***** NOTE ******

This version is now deprecated. Please use the one at:
https://github.com/varunratnakar/wings

*****************

This repository contains the source for the Wings Project (http://www.wings-workflows.org/)

It Contains:

Wings Core Framework
--------------------
	- wings.war: Wings Tomcat Servlet
	- WingsCore/: Java Source code for the Wings Reasoner. 
		Compile (ant jar) to get wings.jar
	- OntologyAPI/: Dependency for Wings Core. 
		Compile (ant jar) to get kbapi.jar

	Note: Replace wings.jar and kbapi.jar in wings.war to get a new Wings Tomcat Servlet

Wings Workflow Portal
---------------------
	- WorkflowPortal/: PHP/Javascript Source code for the Workflow Portal. 
		Compile (./install) to get ModX package

	Note: Workflow Portal requires modx (www.modx.com) to be installed


For more details on Installation, please refer to:
	http://www.wings-workflows.org/documentation (Installation Guides Section)

