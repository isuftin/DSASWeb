package gov.usgs.cida.dsas.dao.pdb;

import gov.usgs.cida.dsas.utilities.features.Constants;
import java.io.Serializable;
import java.math.BigInteger;

/**
 *
 * @author smlarson
 */
public class Pdb implements Serializable {

    private static final long serialVersionUID = -798301212870571106L;
    
	public final static String[] REQUIRED_FIELD_NAMES = new String[]{Constants.DB_DATE_ATTR, Constants.UNCY_ATTR, Constants.BIAS_UNCY_ATTR, Constants.BIAS_ATTR, Constants.PROFILE_ID};
    private int profileId;
    private BigInteger segmentId;
   // private String xyGeom; // or coord xy doubles each
    private double x;
    private double y;
    private double bias;
    private double uncyb;
    private String last_update;
    
    public Pdb (){
        
        this.profileId = 0;
        this.segmentId = BigInteger.ZERO;
        this.x = 0;
        this.y = 0;
        this.bias = 0;
        this.uncyb = 0;
        this.last_update= "";
    }
            
    
    public Pdb(int profileId, BigInteger segmentId, double x, double y, double bias, double uncyb, String date) {
        this.profileId = profileId;
        this.segmentId = segmentId;
        this.x = x;
        this.y = y;
        this.bias = bias;
        this.uncyb = uncyb;
        this.last_update= date;
		
    }

    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public BigInteger getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(BigInteger segmentId) {
        this.segmentId = segmentId;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getBias() {
        return bias;
    }

    public void setBias(double bias) {
        this.bias = bias;
    }

    public double getUncyb() {
        return uncyb;
    }

    public void setUncyb(double uncyb) {
        this.uncyb = uncyb;
    }

    public String getLast_update() {
        return last_update;
    }

    public void setLast_update(String last_update) {
        this.last_update = last_update;
    }
    
}

