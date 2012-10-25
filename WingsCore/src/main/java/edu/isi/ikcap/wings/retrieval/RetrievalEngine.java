package edu.isi.ikcap.wings.retrieval;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.KBTriple;
import edu.isi.ikcap.ontapi.OntFactory;
import edu.isi.ikcap.wings.catalogs.components.ComponentCatalog;
import edu.isi.ikcap.wings.catalogs.data.DataCatalog;
import edu.isi.ikcap.wings.retrieval.solver.CompoundRetrievalSolver;
import edu.isi.ikcap.wings.retrieval.solver.RetrievalSolver;
import edu.isi.ikcap.wings.retrieval.solver.RetrievalSolver.JenaFiltersEnum;
import edu.isi.ikcap.wings.retrieval.solver.RetrievalSolver.SubsumptionDegree;
import edu.isi.ikcap.wings.retrieval.solver.componentsubsumption.CSCBasicNoLog;
import edu.isi.ikcap.wings.retrieval.solver.constraintclassifier.CCFiltering;
import edu.isi.ikcap.wings.retrieval.solver.constraintclassifier.ConstraintClassifier;
import edu.isi.ikcap.wings.retrieval.solver.datasubsumption.DSCBasic;
import edu.isi.ikcap.wings.retrieval.solver.doublemappingremover.DMRBasic;
import edu.isi.ikcap.wings.retrieval.solver.doublemappingremover.DoubleMappingRemover;
import edu.isi.ikcap.wings.retrieval.solver.initialmapper.IMHardCoded;
import edu.isi.ikcap.wings.retrieval.solver.initialmapper.InitialMapper;
import edu.isi.ikcap.wings.retrieval.solver.structuralchecker.SPCAdHoc;
import edu.isi.ikcap.wings.retrieval.util.NamespaceManager;
import edu.isi.ikcap.wings.retrieval.wfc.WorkflowCatalog;
import edu.isi.ikcap.wings.retrieval.wfc.WorkflowCatalogFactory;
import edu.isi.ikcap.wings.workflows.template.ConstraintEngine;
import edu.isi.ikcap.wings.workflows.template.Link;
import edu.isi.ikcap.wings.workflows.template.Node;
import edu.isi.ikcap.wings.workflows.template.Template;
import edu.isi.ikcap.wings.workflows.util.PropertiesHelper;

// TODO Create a superclass TemplateEntity for a Node and Variable and use it in the mappings 
// This way we don't have to requery ws in step 1 and 3 (efficient)
// TODO Create a way to assign the pc and dc
public class RetrievalEngine {

	private static RetrievalEngine instance = null;
	private ComponentCatalog pc = null;
	private DataCatalog dc = null;

	private static OntFactory ontologyFactory = new OntFactory(OntFactory.JENA);
	
	private RetrievalEngine() {

	}

	public static RetrievalEngine getEngine() {
		if (instance == null)
			instance = new RetrievalEngine();
		return instance;
	}

	public WorkflowRetrievalResults retrieve(Query query, RetrievalSolver solver, boolean removeDoubleMappings, PrintStream outputStream) {
		boolean systemOut = (outputStream != null);
		if (dc == null)
			dc = PropertiesHelper.getDCFactory().getDC(null);
		if (pc == null)
			pc = PropertiesHelper.getPCFactory().getPC(null);
		WorkflowRetrievalResults results = new WorkflowRetrievalResults();
		long totalTime = System.currentTimeMillis();
		// Classify constraints
		if (systemOut)
			outputStream.print("Classfying constraints... ");
		HashMap<JenaFiltersEnum, KBTripleList> classifiedConstraints = solver.classifyConstraints(query);
		if (systemOut)
			outputStream.println("DONE");
		// For each workflow in the library
		WorkflowCatalog wc = WorkflowCatalogFactory.getCatalog();
		// TODO here we could use the WORKFLOW_PROPERTIES to get a smaller set
		// of workflows
		ArrayList<Template> workflows = wc.getAllWorkflows();
		if (systemOut)
			outputStream.println("Evaluating " + workflows.size() + " workflows...");
		for (Iterator iterator = workflows.iterator(); iterator.hasNext();) {
			long workflowTime = System.currentTimeMillis();
			Template currentWorkflow = (Template) iterator.next();
			WorkflowRetrievalResults result = retrieveMappings(currentWorkflow, solver, classifiedConstraints, removeDoubleMappings, outputStream);
			if (systemOut)
				outputStream.println("Workflow " + currentWorkflow.getName() + " ==> " + (System.currentTimeMillis() - workflowTime) + " ms");
			if (result != null)
				results.add(result);
		} // End For each workflow
		if (systemOut)
			outputStream.println("Total: " + workflows.size() + " workflows evaluated in " + (System.currentTimeMillis() - totalTime) + " ms");
		return results;
	}

	public WorkflowRetrievalResults retrieveMappings(Template workflow, RetrievalSolver solver, HashMap<JenaFiltersEnum, KBTripleList> classifiedConstraints,
			boolean removeDoubleMappings, PrintStream outputStream) {
		boolean systemOut = (outputStream != null);
		if (dc == null)
			dc = PropertiesHelper.getDCFactory().getDC(null);
		if (pc == null)
			pc = PropertiesHelper.getPCFactory().getPC(null);
		// Get the initial mappings for the variables using WDP
		if (systemOut) {
			outputStream.println("Current workflow: " + workflow.getName());
			outputStream.print("\tGetting initial mappings... ");
		}
		MappingsListsList currentWorkflowMappings = solver.getInitialMappings(workflow, classifiedConstraints.get(JenaFiltersEnum.WORKFLOW_DESIGN_PROPERTIES));
		if (currentWorkflowMappings.size() == 0) {
			if (systemOut)
				outputStream.println("DONE (No Mappings)");
			return null;
		}
		if (systemOut)
			outputStream.println("DONE");
		// Remove mappings list that map two variables to the same entity
		if (removeDoubleMappings) {
			if (systemOut)
				outputStream.print("\tRemoving double mappings... ");
			currentWorkflowMappings = solver.removeDoubleMappings(currentWorkflowMappings);
			if (currentWorkflowMappings.size() == 0) {
				if (systemOut)
					outputStream.println("DONE (No Mappings)");
				return null;
			}
			if (systemOut)
				outputStream.println("DONE");
		}
		// For each mapping
		if (systemOut)
			outputStream.println("\tEvaluating " + currentWorkflowMappings.size() + " mappings lists...");
		for (Iterator<MappingsList> mapsIt = currentWorkflowMappings.iterator(); mapsIt.hasNext();) {
			MappingsList mappings = mapsIt.next();
			// 1. Use subsumption to check what are the valid mappings for the
			// components
			// TODO Add a mechanism to take into account intermediate values (eg
			// 0.7)
			// TODO An index that relates components classes with workflows that
			// use them could help here
			if (systemOut)
				outputStream.print("\t\tChecking subsumption of components... ");
			float result = solver.checkComponentsSubsumption(workflow, classifiedConstraints.get(JenaFiltersEnum.COMPONENT_PROPERTIES), mappings, pc);
			if (result != 1f) {
				mapsIt.remove();
				if (systemOut)
					outputStream.println("DONE (No subsumption)");
				continue;
			}
			if (systemOut)
				outputStream.println("DONE");
			// 2. Use ??? (rules or ad hoc procedures (1 for each property))
			// to solve the structural properties constraints
			if (systemOut)
				outputStream.print("\t\tChecking structural properties... ");
			result = solver.checkStructuralProperties(workflow, classifiedConstraints.get(JenaFiltersEnum.STRUCTURAL_PROPERTIES), mappings);
			if (result != 1f) {
				mapsIt.remove();
				if (systemOut)
					outputStream.println("DONE (Structure fault)");
				continue;
			}
			outputStream.println("DONE");
			// 3. Use 1.1 to check the valid mappings for the data
			if (systemOut)
				outputStream.print("\t\tChecking subsumption of data variables... ");
			SubsumptionDegree subsumptionResult = solver.checkDataVariablesSubsumption(workflow, classifiedConstraints.get(JenaFiltersEnum.DATA_PROPERTIES),
					mappings, dc);
			outputStream.println("DONE (" + subsumptionResult + ")");
			if (subsumptionResult != SubsumptionDegree.SUBSUMED && subsumptionResult != SubsumptionDegree.EQUIVALENT) {
				mapsIt.remove();
				continue;
			}
			// TODO 4. What shall we do with the OTHER_PROPERTIES?
			// TODO What happens if any of the aforementioned properties has a
			// variable without mapping
		} // End For each mapping
		if (currentWorkflowMappings.size() == 0)
			return null;
		WorkflowRetrievalResults output = new WorkflowRetrievalResults();
		output.add(workflow, currentWorkflowMappings);
		return output;
	}

	public static void main(String[] args) throws FileNotFoundException {
		// prueba9(null);
		// prueba10("paper-T3-typed", 6);
		// prueba10("paper-T4-enriched", 4);
		prueba10("paper-T4-var6-enriched", 55, System.out);
		System.exit(0);
		// List<String> workflows = getWorkflows(relevant, irrelevant, typed,
		// enriched)
		List<String> workflows100 = getWorkflows(true, false, false, true);
		List<String> workflows20 = selectRandomWorkflows(workflows100, 20, 12543);
		List<String> workflows = workflows20;
		int[] queries = { 6, 1, 12, 13, 14, 15, 2, 22, 23, 24, 25, 32, 33, 34, 35, 4, 42, 43, 44, 45, 3, 5, 52, 53, 54, 55 };
		ArrayList<HashMap<Integer, Prueba11Results>> allResults = new ArrayList<HashMap<Integer, Prueba11Results>>();
		int EXECUTIONS = 10;
		// HashMap<Integer, Prueba11Results> results = new HashMap<Integer,
		// Prueba11Results>();
		// Output stream
		PrintStream outputStream = new PrintStream("../results.txt");
		for (int loop = 0; loop < EXECUTIONS; loop++) {
			HashMap<Integer, Prueba11Results> results = new HashMap<Integer, Prueba11Results>();
			for (int i = 0; i < queries.length; i++) {
				results.put(queries[i], prueba11(queries[i], workflows, outputStream));
			}
			allResults.add(results);
		}

		// Times
		for (int queryIndex = 0; queryIndex < queries.length; queryIndex++) {
			int query = queries[queryIndex];
			String times = "";
			for (int i = 0; i < EXECUTIONS; i++) {
				HashMap<Integer, Prueba11Results> results = allResults.get(i);
				times += "\t" + results.get(query).getTotalTime();
			}
			outputStream.println("QUERY " + query + times);
		}

		// Workflows retrieved
		HashMap<Integer, Prueba11Results> resultsAllQueries = allResults.get(0);

		HashMap<String, boolean[]> queriesForWorkflows = new HashMap<String, boolean[]>();
		for (int queryIndex = 0; queryIndex < queries.length; queryIndex++) {
			int query = queries[queryIndex];
			Prueba11Results results1Query = resultsAllQueries.get(query);
			ArrayList<WorkflowRetrievalResults> results = results1Query.getResults();
			String workflowsStr = "";
			for (WorkflowRetrievalResults result : results) {
				if (result != null) {
					for (String templateName : result.getTemplateNames()) {
						boolean[] queriesForWorkflowsQueries = queriesForWorkflows.get(templateName);
						if (queriesForWorkflowsQueries == null)
							queriesForWorkflowsQueries = new boolean[queries.length];
						queriesForWorkflowsQueries[queryIndex] = true;
						queriesForWorkflows.put(templateName, queriesForWorkflowsQueries);
						workflowsStr += templateName + "\t";
					}
				} else {
					workflowsStr += "NO\t";
				}
			}
			outputStream.println("QUERY " + query + "\t" + workflowsStr);
		}
		String noSuccessfulQueries = "";
		String queriesName = "\t";
		for (int i = 0; i < queries.length; i++) {
			queriesName += "\t" + queries[i];
			noSuccessfulQueries += "false\t";
		}
		System.out.println(queriesName);
		for (String workflow : workflows) {
			boolean[] successfulQueries = queriesForWorkflows.get(workflow);
			String queriesStr = "";
			if (successfulQueries == null) {
				queriesStr = noSuccessfulQueries;
			} else {
				for (int i = 0; i < successfulQueries.length; i++) {
					queriesStr += successfulQueries[i] + "\t";
				}
			}
			outputStream.println(workflow + "\t" + queriesStr);
		}

		// System.out.println();
		// System.out.println(allResults.get(1).toString());

		// System.out.println();System.out.println();System.out.println();
		// Set<Entry<Integer, Prueba11Results>> entries = results.entrySet();
		// for (Entry<Integer, Prueba11Results> entry : entries) {
		// System.out.println();
		// Prueba11Results summarizedResult = entry.getValue();
		// //ArrayList<WorkflowRetrievalResults> queryResult =
		// entry.getValue().getResults();
		//			
		// System.out.println("QUERY " + entry.getKey());
		// System.out.println("TOTAL TIME: " + summarizedResult.getTotalTime());
		// System.out.println(summarizedResult.toString());
		// }
		// System.out.println();
		// for (int i = 0; i < queries.length; i++) {
		// System.out.println("Query " + queries[i] + "\t" +
		// results.get(queries[i]).getTotalTime());
		// }
	}

	@SuppressWarnings("unused")
	private static List<String> getRelevantPlusIrrelevantWorkflows() {
		ArrayList<String> output = new ArrayList<String>(getAllRelevantWorkflows());
		output.addAll(getAllIrrelevantWorkflows());
		return output;
	}

	// TODO Change String[] to List<String>
	private static List<String> selectRandomWorkflows(List<String> workflows, int length, long seed) {
		if (length > workflows.size())
			return null;
		List<String> output = new ArrayList<String>();
		// boolean[] selected = new boolean[workflows.size()];
		Random rng = new Random(seed);
		for (int i = 0; i < length; i++) {
			int nextIndex = rng.nextInt(workflows.size());
			output.add(workflows.remove(nextIndex));
		}
		return output;
	}

	private static List<String> getWorkflows(boolean relevant, boolean irrelevant, boolean typed, boolean enriched) {
		ArrayList<String> output = new ArrayList<String>();
		if (relevant)
			if (typed)
				output.addAll(getRelevantTypedWorkflows());
			else if (enriched)
				output.addAll(getRelevantEnrichedWorkflows());
		if (irrelevant)
			if (typed)
				output.addAll(getIrrelevantTypedWorkflows());
			else if (enriched)
				output.addAll(getIrrelevantEnrichedWorkflows());
		return output;
	}

	private static List<String> getAllRelevantWorkflows() {
		ArrayList<String> output = new ArrayList<String>(getRelevantEnrichedWorkflows());
		output.addAll(getRelevantTypedWorkflows());
		return output;
	}

	private static List<String> getAllIrrelevantWorkflows() {
		ArrayList<String> output = new ArrayList<String>(getIrrelevantEnrichedWorkflows());
		output.addAll(getIrrelevantTypedWorkflows());
		return output;
	}

	private static List<String> getRelevantTypedWorkflows() {
		String[] workflows = { "DiscSampleThenBayesNModel-typed", "DiscSampleThenID3Model-typed", "DiscSampleThenLmtModel-typed", "DiscSampleThenModel-typed",
				"DiscretizeThenModel-typed", "DiscretizeThenModelAndClassify-typed", "ModelerThenBayesNClassifier-typed", "ModelerThenClassifier-area1-typed",
				"ModelerThenClassifier-disc-typed", "ModelerThenClassifier-mv-typed", "ModelerThenClassifier-norm-typed", "ModelerThenClassifier-tdisc-typed",
				"ModelerThenClassifier-tmv-typed", "ModelerThenClassifier-typed", "ModelerThenHNBClassify-typed", "ModelerThenID3Classify-typed",
				"ModelerThenJ48Classify-typed", "RandmModelerThenClassifier-typed", "Sample-ModelThenBNClassify-typed", "Sample-ModelThenClassi-mv-typed",
				"Sample-ModelThenClassi-typed", "Sample-ModelThenDTClassify-typed", "Sample-ModelThenHNBClassify-typed", "Sample-ModelThenID3Classify-typed",
				"Sample-ModelThenJ48Classify-typed", "Sample10-ModelThenClas-typed", "Sample100-ModelThenCla-typed", "SampleThenBayesModel-typed",
				"SampleThenBayesNModel-typed", "SampleThenID3Model-typed", "SampleThenModel-typed", "TwoDiscretizeThenModelAndClassify-typed",
				"paper-T1-typed", "paper-T1-var1-typed", "paper-T1-var2-typed", "paper-T2-typed", "paper-T2-var1-typed", "paper-T2-var2-typed",
				"paper-T3-typed", "paper-T3-var1-typed", "paper-T3-var2-typed", "paper-T3-var2-var-typed", "paper-T3-var2-var2-typed", "paper-T3-var3-typed",
				"paper-T4-typed", "paper-T4-var1-typed", "paper-T4-var1-var-typed", "paper-T4-var4-typed", "paper-T4-var5-typed", "paper-T4-var6-typed" };
		return Arrays.asList(workflows);
	}

	private static List<String> getIrrelevantTypedWorkflows() {
		String[] workflows = { "Irr-DiscSampleThenBayesNModel-typed", "Irr-DiscSampleThenID3Model-typed", "Irr-DiscSampleThenLmtModel-typed",
				"Irr-DiscSampleThenModel-typed", "Irr-DiscretizeThenModel-typed", "Irr-DiscretizeThenModelAndClassify-typed",
				"Irr-ModelerThenBayesNClass-typed", "Irr-ModelerThenClassifier-area1-typed", "Irr-ModelerThenClassifier-disc-typed",
				"Irr-ModelerThenClassifier-mv-typed", "Irr-ModelerThenClassifier-norm-typed", "Irr-ModelerThenClassifier-tdisc-typed",
				"Irr-ModelerThenClassifier-tmv-typed", "Irr-ModelerThenClassifier-typed", "Irr-ModelerThenHNBClassify-typed",
				"Irr-ModelerThenID3Classify-typed", "Irr-ModelerThenJ48Classify-typed", "Irr-RandmModelerThenClassi-typed",
				"Irr-Sample-ModelThenBNClassify-typed", "Irr-Sample-ModelThenClassi-mv-typed", "Irr-Sample-ModelThenClassi-typed",
				"Irr-Sample-ModelThenDTClassify-typed", "Irr-Sample-ModelThenHNBClassify-typed", "Irr-Sample-ModelThenID3Classify-typed",
				"Irr-Sample-ModelThenJ48Classify-typed", "Irr-Sample10-ModelThenClas-typed", "Irr-Sample100-ModelThenCla-typed",
				"Irr-SampleThenBayesModel-typed", "Irr-SampleThenBayesNModel-typed", "Irr-SampleThenID3Model-typed", "Irr-SampleThenModel-typed",
				"Irr-TwoDiscretizeThenModel-typed", "Irr-paper-T1-typed", "Irr-paper-T1-var1-typed", "Irr-paper-T1-var2-typed", "Irr-paper-T2-typed",
				"Irr-paper-T2-var1-typed", "Irr-paper-T2-var2-typed", "Irr-paper-T3-typed", "Irr-paper-T3-var1-typed", "Irr-paper-T3-var2-typed",
				"Irr-paper-T3-var2-var-typed", "Irr-paper-T3-var2-var2-typed", "Irr-paper-T3-var3-typed", "Irr-paper-T4-typed", "Irr-paper-T4-var1-typed",
				"Irr-paper-T4-var1-var-typed", "Irr-paper-T4-var4-typed", "Irr-paper-T4-var5-typed", "Irr-paper-T4-var6-typed" };
		return Arrays.asList(workflows);
	}

	private static List<String> getRelevantEnrichedWorkflows() {
		String[] workflows = { "DiscSampleThenBayesNModel-enriched", "DiscSampleThenID3Model-enriched", "DiscSampleThenLmtModel-enriched",
				"DiscSampleThenModel-enriched", "DiscretizeThenModel-enriched", "DiscretizeThenModelAndClassify-enriched",
				"ModelerThenBayesNClassifier-enriched", "ModelerThenClassifier-area1-enriched", "ModelerThenClassifier-disc-enriched",
				"ModelerThenClassifier-enriched", "ModelerThenClassifier-mv-enriched", "ModelerThenClassifier-norm-enriched",
				"ModelerThenClassifier-tdisc-enriched", "ModelerThenClassifier-tmv-enriched", "ModelerThenHNBClassify-enriched",
				"ModelerThenID3Classify-enriched", "ModelerThenJ48Classify-enriched", "RandmModelerThenClassi-enriched", "Sample-ModelThenBNClassify-enriched",
				"Sample-ModelThenClassi-enriched", "Sample-ModelThenClassi-mv-enriched", "Sample-ModelThenDTClassify-enriched",
				"Sample-ModelThenHNBClassify-enriched", "Sample-ModelThenID3Classify-enriched", "Sample-ModelThenJ48Classify-enriched",
				"Sample10-ModelThenClas-enriched", "Sample100-ModelThenCla-enriched", "SampleThenBayesModel-enriched", "SampleThenBayesNModel-enriched",
				"SampleThenID3Model-enriched", "SampleThenModel-enriched", "TwoDiscretizeThenModelAndClassify-enriched", "paper-T1-enriched",
				"paper-T1-var1-enriched", "paper-T1-var2-enriched", "paper-T2-enriched", "paper-T2-var1-enriched", "paper-T2-var2-enriched",
				"paper-T3-enriched", "paper-T3-var1-enriched", "paper-T3-var2-enriched", "paper-T3-var2-var-enriched", "paper-T3-var2-var2-enriched",
				"paper-T3-var3-enriched", "paper-T4-enriched", "paper-T4-var1-enriched", "paper-T4-var1-var-enriched", "paper-T4-var4-enriched",
				"paper-T4-var5-enriched", "paper-T4-var6-enriched" };
		return Arrays.asList(workflows);
	}

	private static List<String> getIrrelevantEnrichedWorkflows() {
		String[] workflows = { "Irr-DiscSampleThenBayesNModel-enriched", "Irr-DiscSampleThenID3Model-enriched", "Irr-DiscSampleThenLmtModel-enriched",
				"Irr-DiscSampleThenModel-enriched", "Irr-DiscretizeThenModel-enriched", "Irr-DiscretizeThenModelAndClassify-enriched",
				"Irr-ModelerThenBayesNClassifier-enriched", "Irr-ModelerThenClassifier-area1-enriched", "Irr-ModelerThenClassifier-disc-enriched",
				"Irr-ModelerThenClassifier-enriched", "Irr-ModelerThenClassifier-mv-enriched", "Irr-ModelerThenClassifier-norm-enriched",
				"Irr-ModelerThenClassifier-tdisc-enriched", "Irr-ModelerThenClassifier-tmv-enriched", "Irr-ModelerThenHNBClassify-enriched",
				"Irr-ModelerThenID3Classify-enriched", "Irr-ModelerThenJ48Classify-enriched", "Irr-RandmModelerThenClassi-enriched",
				"Irr-Sample-ModelThenBNClassify-enriched", "Irr-Sample-ModelThenClassi-enriched", "Irr-Sample-ModelThenClassi-mv-enriched",
				"Irr-Sample-ModelThenDTClassify-enriched", "Irr-Sample-ModelThenHNBClassify-enriched", "Irr-Sample-ModelThenID3Classify-enriched",
				"Irr-Sample-ModelThenJ48Classify-enriched", "Irr-Sample10-ModelThenClas-enriched", "Irr-Sample100-ModelThenCla-enriched",
				"Irr-SampleThenBayesModel-enriched", "Irr-SampleThenBayesNModel-enriched", "Irr-SampleThenID3Model-enriched", "Irr-SampleThenModel-enriched",
				"Irr-TwoDiscretizeThenModel-enriched", "Irr-paper-T1-enriched", "Irr-paper-T1-var1-enriched", "Irr-paper-T1-var2-enriched",
				"Irr-paper-T2-enriched", "Irr-paper-T2-var1-enriched", "Irr-paper-T2-var2-enriched", "Irr-paper-T3-enriched", "Irr-paper-T3-var1-enriched",
				"Irr-paper-T3-var2-enriched", "Irr-paper-T3-var2-var-enriched", "Irr-paper-T3-var2-var2-enriched", "Irr-paper-T3-var3-enriched",
				"Irr-paper-T4-enriched", "Irr-paper-T4-var1-enriched", "Irr-paper-T4-var1-var-enriched", "Irr-paper-T4-var4-enriched",
				"Irr-paper-T4-var5-enriched", "Irr-paper-T4-var6-enriched" };
		return Arrays.asList(workflows);
	}

	// Tests a query in the listed workflows
	private static Prueba11Results prueba11(int query, List<String> workflows100, PrintStream outputStream) {
		PrintStream stream = outputStream;
		if (stream == null)
			stream = System.out;
		Prueba11Results results = new Prueba11Results();
		for (String workflow : workflows100) {
			stream.println();
			stream.println("WORKFLOW " + workflow);
			results.add(workflow, prueba10(workflow, query, stream));
		}
		return results;
	}

	private static Prueba10Results prueba10(String workflowName, int query, PrintStream outputStream) {
		PrintStream stream = outputStream;
		if (stream == null)
			stream = System.out;
		RetrievalEngine re = RetrievalEngine.getEngine();
		RetrievalSolver rs = new CompoundRetrievalSolver(new CCFiltering(), new IMHardCoded(), new DMRBasic(), new CSCBasicNoLog(), new SPCAdHoc(),
				new DSCBasic());
		// Preload ac and dc to have a clean measure of time
		if (re.dc == null)
			re.dc = PropertiesHelper.getDCFactory().getDC(null);
		if (re.pc == null)
			re.pc = PropertiesHelper.getPCFactory().getPC(null);

		WorkflowRetrievalResults resultsArr;
		HashMap<JenaFiltersEnum, KBTripleList> classifiedConstraints = rs.classifyConstraints(getPaperQuery(query, "wft"));
		WorkflowCatalog wc = WorkflowCatalogFactory.getCatalog();
		Template workflow = wc.getWorkflow(workflowName);
		long time = System.currentTimeMillis();
		resultsArr = re.retrieveMappings(workflow, rs, classifiedConstraints, true, stream);
		stream.println("----------------------------");
		stream.println("Execution time: " + (System.currentTimeMillis() - time));
		stream.println("RESULTS: " + resultsArr);
		return new Prueba10Results(workflowName, query, System.currentTimeMillis() - time, resultsArr);
	}

	@SuppressWarnings("unused")
	private static void prueba9(String fileName) {
		RetrievalEngine re = new RetrievalEngine();
		RetrievalSolver rs = new CompoundRetrievalSolver(new CCFiltering(), new IMHardCoded(), new DMRBasic(), new CSCBasicNoLog(), new SPCAdHoc(),
				new DSCBasic());
		ArrayList<WorkflowRetrievalResults> resultsArr = new ArrayList<WorkflowRetrievalResults>();
		PrintStream ps = System.out;
		if (fileName != null) {
			try {
				ps = new PrintStream(fileName);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}
		for (int i = 1; i <= 5; i++) {
			ps.println();
			Query q = getPaperQuery(i, "wft");
			if (q == null)
				ps.println("Paper Query " + i + " not implemented");
			else {
				WorkflowRetrievalResults results = re.retrieve(q, rs, true, ps);
				ps.println("QUERY " + i);
				ps.println(q.toStringDebug());
				ps.println(results.toStringDebug());
				resultsArr.add(i - 1, results);
			}
		}
		ps.println();
		ps.println("SUMMARY");
		for (int i = 1; i <= 5; i++) {
			ps.println();
			ps.println("Query " + i);
			Query q = getPaperQuery(i, "wft");
			if (q != null) {
				ps.println(q.toStringDebug());
				WorkflowRetrievalResults results = resultsArr.get(i - 1);
				ps.println("Retrieved workflows:");
				ps.println(results.getTemplateNames());
				ps.println("Mappings:");
				ps.println(results.toStringDebug());
			}
		}
	}

	@SuppressWarnings("unused")
	private static void prueba8() {
		Template t = WorkflowCatalogFactory.getCatalog().getWorkflow("ModelThenClassifier-jk");
		ConstraintEngine ce = t.getConstraintEngine();
		ArrayList<KBTriple> constraints = ce.getConstraints();
		for (KBTriple triple : constraints) {
			System.out.println(triple);
		}
		// System.out.println((new
		// KBTripleList(t.getConstraintEngine().getConstraints())).toStringDebug());
	}

	@SuppressWarnings("unused")
	private static void prueba7() {
		Template t = WorkflowCatalogFactory.getCatalog().getWorkflow("Sample-ModelThenClassifier");
		Node[] nodes = t.getNodes();
		for (int i = 0; i < nodes.length; i++) {
			for (int j = 0; j < nodes.length; j++) {
				System.out.println(getNameFromID(nodes[i].getID())
						+ " --> "
						+ getNameFromID(nodes[j].getID())
						+ " = "
						+ WorkflowCatalogFactory.getCatalog().checkComponentInmediatelyPrecedes(t, getResourceFromID(nodes[i].getID()),
								getResourceFromID(nodes[j].getID())));
				System.out.println(getNameFromID(nodes[i].getID())
						+ " -->* "
						+ getNameFromID(nodes[j].getID())
						+ " = "
						+ WorkflowCatalogFactory.getCatalog().checkComponentPrecedes(t, getResourceFromID(nodes[i].getID()),
								getResourceFromID(nodes[j].getID())));
			}
		}
		Link[] links = t.getLinks();
		for (int i = 0; i < links.length; i++) {
			for (int j = 0; j < links.length; j++) {
				System.out.println(getNameFromID(links[i].getVariable().getID())
						+ " --> "
						+ getNameFromID(links[j].getVariable().getID())
						+ " = "
						+ WorkflowCatalogFactory.getCatalog().checkDatapointInmediatelyPrecedes(t,
								getResourceFromID(links[i].getVariable().getID()),
								getResourceFromID(links[j].getVariable().getID())));
				System.out.println(getNameFromID(links[i].getVariable().getID())
						+ " -->* "
						+ getNameFromID(links[j].getVariable().getID())
						+ " = "
						+ WorkflowCatalogFactory.getCatalog().checkDatapointPrecedes(t, getResourceFromID(links[i].getVariable().getID()),
								getResourceFromID(links[j].getVariable().getID())));
			}
		}
		for (int i = 0; i < nodes.length; i++) {
			for (int j = 0; j < links.length; j++) {
				System.out.println(getNameFromID(links[j].getVariable().getID())
						+ " --> "
						+ getNameFromID(nodes[i].getID())
						+ " = "
						+ WorkflowCatalogFactory.getCatalog().checkInputData(t, getResourceFromID(nodes[i].getID()),
								getResourceFromID(links[j].getVariable().getID())));
			}
		}
	}

	private static String getNameFromID(String id) {
		int pos = id.lastIndexOf("#");
		if (pos == -1)
			return id;
		return id.substring(pos + 1);
	}

	@SuppressWarnings("unused")
	private static void prueba6() {
		RetrievalEngine re = new RetrievalEngine();
		RetrievalSolver rs = new CompoundRetrievalSolver(new CCFiltering(), new IMHardCoded(), new DMRBasic(), new CSCBasicNoLog(), new SPCAdHoc(),
				new DSCBasic());
		for (int i = 1; i <= 5; i++) {
			System.out.println();
			Query q = getQuery(i, "wft");
			WorkflowRetrievalResults results = re.retrieve(q, rs, true, System.out);
			System.out.println("QUERY " + i);
			System.out.println(q.toStringDebug());
			System.out.println(results.toStringDebug());
		}
	}

	@SuppressWarnings("unused")
	private static void prueba5() {
		PrintWriter log;
		try {
			log = new PrintWriter(new File("mi_log.txt"));
			String wfVariable = "wft";
			Query query = getQuery(2, wfVariable);
			log.println("CONSULTA:");
			log.println(query.toStringDebug().replace("),", "),\n"));
			RetrievalEngine re = new RetrievalEngine();
			// Classify constraints
			ConstraintClassifier cc = new CCFiltering();
			HashMap<JenaFiltersEnum, KBTripleList> classifiedConstraints = cc.classifyConstraints(query);
			// For each workflow in the library
			WorkflowCatalog wc = WorkflowCatalogFactory.getCatalog();
			ArrayList<Template> t = wc.getAllWorkflows();
			for (Iterator iterator = t.iterator(); iterator.hasNext();) {
				Template template = (Template) iterator.next();
				// Get the initial mappings for the variables using WDP
				InitialMapper im = new IMHardCoded();
				MappingsListsList mappings = im.getInitialMappings(template, classifiedConstraints.get(JenaFiltersEnum.WORKFLOW_DESIGN_PROPERTIES));
				log.println("TEMPLATE: " + template.getName());
				log.println("IM: ");
				log.println(mappings.toStringDebug2());
				// KBObject c0 = new KBObject(new
				// ResourceImpl(ConstraintsFilter.WFQV, "c0"));
				// KBObject c1 = new KBObject(new
				// ResourceImpl(ConstraintsFilter.WFQV, "c1"));
				// KBObject d0 = new KBObject(new
				// ResourceImpl(ConstraintsFilter.WFQV, "d0"));
				// for(int i = 0; i < mappings.size(); i++) {
				// MappingsList m = mappings.getMappings(i);
				// log.println(m.getEntity(c0).getName() + "," +
				// m.getEntity(c1).getName() + "," + m.getEntity(d0).getName());
				// }
				log.println("-----------------------");
				log.println("-----------------------");
				DoubleMappingRemover dmr = new DMRBasic();
				mappings = dmr.removeDoubleMappings(mappings);
				log.println(mappings.toStringDebug2());
				// for(int i = 0; i < mappings.size(); i++) {
				// MappingsList m = mappings.getMappings(i);
				// log.println(m.getEntity(c0).getName() + "," +
				// m.getEntity(c1).getName() + "," + m.getEntity(d0).getName());
				// }
				log.println("-----------------------");
				log.println("-----------------------");
				log.println("-----------------------");
				log.println("-----------------------");
			}
			log.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private static void prueba4() {
		WorkflowCatalog wc = WorkflowCatalogFactory.getCatalog();
		ArrayList<Template> t = wc.getAllWorkflows();
		for (Template template : t) {
			System.out.println("Name: " + template.getName());
			System.out.println("Namespace: " + template.getNamespace());
			System.out.println("ID: " + template.getID());
			System.out.println("URL:" + template.getUrl());
			System.out.println("Componentes:");
			System.out.println(wc.getComponents(template));
			System.out.println("Datasets:");
			System.out.println(wc.getDatasets(template));
			System.out.println("Input datasets:");
			System.out.println(wc.getInputDatasets(template));
			System.out.println("Output datasets:");
			System.out.println(wc.getInputDatasets(template));
			System.out.println("---------------------------------------------");
			System.out.println("---------------------------------------------");
			System.out.println();
			System.out.println();
		}
	}

	@SuppressWarnings("unused")
	private static void prueba3() {
		MappingsListsList mll = new MappingsListsList();
		// mll.addMapping(new KBObject("http://isi.edu#v1"), new
		// KBObject("http://wings-workflows.org/ontology#o1"));
		// mll.addMapping(new KBObject("http://isi.edu#v2"), new
		// KBObject("http://wings-workflows.org/ontology#o2"));
		// mll.addMapping(new KBObject("http://isi.edu#v1"), new
		// KBObject("http://wings-workflows.org/ontology#o2"));
		// mll.addMapping(new KBObject("http://isi.edu#v2"), new
		// KBObject("http://wings-workflows.org/ontology#o1"));
		KBObject var1 = getResourceFromID("http://wings-workflows.org/ontology#v1");
		KBObject var2 = getResourceFromID("http://wings-workflows.org/ontology#v2");
		KBObject obj1 = getResourceFromID("http://wings-workflows.org/ontology#o1");
		KBObject obj2 = getResourceFromID("http://wings-workflows.org/ontology#o2");
		KBObject var3 = getResourceFromID("http://wings-workflows.org/ontology#v3");
		KBObject obj3 = getResourceFromID("http://wings-workflows.org/ontology#o3");
		KBObject obj4 = getResourceFromID("http://wings-workflows.org/ontology#o4");
		ArrayList<KBObject> arrObjs = new ArrayList<KBObject>();
		arrObjs.add(obj3);
		arrObjs.add(obj4);
		System.out.println("Step 0: ");
		System.out.println(mll.toStringDebug());
		mll.addMapping(var1, obj1);
		System.out.println("Step 1: ");
		System.out.println(mll.toStringDebug());
		mll.addMapping(var2, obj2);
		System.out.println("Step 2: ");
		System.out.println(mll.toStringDebug());
		mll.addMapping(var1, obj2);
		System.out.println("Step 3: ");
		System.out.println(mll.toStringDebug());
		mll.addMapping(var2, obj1);
		System.out.println("Step 4: ");
		System.out.println(mll.toStringDebug());
		mll.addMapping(var3, arrObjs);
		System.out.println("Step 5: ");
		System.out.println(mll.toStringDebug());
	}

	@SuppressWarnings("unused")
	private static void prueba2() {
		WorkflowCatalog wc = WorkflowCatalogFactory.getCatalog();
		ArrayList<Template> t = wc.getAllTemplates();
		System.out.println(t);
	}

	@SuppressWarnings("unused")
	private static void prueba1() {
		String wfVariable = "wft";
		//RDFNode nodo = new ResourceImpl(NamespaceManager.getNamespace("wfqv"), wfVariable);
		Query query = getQuery(1, wfVariable);
		ConstraintClassifier cc = new CCFiltering();
		HashMap<JenaFiltersEnum, KBTripleList> classifiedConstraints = cc.classifyConstraints(query);
		System.out.println("CLASSIFICATION:");
		for (Iterator<JenaFiltersEnum> iterator = classifiedConstraints.keySet().iterator(); iterator.hasNext();) {
			JenaFiltersEnum set = iterator.next();
			System.out.println(set.toString());
			System.out.println(classifiedConstraints.get(set));
		}
	}

	private static Query getQuery(int num, String wfVariable) {
		try {
			String proc = "getQuery" + num;
			Class[] parameters = { String.class };
			Object[] values = { wfVariable };
			return (Query) RetrievalEngine.class.getDeclaredMethod(proc, parameters).invoke(null, values);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@SuppressWarnings("unused")
	private static Query getQuery5(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Instance"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	@SuppressWarnings("unused")
	private static Query getQuery4(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasComponent"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasComponent"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c1"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "subClassOf"), getResourceFromID(NamespaceManager.getNamespace("acdom"), "Modeler"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c1"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "subClassOf"), getResourceFromID(NamespaceManager.getNamespace("acdom"), "Classifier"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	@SuppressWarnings("unused")
	private static Query getQuery3(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasComponent"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "subClassOf"), getResourceFromID(NamespaceManager.getNamespace("acdom"), "Modeler"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	@SuppressWarnings("unused")
	private static Query getQuery2(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasComponent"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasComponent"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c1"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "subClassOf"), getResourceFromID(NamespaceManager.getNamespace("acdom"), "RandomSampleN"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputData"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c1"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "subClassOf"), getResourceFromID(NamespaceManager.getNamespace("acdom"), "Classifier"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "precedes"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c1"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	@SuppressWarnings("unused")
	private static Query getQuery1(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasOutputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d1"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasComponent"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "subClassOf"), getResourceFromID(NamespaceManager.getNamespace("acdom"), "Classifier"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasDomain"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "dom0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputData"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d1"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasDomain"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "dom0"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	private static Query getPaperQuery(int num, String wfVariable) {
		try {
			String proc = "getPaperQuery" + num;
			Class[] parameters = { String.class };
			Object[] values = { wfVariable };
			try {
				Query query = (Query) RetrievalEngine.class.getDeclaredMethod(proc, parameters).invoke(null, values);
				return query;
			} catch (Exception e) {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// Q1: Find workflows where input data is cpu data, which is continuous and
	// has missing values
	// ?wft wfq:hasInputDataset ?d0
	// ?d0 dcdom:hasDomain dcdom:cpu
	// ?d0 dcdom:isDiscrete "false"^^xsd:boolean
	// ?d0 dcdom:hasMissingValues "true"^^xsd:boolean
	// ?d0 wfq:hasType dcdom:Instance
	@SuppressWarnings("unused")
	private static Query getPaperQuery1(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasDomain"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "cpu"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "isDiscrete"), ontologyFactory.getDataObject(false));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasMissingValues"), ontologyFactory.getDataObject(true));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Instance"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q12: Find workflows where input data is cpu data, which is discrete and
	// has missing values
	// ?wft wfq:hasInputDataset ?d0
	// ?d0 wfq:hasType dcdom:Instance
	// ?d0 dcdom:hasDomain dcdom:cpu
	// ?d0 dcdom:isDiscrete "true"^^xsd:boolean
	// ?d0 dcdom:hasMissingValues "true"^^xsd:boolean
	@SuppressWarnings("unused")
	private static Query getPaperQuery12(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasDomain"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "cpu"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "isDiscrete"), ontologyFactory.getDataObject(true));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasMissingValues"), ontologyFactory.getDataObject(true));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q13: Find workflows with discrete input data, from east-la area, for the
	// cpu domain
	// ?wft wfq:hasInputDataset ?d0
	// ?d0 dcdom:hasDomain dcdom:cpu
	// ?d0 dcdom:hasArea dcdom:east-la
	// ?d0 dcdom:isDiscrete "true"^^xsd:boolean
	@SuppressWarnings("unused")
	private static Query getPaperQuery13(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasDomain"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "cpu"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasArea"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "east-la"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "isDiscrete"), ontologyFactory.getDataObject(true));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q14: Find workflows with a continuous instance as input, which has no
	// missing values
	// ?wft wfq:hasInputDataset ?d0
	// ?d0 wfq:hasType dcdom:Instance
	// ?d0 dcdom:isDiscrete "false"^^xsd:boolean
	// ?d0 dcdom:hasMissingValues "false"^^xsd:boolean
	@SuppressWarnings("unused")
	private static Query getPaperQuery14(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "isDiscrete"), ontologyFactory.getDataObject(false));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasMissingValues"), ontologyFactory.getDataObject(false));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q15: Find workflows with instance dataset as input, which has no missing
	// values
	// ?wft dcdom:hasInputDataset ?d0
	// ?d0 wfq:hasType dcdom:Instance
	// ?d0 dcdom:hasMissingValues "false"^^xsd:boolean
	@SuppressWarnings("unused")
	private static Query getPaperQuery15(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasMissingValues"), ontologyFactory.getDataObject(false));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q2: Find workflows that generate a classification of Iris data
	// ?wft wfq:hasOutputDataset ?d0
	// ?d0 wfq:hasType dcdom:Classification
	// ?d0 dcdom:hasDomain dcdom:iris
	@SuppressWarnings("unused")
	private static Query getPaperQuery2(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasOutputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasDomain"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "iris"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Classification"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q22: Find workflows that generate a classification of Weather nominal
	// data
	// ?wft wfq:hasOutputDataset ?d0
	// ?d0 wfq:hasType dcdom:Classification
	// ?d0 dcdom:hasDomain dcdom:weather-nominal
	@SuppressWarnings("unused")
	private static Query getPaperQuery22(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasOutputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Classification"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasDomain"), getResourceFromID(NamespaceManager.getNamespace("dcdom"),
				"weather-nominal"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q23: Find workflows that generate a classification of Weather-Nominal
	// data in west-la area
	// ?wft wfq:hasOutputDataset ?d0
	// ?d0 wfq:hasType dcdom:Classification
	// ?d0 dcdom:hasDomain dcdom:weather-nominal
	// ?d0 dcdom:hasArea dcdom:west-la
	@SuppressWarnings("unused")
	private static Query getPaperQuery23(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasOutputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Classification"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasDomain"), getResourceFromID(NamespaceManager.getNamespace("dcdom"),
				"weather-nominal"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasArea"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "west-la"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q24: Find workflows that generate a classification
	// ?wft wfq:hasOutputDataset ?d0
	// ?d0 wfq:hasType dcdom:Classification
	@SuppressWarnings("unused")
	private static Query getPaperQuery24(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasOutputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Classification"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q25: Find workflows that generate a classification of cpu data
	// ?wft wfq:hasOutputDataset ?d0
	// ?d0 dcdom:hasDomain dcdom:cpu
	// ?d0 wfq:hasType dcdom:Classification
	@SuppressWarnings("unused")
	private static Query getPaperQuery25(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasOutputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Classification"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasDomain"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "cpu"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q32: Find workflows that do any kind of Classification with the data
	// ?wft hasComponent ?c
	// ?c wfq:subClassOf acdom:Classifier
	@SuppressWarnings("unused")
	private static Query getPaperQuery32(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasComponent"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "subClassOf"), getResourceFromID(NamespaceManager.getNamespace("acdom"), "Classifier"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q33: Find workflows that Sample and then Model the data
	// ?wft wfq:hasComponent ?cSample
	// ?wft wfq:hasComponent ?cModel
	// ?cSample wfq:subClassOf acdom:RandomSampleN
	// ?cModel wfq:subClassOf acdom:Modeler
	// ?cSample wfq:inmediatelyPrecedes ?cModel
	@SuppressWarnings("unused")
	private static Query getPaperQuery33(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasComponent"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "cSample"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasComponent"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "cModel"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "cSample"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "subClassOf"), getResourceFromID(NamespaceManager.getNamespace("acdom"), "RandomSampleN"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "cModel"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "subClassOf"), getResourceFromID(NamespaceManager.getNamespace("acdom"), "Modeler"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "cSample"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "inmediatelyPrecedes"), getResourceFromID(NamespaceManager.getNamespace("wfqv"),
				"cModel"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q34: Find workflows that normalize the input data:
	// ?wft wfq:hasInputDataset ?input
	// ?wft wfq:hasComponent ?normalize
	// ?normalize wfq:subClassOf acdom:Normalize
	// ?normalize wfq:hasInputData ?input
	@SuppressWarnings("unused")
	private static Query getPaperQuery34(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "input"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasComponent"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "normalize"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "normalize"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "subClassOf"), getResourceFromID(NamespaceManager.getNamespace("acdom"), "Normalize"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "normalize"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputData"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "input"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q35: Find workflows that have ID3 classified data as output:
	// ?wft wfq:hasOutputDataset ?output
	// ?wft wfq:hasComponent ?id3Classifier
	// ?id3Classifier wfq:subClassOf acdom:Id3Classifier
	// ?id3Classifier wfq:hasOutputData ?output
	@SuppressWarnings("unused")
	private static Query getPaperQuery35(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasOutputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "output"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasComponent"), getResourceFromID(NamespaceManager.getNamespace("wfqv"),
				"id3classifier"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "id3classifier"), 
				getResourceFromID(NamespaceManager.getNamespace("wfq"), "subClassOf"), getResourceFromID(
				NamespaceManager.getNamespace("acdom"), "Id3Classifier"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "id3classifier"), 
				getResourceFromID(NamespaceManager.getNamespace("wfq"), "hasOutputData"), getResourceFromID(NamespaceManager
				.getNamespace("wfqv"), "output"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q4: use sampled training data to produce a Bayes model, then use model to
	// generate a classification of Iris test data, which is discrete and has
	// missing values
	// ?wft wfq:hasDataset ?sampledTrainingData
	// ?wft wfq:hasDataset ?bayesModel
	// ?wft wfq:hasDataset ?testData
	// ?wft wfq:hasOutputDataset ?classification
	// ?sampledTrainingData dcdom:isSampled "true"^^xsd:boolean
	// ?sampledTrainingData wfq:hasType dcdom:Instance
	// ?bayesModel wfq:hasType dcdom:BayesModel
	// ?testData dcdom:isDiscrete true
	// ?testData dcdom:hasMissingValues true
	// ?testData dcdom:hasDomain dcdom:iris
	// ?testData dcdom:hasType dcdom:Instance
	// ?classification wfq:hasType dcdom:Classification
	// ?sampledTrainingData wfq:datapointInmediatelyPrecedes ?bayesModel
	// ?testData wfq:datapointInmediatelyPrecedes ?classification
	@SuppressWarnings("unused")
	private static Query getPaperQuery4(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"),
				"sampledTrainingData"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "bayesModel"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasOutputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"),
				"classification"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "sampledTrainingData"), 
				getResourceFromID(NamespaceManager.getNamespace("dcdom"), "isSampled"), ontologyFactory.getDataObject(true));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "sampledTrainingData"), 
				getResourceFromID(NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"),
				"Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "bayesModel"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "BayesModel"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "isDiscrete"), ontologyFactory.getDataObject(true));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasMissingValues"), ontologyFactory.getDataObject(true));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDomain"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "iris"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "classification"), 
				getResourceFromID(NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"),
				"Classification"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "sampledTrainingData"), 
				getResourceFromID(NamespaceManager.getNamespace("wfq"), "datapointInmediatelyPrecedes"), getResourceFromID(NamespaceManager
				.getNamespace("wfqv"), "bayesModel"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "datapointInmediatelyPrecedes"), getResourceFromID(
				NamespaceManager.getNamespace("wfqv"), "classification"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q42: Use sampled training data to produce a Bayes model, then use model
	// to generate a classification, which is discrete and has missing values
	// ?wft wfq:hasDataset ?sampledTrainingData
	// ?wft wfq:hasDataset ?bayesModel
	// ?wft wfq:hasDataset ?testData
	// ?wft wfq:hasOutputDataset ?classification
	// ?sampledTrainingData dcdom:isSampled "true"^^xsd:boolean
	// ?sampledTrainingData wfq:hasType dcdom:Instance
	// ?bayesModel wfq:hasType dcdom:BayesModel
	// ?testData dcdom:isDiscrete "true"^^xsd:boolean
	// ?testData dcdom:hasMissingValues "true"^^xsd:boolean
	// ?testData wfq:hasType dcdom:Instance
	// ?classification wfq:hasType dcdom:Classification
	// ?sampledTrainingData wfq:datapointInmediatelyPrecedes ?bayesModel
	// ?testData wfq:datapointInmediatelyPrecedes ?classification
	@SuppressWarnings( { "unused" })
	private static Query getPaperQuery42(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"),
				"sampledTrainingData"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "bayesModel"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasOutputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"),
				"classification"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "sampledTrainingData"), 
				getResourceFromID(NamespaceManager.getNamespace("dcdom"), "isSampled"), ontologyFactory.getDataObject(true));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "sampledTrainingData"), 
				getResourceFromID(NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"),
				"Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "bayesModel"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "BayesModel"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "isDiscrete"), ontologyFactory.getDataObject(true));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasMissingValues"), ontologyFactory.getDataObject(true));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "classification"), 
				getResourceFromID(NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"),
				"Classification"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "sampledTrainingData"), 
				getResourceFromID(NamespaceManager.getNamespace("wfq"), "datapointInmediatelyPrecedes"), getResourceFromID(NamespaceManager
				.getNamespace("wfqv"), "bayesModel"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "datapointInmediatelyPrecedes"), getResourceFromID(
				NamespaceManager.getNamespace("wfqv"), "classification"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q43: Use sampled training data to produce a Decision Tree model, then use
	// model to generate a classification, which is discrete and has missing
	// values
	// ?wft wfq:hasDataset ?sampledTrainingData
	// ?wft wfq:hasDataset ?decisionTreeModel
	// ?wft wfq:hasDataset ?testData
	// ?wft wfq:hasOutputDataset ?classification
	// ?sampledTrainingData dcdom:isSampled "true"^^xsd:boolean
	// ?sampledTrainingData wfq:hasType dcdom:Instance
	// ?decisionTreeModel wfq:hasType dcdom:DecisionTreeModel
	// ?testData dcdom:isDiscrete "true"^^xsd:boolean
	// ?testData dcdom:hasMissingValues "true"^^xsd:boolean
	// ?testData wfq:hasType dcdom:Instance
	// ?testData dcdom:hasDomain dcdom:iris
	// ?classification wfq:hasType dcdom:Classification
	// ?sampledTrainingData wfq:datapointInmediatelyPrecedes ?decisionTreeModel
	// ?testData wfq:datapointInmediatelyPrecedes ?classification
	@SuppressWarnings( { "unused" })
	private static Query getPaperQuery43(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"),
				"sampledTrainingData"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"),
				"decisionTreeModel"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasOutputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"),
				"classification"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "sampledTrainingData"), 
				getResourceFromID(NamespaceManager.getNamespace("dcdom"), "isSampled"), ontologyFactory.getDataObject(true));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "sampledTrainingData"), 
				getResourceFromID(NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"),
				"Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "decisionTreeModel"), 
				getResourceFromID(NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"),
				"DecisionTreeModel"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "isDiscrete"), ontologyFactory.getDataObject(true));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasMissingValues"), ontologyFactory.getDataObject(true));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDomain"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "iris"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "classification"), 
				getResourceFromID(NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"),
				"Classification"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "sampledTrainingData"), 
				getResourceFromID(NamespaceManager.getNamespace("wfq"), "datapointInmediatelyPrecedes"), getResourceFromID(NamespaceManager
				.getNamespace("wfqv"), "decisionTreeModel"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "datapointInmediatelyPrecedes"), getResourceFromID(
				NamespaceManager.getNamespace("wfqv"), "classification"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q44: (removed constraints on testdata) use sampled training data to
	// produce a Bayes model, then use model to generate a classification of
	// Iris test data, which is discrete and has missing values
	// ?wft wfq:hasDataset ?sampledTrainingData
	// ?wft wfq:hasDataset ?bayesModel
	// ?wft wfq:hasDataset ?testData
	// ?wft wfq:hasOutputDataset ?classification
	// ?sampledTrainingData dcdom:isSampled "true"^^xsd:boolean
	// ?sampledTrainingData wfq:hasType dcdom:Instance
	// ?bayesModel wfq:hasType dcdom:BayesModel
	// ?testData wfq:hasType dcdom:Instance
	// ?classification wfq:hasType dcdom:Classification
	// ?sampledTrainingData wfq:datapointInmediatelyPrecedes ?bayesModel
	// ?testData wfq:datapointInmediatelyPrecedes ?classification
	@SuppressWarnings( { "unused" })
	private static Query getPaperQuery44(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"),
				"sampledTrainingData"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "bayesModel"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasOutputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"),
				"classification"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "sampledTrainingData"), 
				getResourceFromID(NamespaceManager.getNamespace("dcdom"), "isSampled"), ontologyFactory.getDataObject(true));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "sampledTrainingData"), 
				getResourceFromID(NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"),
				"Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "bayesModel"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "BayesModel"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "classification"), 
				getResourceFromID(NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"),
				"Classification"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "sampledTrainingData"), 
				getResourceFromID(NamespaceManager.getNamespace("wfq"), "datapointInmediatelyPrecedes"), getResourceFromID(NamespaceManager
				.getNamespace("wfqv"), "bayesModel"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "testData"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "datapointInmediatelyPrecedes"), getResourceFromID(
				NamespaceManager.getNamespace("wfqv"), "classification"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q45 (with hasIntermediateDataset): Find workflows with intermediate data
	// which is sampled and discrete Instance
	// ?wft wfq:hasIntermediateDataset ?d0
	// ?d0 wfq:hasType dcdom:Instance
	// ?d0 dcdom:isDiscrete "true"^^xsd:boolean
	// ?d0 dcdom:isSampled "true"^^xsd:boolean
	@SuppressWarnings( { "unused" })
	private static Query getPaperQuery45(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasIntermediateDataset"), 
				getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "isDiscrete"), ontologyFactory.getDataObject(true));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "isSampled"), ontologyFactory.getDataObject(true));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// // Q44: Retrieve workflows with continuous data as an intermediate node,
	// that comes from a normalization:
	// // ?wft wfq:hasIntermediateDataset ?data
	// // ?wft wfq:hasDataset ?normalized
	// // ?data dcdom:isDiscrete "true"^^xsd:boolean
	// // ?normalized wfq:datapointPrecedes ?data
	// // ?normalized wfq:hasType dcdom:Instance
	// @SuppressWarnings("unused")
	// private static Query getPaperQuery44(String wfVariable) {
	// KBTripleList constraints = new KBTripleList();
	// KBTriple triple;
	// triple = ontologyFactory.getTriple(
	// getResourceFromID(NamespaceManager.getNamespace("wfqv"),
	// wfVariable)),
	// getResourceFromID(NamespaceManager.getNamespace("wfq"),
	// "hasIntermediateDataset")),
	// getResourceFromID(NamespaceManager.getNamespace("wfqv"),
	// "data")));
	// constraints.add(triple);
	// triple = ontologyFactory.getTriple(
	// getResourceFromID(NamespaceManager.getNamespace("wfqv"),
	// wfVariable)),
	// getResourceFromID(NamespaceManager.getNamespace("wfq"),
	// "hasDataset")),
	// getResourceFromID(NamespaceManager.getNamespace("wfqv"),
	// "normalized")));
	// constraints.add(triple);
	// triple = ontologyFactory.getTriple(new KBObject(new
	// ResourceImpl(NamespaceManager.getNamespace("wfqv"), "data"),
	// getResourceFromID(NamespaceManager.getNamespace("dcdom"),
	// "isDiscrete")),
	// ontologyFactory.getDataObject(true));
	// constraints.add(triple);
	// triple = ontologyFactory.getTriple(new KBObject(new
	// ResourceImpl(NamespaceManager.getNamespace("wfqv"), "normalized"),
	// getResourceFromID(NamespaceManager.getNamespace("wfq"),
	// "datapointPrecedes")),
	// getResourceFromID(NamespaceManager.getNamespace("wfqv"),
	// "data")));
	// constraints.add(triple);
	// triple = ontologyFactory.getTriple(new KBObject(new
	// ResourceImpl(NamespaceManager.getNamespace("wfqv"), "normalized"),
	// getResourceFromID(NamespaceManager.getNamespace("wfq"),
	// "hasType")),
	// getResourceFromID(NamespaceManager.getNamespace("dcdom"),
	// "Instance")));
	// constraints.add(triple);
	// return new Query(constraints, wfVariable);
	// }

	// Q3: Find workflows with cpu-2008-07-09 as input to a classifier
	// ?wft wfq:hasDataset ?d0
	// ?wft wfq:hasComponent ?c0
	// ?d0 wfq:canBeBound
	// "/dc/dm/library-dc-test-v1.8.owl#cpu-2007-07-30-194524"
	// labor-2007-07-30-195521 cpu-2007-07-31-161618 labor-2007-07-30-195539
	// labor-2007-07-30-195519 cpu-2007-07-30-194528 labor-2007-07-31-161000
	// weather-2007-07-31-101656
	// ?d0 wfq:hasType dcdom:Instance
	// ?c0 wfq:subClassOf acdom:Classifier
	// ?c0 wfq:hasInputData ?d0
	@SuppressWarnings("unused")
	private static Query getPaperQuery3(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasComponent"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("wflow"), "canBeBound"), getResourceFromID("http://wings-workflows.org/ontology/dc/dm/library-dc-test-v1.8.owl#",
				"cpu-2007-07-30-194524"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "subClassOf"), getResourceFromID(NamespaceManager.getNamespace("acdom"), "Classifier"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputData"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "d0"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Query 5: normalize labor data to create a decision tree model
	// wft hasDataset laborData
	// wft hasDataset dtm
	// wft hasComponent c0
	// laborData hasType Instance
	// laborData hasDomain labor
	// laborData hasMissingValues true
	// laborData datapointPrecedes dtm
	// dtm hasType DecisionTreeModel
	// c0 subClassOf Normalize
	// c0 hasInputData laborData
	@SuppressWarnings( { "unused" })
	private static Query getPaperQuery5(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "laborData"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "dtm"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasComponent"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "laborData"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "laborData"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasDomain"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "labor"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "laborData"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasMissingValues"), ontologyFactory.getDataObject(true));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "dtm"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"),
				"DecisionTreeModel"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "subClassOf"), getResourceFromID(NamespaceManager.getNamespace("acdom"), "Normalize"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputData"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "laborData"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "laborData"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "datapointPrecedes"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "dtm"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q52: �normalize iris data to create a decision tree model (iris data is
	// continuous and has no missing values) �
	// ?wft wfq:hasDataset ?irisData
	// ?wft wfq:hasDataset ?dtm
	// ?wft wfq:hasComponent ?c0
	// ?irisData dcdom:hasDomain dcdom:iris
	// ?irisData dcdom:hasMissingValues "false"^^xsd:boolean
	// ?irisData wfq:datapointPrecedes ?dtm
	// ?irisData wfq:hasType dcdom:Instance
	// ?c0 wfq:subClassOf acdom:Normalize
	// ?c0 wfq:hasInputData ?irisData
	// ?dtm wfq:hasType dcdom:DecisionTreeModel
	@SuppressWarnings( { "unused" })
	private static Query getPaperQuery52(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "irisData"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "dtm"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasComponent"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "irisData"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasDomain"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "iris"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "laborData"), getResourceFromID(
				NamespaceManager.getNamespace("dcdom"), "hasMissingValues"), ontologyFactory.getDataObject(false));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "irisData"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "datapointPrecedes"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "dtm"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "irisData"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "subClassOf"), getResourceFromID(NamespaceManager.getNamespace("acdom"), "Normalize"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputData"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "irisData"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "dtm"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"),
				"DecisionTreeModel"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q53: �normalize training data to create a decision tree model�
	// ?wft wfq:hasDataset ?data1
	// ?wft wfq:hasDataset ?dtm
	// ?wft wfq:hasComponent ?c0
	// ?data1 wfq:hasType dcdom:Instance
	// ?c0 wfq:subClassOf acdom:Normalize
	// ?c0 wfq:hasInputData ?data1
	// ?dtm wfq:hasType dcdom:DecisionTreeModel
	@SuppressWarnings("unused")
	private static Query getPaperQuery53(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "data1"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "dtm"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasComponent"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "data1"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "subClassOf"), getResourceFromID(NamespaceManager.getNamespace("acdom"), "Normalize"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputData"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "data1"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "dtm"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"),
				"DecisionTreeModel"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q54: �normalize training data to create a bayes model�
	// ?wft wfq:hasDataset ?data1
	// ?wft wfq:hasDataset ?dtm
	// ?wft wfq:hasComponent ?c0
	// ?data1 wfq:hasType dcdom:Instance
	// ?c0 wfq:subClassOf acdom:Normalize
	// ?c0 wfq:hasInputData ?data1
	// ?dtm wfq:hasType dcdom:BayesModel
	@SuppressWarnings("unused")
	private static Query getPaperQuery54(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "data1"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "dtm"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasComponent"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "data1"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "subClassOf"), getResourceFromID(NamespaceManager.getNamespace("acdom"), "Normalize"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputData"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "data1"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "dtm"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "BayesModel"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	// Q55: �discretize training data to create a bayes model�
	// ?wft wfq:hasDataset ?data1
	// ?wft wfq:hasDataset ?dtm
	// ?wft wfq:hasComponent ?c0
	// ?data1 wfq:hasType dcdom:Instance
	// ?c0 wfq:subClassOf acdom:Discretize
	// ?c0 wfq:hasInputData ?data1
	// ?dtm wfq:hasType dcdom:BayesModel
	@SuppressWarnings("unused")
	private static Query getPaperQuery55(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "data1"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "dtm"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasComponent"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "data1"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "Instance"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "subClassOf"), getResourceFromID(NamespaceManager.getNamespace("acdom"), "Discretize"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "c0"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputData"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "data1"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "dtm"), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasType"), getResourceFromID(NamespaceManager.getNamespace("dcdom"), "BayesModel"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}

	@SuppressWarnings("unused")
	private static Query getPaperQuery6(String wfVariable) {
		KBTripleList constraints = new KBTripleList();
		KBTriple triple;
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), wfVariable), getResourceFromID(
				NamespaceManager.getNamespace("wfq"), "hasInputDataset"), getResourceFromID(NamespaceManager.getNamespace("wfqv"), "data"));
		constraints.add(triple);
		triple = ontologyFactory.getTriple(getResourceFromID(NamespaceManager.getNamespace("wfqv"), "data"), getResourceFromID(
				NamespaceManager.getNamespace("wflow"), "canBeBound"), getResourceFromID(
				"http://wings-workflows.org/ontology/dc/dm/library-dc-test-v1.8.owl#weather-nominal-2007-07-31-155627"));
		constraints.add(triple);
		return new Query(constraints, wfVariable);
	}
	
	
	
	private static KBObject getResourceFromID(String defaultDomain, String varID) {
		int pos = varID.indexOf("#");
		if (pos < 0) {
			varID = defaultDomain + "#" + varID;
		}
		return ontologyFactory.getObject(varID);
	}
	
	private static KBObject getResourceFromID(String varID) {
		return ontologyFactory.getObject(varID);
	}
}
