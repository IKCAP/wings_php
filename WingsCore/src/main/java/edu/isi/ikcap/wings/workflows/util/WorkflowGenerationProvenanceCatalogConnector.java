////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2007, Your Corporation. All Rights Reserved.
////////////////////////////////////////////////////////////////////////////////

package edu.isi.ikcap.wings.workflows.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Name: WorkflowGenerationProvenanceCatalogConnector
 * <p/>
 * Package: edu.isi.ikcap.workflows
 * <p/>
 * User: moody
 * <p/>
 * Date: Aug 23, 2007
 * <p/>
 * Time: 8:28:02 PM
 */
public class WorkflowGenerationProvenanceCatalogConnector {

	/**
	 * the class name of the mysql driver
	 */
	public final String MYSQL_DRIVER_CLASS = "com.mysql.jdbc.Driver";

	/**
	 * the mysql host for the workflow generation provenance catalog
	 */
	public String mySqlWgpcHost;

	/**
	 * the mysql user for the workflow generation provenance catalog
	 */
	public String mySqlWgpcUser;

	/**
	 * the mysql password for the workflow generation provenance catalog
	 */
	public String mySqlWgpcPassword;

	/**
	 * the mysql port for the workflow generation provenance catalog
	 */
	public String mySqlWgpcPort;

	/**
	 * the name of the workflow generation provenance catalog database
	 */
	public String workflowGenerationProvenanceCatalog;

	/**
	 * jdbc:mysql://localhost:port/test?user=monty&password=greatsqldb
	 */
	public String jdbcConnectionString;

	/**
	 * the seed table in the workflow generation provenance catalog
	 */
	public String seedTable;

	/**
	 * the candidate templates table in the workflow generation provenance
	 * catalog
	 */
	public String atomicSeedsTable;

	/**
	 * the q2.1 table in the workflow generation provenance catalog
	 */
	public String query2point1Table;

	/**
	 * the q3.1 table in the workflow generation provenance catalog
	 */
	public String query3point1Table;

	/**
	 * the specialized templates table in the workflow generation provenance
	 * catalog
	 */
	public String candidateWorkflowsTable;

	/**
	 * the data metrics table in the workflow generation provenance catalog
	 */
	public String dataMetricsTable;

	/**
	 * the partially specilized templates table in the workflow generation
	 * provenance catalog
	 */
	public String boundWorkflowsTable;

	/**
	 * the q4.5 table in the workflow generation provenance catalog
	 */
	public String query4point5Table;

	/**
	 * the table that holds the templates and daxes in the workflow generation
	 * provenance catalog
	 */
	public String instancesAndDaxesTable;

	/**
	 * an array of tableNames
	 */
	private ArrayList<String> tableNames;

	private Connection jdbcConnection;

	/**
	 * default constructor with all the fields hard coded
	 */
	public WorkflowGenerationProvenanceCatalogConnector() {
		// Read Properties File (wings.properties)
		Properties conf = PropertiesHelper.loadWingsProperties();
		this.mySqlWgpcHost = conf.getProperty("wgpc.host");
		this.mySqlWgpcUser = conf.getProperty("wgpc.user");
		this.mySqlWgpcPassword = conf.getProperty("wgpc.password");
		this.mySqlWgpcPort = conf.getProperty("wgpc.port");
		this.workflowGenerationProvenanceCatalog = conf.getProperty("wgpc.dbname");

		this.seedTable = "seeds";
		this.atomicSeedsTable = "atomic_seeds";
		this.candidateWorkflowsTable = "candidate_workflows";
		this.boundWorkflowsTable = "bound_workflows";
		this.instancesAndDaxesTable = "instances_and_daxes";
		this.dataMetricsTable = "data_metrics";
		this.query2point1Table = "query_2_point_1";
		this.query3point1Table = "query_3_point_1";
		this.query4point5Table = "query_4_point_5";

		this.jdbcConnectionString = this.makeJdbcConnectionString();
		try {
			Class.forName(this.MYSQL_DRIVER_CLASS);
			this.jdbcConnection = DriverManager.getConnection(this.jdbcConnectionString, this.mySqlWgpcUser, this.mySqlWgpcPassword);
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.tableNames = new ArrayList<String>();
		this.tableNames.add(this.seedTable);
		this.tableNames.add(this.atomicSeedsTable);
		this.tableNames.add(this.boundWorkflowsTable);
		this.tableNames.add(this.query2point1Table);
		this.tableNames.add(this.query3point1Table);
		this.tableNames.add(this.candidateWorkflowsTable);
		this.tableNames.add(this.dataMetricsTable);
		this.tableNames.add(this.query4point5Table);
		this.tableNames.add(this.instancesAndDaxesTable);
	}

	/**
	 * full constructor
	 * 
	 * @param mySqlWgpcHost
	 *            the host
	 * @param mySqlWgpcUser
	 *            the user
	 * @param mySqlWgpcPassword
	 *            the password
	 * @param mySqlWgpcPort
	 *            the port
	 * @param workflowGenerationProvenanceCatalog
	 *            the db name
	 * @param seedTable
	 *            the seed table
	 * @param atomicSeedsTable
	 *            the template table
	 * @param query2point1Table
	 *            the q2.1 table
	 * @param query3point1Table
	 *            the q3.1 table
	 * @param candidateWorkflowsTable
	 *            the specialize templates table
	 * @param dataMetricsTable
	 *            the data metrics table
	 * @param boundWorkflowsTable
	 *            the partiallay specified candidates table
	 * @param query4point5Table
	 *            the q4.5 table
	 * @param instancesAndDaxesTables
	 *            the templates and dax tables
	 */
	public WorkflowGenerationProvenanceCatalogConnector(String mySqlWgpcHost, String mySqlWgpcUser, String mySqlWgpcPassword, String mySqlWgpcPort,
			String workflowGenerationProvenanceCatalog, String seedTable, String atomicSeedsTable, String query2point1Table, String query3point1Table,
			String candidateWorkflowsTable, String dataMetricsTable, String boundWorkflowsTable, String query4point5Table, String instancesAndDaxesTables) {
		this.mySqlWgpcHost = mySqlWgpcHost;
		this.mySqlWgpcUser = mySqlWgpcUser;
		this.mySqlWgpcPassword = mySqlWgpcPassword;
		this.mySqlWgpcPort = mySqlWgpcPort;
		this.workflowGenerationProvenanceCatalog = workflowGenerationProvenanceCatalog;
		this.seedTable = seedTable;
		this.atomicSeedsTable = atomicSeedsTable;
		this.query2point1Table = query2point1Table;
		this.query3point1Table = query3point1Table;
		this.candidateWorkflowsTable = candidateWorkflowsTable;
		this.dataMetricsTable = dataMetricsTable;
		this.boundWorkflowsTable = boundWorkflowsTable;
		this.instancesAndDaxesTable = instancesAndDaxesTables;
		this.query4point5Table = query4point5Table;
		this.jdbcConnectionString = this.makeJdbcConnectionString();
		try {
			Class.forName(this.MYSQL_DRIVER_CLASS);
			this.jdbcConnection = DriverManager.getConnection(this.jdbcConnectionString, this.mySqlWgpcUser, this.mySqlWgpcPassword);
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.tableNames = new ArrayList<String>();
		this.tableNames.add(this.seedTable);
		this.tableNames.add(this.atomicSeedsTable);
		this.tableNames.add(this.boundWorkflowsTable);
		this.tableNames.add(this.query2point1Table);
		this.tableNames.add(this.query3point1Table);
		this.tableNames.add(this.candidateWorkflowsTable);
		this.tableNames.add(this.dataMetricsTable);
		this.tableNames.add(this.query4point5Table);
		this.tableNames.add(this.instancesAndDaxesTable);
	}

	/*
	 * public static void main(String[] args) {
	 * WorkflowGenerationProvenanceCatalogConnector wgpc; wgpc = new
	 * WorkflowGenerationProvenanceCatalogConnector();
	 * wgpc.clearWorkflowGenerationProvenanceCatalog(); }
	 */

	/**
	 * creates the jdbc connection string used to create a connection call this
	 * ONCE, preferably from the constructor
	 * jdbc:mysql://localhost:port/test?user=monty&password=greatsqldb
	 * 
	 * @return the jdbc connection string
	 */
	public String makeJdbcConnectionString() {
		StringBuilder result = new StringBuilder();
		result.append("jdbc:mysql://");
		result.append(this.getMySqlWgpcHost());
		if (this.getMySqlWgpcPort() != null) {
			result.append(":");
			result.append(this.getMySqlWgpcPort());
		}
		result.append("/");
		result.append(this.getWorkflowGenerationProvenanceCatalog());
		/*
		 * result.append("?user="); result.append(this.getMySqlWgpcUser());
		 * if(this.getMySqlWgpcPassword() != null) {
		 * result.append("&password=");
		 * result.append(this.getMySqlWgpcPassword()); }
		 */
		// System.out.println(result);
		return result.toString();
	}

	public Connection getConnection() {
		return this.jdbcConnection;
	}

	// Compress/Decompress Functions (used for metrics)
	public String compress(String what) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DeflaterOutputStream dos = new DeflaterOutputStream(out);
			dos.write(what.getBytes("UTF8"));
			dos.finish();
			dos.flush();
			dos.close();
			return out.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String decompress(String zipped) {
		try {
			InputStream in = new ByteArrayInputStream(zipped.getBytes());
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InflaterInputStream iis = new InflaterInputStream(in);

			byte[] buffer = new byte[1024];
			int offset = -1;
			while ((offset = iis.read(buffer)) != -1) {
				out.write(buffer, 0, offset);
			}
			iis.close();
			return out.toString("UTF8");
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * returns "'string'"
	 * 
	 * @param string
	 *            a string
	 * @return a quoted string
	 */
	public String quoteString(String string) {
		return "'" + string + "'";
	}

	/**
	 * generates a comma separated list of quoted values
	 * 
	 * @param values
	 *            a list of string values
	 * @return a comma separated list of quoted string values
	 */
	public String arrayListToCommaSeparatedQuotedValues(ArrayList<String> values) {
		int length = values.size();
		int counter = 0;
		StringBuilder result = new StringBuilder();
		for (String value : values) {
			result.append(quoteString(value));
			if (++counter != length) {
				result.append(",");
			}
		}
		return result.toString();
	}

	/**
	 * generates a comma separated list of values
	 * 
	 * @param values
	 *            a list of string values
	 * @return a comma separated list of quoted string values
	 */
	public String arrayListToCommaSeparatedValues(ArrayList<String> values) {
		int length = values.size();
		int counter = 0;
		StringBuilder result = new StringBuilder();
		for (String value : values) {
			result.append(value);
			if (++counter != length) {
				result.append(",");
			}
		}
		return result.toString();
	}

	/**
	 * a generic insert method
	 * 
	 * @param tableName
	 *            the table to insert into
	 * @param values
	 *            a list of string values to insert
	 * @return true iff successful
	 */
	public boolean insertIntoTable(String tableName, ArrayList<Object> values) {
		// failure <==> false
		int result = 0;
		String insert = "INSERT INTO " + tableName + " VALUES (";
		for (int i = 0; i < values.size(); i++) {
			if (i > 0)
				insert += ", ";
			insert += "?";
		}
		insert += ")";
		try {
			Connection conn = this.jdbcConnection;
			PreparedStatement ps = conn.prepareStatement(insert);
			for (int i = 0; i < values.size(); i++) {
				ps.setObject(i + 1, values.get(i));
				// ps.setString(i + 1, values.get(i));
			}
			result = ps.executeUpdate();
		} catch (SQLException ex) {
			// handle any errors
			System.err.println("SQLException: " + ex.getMessage());
			System.err.println("SQLState: " + ex.getSQLState());
			System.err.println("VendorError: " + ex.getErrorCode());
			System.err.println(insert);
			// System.err.println(values);
		}
		return result != 0;
	}

	public void storeComponentInformation(String seedId, String daxId, String jobId, String componentId, String componentType, byte[] inputMaps,
			byte[] outputMaps) {
		// failure <==> false
		try {
			Connection conn = this.jdbcConnection;
			PreparedStatement ps = conn.prepareStatement("insert into " + "dax_component_argument_maps values (?, ?, ?, ?, ?, ?, ?)");
			ps.setString(1, seedId);
			ps.setString(2, daxId);
			ps.setString(3, jobId);
			ps.setString(4, componentId);
			ps.setString(5, componentType);
			ps.setBytes(6, inputMaps);
			ps.setBytes(7, outputMaps);
			ps.executeUpdate();
		} catch (SQLException ex) {
			// handle any errors
			System.err.println("SQLException: " + ex.getMessage());
			System.err.println("SQLState: " + ex.getSQLState());
			System.err.println("VendorError: " + ex.getErrorCode());
		}
	}

	public ArrayList getComponentInformation(String seedId, String daxId, String jobId) {
		ArrayList compInfo = new ArrayList();
		try {
			Connection conn = this.jdbcConnection;
			Statement stmt = conn.createStatement();
			String select = "select component_id, component_type, input_maps, " + "output_maps from dax_component_argument_maps where seed_id='" + seedId
					+ "' and dax_id='" + daxId + "' and job_id='" + jobId + "';";

			ResultSet result = stmt.executeQuery(select);
			if (result.next()) {
				String componentId = result.getString("component_id");
				String componentType = result.getString("component_type");
				byte[] inputMaps = result.getBytes("input_maps");
				byte[] outputMaps = result.getBytes("output_maps");
				compInfo.add(componentId);
				compInfo.add(componentType);
				compInfo.add(inputMaps);
				compInfo.add(outputMaps);
			}
		} catch (SQLException ex) {
			// handle any errors
			System.err.println("SQLException: " + ex.getMessage());
			System.err.println("SQLState: " + ex.getSQLState());
			System.err.println("VendorError: " + ex.getErrorCode());
		}
		return compInfo;
	}

	/**
	 * retrieves the data metrices/characteristics (xml string) for a data
	 * source id from the Predictive Data Catalog
	 * 
	 * @param dataObjectId
	 *            a data source id
	 * @return an xml string describing the data metrics/characteristics for a
	 *         data source id
	 */
	public String getDataMetrics(String dataObjectId) {
		String xml = null;
		try {
			Connection conn = this.jdbcConnection;
			Statement stmt = conn.createStatement();
			String select = "select metrics from " + getDataMetricsTable() + " where data_object_id='" + dataObjectId + "';";

			ResultSet result = stmt.executeQuery(select);
			if (result.next()) {
				xml = decompress(result.getString("metrics"));
			}
			result.close();
			stmt.close();
		} catch (SQLException ex) {
			// handle any errors
			System.err.println("SQLException: " + ex.getMessage());
			System.err.println("SQLState: " + ex.getSQLState());
			System.err.println("VendorError: " + ex.getErrorCode());
			System.exit(1);
		}

		return xml;
	}

	/**
	 * delete all row from all tables in the wgpc
	 */
	public void clearWorkflowGenerationProvenanceCatalog() {
		for (String tableName : tableNames) {
			this.deleteAllFromTable(tableName);
		}
	}

	/**
	 * deletes all rows from table
	 * 
	 * @param tableName
	 *            the table name
	 * @return true iff successful
	 */
	public boolean deleteAllFromTable(String tableName) {
		// failure <==> false
		int result = 0;
		try {
			Connection conn = this.jdbcConnection;
			Statement stmt = conn.createStatement();
			String delete = "DELETE FROM " + tableName + ";";
			result = stmt.executeUpdate(delete);
			stmt.close();
		} catch (SQLException ex) {
			// handle any errors
			System.err.println("SQLException: " + ex.getMessage());
			System.err.println("SQLState: " + ex.getSQLState());
			System.err.println("VendorError: " + ex.getErrorCode());
		}
		return result != 0;
	}

	/**
	 * Getter for property 'mySqlWgpcHost'.
	 * 
	 * @return Value for property 'mySqlWgpcHost'.
	 */
	public String getMySqlWgpcHost() {
		return mySqlWgpcHost;
	}

	/**
	 * Setter for property 'mySqlWgpcHost'.
	 * 
	 * @param mySqlWgpcHost
	 *            Value to set for property 'mySqlWgpcHost'.
	 */
	public void setMySqlWgpcHost(String mySqlWgpcHost) {
		this.mySqlWgpcHost = mySqlWgpcHost;
	}

	/**
	 * Getter for property 'mySqlWgpcUser'.
	 * 
	 * @return Value for property 'mySqlWgpcUser'.
	 */
	public String getMySqlWgpcUser() {
		return mySqlWgpcUser;
	}

	/**
	 * Setter for property 'mySqlWgpcUser'.
	 * 
	 * @param mySqlWgpcUser
	 *            Value to set for property 'mySqlWgpcUser'.
	 */
	public void setMySqlWgpcUser(String mySqlWgpcUser) {
		this.mySqlWgpcUser = mySqlWgpcUser;
	}

	/**
	 * Getter for property 'mySqlWgpcPassword'.
	 * 
	 * @return Value for property 'mySqlWgpcPassword'.
	 */
	public String getMySqlWgpcPassword() {
		return mySqlWgpcPassword;
	}

	/**
	 * Setter for property 'mySqlWgpcPassword'.
	 * 
	 * @param mySqlWgpcPassword
	 *            Value to set for property 'mySqlWgpcPassword'.
	 */
	public void setMySqlWgpcPassword(String mySqlWgpcPassword) {
		this.mySqlWgpcPassword = mySqlWgpcPassword;
	}

	/**
	 * Getter for property 'mySqlWgpcPort'.
	 * 
	 * @return Value for property 'mySqlWgpcPort'.
	 */
	public String getMySqlWgpcPort() {
		return mySqlWgpcPort;
	}

	/**
	 * Setter for property 'mySqlWgpcPort'.
	 * 
	 * @param mySqlWgpcPort
	 *            Value to set for property 'mySqlWgpcPort'.
	 */
	public void setMySqlWgpcPort(String mySqlWgpcPort) {
		this.mySqlWgpcPort = mySqlWgpcPort;
	}

	/**
	 * Getter for property 'workflowGenerationProvenanceCatalog'.
	 * 
	 * @return Value for property 'workflowGenerationProvenanceCatalog'.
	 */
	public String getWorkflowGenerationProvenanceCatalog() {
		return workflowGenerationProvenanceCatalog;
	}

	/**
	 * Setter for property 'workflowGenerationProvenanceCatalog'.
	 * 
	 * @param workflowGenerationProvenanceCatalog
	 *            Value to set for property
	 *            'workflowGenerationProvenanceCatalog'.
	 */
	public void setWorkflowGenerationProvenanceCatalog(String workflowGenerationProvenanceCatalog) {
		this.workflowGenerationProvenanceCatalog = workflowGenerationProvenanceCatalog;
	}

	/**
	 * Getter for property 'jdbcConnectionString'.
	 * 
	 * @return Value for property 'jdbcConnectionString'.
	 */
	public String getJdbcConnectionString() {
		return jdbcConnectionString;
	}

	/**
	 * Setter for property 'jdbcConnectionString'.
	 * 
	 * @param jdbcConnectionString
	 *            Value to set for property 'jdbcConnectionString'.
	 */
	public void setJdbcConnectionString(String jdbcConnectionString) {
		this.jdbcConnectionString = jdbcConnectionString;
	}

	/**
	 * Getter for property 'seedTable'.
	 * 
	 * @return Value for property 'seedTable'.
	 */
	public String getSeedTable() {
		return seedTable;
	}

	/**
	 * Setter for property 'seedTable'.
	 * 
	 * @param seedTable
	 *            Value to set for property 'seedTable'.
	 */
	public void setSeedTable(String seedTable) {
		this.seedTable = seedTable;
	}

	/**
	 * Getter for property 'atomicSeedsTable'.
	 * 
	 * @return Value for property 'atomicSeedsTable'.
	 */
	public String getAtomicSeedsTable() {
		return atomicSeedsTable;
	}

	/**
	 * Setter for property 'atomicSeedsTable'.
	 * 
	 * @param atomicSeedsTable
	 *            Value to set for property 'atomicSeedsTable'.
	 */
	public void setAtomicSeedsTable(String atomicSeedsTable) {
		this.atomicSeedsTable = atomicSeedsTable;
	}

	/**
	 * Getter for property 'query2point1Table'.
	 * 
	 * @return Value for property 'query2point1Table'.
	 */
	public String getQuery2point1Table() {
		return query2point1Table;
	}

	/**
	 * Setter for property 'query2point1Table'.
	 * 
	 * @param query2point1Table
	 *            Value to set for property 'query2point1Table'.
	 */
	public void setQuery2point1Table(String query2point1Table) {
		this.query2point1Table = query2point1Table;
	}

	/**
	 * Getter for property 'query3point1Table'.
	 * 
	 * @return Value for property 'query3point1Table'.
	 */
	public String getQuery3point1Table() {
		return query3point1Table;
	}

	/**
	 * Setter for property 'query3point1Table'.
	 * 
	 * @param query3point1Table
	 *            Value to set for property 'query3point1Table'.
	 */
	public void setQuery3point1Table(String query3point1Table) {
		this.query3point1Table = query3point1Table;
	}

	/**
	 * Getter for property 'candidateWorkflowsTable'.
	 * 
	 * @return Value for property 'candidateWorkflowsTable'.
	 */
	public String getCandidateWorkflowsTable() {
		return candidateWorkflowsTable;
	}

	/**
	 * Setter for property 'candidateWorkflowsTable'.
	 * 
	 * @param candidateWorkflowsTable
	 *            Value to set for property 'candidateWorkflowsTable'.
	 */
	public void setCandidateWorkflowsTable(String candidateWorkflowsTable) {
		this.candidateWorkflowsTable = candidateWorkflowsTable;
	}

	/**
	 * Getter for property 'dataMetricsTable'.
	 * 
	 * @return Value for property 'dataMetricsTable'.
	 */
	public String getDataMetricsTable() {
		return dataMetricsTable;
	}

	/**
	 * Setter for property 'dataMetricsTable'.
	 * 
	 * @param dataMetricsTable
	 *            Value to set for property 'dataMetricsTable'.
	 */
	public void setDataMetricsTable(String dataMetricsTable) {
		this.dataMetricsTable = dataMetricsTable;
	}

	/**
	 * Getter for property 'query4point5Table'.
	 * 
	 * @return Value for property 'query4point5Table'.
	 */
	public String getQuery4point5Table() {
		return query4point5Table;
	}

	/**
	 * Setter for property 'query4point5Table'.
	 * 
	 * @param query4point5Table
	 *            Value to set for property 'query4point5Table'.
	 */
	public void setQuery4point5Table(String query4point5Table) {
		this.query4point5Table = query4point5Table;
	}

	/**
	 * Getter for property 'boundWorkflowsTable'.
	 * 
	 * @return Value for property 'boundWorkflowsTable'.
	 */
	public String getBoundWorkflowsTable() {
		return boundWorkflowsTable;
	}

	/**
	 * Setter for property 'boundWorkflowsTable'.
	 * 
	 * @param boundWorkflowsTable
	 *            Value to set for property 'boundWorkflowsTable'.
	 */
	public void setBoundWorkflowsTable(String boundWorkflowsTable) {
		this.boundWorkflowsTable = boundWorkflowsTable;
	}

	/**
	 * Getter for property 'templatesAndDaxesTables'.
	 * 
	 * @return Value for property 'templatesAndDaxesTables'.
	 */
	public String getTemplatesAndDaxesTable() {
		return instancesAndDaxesTable;
	}

	/**
	 * Setter for property 'instancesAndDaxesTable'.
	 * 
	 * @param instancesAndDaxesTable
	 *            Value to set for property 'instancesAndDaxesTable'.
	 */
	public void setInstancesAndDaxesTable(String instancesAndDaxesTable) {
		this.instancesAndDaxesTable = instancesAndDaxesTable;
	}

	/**
	 * Getter for property 'tableNames'.
	 * 
	 * @return Value for property 'tableNames'.
	 */
	public ArrayList<String> getTableNames() {
		return tableNames;
	}

	/**
	 * Setter for property 'tableNames'.
	 * 
	 * @param tableNames
	 *            Value to set for property 'tableNames'.
	 */
	public void setTableNames(ArrayList<String> tableNames) {
		this.tableNames = tableNames;
	}
}
