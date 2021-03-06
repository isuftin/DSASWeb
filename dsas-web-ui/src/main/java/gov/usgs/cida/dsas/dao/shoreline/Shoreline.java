package gov.usgs.cida.dsas.dao.shoreline;

import java.io.Serializable;
import java.math.BigInteger;

/**
 *
 * @author isuftin
 */
public class Shoreline implements Serializable {

	private static final long serialVersionUID = 5129615799852575469L;
	private BigInteger id;
	private String date;
	private boolean mhw;
	private String workspace;
	private String source;
	private String auxName;
	private String auxValue;
	private String name;

	public Shoreline() {
		this.id = BigInteger.ZERO;
		this.date = "";
		this.mhw = false;
		this.workspace = "";
		this.source = "";
		this.auxName = "";
		this.auxValue = "";
		this.name = "";
	}
	
	public Shoreline(BigInteger id, String date, boolean mhw, String workspace, String source, char type, String auxName, String auxValue, String name) {
		this.id = id;
		this.date = date;
		this.mhw = mhw;
		this.workspace = workspace;
		this.source = source;
		this.auxName = auxName;
		this.auxValue = auxValue;
		this.name = name;
	}

	/**
	 * @return the id
	 */
	public BigInteger getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(BigInteger id) {
		this.id = id;
	}

	/**
	 * @return the date
	 */
	public String getDate() {
		return date;
	}

	/**
	 * @param date the date to set
	 */
	public void setDate(String date) {
		this.date = date;
	}

	/**
	 * @return the mhw
	 */
	public boolean isMhw() {
		return mhw;
	}

	/**
	 * @param mhw the mhw to set
	 */
	public void setMhw(boolean mhw) {
		this.mhw = mhw;
	}

	/**
	 * @return the workspace
	 */
	public String getWorkspace() {
		return workspace;
	}

	/**
	 * @param workspace the workspace to set
	 */
	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}

	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * @return the auxName
	 */
	public String getAuxName() {
		return auxName;
	}

	/**
	 * @param auxName the auxName to set
	 */
	public void setAuxName(String auxName) {
		this.auxName = auxName;
	}

	/**
	 * @return the auxValue
	 */
	public String getAuxValue() {
		return auxValue;
	}

	/**
	 * @param auxValue the auxValue to set
	 */
	public void setAuxValue(String auxValue) {
		this.auxValue = auxValue;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
