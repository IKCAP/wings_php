package edu.isi.ikcap.wings.util;

/**
 * A data class that stores the storage in GB/MB/KB.
 * 
 * @author Karan Vahi
 * @version $Revision: 1.1 $
 */

public class Storage implements Cloneable {

	/**
	 * The enumerated UNITS.
	 */
	public static enum UNIT {
		KB, MB, GB, TB
	};

	/**
	 * The size.
	 */
	private float mSize;

	/**
	 * The units in which the size is stored. MB for megabyte, KB for Kilobytes,
	 * TB for terabytes. The default unit is K
	 */
	private UNIT mUnit;

	/**
	 * index to do conversions between units.
	 */
	private int mIndex;

	/**
	 * The overloaded constructor.
	 * 
	 * @param size
	 *            the size.
	 */
	public Storage(float size) {
		setSize(size, UNIT.KB);
	}

	/**
	 * The overloaded constructor.
	 * 
	 * @param size
	 *            the size.
	 * @param unit
	 *            the unit of the size
	 */
	public Storage(float size, UNIT unit) {
		setSize(size, unit);
	}

	/**
	 * Sets the size.
	 * 
	 * @param size
	 *            the size.
	 * @param unit
	 *            the unit of the size
	 */
	public void setSize(float size, String unit) {
		setSize(size, UNIT.valueOf(unit));
	}

	/**
	 * Sets the size.
	 * 
	 * @param size
	 *            the size.
	 * @param unit
	 *            the unit of the size
	 */
	public void setSize(float size, UNIT unit) {
		mSize = size;
		mUnit = unit;
		mIndex = getIndex(mUnit);
	}

	/**
	 * Sets the size.
	 * 
	 * @param size
	 *            the size optionally with the units.
	 */
	// public void setSize( String size ) {
	// size = size.trim();
	// char c = size.charAt( size.length() - 1);
	//
	// if ( Character.isLetter( c ) ){
	// if ( validUnit(c)){
	// mUnit = c;
	// mIndex = this.getIndex( c );
	// mSize = Float.parseFloat( size.substring( 0, size.length() - 1));
	// }
	// else {
	// throw new RuntimeException( "Invald unit " + c );
	// }
	// }
	// else{
	// mSize = Float.parseFloat(size);
	// mUnit = 'K';
	// }
	// }

	/**
	 * Returns the size in the units associated with it.
	 * 
	 * @return size in float
	 */
	public float getSize() {
		return mSize;
	}

	/**
	 * Returns the size in particular unit.
	 * 
	 * @param u
	 *            the unit.
	 * 
	 * @return size in float
	 */
	public float getSize(UNIT u) {
		int index = getIndex(u);
		// System.out.println( "difference is " + (mIndex - index) );
		// System.out.println( "multiplying factor is " +
		// this.getMultiplyingFactor( 1024, mIndex - index));
		// return mSize * (float) Math.pow( 1024, mIndex - index );
		return mSize * this.getMultiplyingFactor(1024, mIndex - index);
	}

	/**
	 * Sets the units associated with the size.
	 * 
	 * @param unit
	 *            the unit
	 */
	public void setUnits(String unit) {
		setUnits(UNIT.valueOf(unit));
	}

	/**
	 * Sets the units associated with the size.
	 * 
	 * @param unit
	 *            the unit
	 */
	public void setUnits(UNIT unit) {
		mUnit = unit;
	}

	/**
	 * Returns the units associated with the size.
	 * 
	 * @return the unit
	 */
	public UNIT getUnits() {
		return mUnit;
	}

	/**
	 * Returns if a character is a valid unit or not
	 * 
	 * @param unit
	 *            the unit
	 * 
	 * @return boolean
	 */
	public boolean validUnit(String unit) {
		boolean result = true;

		try {
			UNIT.valueOf(unit);
		} catch (Exception e) {
			result = false;
		}
		return result;
	}

	/**
	 * Returns a textual description of the content
	 * 
	 * @return String.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(mSize).append(mUnit);
		return sb.toString();
	}

	/**
	 * Returns the clone of the object.
	 * 
	 * @return the clone
	 */
	public Object clone() {
		Storage s;
		try {
			s = (Storage) super.clone();
		} catch (CloneNotSupportedException e) {
			// somewhere in the hierarch chain clone is not implemented
			throw new RuntimeException("Clone not implemented in the base class of " + this.getClass().getName(), e);
		}
		s.mIndex = this.mIndex;
		s.mSize = this.mSize;
		s.mUnit = this.mUnit;

		return s;
	}

	/**
	 * Returns the index for the associated unit.
	 * 
	 * @return the index.
	 */
	@SuppressWarnings("unused")
	private int getIndex() {
		return mIndex;
	}

	/**
	 * Returns the index for a unit.
	 * 
	 * @param u
	 *            the unit
	 * @return the index.
	 */
	private int getIndex(UNIT u) {
		return u.ordinal() + 1;
		// int index = -1;
		// switch ( u ){
		// case 'K':
		// index = 1;
		// break;
		//
		// case 'M':
		// index = 2;
		// break;
		//
		// case 'G':
		// index = 3;
		// break;
		//
		// default:
		// throw new RuntimeException( "Invalid unit scheme" + u );
		// }
		// return index;
	}

	/**
	 * Returns multiplying factor for conversion. Simulates ^ operator.
	 * 
	 * @param base
	 *            the base
	 * @param power
	 *            the power to raise the base to.
	 * 
	 * @return multiplying value
	 */
	private float getMultiplyingFactor(int base, int power) {
		float result = 1;

		if (power >= 0) {
			for (int i = 0; i < power; i++) {
				result *= base;
			}
		} else {
			for (int i = 0; i < -power; i++) {
				result /= base;
			}
		}
		return result;
	}

	/**
	 * Test program
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		Storage s = new Storage(1024);

		s.setSize(121, "MB");

		System.out.println("Size in MB is " + s.getSize(UNIT.MB));
		System.out.println("Size in GB is " + s.getSize(UNIT.GB));

	}
}
