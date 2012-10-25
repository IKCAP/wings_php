package edu.isi.ikcap.wings.catalogs.components.classes;

import edu.isi.ikcap.wings.util.Storage;

import java.util.List;
import java.util.LinkedList;

/**
 * A data class to store the transformation characteristics, returned by the
 * Tangram api's.
 * 
 * @author Karan Vahi
 * @author Varun Ratnakar
 * 
 * @version $Revision: 1.1 $
 */
public class TransformationCharacteristics {

	/**
	 * Array storing the names of the attributes that are stored with the
	 * transformation.
	 */
	public static final String TRANSFORMATION_CHARACTERISTICS[] = { "name", "site-handle", "location", "architecture", "operating-system", "dependant-library",
			"expected-runtime", "maximum-runtime", "memory-usage", "diskspace-usage", "compilation-method", "environment-variable", "namespace", "version" };

	/**
	 * The constant to be passed to the accessor functions to get or set the the
	 * name of the transformation.
	 */
	public static final int NAME = 0;

	/**
	 * The constant to be passed to the accessor functions to get or set the the
	 * site handle.
	 */
	public static final int SITE_HANDLE = 1;

	/**
	 * The constant to be passed to the accessor functions to get or set the the
	 * location of the code on the site.
	 */
	public static final int CODE_LOCATION = 2;

	/**
	 * The constant to be passed to the accessor functions to get or set the the
	 * architected.
	 */
	public static final int ARCHITECTURE = 3;

	/**
	 * The constant to be passed to the accessor functions to get or set the the
	 * operating system.
	 */
	public static final int OPERATING_SYSTEM = 4;

	/**
	 * The constant to be passed to the accessor functions to get or set the the
	 * glibc/dependant library.
	 */
	public static final int DEPENDANT_LIBRARY = 5;

	/**
	 * The constant to be passed to the accessor functions to get or set the the
	 * expected runtime.
	 */
	public static final int EXPECTED_RUNTIME = 6;

	/**
	 * The constant to be passed to the accessor functions to get or set the the
	 * expected runtime.
	 */
	public static final int MAXIMUM_RUNTIME = 7;

	/**
	 * The constant to be passed to the accessor functions to get or set the the
	 * memory usage.
	 */
	public static final int MEMORY_USAGE = 8;

	/**
	 * The constant to be passed to the accessor functions to get or set the the
	 * diskspace usage.
	 */
	public static final int DISKSPACE_USAGE = 9;

	/**
	 * The constant to be passed to the accessor functions to get or set the the
	 * compilation method.
	 */
	public static final int COMPILATION_METHOD = 10;

	/**
	 * The constant to be passed to the accessor functions to get or set the the
	 * environment variables.
	 */
	public static final int ENVIRONMENT_VARIABLE = 11;

	/**
	 * The constant to be passed to the accessor functions to get or set the the
	 * namespace of the component.
	 */
	public static final int NAMESPACE = 12;

	/**
	 * The constant to be passed to the accessor functions to get or set the the
	 * version of the component.
	 */
	public static final int VERSION = 13;

	/**
	 * The name of the component/transformation.
	 */
	private String mName;

	/**
	 * The Site where the component/transformation resides.
	 */
	private String mSite;

	/**
	 * The path on the file system where the transformation is at the site.
	 */
	private String mLocation;

	/**
	 * The architecture type of the resource.
	 */
	private String mArch;

	/**
	 * The operating system on which the transformation can run.
	 */
	private String mOS;

	/**
	 * The glibc version of the component/transformation location.
	 */
	private String mGlibc;

	/**
	 * The expected runtime in seconds.
	 */
	private double mExpectedRuntime;

	/**
	 * The upper bound of the time. Corresponds to the max walltime
	 */
	private double mUpperBoundTime;

	/**
	 * The memory usage of the code.
	 */
	private Storage mMemoryUsage;

	/**
	 * The disk space required by the code.
	 */
	private Storage mDiskSpace;

	/**
	 * The compilation method used to compile the executable.
	 */
	private String mCompilationMethod;

	/**
	 * The environment variables that need to be set.
	 */
	private List mEnvironmentVariables;

	/**
	 * The namespace of the component.
	 */
	private String mNamespace;

	/**
	 * The version of the component.
	 */
	private String mVersion;

	/**
	 * The default constructor.
	 */
	public TransformationCharacteristics() {
		mCompilationMethod = "INSTALLED";
		mEnvironmentVariables = new LinkedList();
	}

	/**
	 * Sets a characteristic associated with the transformation/component.
	 * 
	 * @param key
	 *            the characteristic, which is one of the predefined keys.
	 * @param object
	 *            the object containing the attribute value.
	 * 
	 * @throws RuntimeException
	 *             if the object passed for the key is not of valid type.
	 * 
	 * @throws Exception
	 *             if illegal key defined.
	 * 
	 * 
	 * @see #NAME
	 * @see #SITE_HANDLE
	 * @see #LOCATION
	 * @see #ARCHITECTURE
	 * @see #OPERATING_SYSTEM
	 * @see #DEPENDANT_LIBRARY
	 * @see #EXPECTED_RUNTIME
	 * @see #MAXIMUM_RUNTIME
	 * @see #MEMORY_USAGE
	 * @see #COMPILATION_METHOD
	 */
	public void setCharacteristic(int key, Object object) throws RuntimeException {
		if (key < NAME || key > VERSION) {
			throw new RuntimeException(" Wrong characteristic key. Please use one of the predefined key types");
		}

		boolean valid = true;

		switch (key) {
		case NAME:
			if (object != null && object instanceof String)
				mName = (String) object;
			else {
				valid = false;
				mName = null;
			}
			break;

		case SITE_HANDLE:
			if (object != null && object instanceof String)
				mSite = (String) object;
			else {
				valid = false;
				mSite = null;
			}
			break;

		case CODE_LOCATION:
			if (object != null && object instanceof String)
				mLocation = (String) object;
			else {
				valid = false;
				mLocation = null;
			}
			break;

		case ARCHITECTURE:
			if (object != null && object instanceof String)
				mArch = (String) object;
			else {
				valid = false;
				mArch = null;
			}
			break;

		case OPERATING_SYSTEM:
			if (object != null && object instanceof String)
				mOS = (String) object;
			else {
				valid = false;
				mOS = null;
			}
			break;

		case DEPENDANT_LIBRARY:
			if (object != null && object instanceof String)
				mGlibc = (String) object;
			else {
				valid = false;
				mGlibc = null;
			}
			break;

		case EXPECTED_RUNTIME:
			if (object != null) {
				if (object instanceof Double) {
					mExpectedRuntime = (Double) object;
				} else if (object instanceof Integer) {
					mExpectedRuntime = new Double((Integer) object);
				} else {
					valid = false;
				}

			} else {
				valid = false;
			}
			break;

		case MAXIMUM_RUNTIME:

			if (object != null) {
				if (object instanceof Double) {
					mUpperBoundTime = (Double) object;
				} else if (object instanceof Integer) {
					mUpperBoundTime = new Double((Integer) object);
				} else {
					valid = false;
				}

			} else {
				valid = false;
			}
			break;

		case MEMORY_USAGE:
			if (object != null && object instanceof Storage)
				mMemoryUsage = (Storage) object;
			else {
				valid = false;
			}
			break;

		case DISKSPACE_USAGE:
			if (object != null && object instanceof Storage)
				mDiskSpace = (Storage) object;
			else {
				valid = false;
			}
			break;

		case COMPILATION_METHOD:
			if (object != null && object instanceof String)
				mCompilationMethod = (String) object;
			else {
				valid = false;
				mCompilationMethod = null;
			}
			break;

		case ENVIRONMENT_VARIABLE:
			if (object != null && object instanceof EnvironmentVariable)
				mEnvironmentVariables.add(object);
			else {
				valid = false;
			}
			break;

		case NAMESPACE:
			if (object != null && object instanceof String)
				mNamespace = (String) object;
			else {
				valid = false;
				mNamespace = null;
			}
			break;

		case VERSION:
			if (object != null && object instanceof String)
				mVersion = (String) object;
			else {
				valid = false;
				mVersion = null;
			}
			break;

		default:
			throw new RuntimeException("Wrong characterisitc key. Please use one of the predefined key types");

		}

		// if object is not null , and valid == false
		// throw exception
		if (!valid && object != null) {
			throw new RuntimeException("Invalid object passed for key " + TRANSFORMATION_CHARACTERISTICS[key]);
		}

	}

	/**
	 * Returns an <code>Object</code> containing the characteristice
	 * corresponding to the key specified.
	 * 
	 * @param key
	 *            the key.
	 * 
	 * @return <code>Object</code> corresponding to the key value.
	 * @throws RuntimeException
	 *             if illegal key defined.
	 * 
	 * 
	 * @see #NAME
	 * @see #SITE_HANDLE
	 * @see #LOCATION
	 * @see #ARCHITECTURE
	 * @see #OPERATING_SYSTEM
	 * @see #DEPENDANT_LIBRARY
	 * @see #EXPECTED_RUNTIME
	 * @see #MAXIMUM_RUNTIME
	 * @see #MEMORY_USAGE
	 * @see #COMPILATION_METHOD
	 * @see #ENVIRONMENT_VARIABLE
	 */
	public Object getCharacteristic(int key) {
		switch (key) {
		case NAME:
			return mName;

		case SITE_HANDLE:
			return mSite;

		case CODE_LOCATION:
			return mLocation;

		case ARCHITECTURE:
			return mArch;

		case OPERATING_SYSTEM:
			return mOS;

		case DEPENDANT_LIBRARY:
			return mGlibc;

		case EXPECTED_RUNTIME:
			return mExpectedRuntime;

		case MAXIMUM_RUNTIME:
			return mUpperBoundTime;

		case MEMORY_USAGE:
			return mMemoryUsage;

		case DISKSPACE_USAGE:
			return mDiskSpace;

		case COMPILATION_METHOD:
			return mCompilationMethod;

		case ENVIRONMENT_VARIABLE:
			return mEnvironmentVariables;

		case NAMESPACE:
			return mNamespace;

		case VERSION:
			return mVersion;

		default:
			throw new RuntimeException("Illegal key " + key);
		}
	}

	/**
	 * Returns a textual description.
	 * 
	 * @return String
	 */
	public String toString() {
		StringBuffer result = new StringBuffer();
		append(result, TRANSFORMATION_CHARACTERISTICS[0], this.getCharacteristic(TransformationCharacteristics.NAME));
		append(result, TRANSFORMATION_CHARACTERISTICS[12], this.getCharacteristic(TransformationCharacteristics.NAMESPACE));
		append(result, TRANSFORMATION_CHARACTERISTICS[13], this.getCharacteristic(TransformationCharacteristics.VERSION));
		append(result, TRANSFORMATION_CHARACTERISTICS[1], this.getCharacteristic(TransformationCharacteristics.SITE_HANDLE));
		append(result, TRANSFORMATION_CHARACTERISTICS[2], this.getCharacteristic(TransformationCharacteristics.CODE_LOCATION));
		append(result, TRANSFORMATION_CHARACTERISTICS[3], this.getCharacteristic(TransformationCharacteristics.ARCHITECTURE));
		append(result, TRANSFORMATION_CHARACTERISTICS[4], this.getCharacteristic(TransformationCharacteristics.OPERATING_SYSTEM));
		append(result, TRANSFORMATION_CHARACTERISTICS[5], this.getCharacteristic(TransformationCharacteristics.DEPENDANT_LIBRARY));
		append(result, TRANSFORMATION_CHARACTERISTICS[6], this.getCharacteristic(TransformationCharacteristics.EXPECTED_RUNTIME));
		append(result, TRANSFORMATION_CHARACTERISTICS[7], this.getCharacteristic(TransformationCharacteristics.MAXIMUM_RUNTIME));
		append(result, TRANSFORMATION_CHARACTERISTICS[8], this.getCharacteristic(TransformationCharacteristics.MEMORY_USAGE));
		append(result, TRANSFORMATION_CHARACTERISTICS[9], this.getCharacteristic(TransformationCharacteristics.DISKSPACE_USAGE));
		append(result, TRANSFORMATION_CHARACTERISTICS[10], this.getCharacteristic(TransformationCharacteristics.COMPILATION_METHOD));
		append(result, TRANSFORMATION_CHARACTERISTICS[11], this.getCharacteristic(TransformationCharacteristics.ENVIRONMENT_VARIABLE));

		return result.toString();

	}

	/**
	 * Appends to a string buffer a key value pair.
	 * 
	 * @param sb
	 *            StringBuffer
	 * @param key
	 *            String
	 * @param value
	 *            String
	 */
	private void append(StringBuffer sb, String key, Object value) {
		sb.append(key).append(" -> ").append(value).append(",");
	}

}
