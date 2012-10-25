////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2007, Your Corporation. All Rights Reserved.
////////////////////////////////////////////////////////////////////////////////

package edu.isi.ikcap.wings.workflows.template;

import java.io.Serializable;

/**
 * Name: Seed
 * <p/>
 * Package: edu.isi.ikcap.workflows
 * <p/>
 * User: moody
 * <p/>
 * Date: Aug 23, 2007
 * <p/>
 * Time: 1:20:40 PM
 */
public interface Seed extends Template, Serializable {

	/**
	 * returns this request id
	 * 
	 * @return requestId
	 */
	public String getSeedId();

	/**
	 * sets this request id
	 * 
	 * @param requestId
	 *            a request id
	 */
	public void setSeedId(String seedId);

	/**
	 * returns an xml representation of the request
	 * 
	 * @return a String representation of the request in xml
	 */
	public String getInternalRepresentation();

	public String deriveTemplateRepresentation();

	// Constraint Queries
	public ConstraintEngine getSeedConstraintEngine();

	public ConstraintEngine getTemplateConstraintEngine();

	public String getName();

	public void setTemplateFile(String templateFile);

	public String getTemplateFile();

	public Metadata getSeedMetadata();

	public RuleSet getSeedRules();

	void clearImportCache();

	void close();

}
