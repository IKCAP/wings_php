if [ -n "${JAVA_HOME}" -a -x "${JAVA_HOME}/bin/java" ]; then
  java="${JAVA_HOME}/bin/java"
else
  java=java
fi

LIBDIR="${WINGS_HOME}/lib"
PELLET="${LIBDIR}/Pellet"
JENADIR="${PELLET}/jena"

export CLASSPATH="${LIBDIR}/wings.jar:${JENADIR}/arq-2.8.1.jar:${JENADIR}/icu4j-3.4.4.jar:${JENADIR}/iri-0.7.jar:${JENADIR}/jena-2.6.2.jar:${JENADIR}/log4j-1.2.13.jar:${JENADIR}/slf4j-api-1.5.6.jar:${JENADIR}/slf4j-log4j12-1.5.6.jar:${JENADIR}/xercesImpl-2.7.1.jar:${PELLET}/xsdlib/relaxngDatatype.jar:${PELLET}/xsdlib/xsdlib.jar:${PELLET}/aterm-java-1.6.jar:${PELLET}/pellet-core.jar:${PELLET}/pellet-datatypes.jar:${PELLET}/pellet-el.jar:${PELLET}/pellet-jena.jar:${PELLET}/pellet-rules.jar:${LIBDIR}/JSON/gson-1.4.jar:${LIBDIR}/Getopt/java-getopt-1.0.12.jar:${LIBDIR}/SWT/swt.jar:${LIBDIR}/MySQL/mysql-connector-java-5.1.6-bin.jar:${LIBDIR}/KBAPI/kbapi.jar:${LIBDIR}/GEF/zest.layouts.jar:${LIBDIR}/GEF/draw2d.jar:${LIBDIR}/GEF/zest.core.jar:${LIBDIR}/Jface/eclipse.core.commands.jar:${LIBDIR}/Jface/eclipse.equinox.common.jar:${LIBDIR}/Jface/eclipse.jface.jar:${LIBDIR}/Jface/eclipse.osgi.jar"

if [ "`uname`" == "Darwin" ]; then
   addon="-XstartOnFirstThread -Dorg.eclipse.swt.internal.carbon.smallFonts=1"
fi

AWG_CONF="file:${WINGS_HOME}/config/log-awg.config"
WGUI_CONF="file:${WINGS_HOME}/config/log-wgui.config"
PELLET_CONF="file:${WINGS_HOME}/config/pellet.properties"
