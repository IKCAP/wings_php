package edu.isi.ikcap.wings.catalogs.data.classes;

import edu.isi.ikcap.wings.util.Storage;

/**
 * A Data class that stores the results returned from the DC for a particular
 * data object With the data object, it associates the following - the data
 * object id - location - the site where it resides - checksum - size -
 * availability time
 * 
 * 
 * This corresponds to /identical to the Data Source Location object that the DC
 * have. Hence the name DataSourceLocationObject instead of DataSourceLocation
 * 
 * Example String reprsentation of object is as follows: DS8
 * gsiftp://hostname0:port0/some/path/some.file0 site=isi size=200MB
 * checksum=sdwdf124532rafasf availabilityTime=0
 * 
 * 
 * @author Karan Vahi
 * @version $Revision: 1.1 $
 */
public class DataSourceLocationObject {

	/**
	 * The logical identifier of the Data Object.
	 */
	private String mIdentifier;

	/**
	 * The site on which the instance of the Data Object resides.
	 */
	private String mSite;

	/**
	 * The location of the data object.
	 */
	private String mLocation;

	/**
	 * The checksum of the DataObject
	 */
	private String mChecksum;

	/**
	 * The availability time of the Data Object.
	 */
	private double mAvailabilityTime;

	/**
	 * The file size of the Data Object.
	 */
	private Storage mFileSize;

	/**
	 * The default constructor.
	 */
	public DataSourceLocationObject() {
		mAvailabilityTime = 0;
	}

	/**
	 * Returns the data object id.
	 * 
	 * @return id
	 */
	public String getDataObjectID() {
		return mIdentifier;
	}

	/**
	 * Set the data object id.
	 * 
	 * @param id
	 *            the DataObjectID
	 */
	public void setDataObjectID(String id) {
		mIdentifier = id;
	}

	/**
	 * Returns the site at which the DataObject instance resides.
	 * 
	 * @return the site.
	 */
	public String getSite() {
		return mSite;
	}

	/**
	 * Sets the site at which the DataObject instance resides.
	 * 
	 * @param site
	 *            the site.
	 */
	public void setSite(String site) {
		mSite = site;
	}

	/**
	 * Returns the location of the data object on the site
	 * 
	 * @return the location.
	 */
	public String getLocation() {
		return mLocation;
	}

	/**
	 * Seturns the location of the data object on the site.
	 * 
	 * @param location
	 *            the location.
	 */
	public void setLocation(String location) {
		mLocation = location;
	}

	/**
	 * Returns the availability time for the DataObject instance resides.
	 * 
	 * @return the availability time.
	 */
	public double getAvailabilityTime() {
		return mAvailabilityTime;
	}

	/**
	 * Sets the availability time for the DataObject instance resides.
	 * 
	 * @param time
	 *            the availability time.
	 */
	public void setAvailabilityTime(double time) {
		mAvailabilityTime = time;
	}

	/**
	 * Returns the checksum of the data object
	 * 
	 * @return the checksum.
	 */
	public String getChecksum() {
		return mChecksum;
	}

	/**
	 * Sets the checksum of the data object
	 * 
	 * @param checksum
	 *            the checksum.
	 */
	public void setChecksum(String checksum) {
		mChecksum = checksum;
	}

	/**
	 * Returns the file size of the data object on the site
	 * 
	 * @return the filesize.
	 */
	public Storage getFileSize() {
		return mFileSize;
	}

	/**
	 * Sets the file size of the data object on the site
	 * 
	 * @param size
	 *            the filesize.
	 */
	public void setFileSize(Storage size) {
		mFileSize = size;
	}

}
