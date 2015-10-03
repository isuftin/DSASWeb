package gov.usgs.cida.coastalhazards.dao.shoreline;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

/**
 *
 * @author isuftin
 */
public class Shoreline implements Serializable {

	private static long serialVersionUID = 5129615799852575469L;
	private BigInteger id;
	private double uncertainty;
	private BigInteger segmentId;
	private String date;
	private boolean mhw;
	private String workspace;
	private String source;
	private String auxName;
	private String auxValue;

	public Shoreline() {
		this.id = BigInteger.ZERO;
		this.uncertainty = 0.0;
		this.segmentId = BigInteger.ZERO;
		this.date = "";
		this.mhw = false;
		this.workspace = "";
		this.source = "";
		this.auxName = "";
		this.auxValue = "";
	}
	
	public Shoreline(BigInteger id, double uncertainty, BigInteger segmentId, String date, boolean mhw, String workspace, String source, char type, String auxName, String auxValue) {
		this.id = id;
		this.uncertainty = uncertainty;
		this.segmentId = segmentId;
		this.date = date;
		this.mhw = mhw;
		this.workspace = workspace;
		this.source = source;
		this.auxName = auxName;
		this.auxValue = auxValue;
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
	 * @return the uncertainty
	 */
	public double getUncertainty() {
		return uncertainty;
	}

	/**
	 * @param uncertainty the uncertainty to set
	 */
	public void setUncertainty(double uncertainty) {
		this.uncertainty = uncertainty;
	}

	/**
	 * @return the segmentId
	 */
	public BigInteger getSegmentId() {
		return segmentId;
	}

	/**
	 * @param segmentId the segmentId to set
	 */
	public void setSegmentId(BigInteger segmentId) {
		this.segmentId = segmentId;
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

}
