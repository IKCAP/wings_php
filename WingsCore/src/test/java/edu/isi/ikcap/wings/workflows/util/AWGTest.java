package edu.isi.ikcap.wings.workflows.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mindswap.pellet.utils.FileUtils;

import edu.isi.ikcap.wings.AWG;
import edu.isi.ikcap.wings.workflows.template.Seed;
import edu.isi.ikcap.wings.workflows.template.Template;
import edu.isi.ikcap.wings.workflows.util.wfinvocation.Plan;

public class AWGTest {
	String requestid;
	String domdir;
	String conf_path;
	
	private final String DOMAINS_PATH = "/domains/";
	private final String DOMAIN = "DMDomain";
	
	@Before
	public void setUp() {
		Logger.getLogger("com.hp.hpl.jena").setLevel(Level.OFF);
		Logger.getLogger("edu.isi.ikcap.workflows2.dc.impl.isi.FileBackedWekaDC").setLevel(Level.OFF);
		Logger.getLogger("edu.isi.ikcap.workflows2.ac.impl.isi.ProcessCatalogOWLwithRules").setLevel(Level.OFF);
		Logger.getRootLogger().setLevel(Level.OFF);                               
				
		domdir = this.getClass().getResource(DOMAINS_PATH+DOMAIN).getPath();
		conf_path = domdir+"/wings.properties";
		requestid = UUID.randomUUID().toString();
		PropertiesHelper.loadWingsProperties(conf_path);
		PropertiesHelper.setOntologyDir(domdir+"/ontology");
		PropertiesHelper.setLogDir(domdir+"/logs");
		PropertiesHelper.setOutputDir(domdir+"/output");
	}

	@After
	public void tearDown() {

	}

	@Test
	public void testTemplateElaboration() throws FileNotFoundException, IOException {
		String template = "ModelThenClassify";
		AWG awg = new AWG(template, requestid, conf_path, true);
		
		awg.initializePC("library");
		awg.initializeWorkflowGenerator();
		awg.setDC(awg.initializeDC());
		awg.initializeItem();
		
		Template it = awg.getSWG().getInferredTemplate(awg.getTemplate());
		Assert.assertNotNull(it);

		String [] arr1 = FileUtils.readFile(domdir+"/test1.output").split("\\\n");
		String [] arr2 = it.getConstraintEngine().getConstraints().toString().split("\\\n");

		java.util.Arrays.sort(arr1);
		java.util.Arrays.sort(arr2);
		
		Assert.assertArrayEquals(arr1, arr2);
	}
		
	@Test
	public void testSeedGeneration() {
		String seed = "Test2Seed";
		AWG awg = new AWG(seed, requestid, conf_path);
		
		awg.initializePC("library");
		awg.initializeWorkflowGenerator();
		awg.setDC(awg.initializeDC());
		awg.initializeItem();

		ArrayList<Seed> seeds = awg.getCandidateSeeds();
		Assert.assertEquals(1, seeds.size());
		
		ArrayList<Template> candidates = awg.backwardSweep(seeds);
		Assert.assertEquals(8, candidates.size());

		ArrayList<Template> bindings = awg.selectInputData(candidates);
		Assert.assertEquals(2, bindings.size());

		awg.getDataMetricsForInputData(bindings);

		ArrayList<Template> configurations = awg.forwardSweep(bindings);
		Assert.assertEquals(1, configurations.size());
		
		ArrayList<Plan> plans = awg.getWFInvocationPlans(configurations);
		Assert.assertEquals(1, plans.size());

		ArrayList<DAX> daxes = awg.getDaxes(configurations);
		Assert.assertEquals(1, daxes.size());
	}
}
