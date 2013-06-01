package edu.isi.ikcap.wings.workflows.util;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertiesHelper {
	public final static String WINGS_PROPERTIES_FILE = "wings.properties";

	private final static int DEFAULT_FACTORY = WflowGenFactory.INTERNALOWL;
	private final static String DEFAULT_DOMAIN = "se18";

	private static Properties conf;
	private static String wings_home = 
		(System.getProperty("wings.home") != null) ? System.getProperty("wings.home") : System.getProperty("user.dir");

	private static HashMap<String, String> pcnsmap = null;
	private static HashMap<String, String> dcnsmap = null;

	private static HashMap<String, Logger> classLoggers = new HashMap<String, Logger>();

	private static String ontdir,logdir,resdir,opdir;

	private static boolean dont_use_logging = false;
	
	public static boolean createDir(String dir) {
		if (dir == null)
			return false;
		File f = new File(dir);
		if (f.exists() && !f.isDirectory()) {
			System.err.println("Error: '" + f.getAbsolutePath() + "' is not a directory !");
			return false;
		}
		if (!f.exists() && !f.mkdir()) {
			System.err.println("Error: Cannot create directory '" + f.getAbsolutePath() + "'");
			return false;
		}
		return true;
	}
	
	public static void disableLogging() {
		dont_use_logging = true;
	}
	
	public static boolean getLoggingStatus() {
		return dont_use_logging;
	}
	
	public static File getDir(String dir) {
		if (dir == null)
			return null;
		File f = new File(dir);
		if (f.exists() && f.isDirectory()) {
			return f;
		}
		return null;
	}

	public static String getProposedLogFileName(String id) {
		if (!createDir(PropertiesHelper.getLogDir())) {
			String tmpdir = System.getProperty("java.io.tmpdir");
			System.err.println("Using temporary directory for logging: " + tmpdir);
			PropertiesHelper.setLogDir(tmpdir);
		}
		return PropertiesHelper.getLogDir() + "/" + id + ".log";
	}

	private static void addFileAppender(Logger logger, String id) {
		if (logger.getAppender(id) != null)
			return;

		String filename = getProposedLogFileName(id);
		try {
			FileAppender fileappender = new FileAppender(new SimpleLayout(), filename);
			fileappender.setAppend(false);
			fileappender.setName(id);
			logger.addAppender(fileappender);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Logger getLogger(String classname, String id) {
		Logger logger = classLoggers.get(classname);
		if (logger == null) {
			logger = Logger.getLogger(classname);
			classLoggers.put(classname, logger);

			if (id != null && !dont_use_logging)
				addFileAppender(logger, id);
		}
		return logger;
	}

	public static void removeLogger(String classname) {
		Logger logger = classLoggers.get(classname);
		if (logger != null) {
			classLoggers.remove(classname);
			logger.removeAllAppenders();
		}
	}

	public static Properties loadWingsProperties() {
		return loadWingsProperties(null);
	}

  /**
   * resets the Properities conf
   */
  public static void resetProperties() {
    ontdir = logdir = resdir = opdir = null;
    conf = null;
    pcnsmap = dcnsmap = null;
    classLoggers.clear();
    dont_use_logging = false;
  }
  
	public static Properties loadWingsProperties(String properties_file) {
		if (conf != null)
			return conf;
		
		if (properties_file == null)
			properties_file = AWGLoggerHelper.getPathToProperties(WINGS_PROPERTIES_FILE);

		File propfile = new File(properties_file);
		if (!propfile.exists()) {
			if (wings_home != null) {
				properties_file = wings_home + "/config/wings.properties";
				System.out.println("Using default configuration");
			}
		}
		conf = loadProperties(properties_file);
		return conf;
	}
	
	private static Properties loadProperties(String properties_file) {
		Properties conf = new Properties();
		try {
			System.out.println("Loading config from " + properties_file);
			conf.load(new FileInputStream(properties_file));
		} catch (IOException e) {
			System.err.println("Error loading config file: Cannot open");
			return null;
		}
		return conf;
	}

	public static String getPCDomain() {
		// Get DC and PC Domain from Properties File
		loadWingsProperties();
		String PCDomain = conf.getProperty("pc.domain");
		if (PCDomain == null)
			PCDomain = DEFAULT_DOMAIN;
		return PCDomain;
	}

	public static String getDCDomain() {
		loadWingsProperties();
		String DCDomain = conf.getProperty("dc.domain");
		if (DCDomain == null)
			DCDomain = DEFAULT_DOMAIN;
		return DCDomain;
	}

	public static String getTemplateDomain() {
		loadWingsProperties();
		String TemplateDomain = conf.getProperty("template.domain");
		if (TemplateDomain == null)
			TemplateDomain = DEFAULT_DOMAIN;
		return TemplateDomain;
	}

	public static String getGraphvizPath() {
		loadWingsProperties();
		return conf.getProperty("graphviz.path");
	}

	public static void setLogDir(String dir) {
		loadWingsProperties();
		logdir = dir;
		conf.setProperty("logs.dir", dir);
	}

	public static String getLogDir() {
		loadWingsProperties();
		if (logdir != null)
			return logdir;
		
		logdir = conf.getProperty("logs.dir");
		if (logdir == null || !new File(logdir).isDirectory()) {
			if (wings_home != null) {
				logdir = wings_home + "/logs";
				System.out.println("Using default logs directory: " + logdir);
			}
		}
		return logdir;
	}

	public static void setOutputDir(String dir) {
		loadWingsProperties();
		opdir = dir;
		conf.setProperty("output.dir", dir);
	}

	public static String getOutputDir() {
		loadWingsProperties();
		if (opdir != null)
			return opdir;
		opdir = conf.getProperty("output.dir");
		if (opdir == null || !new File(opdir).isDirectory()) {
			if (wings_home != null) {
				opdir = wings_home + "/output";
				System.out.println("Using default output directory: " + opdir);
			}
		}
		return opdir;
	}

	public static void setResourceDir(String dir) {
		loadWingsProperties();
		resdir = dir;
		conf.setProperty("resource.dir", dir);
	}
	
	public static String getResourceDir() {
		loadWingsProperties();
		if (resdir != null)
			return resdir;
		
		resdir = conf.getProperty("resource.dir");
		if (resdir == null || !new File(resdir).isDirectory()) {
			if (wings_home != null) {
				resdir = wings_home + "/src/main/resources";
				System.out.println("Using default resource directory: " + resdir);
			}
		}
		return resdir;
	}

	public static String getOntologyDir() {
		loadWingsProperties();
		if (ontdir != null)
			return ontdir;

		ontdir = conf.getProperty("ontology.root.dir");
		if (ontdir == null || !new File(ontdir).isDirectory()) {
			if (wings_home != null) {
				ontdir = wings_home + "/ontology";
				System.out.println("Using default ontology directory: " + ontdir);
			}
		}
		return ontdir;
	}

	public static void setOntologyDir(String dir) {
		loadWingsProperties();
		ontdir = dir;
		conf.setProperty("ontology.root.dir", dir);
	}
	
	public static String getOntologyURL() {
		loadWingsProperties();
		return conf.getProperty("ontology.root.url");
	}

	public static String getWorkflowOntologyPath() {
		loadWingsProperties();
		return conf.getProperty("ontology.wflow.path");
	}
	
	public static String getWorkflowOntologyURL() {
		return getOntologyURL() + "/" + getWorkflowOntologyPath();
	}

	public static WflowGenFactory getPCFactory() {
		loadWingsProperties();
		String PCFactory = conf.getProperty("pc.factory");
		WflowGenFactory PCWflowFac = null;
		if (PCFactory != null) {
			if (PCFactory.equals("internal"))
				PCWflowFac = new WflowGenFactory(WflowGenFactory.INTERNALOWL);
		}
		if(PCWflowFac == null) 
			PCWflowFac = new WflowGenFactory(DEFAULT_FACTORY);
		return PCWflowFac;
	}

	public static WflowGenFactory getDCFactory() {
		loadWingsProperties();
		String DCFactory = conf.getProperty("dc.factory");
		WflowGenFactory DCWflowFac = null;
		if (DCFactory != null) {
			if (DCFactory.equals("internal"))
				DCWflowFac = new WflowGenFactory(WflowGenFactory.INTERNALOWL);
		}
		if(DCWflowFac == null) 
			DCWflowFac = new WflowGenFactory(DEFAULT_FACTORY);
		return DCWflowFac;
	}

	public static boolean getProvenanceFlag() {
		loadWingsProperties();
		try {
			return Boolean.parseBoolean(conf.getProperty("storeprovenance"));
		} catch (Exception e) {
			return false;
		}
	}

	public static int getTrimmingNumber() {
		loadWingsProperties();
		try {
			return Integer.parseInt(conf.getProperty("trimming.numworkflows"));
		} catch (Exception e) {
			return 0;
		}
	}
	
	public static String getOutputFormat() {
		loadWingsProperties();
		String oformat = conf.getProperty("output.format");
		if (oformat == null)
			oformat = "xml";
		return oformat;
	}

	private static HashMap<String, String> getKeyValueMap(String pref, String suf) {
		HashMap<String, String> map = new HashMap<String, String>();
		String fac = conf.getProperty(pref + ".factory");
		String dom = conf.getProperty(pref + ".domain");
		String pattern = pref + "\\." + fac + "\\.([^\\.]+)\\." + suf + "\\.(.+)";
		Pattern pat = Pattern.compile(pattern);
		for (Object o : conf.keySet()) {
			String s = (String) o;
			Matcher m = pat.matcher(s);
			if (m.find()) {
				String mdom = m.group(1);
				String mkey = m.group(2);
				String value = conf.getProperty(s);
				if (mdom.equals(dom))
					map.put(mkey, value);
				else if (mdom.equals("*") || !map.containsKey(mkey))
					map.put(mkey, value);
			}
		}
		return map;
	}

	public static HashMap<String, String> getDCPrefixNSMap() {
		loadWingsProperties();
		if (dcnsmap == null)
			dcnsmap = getKeyValueMap("dc", "ns");
		return dcnsmap;
	}

	public static HashMap<String, String> getPCPrefixNSMap() {
		loadWingsProperties();
		if (pcnsmap == null)
			pcnsmap = getKeyValueMap("pc", "ns");
		return pcnsmap;
	}

	private static String getPropertyForCurrentDomain(String pref, String prop) {
		loadWingsProperties();
		String fac = conf.getProperty(pref + ".factory");
		String dom = conf.getProperty(pref + ".domain");
		String tmp = conf.getProperty(pref + "." + fac + "." + dom + "." + prop);
		if (tmp == null)
			tmp = conf.getProperty(pref + "." + fac + ".*." + prop);
		return tmp;
	}

	public static String getDCPropertyForCurrentDomain(String prop) {
		return getPropertyForCurrentDomain("dc", prop);
	}

	public static String getPCPropertyForCurrentDomain(String prop) {
		return getPropertyForCurrentDomain("pc", prop);
	}
	
	public static String getPCDomainDir() {
		String dirstr = PropertiesHelper.getPCPropertyForCurrentDomain("directory");
		if(dirstr != null && getDir(dirstr) != null) {
			return dirstr;
		}
		// Use Default otherwise
		return getPCDir()+"/"+getPCDomain();
	}
	
	public static String getDCDomainDir() {
		String dirstr = PropertiesHelper.getDCPropertyForCurrentDomain("directory");
		if(dirstr != null && getDir(dirstr) != null) {
			return dirstr;
		}
		// Use Default otherwise
		return getDCDir()+"/"+getDCDomain();
	}

	public static String getTemplatesDir() {
		String dirstr = conf.getProperty("template."+getTemplateDomain()+".directory");
		if(dirstr != null && getDir(dirstr) != null) {
			return dirstr;
		}
		// Use Default otherwise
		return getOntologyDir()+"/"+getTemplateDomain();
	}

	public static String getSeedsDir() {
		String dirstr = conf.getProperty("seed."+getTemplateDomain()+".directory");
		if(dirstr != null && getDir(dirstr) != null) {
			return dirstr;
		}
		// Use Default otherwise
		return getOntologyDir()+"/"+getTemplateDomain()+"/seeds";
	}
	
	public static String getPCDir() {
		return getOntologyDir()+"/ac";
	}
	
	public static String getDCDir() {
		return getOntologyDir()+"/dc";
	}

	public static String getPCURL() {
		return getOntologyURL()+"/ac";
	}
	
	public static String getDCURL() {
		return getOntologyURL()+"/dc";
	}
	
	public static String getPCDomainURL() {
		return getPCURL()+"/"+getPCDomain();
	}
	
	public static String getDCDomainURL() {
		return getDCURL()+"/"+getDCDomain();
	}
	
	public static String getTemplateURL() {
		return getOntologyURL()+"/"+getTemplateDomain();
	}
	
	public static String getSeedURL() {
		return getTemplateURL()+"/seeds";
	}
	
	public static String getPCNewComponentPrefix() {
		return getPCPropertyForCurrentDomain("componentns");
	}

	public static String getDCNewDataPrefix() {
		return getDCPropertyForCurrentDomain("datans");
	}

	public static String getQueryNamespace() {
		loadWingsProperties();
		return conf.getProperty("query.ns.wfq");
	}

	public static String getQueryVariablesNamespace() {
		loadWingsProperties();
		return conf.getProperty("query.ns.wfqv");
	}
	
	
	/**
	 * Set domain paths dynamically from a given directory
	 */
	public static boolean setDomainDir(String dirstr) {
		File dir = getDir(dirstr);
		if(dir == null) {
			System.err.println("Error: Domain Directory '"+dirstr+"' does not Exist !");
			return false;
		}
		dirstr = dir.getAbsolutePath();
		Properties props = loadProperties(dirstr+"/domain.properties");
		if(props == null) {
			System.err.println("Domain isn't configured correctly. domain.properties file is missing");
			return false;
		}
		loadWingsProperties();
		
		// Override Wings Properties
		for(Object prop: props.keySet()) {
			if(prop != null)
				conf.setProperty(prop.toString(), props.getProperty(prop.toString()));
		}
		conf.setProperty("pc.internal."+getPCDomain()+".directory", dirstr+"/component_catalog");
		conf.setProperty("dc.internal."+getDCDomain()+".directory", dirstr+"/data_catalog");
		conf.setProperty("pc.internal."+getPCDomain()+".components.dir", dirstr+"/component_catalog/bin");
		conf.setProperty("dc.internal."+getDCDomain()+".data.dir", dirstr+"/data_catalog/data");
		conf.setProperty("template."+getTemplateDomain()+".directory", dirstr+"/templates");
		conf.setProperty("seed."+getTemplateDomain()+".directory", dirstr+"/templates/seeds");
		// Re-initialize namespace maps
		pcnsmap = null;
		dcnsmap = null;
		return true;
	}
	
	public static boolean initDomainDir(String dirstr) {
		if(getDir(dirstr) != null) {
			System.err.println("Error: Directory '"+dirstr+"' already Exists !");
			return false;
		}
		if(!createDir(dirstr)) return false;
		File dir = getDir(dirstr);
		dirstr = dir.getAbsolutePath();
		String domain = dir.getName().replaceAll("/[^a-zA-Z0-9_]/", "_");
		Properties props = new Properties();
		props.setProperty("pc.domain", domain);
		props.setProperty("dc.domain", domain);
		props.setProperty("template.domain", domain);
		props.setProperty("pc.factory", "internal");
		props.setProperty("dc.factory", "internal");
		props.setProperty("output.format", "shell");
		props.setProperty("pc.internal."+domain+".ns.acdom", "http://wings-workflows.org/ontology/ac/"+domain+"/library.owl#");
		props.setProperty("dc.internal."+domain+".ns.dcdom", "http://wings-workflows.org/ontology/dc/"+domain+"/ontology.owl#");
		props.setProperty("dc.internal."+domain+".ns.dclib", "http://wings-workflows.org/ontology/dc/"+domain+"/library.owl#");
		try {
			FileOutputStream out = new FileOutputStream(dirstr+"/domain.properties");
			props.store(out, "Do Not Edit this until you know what you are doing");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		if(!createDir(dirstr+"/component_catalog")) return false;
		if(!createDir(dirstr+"/component_catalog/bin")) return false;
		if(!createDir(dirstr+"/data_catalog")) return false;
		if(!createDir(dirstr+"/data_catalog/data")) return false;
		if(!createDir(dirstr+"/templates")) return false;
		if(!createDir(dirstr+"/templates/seeds")) return false;
		
		if(!NewDomainStubsWriter.writeDataOntologyStubTTL(domain, dirstr+"/data_catalog/ontology.owl"));
		if(!NewDomainStubsWriter.writeDataLibraryStubTTL(domain, dirstr+"/data_catalog/library.owl"));
		if(!NewDomainStubsWriter.writeComponentLibraryStubTTL(domain, dirstr+"/component_catalog/library.owl"));
		if(!NewDomainStubsWriter.writeComponentRulesStub(domain, dirstr+"/component_catalog/library.rules"));
		System.out.println("Initialized Domain '"+domain+"' at '"+dirstr+"'");
		return true;
	}
}
