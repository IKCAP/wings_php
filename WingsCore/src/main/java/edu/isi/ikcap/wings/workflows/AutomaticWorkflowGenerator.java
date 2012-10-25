////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2007, Your Corporation. All Rights Reserved.
////////////////////////////////////////////////////////////////////////////////

package edu.isi.ikcap.wings.workflows;

import edu.isi.ikcap.wings.workflows.template.Seed;
import edu.isi.ikcap.wings.workflows.template.Template;
import edu.isi.ikcap.wings.workflows.util.DAX;

import java.util.ArrayList;

/**
 * Name: AutomaticWorkflowGenerator
 * <p/>
 * Package: edu.isi.ikcap.workflows
 * <p/>
 * User: moody
 * <p/>
 * Date: Aug 23, 2007
 * <p/>
 * Time: 12:34:58 PM
 */
public interface AutomaticWorkflowGenerator {

	public Seed initializeSeed(String seedName);

	/**
	 * Step 1
	 * 
	 * @param seed
	 *            the seed
	 * @return a list of candiate seeds
	 */
	public ArrayList<Seed> findCandidateSeeds(Seed seed);

	/**
	 * Step 2
	 * 
	 * @param template
	 *            a candiate template from step 1.
	 * @return a list of specialized templates customized to request constraints
	 */
	public ArrayList<Template> specializeTemplates(Template template);

	/**
	 * step 3
	 * 
	 * @param specializedTemplate
	 *            a template with concrete components
	 * @return a list of partially specified template instances - input data
	 *         objects bound
	 */
	public ArrayList<Template> selectInputDataObjects(Template specializedTemplate);

	/**
	 * Step 4: 4.1 sets the data metrics for the partially instantiated
	 * candidate instances
	 * 
	 * @param partialCandidateInstances
	 *            a list of candidate instances with input data variables bound
	 */
	public void setDataMetricsForInputDataObjects(ArrayList<Template> partialCandidateInstances);

	/**
	 * Step 4: 4.2, 4.3, and 4.4
	 * 
	 * @param template
	 *            a specialized template
	 * @return partially specified candidate instance with all input data
	 *         selected.
	 */
	public ArrayList<Template> configureTemplates(Template template);

	/*
	 * Step x This just adds inferred constraints (from the PC) on the template
	 */
	public Template getInferredTemplate(Template template);

	/*
	 * Save as getInferredTemplate, except you can pass a parameter whether to
	 * infer only types or to fire rules(maybe) and infer more information
	 */
	public Template getInferredTemplate(Template template, boolean infer_types_only);

	public DAX getTemplateDAX(Template template);
	
	public ArrayList<String> getExplanations();
}
