package edu.isi.ikcap.wings.workflows.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection; //import java.util.Date;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import edu.isi.ikcap.wings.util.logging.LogEvent;

public class SimpleRDFSoapClient {

	private String url;
	private HashMap<String, String> nsPrefixes;

	private Logger logger;

	// Constructors
	public SimpleRDFSoapClient(String url, String ldid) {
		this.url = url;
		this.initStandardNamespaces();

		this.logger = PropertiesHelper.getLogger(this.getClass().getName(), ldid);
	}

	public SimpleRDFSoapClient(String url, HashMap extraNamespaces, String ldid) {
		this.url = url;
		this.initStandardNamespaces();
		this.nsPrefixes.putAll(extraNamespaces);

		this.logger = PropertiesHelper.getLogger(this.getClass().getName(), ldid);
	}

	// Public getter/setters
	public void setURL(String url) {
		this.url = url;
	}

	public void addNamespaces(HashMap<String, String> namespaces) {
		this.nsPrefixes.putAll(namespaces);
	}

	public String getURL() {
		return this.url;
	}

	public HashMap<String, String> getNamespaces() {
		return this.nsPrefixes;
	}

	// Main Public Functions

	public String createSimpleSoapRequest(String methodName, String[] argNames, String[] values) {
		if (argNames.length != values.length) {
			logger.warn("Mismatched number of args/vals to createSoapRequest for method: " + methodName);
			return null;
		}
		String request = soapOpen() + "<" + methodName + ">\n";
		for (int i = 0; i < argNames.length; i++) {
			request += "<" + argNames[i] + " xsi:type='xsd:string'>";
			request += encodeXMLString(values[i]);
			request += "</" + argNames[i] + ">\n";
		}
		request += "</" + methodName + ">\n";
		request += soapEnd();
		request = request.replaceAll("\r", "");
		return request;
	}

	public Document getSoapResponse(String request, LogEvent lev) {
		try {
			URL url = new URL(this.url);
			URLConnection conn = url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "text/xml");
			conn.setRequestProperty("SoapAction", "");

			if (lev != null)
				logger.debug(lev.createStartLogMsg());

			if (lev != null)
				logger.debug(lev.createLogMsg().addWQ(LogEvent.QUERY_INPUT, request));

			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
			bw.write(request);
			bw.flush();
			bw.close();

			String retval = "";
			String temp;
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			while ((temp = br.readLine()) != null) {
				retval += temp + "\n";
			}
			br.close();

			if (lev != null)
				logger.debug(lev.createLogMsg().addWQ(LogEvent.QUERY_OUTPUT, retval.replaceAll(">", ">\n")));

			DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
			fac.setNamespaceAware(true);
			Document doc = fac.newDocumentBuilder().parse(new ByteArrayInputStream(retval.getBytes()));

			NodeList faults = doc.getElementsByTagName("faultcode");
			for (int i = 0; i < faults.getLength(); i++) {
				String warn = faults.item(i).getChildNodes().item(0).getNodeValue();
				NodeList l = doc.getElementsByTagName("faultstring");
				if (l.getLength() > 0) {
					warn += "\n" + l.item(i).getChildNodes().item(0).getNodeValue();
				}
				l = doc.getElementsByTagName("faultdetail");
				if (l.getLength() > 0) {
					warn += "\n" + l.item(i).getChildNodes().item(0).getNodeValue();
				}
				if (lev != null && logger.isDebugEnabled()) {
					logger.warn(lev.createLogMsg().addMsg(warn));
				}
			}
			if (lev != null)
				logger.debug(lev.createEndLogMsg());

			if (faults.getLength() > 0)
				return null;

			return doc;
		} catch (Exception e) {
			if (lev != null && logger.isDebugEnabled()) {
				logger.warn(lev.createLogMsg().addMsg("Error Sending The following request to PC:\n" + request + "\n" + getStackTrace(e)));
			}
			if (lev != null)
				logger.debug(lev.createEndLogMsg());
			return null;
		}
	}

	private static String getStackTrace(Throwable t) {
		String stackTrace = null;
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			pw.close();
			sw.close();
			stackTrace = sw.getBuffer().toString();
		} catch (Exception ex) {
		}
		return stackTrace;
	}

	// Private Helper Functions

	private String getNsMapXML() {
		String str = "";
		for (String prefix : nsPrefixes.keySet()) {
			if (!prefix.equals(""))
				str += "    xmlns:" + prefix + "='" + nsPrefixes.get(prefix) + "'\n";
		}
		return str;
	}

	private String encodeXMLString(String str) {
		str = str.replaceAll("<", "&lt;");
		str = str.replaceAll(">", "&gt;");
		// str = str.replaceAll("\"", "&quot;");
		// str = str.replaceAll("&", "&amp;");
		return str;
	}

	private String soapOpen() {
		String str = "<SOAP-ENV:Envelope\n" + "    SOAP-ENV:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/'\n"
				+ "    xmlns:SOAP-ENC='http://schemas.xmlsoap.org/soap/encoding/'\n" + "    xmlns:SOAP-ENV='http://schemas.xmlsoap.org/soap/envelope/'\n"
				+ this.getNsMapXML() + "    >\n" + "  <SOAP-ENV:Body>\n";

		return str;
	}

	private String soapEnd() {
		String str = "  </SOAP-ENV:Body>\n" + "</SOAP-ENV:Envelope> ";
		return str;
	}

	private void initStandardNamespaces() {
		nsPrefixes = new HashMap();
		nsPrefixes.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		nsPrefixes.put("xsd", "http://www.w3.org/2001/XMLSchema");
		nsPrefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		nsPrefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		nsPrefixes.put("owl", "http://www.w3.org/2002/07/owl#");
	}
}
