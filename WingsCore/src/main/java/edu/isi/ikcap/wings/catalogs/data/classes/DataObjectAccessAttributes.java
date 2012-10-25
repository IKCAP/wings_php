////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2007, Your Corporation. All Rights Reserved.
////////////////////////////////////////////////////////////////////////////////

package edu.isi.ikcap.wings.catalogs.data.classes;

/**
 * Name: DataObjectAccessAttributes
 * <p/>
 * Package: edu.isi.ikcap.wings.catalogs.data
 * <p/>
 * User: moody
 * <p/>
 * Date: Aug 8, 2007
 * <p/>
 * Time: 11:05:12 AM
 */
public class DataObjectAccessAttributes {

	/**
	 * the access protocol - http, gsiftp, ftp, etc. gsiftp
	 */
	protected String accessProtocol;

	/**
	 * the uri - including the file name: hostname0:port0/some/path/some.file0
	 * 
	 */
	protected String uri;

	/**
	 * indicates a homogenous cluster of resources: site=isi, site=isi-ppc
	 */
	protected String site;

	/**
	 * size of the data set, including units 200MB
	 */
	protected String sizeAndUnits;

	/**
	 * the check sum sdwdf124532rafasf
	 */
	protected String checkSum;

	/**
	 * the time at which this data object will be available, plus the units 0
	 * indicates the data source is currently available 5sec indicates the data
	 * source will be available in 5 seconds
	 */
	protected String availabilityTimeAndUnits;

	/**
	 * full constructor
	 * 
	 * @param accessProtocol
	 *            the access protocol, e.g. gsiftp
	 * @param uri
	 *            the uri including the file name, e.g.
	 *            hostname0:port0/some/path/some.file0
	 * @param site
	 *            the site indicating a homogenous cluster of resources
	 * @param sizeAndUnits
	 *            the size and units of the of the data object
	 * @param checkSum
	 *            the checksum of the data object
	 * @param availabilityTimeAndUnits
	 *            the time at which this data object will be available with
	 *            units
	 */
	public DataObjectAccessAttributes(String accessProtocol, String uri, String site, String sizeAndUnits, String checkSum, String availabilityTimeAndUnits) {
		this.accessProtocol = accessProtocol;
		this.uri = uri;
		this.site = site;
		this.sizeAndUnits = sizeAndUnits;
		this.checkSum = checkSum;
		this.availabilityTimeAndUnits = availabilityTimeAndUnits;
	}

	/**
	 * partial constructor
	 * 
	 * @param accessProtocol
	 *            the access protocol, e.g. gsiftp
	 * @param uri
	 *            the uri including the file name, e.g.
	 *            hostname0:port0/some/path/some.file0
	 * @param site
	 *            the site indicating a homogenous cluster of resources
	 */
	public DataObjectAccessAttributes(String accessProtocol, String uri, String site) {
		this.accessProtocol = accessProtocol;
		this.uri = uri;
		this.site = site;
		this.sizeAndUnits = "100MB";
		this.checkSum = "sdwdf124532rafasf";
		this.availabilityTimeAndUnits = "10sec";

	}

	public String toString() {
		return accessProtocol + "//" + uri + " site=" + site + " checksum=" + checkSum + " availabilityTime=" + availabilityTimeAndUnits;
	}

	/**
	 * 
	 * @return accessProtocol
	 */
	public String getAccessProtocol() {
		return accessProtocol;
	}

	/**
	 * 
	 * @param accessProtocol
	 *            gsiftp, http, ftp, etc.
	 */
	public void setAccessProtocol(String accessProtocol) {
		this.accessProtocol = accessProtocol;
	}

	/**
	 * 
	 * @return uri
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * 
	 * @param uri
	 *            the path to the file
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * 
	 * @return site the site
	 */
	public String getSite() {
		return site;
	}

	/**
	 * 
	 * @param site
	 *            the site
	 */
	public void setSite(String site) {
		this.site = site;
	}

	/**
	 * 
	 * @return sizeAndUnits
	 */
	public String getSizeAndUnits() {
		return sizeAndUnits;
	}

	/**
	 * 
	 * @param sizeAndUnits
	 *            the size and units of the data object
	 */
	public void setSizeAndUnits(String sizeAndUnits) {
		this.sizeAndUnits = sizeAndUnits;
	}

	/**
	 * 
	 * @return checkSum
	 */
	public String getCheckSum() {
		return checkSum;
	}

	/**
	 * 
	 * @param checkSum
	 *            the check sum of the data object
	 */
	public void setCheckSum(String checkSum) {
		this.checkSum = checkSum;
	}

	/**
	 * 
	 * @return availabilityTimeAndUnits
	 */
	public String getAvailabilityTimeAndUnits() {
		return availabilityTimeAndUnits;
	}

	/**
	 * 
	 * @param availabilityTimeAndUnits
	 *            the time (from now) when the data objcet will be available
	 */
	public void setAvailabilityTimeAndUnits(String availabilityTimeAndUnits) {
		this.availabilityTimeAndUnits = availabilityTimeAndUnits;
	}
}
