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
    private BigInteger id;
    private String profileId;
    private int segmentId;
   // private String xyGeom; // or coord xy doubles each
    private double x;
    private double y;
    private String bias;
    private String uncyb;
    private String last_update;
    
    public Pdb (){
        
        this.id = BigInteger.ZERO;
        this.profileId = "";
        this.segmentId = 0;
        this.x = 0;
        this.y = 0;
        this.bias = "";
        this.uncyb = "";
        this.last_update= "";
    }
            
    
    public Pdb(BigInteger id, String profileId, int segmentId, double x, double y, String bias, String uncyb, String date) {
	// check the type for the Geom and the date
        this.id = id;
        this.profileId = profileId;
        this.segmentId = segmentId;
        this.x = x;
        this.y = y;
        this.bias = bias;
        this.uncyb = uncyb;
        this.last_update= date;
		
    }

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public int getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(int segmentId) {
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

    public String getBias() {
        return bias;
    }

    public void setBias(String bias) {
        this.bias = bias;
    }

    public String getUncyb() {
        return uncyb;
    }

    public void setUncyb(String uncyb) {
        this.uncyb = uncyb;
    }

    public String getLast_update() {
        return last_update;
    }

    public void setLast_update(String last_update) {
        this.last_update = last_update;
    }
    
}

