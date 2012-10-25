////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2007, Your Corporation. All Rights Reserved.
////////////////////////////////////////////////////////////////////////////////

package edu.isi.ikcap.wings;

import edu.isi.ikcap.wings.catalogs.components.ComponentCatalog;
import edu.isi.ikcap.wings.catalogs.data.DataCatalog;
import edu.isi.ikcap.wings.util.WGetOpt;
import edu.isi.ikcap.wings.util.logging.LogEvent;
import edu.isi.ikcap.wings.workflows.impl.StandaloneWorkflowGenerator;
import edu.isi.ikcap.wings.workflows.template.Seed;
import edu.isi.ikcap.wings.workflows.template.Template;
import edu.isi.ikcap.wings.workflows.template.sets.Binding;
import edu.isi.ikcap.wings.workflows.template.sets.WingsSet;
import edu.isi.ikcap.wings.workflows.template.variables.Variable;
import edu.isi.ikcap.wings.workflows.util.DAX;
import edu.isi.ikcap.wings.workflows.util.PropertiesHelper;
import edu.isi.ikcap.wings.workflows.util.UuidGen;
import edu.isi.ikcap.wings.workflows.util.WflowGenFactory;
import org.apache.log4j.Logger;

import com.google.gson.Gson;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Name: AWGTest
 * <p/>
 * Package: edu.isi.ikcap.workflows
 * <p/>
 * User: moody, varunr
 * <p/>
 * Date: Aug 28, 2007
 * <p/>
 * Time: 9:53:57 AM
 */
public class AWG {
	boolean workOnTemplate = false;
	
	String seedName;
	String templateName;
	
	String DCDomain, PCDomain, TemplateDomain;

	Logger logger;

	StandaloneWorkflowGenerator swg;

	ComponentCatalog pc;
	DataCatalog dc;

	boolean storeProvenance;

	String requestId;
	String seedId, templateId;
	
	Seed seed;
	Template template;

	public AWG(String requestName, String requestId, String propFile) {
		this.initHelper(requestName, requestId, propFile);
		// Current request is the same as a seed
		this.seedName = requestName;
	}

	public AWG(String requestName, String requestId, String propFile, boolean isTemplate) {
		this.workOnTemplate = isTemplate;
		this.initHelper(requestName, requestId, propFile);

		if(isTemplate) 
			this.templateName = requestName;
		else
			this.seedName = requestName;
	}
	
	private void initHelper(String requestName, String requestId, String propFile) {
		this.requestId = requestId;
		if (this.requestId == null) {
			this.requestId = UuidGen.generateAUuid(requestName);
		}

		PropertiesHelper.loadWingsProperties(propFile);
		logger = PropertiesHelper.getLogger(AWG.class.getName(), this.requestId);

		DCDomain = PropertiesHelper.getDCDomain();
		PCDomain = PropertiesHelper.getPCDomain();
		TemplateDomain = PropertiesHelper.getTemplateDomain();

		storeProvenance = PropertiesHelper.getProvenanceFlag();
	}
	
	
	public String getRequestId() {
		return this.requestId;
	}

	public File getLogFile() {
		File f = new File(PropertiesHelper.getProposedLogFileName(requestId));
		return f;
	}

	public ComponentCatalog initializePC(String libname) {
		// Initialize the PC
		LogEvent event = getEvent(LogEvent.EVENT_WG_INITIALIZE_PC);
		logger.info(event.createStartLogMsg().addWQ(LogEvent.DOMAIN, PCDomain));

		WflowGenFactory PCWflowFac = PropertiesHelper.getPCFactory();
		pc = PCWflowFac.getPC(libname, requestId);

		logger.info(event.createEndLogMsg());
		return pc;
	}

	public void initializeWorkflowGenerator() {
		swg = new StandaloneWorkflowGenerator(dc, pc, TemplateDomain, storeProvenance, requestId);
		swg.setProvenanceFlag(storeProvenance);

	}

	public DataCatalog initializeDC() {
		LogEvent event = getEvent(LogEvent.EVENT_WG_INITIALIZE_DC);
		logger.info(event.createStartLogMsg().addWQ(LogEvent.DOMAIN, DCDomain));

		WflowGenFactory DCWflowFac = PropertiesHelper.getDCFactory();
		dc = DCWflowFac.getDC(requestId);

		logger.info(event.createEndLogMsg());
		return dc;
	}

	public void setDC(DataCatalog dc) {
		this.dc = dc;
		swg.setDc(dc);
	}

	public void setPC(ComponentCatalog pc) {
		this.pc = pc;
		swg.setPc(pc);
	}

	public void initializeItem() {
		if(this.workOnTemplate) 
			initializeTemplate();
		else
			initializeSeed();
	}
	
	private void initializeSeed() {
		LogEvent event = getEvent(LogEvent.EVENT_WG_LOAD_SEED);
		logger.info(event.createStartLogMsg().addWQ(LogEvent.SEED_NAME, seedName));

		seed = swg.initializeSeed(seedName);
		seedId = seed.getSeedId();

		logger.info(event.createLogMsg().addWQ(LogEvent.SEED_ID, seedId));
		logger.info(event.createEndLogMsg());
	}

	private void initializeTemplate() {
		LogEvent event = getEvent(LogEvent.EVENT_WG_LOAD_SEED);
		logger.info(event.createStartLogMsg().addWQ(LogEvent.TEMPLATE, templateName));

		template = swg.initializeTemplate(templateName);
		templateId = template.getID();

		logger.info(event.createLogMsg().addWQ(LogEvent.TEMPLATE_ID, templateId));
		logger.info(event.createEndLogMsg());
	}

	private LogEvent getEvent(String evid) {
		return new LogEvent(evid, "Wings", LogEvent.REQUEST_ID, this.requestId);
	}

	public ArrayList<Seed> getCandidateSeeds() {
		LogEvent event = getEvent(LogEvent.EVENT_WG_GET_CANDIDATE_SEEDS);

		logger.info(event.createStartLogMsg());

		// Get contained seeds (TODO: unimplemented)
		ArrayList<Seed> seeds = swg.findCandidateSeeds(this.seed);
		logger.info(event.createLogMsg().addList(LogEvent.SEED, seeds));

		if (storeProvenance) {
			swg.getWgpc().addSeedsToProvenanceCatalog(seedId, seeds);
		}

		logger.info(event.createEndLogMsg());

		return seeds;
	}

	public ArrayList<Template> backwardSweep(ArrayList<Seed> candidates) {
		LogEvent event = getEvent(LogEvent.EVENT_WG_BACKWARD_SWEEP);
		logger.info(event.createStartLogMsg());

		ArrayList<Template> candidateWorkflows = new ArrayList<Template>();
		for (Seed candidate : candidates) {
			Template inferredTemplate = swg.getInferredTemplate((Template) candidate);
			ArrayList<Template> innerSpecialized = swg.specializeTemplates(inferredTemplate);
			for (Template template : innerSpecialized) {
				template.getRules().addRules(candidate.getSeedRules());
				template.setCreatedFrom(candidate);
				template.getMetadata().addCreationSource(candidate.getName() + "(Specialized)");
				candidateWorkflows.add(template);
			}
		}

		logger.info(event.createLogMsg()
				.addWQ(LogEvent.MSG, "Backward Sweep generated " + candidateWorkflows.size() + " from " + candidates.size() + " seeds."));

		if (storeProvenance) {
			swg.getWgpc().addCandidateWorkflowsToProvenanceCatalog(seedId, candidateWorkflows);
		}

		logger.info(event.createEndLogMsg());
		return candidateWorkflows;
	}

	public ArrayList<Template> selectInputData(ArrayList<Template> candidateWorkflows) {
		LogEvent event = getEvent(LogEvent.EVENT_WG_DATA_SELECTION);
		logger.info(event.createStartLogMsg());

		swg.setCurrentLogEvent(event);
		ArrayList<Template> boundWorkflows = new ArrayList<Template>();
		for (Template candidateWorkflow : candidateWorkflows) {
			ArrayList<Template> innerPartials = swg.selectInputDataObjects(candidateWorkflow);
			for (Template partial : innerPartials) {
				partial.setCreatedFrom(candidateWorkflow);
				partial.getMetadata().addCreationSource(candidateWorkflow.getCreatedFrom().getName() + "(Bound)");
				boundWorkflows.add(partial);
			}
		}

		logger.info(event.createLogMsg().addWQ(LogEvent.MSG,
				"Select Input Data Objects returned " + boundWorkflows.size() + " templates from " + candidateWorkflows.size() + " templates."));

		if (storeProvenance) {
			swg.getWgpc().addBoundWorkflowsToProvenanceCatalog(seedId, boundWorkflows);
		}

		logger.info(event.createEndLogMsg());

		return boundWorkflows;
	}

	public void getDataMetricsForInputData(ArrayList<Template> boundWorkflows) {
		LogEvent event = getEvent(LogEvent.EVENT_WG_FETCH_METRICS);
		logger.info(event.createStartLogMsg());

		swg.setCurrentLogEvent(event);

		swg.setDataMetricsForInputDataObjects(boundWorkflows);
		logger.info(event.createEndLogMsg());
	}

	public void writeDataSelections(ArrayList<Template> boundWorkflows, String file) {
		ArrayList<HashMap<String, String>> dataBindings = new ArrayList<HashMap<String, String>>();
		for (Template boundWorkflow : boundWorkflows) {
			HashMap<String, String> dataBinding = new HashMap<String, String>();
			for (Variable iv : boundWorkflow.getInputVariables()) {
				if (iv.isDataVariable()) {
					dataBinding.put(iv.getName(), iv.getBinding().toString());
				}
			}
			dataBindings.add(dataBinding);
		}
		try {
			PrintStream out = new PrintStream(new FileOutputStream(file));
			out.println(new Gson().toJson(dataBindings));
			out.close();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		// System.out.println(dataBinding);
	}
	
	public void writeTemplateRDF(Template t, String file) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(file));
			out.println(t.deriveInternalRepresentation());
			out.close();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		} 
	}

	public ArrayList<Template> forwardSweep(ArrayList<Template> boundWorkflows) {
		LogEvent event = getEvent(LogEvent.EVENT_WG_FORWARD_SWEEP);
		logger.info(event.createStartLogMsg());

		swg.setCurrentLogEvent(event);

		ArrayList<Template> configuredWorkflows = new ArrayList<Template>();
		for (Template boundWorkflow : boundWorkflows) {
			ArrayList<Template> instances = swg.configureTemplates(boundWorkflow);

			if (instances == null) {
				continue;
			}

			for (Template instance : instances) {
				if (instance.getRules() != null && instance.getRules().getRulesText() != null) {
					instance = instance.applyRules();
					if (instance == null) {
						logger.warn(event.createLogMsg().addWQ(LogEvent.MSG, "Invalid Workflow Instance " + instance + " : Template Rules not satisfied"));
						continue;
					}
				}
				instance.setCreatedFrom(boundWorkflow);
				instance.getMetadata().addCreationSource(boundWorkflow.getCreatedFrom().getCreatedFrom().getName() + "(Configured)");
				configuredWorkflows.add(instance);
			}
		}

		logger.info(event.createEndLogMsg());
		return configuredWorkflows;
	}

	public void writeParameterSelections(ArrayList<Template> configuredWorkflows, String file) {
		ArrayList<HashMap<String, String>> paramBindings = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, Binding>> paramBindings_b = new ArrayList<HashMap<String, Binding>>();
		
		for (Template configuredWorkflow : configuredWorkflows) {
			HashMap<String, Binding> paramBinding_b = new HashMap<String, Binding>();
			for (Variable iv : configuredWorkflow.getInputVariables()) {
				if (iv.isParameterVariable() && iv.getBinding() != null) {
					paramBinding_b.put(iv.getName(), iv.getBinding());
				}
			}
			paramBindings_b.add(paramBinding_b);
		}
		
		while(paramBindings_b.size() > 0) {
			boolean hasSets = false;
			HashMap<String, Binding> paramBinding_b = paramBindings_b.remove(0);
			HashMap<String, String> paramBinding = new HashMap<String, String>();
			for (String varid : paramBinding_b.keySet()) {
				Binding b = paramBinding_b.get(varid);
				if (b.isSet()) {
					for (WingsSet s : b) {
						HashMap<String, Binding> paramBinding_x = new HashMap<String, Binding>(paramBinding_b);
						paramBinding_x.put(varid, (Binding) s);
						paramBindings_b.add(paramBinding_x);
					}
					hasSets = true;
				} else
					paramBinding.put(varid, b.toString());
			}
			if(!hasSets)
				paramBindings.add(paramBinding);
		}
		try {
			PrintStream out = new PrintStream(new FileOutputStream(file));
			out.println(new Gson().toJson(paramBindings));
			out.close();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	public ArrayList<DAX> getDaxes(ArrayList<Template> configuredWorkflows) {
		ArrayList<DAX> daxes = new ArrayList<DAX>();
		int i = 1;
		int size = configuredWorkflows.size();
		for (Template instance : configuredWorkflows) {
			DAX dax = swg.getTemplateDAX(instance);
			if (dax != null) {
				dax.setInstanceId(instance.getID());
				dax.setIndex(i);
				dax.setTotalDaxes(size);
				dax.fillInputMaps(instance.getInputVariables());
				dax.fillIntermediateMaps(instance.getIntermediateVariables());
				dax.fillOutputMaps(instance.getOutputVariables());
				if (dax != null) {
					daxes.add(dax);
				}
				i++;
			}
		}

		ArrayList<String> daxids = new ArrayList<String>();

		// Store the request-id : dax-ids heirarchy in the log
		for (DAX dax : daxes) {
			daxids.add(dax.getID());
		}

		logger.info(LogEvent.createIdHierarchyLogMsg(LogEvent.REQUEST_ID, this.requestId, LogEvent.DAX_ID, daxids.iterator()));

		if (storeProvenance) {
			swg.getWgpc().addConfiguredWorkflowsToProvenanceCatalog(seedId, configuredWorkflows, daxes);
		}

		return daxes;
	}

	public void writeDaxes(ArrayList<DAX> daxes) {
		if (!PropertiesHelper.createDir(PropertiesHelper.getOutputDir())) {
			String tmpdir = System.getProperty("java.io.tmpdir");
			System.err.println("Using temporary directory: " + tmpdir);
			PropertiesHelper.setOutputDir(tmpdir);
		}

		String outputDir = PropertiesHelper.getOutputDir() + "/" + this.requestId;
		new File(outputDir).mkdir();
		for (DAX dax : daxes) {
			dax.write(outputDir + "/" + dax.getFile());
			if (dax.getOutputFormat() == DAX.SHELL) {
				File file = new File(outputDir + "/" + dax.getFile());
				file.setExecutable(true);
			}
			dax.writeMapping(outputDir + "/" + dax.getFile() + ".map");
		}
	}

	public void writeLogSummary(ArrayList<Template> candidateWorkflows, ArrayList<Template> boundWorkflows, ArrayList<Template> configuredWorkflows,
			ArrayList<DAX> daxes) {

		Seed currentSeed = swg.getCurrentSeed();
		logger.info("Workflow Generation produced " + candidateWorkflows.size() + " candidate workflows: ");
		for (Template template : candidateWorkflows) {
			logger.info("     Candidate Workflow: " + template.getID() + " " + template);
		}

		logger.info("and " + boundWorkflows.size() + " bound workflows: ");
		for (Template template : boundWorkflows) {
			logger.info("     Bound Workflow: " + template.getID() + " " + template);
		}

		logger.info("and " + configuredWorkflows.size() + " configured workflows: ");
		for (Template template : configuredWorkflows) {
			logger.info("     Configured Workflow: " + template.getID() + " " + template);
		}

		logger.info("and " + daxes.size() + " DAXes: ");
		for (DAX dax : daxes) {
			logger.info("     Executable Workflow: " + dax.getFile());
		}

		logger.info("Seed id: " + currentSeed.getSeedId());
	}

	static void copy(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	private ArrayList<Template> randomSelection(ArrayList<Template> items, int num) {
		ArrayList<Template> ret = new ArrayList<Template>();

		Random random = new Random();

		int size = items.size();
		logger.info("Pruning search space by randomly choosing " + num + " out of " + size + " templates");

		int[] indices = new int[size];
		for (int i = 0; i < size; i++) {
			indices[i] = 0;
		}

		for (int i = 0; i < num; i++) {
			boolean unique = false;
			int randomNo = 0;
			while (!unique) {
				randomNo = random.nextInt(size);
				if (indices[randomNo] == 0) {
					unique = true;
				}
				indices[randomNo] = 1;
			}
			logger.info("Choosing " + randomNo);
			ret.add(items.get(randomNo));
		}
		return ret;
	}
	
	public Seed getSeed() {
		return this.seed;
	}
	
	public Template getTemplate() {
		return this.template;
	}
	
	public StandaloneWorkflowGenerator getSWG() {
		return this.swg;
	}

	public static void main(String[] args) {
		HashMap<String, String> options = WGetOpt.getOptions("AWG", args);
		if (options == null) {
			System.exit(1);
		}

		// Load Wings Properties
		PropertiesHelper.loadWingsProperties(options.get("conf"));

		// Check if the user is initializing a custom domain
		if (options.get("initdomaindir") != null) {
			if (!PropertiesHelper.initDomainDir(options.get("initdomaindir"))) {
				System.exit(1);
			}
			System.exit(0);
		}

		// Check if the user is loading in a custom domain
		if (options.get("domaindir") != null) {
			if (!PropertiesHelper.setDomainDir(options.get("domaindir"))) {
				System.exit(1);
			}
		}

		if (options.get("logdir") != null) {
			PropertiesHelper.setLogDir(options.get("logdir"));
		}

		if (options.get("outputdir") != null) {
			PropertiesHelper.setOutputDir(options.get("outputdir"));
		}

		String itemid = null;
		boolean isTemplate = false;
		
		// Check That Seed or Template is provided
		if (options.get("seed") != null) {
			itemid = options.get("seed");
		}
		else if (options.get("template") != null) {
			itemid = options.get("template");
			isTemplate = true;
		}
		if(itemid == null) {
			System.err.println("Error: Seed or Template Not Specified");
			WGetOpt.displayUsage("AWG");
			System.exit(1);
		}
		
		AWG awg = new AWG(itemid, options.get("requestid"), options.get("conf"), isTemplate);

		LogEvent ev = awg.start();
		awg.initializePC(options.get("libname"));
		awg.initializeWorkflowGenerator();
		// Initialize the DC later
		// (DC initialization messes up Jena Maps and we can't load the
		// template)
		awg.setDC(awg.initializeDC());
		awg.initializeItem();
		
		// -------- Template/Seed Operations -------
		if (options.get("validate") != null) {
			awg.writeTemplateRDF(isTemplate ? awg.template : awg.seed, options.get("validate"));
			System.exit(0);
		}

		if (options.get("elaborate") != null) {
			Template it = awg.swg.getInferredTemplate(isTemplate ? awg.template : awg.seed);
			if(it == null) {
				awg.end(ev, 1);
			}
			awg.writeTemplateRDF(it, options.get("elaborate"));
			System.exit(0);
		}

		// ------- The Rest are Only Seed Operations -------
		if(isTemplate) {
			System.err.println("Error: Seed Required ( with -s ) for desired operation");
			WGetOpt.displayUsage("AWG");
			System.exit(1);
		}

		int trim = 0;
		if (options.get("trim") != null) {
			trim = Integer.parseInt(options.get("trim"));
		} else {
			trim = PropertiesHelper.getTrimmingNumber();
		}

		ArrayList<Seed> seeds = awg.getCandidateSeeds();
		if (seeds.size() == 0) {
			awg.end(ev, 1);
		}

		ArrayList<Template> candidates = awg.backwardSweep(seeds);
		if (candidates.size() == 0) {
			awg.end(ev, 1);
		}
		if (trim > 0 && candidates.size() > trim) {
			candidates = awg.randomSelection(candidates, trim);
		}

		ArrayList<Template> bindings = awg.selectInputData(candidates);
		if (bindings.size() == 0) {
			awg.end(ev, 1);
		}
		if (trim > 0 && bindings.size() > trim) {
			bindings = awg.randomSelection(bindings, trim);
		}

		if (options.get("getData") != null) {
			// Write selected data bindings
			awg.writeDataSelections(bindings, options.get("getData"));
			System.exit(0);
		}
		awg.getDataMetricsForInputData(bindings);

		ArrayList<Template> configurations = awg.forwardSweep(bindings);
		if (configurations.size() == 0) {
			awg.end(ev, 1);
		}
		if (trim > 0 && configurations.size() > trim) {
			configurations = awg.randomSelection(configurations, trim);
		}

		if (options.get("getParameters") != null) {
			// Write parameter bindings
			awg.writeParameterSelections(configurations, options.get("getParameters"));
			System.exit(0);
		}
		ArrayList<DAX> daxes = awg.getDaxes(configurations);
		if (daxes.size() == 0) {
			awg.end(ev, 1);
		}

		awg.writeDaxes(daxes);
		// awg.writeLogSummary(candidates, bindings, configurations, daxes);

		awg.end(ev, 0);
	}

	public LogEvent start() {
		LogEvent event = getEvent(LogEvent.EVENT_WG);
		logger.info(event.createStartLogMsg());
		logger.info(event.createLogMsg().add(LogEvent.ONTOLOGY_LOCATION,
				"file:" + PropertiesHelper.getOntologyDir() + "/" + PropertiesHelper.getWorkflowOntologyPath()));
		return event;
	}

	public void end(LogEvent event, int exitcode) {
		logger.info(event.createEndLogMsg().add("exitcode", exitcode));
		System.exit(exitcode);
	}
}
