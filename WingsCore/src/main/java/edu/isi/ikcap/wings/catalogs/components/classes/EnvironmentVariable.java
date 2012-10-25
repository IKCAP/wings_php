package edu.isi.ikcap.wings.catalogs.components.classes;

/**
 * This Class hold informations about environment variables.
 * 
 * @version $Revision: 1.1 $
 */
public class EnvironmentVariable {

	/**
	 * The name of the variable.
	 */
	private String mKey;

	/**
	 * The value of the variable
	 */
	private String mValue;

	/**
	 * 
	 * C'tpr for the class;
	 * 
	 * @throws java.lang.Exception
	 */
	public EnvironmentVariable() {
		mKey = null;
		mValue = null;

	}

	/**
	 * This constructor allows to set the namespace , key and value of the
	 * PoolProfile.
	 * 
	 * @param key
	 *            Takes a String as the key.
	 * @param value
	 *            The value for the key as String
	 * 
	 */
	public EnvironmentVariable(String key, String value) {
		mKey = new String(key);
		mValue = new String(value);
	}

	/**
	 * Returns the Key of the Profile
	 * 
	 * @return String
	 */
	public String getKey() {
		return mKey;
	}

	/**
	 * Returns the Value for the profile
	 * 
	 * @return String
	 */
	public String getValue() {
		return mValue;
	}

	/**
	 * Returns the textual description of the contents of <code>Profile</code>
	 * object in the multiline format.
	 * 
	 * @return the textual description in multiline format.
	 */
	public String toMultiLine() {
		return this.toString();
	}

	/**
	 * This method returns a string of the contents of this object. The values
	 * are always escaped.
	 * 
	 * @return String
	 * @see edu.isi.ikcap.wings.util.logging.Escape
	 */
	public String toString() {
		String output = mKey + "\" \"" + mValue + "\"";
		// System.out.println(output);
		return output;
	}

	/**
	 * Returns a copy of the object.
	 * 
	 * @return copy of the object.
	 */
	public Object clone() {
		EnvironmentVariable env = null;
		try {
			env = new EnvironmentVariable(mKey, mValue);

		} catch (Exception e) {
		}
		return env;
	}

}
