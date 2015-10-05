/*
 * U.S.Geological Survey Software User Rights Notice
 * 
 * Copied from http://water.usgs.gov/software/help/notice/ on September 7, 2012.  
 * Please check webpage for updates.
 * 
 * Software and related material (data and (or) documentation), contained in or
 * furnished in connection with a software distribution, are made available by the
 * U.S. Geological Survey (USGS) to be used in the public interest and in the 
 * advancement of science. You may, without any fee or cost, use, copy, modify,
 * or distribute this software, and any derivative works thereof, and its supporting
 * documentation, subject to the following restrictions and understandings.
 * 
 * If you distribute copies or modifications of the software and related material,
 * make sure the recipients receive a copy of this notice and receive or can get a
 * copy of the original distribution. If the software and (or) related material
 * are modified and distributed, it must be made clear that the recipients do not
 * have the original and they must be informed of the extent of the modifications.
 * 
 * For example, modified files must include a prominent notice stating the 
 * modifications made, the author of the modifications, and the date the 
 * modifications were made. This restriction is necessary to guard against problems
 * introduced in the software by others, reflecting negatively on the reputation of the USGS.
 * 
 * The software is public property and you therefore have the right to the source code, if desired.
 * 
 * You may charge fees for distribution, warranties, and services provided in connection
 * with the software or derivative works thereof. The name USGS can be used in any
 * advertising or publicity to endorse or promote any products or commercial entity
 * using this software if specific written permission is obtained from the USGS.
 * 
 * The user agrees to appropriately acknowledge the authors and the USGS in publications
 * that result from the use of this software or in products that include this
 * software in whole or in part.
 * 
 * Because the software and related material are free (other than nominal materials
 * and handling fees) and provided "as is," the authors, the USGS, and the 
 * United States Government have made no warranty, express or implied, as to accuracy
 * or completeness and are not obligated to provide the user with any support, consulting,
 * training or assistance of any kind with regard to the use, operation, and performance
 * of this software nor to provide the user with any updates, revisions, new versions or "bug fixes".
 * 
 * The user assumes all risk for any damages whatsoever resulting from loss of use, data,
 * or profits arising in connection with the access, use, quality, or performance of this software.
 */

package gov.usgs.cida.utilities.features;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class Constants {
    
    public static final CoordinateReferenceSystem REQUIRED_CRS_WGS84 = DefaultGeographicCRS.WGS84;
    public static final String DEFAULT_GEOM_ATTR = "the_geom";
    public static final String BASELINE_ORIENTATION_ATTR = "Orient";
    public static final String TRANSECT_ID_ATTR = "TransectID";
    public static final String DISTANCE_ATTR = "Distance";
    public static final String DATE_ATTR = "Date_";
    public static final String DB_DATE_ATTR = "date";
    public static final String UNCY_ATTR = "uncy";
    public static final String BASELINE_DIST_ATTR = "base_dist";
    public static final String BASELINE_ID_ATTR = "BaselineID";
	public static final String SURVEY_ID_ATTR = "survey_id";
	public static final String RECORD_ID_ATTR = "recordId";
	public static final String SOURCE_ATTR = "source";
	public static final String SOURCE_ABBRV_ATTR = "src";
    public static final String LRR_ATTR = "LRR";
    public static final String LCI_ATTR = "LCI";
    public static final String SCE_ATTR = "SCE";
    public static final String NSD_ATTR = "NSD";
    public static final String WLR_ATTR = "WLR";
    public static final String WCI_ATTR = "WCI";
    public static final String ECI_ATTR = "ECI";
    public static final String NSM_ATTR = "NSM";
    public static final String EPR_ATTR = "EPR";
    public static final String MHW_ATTR = "MHW";
    public static final String NAME_ATTR = "name";
    public static final String DEFAULT_D_ATTR = "defaultd";
    public static final String AVG_SLOPE_ATTR = "avg_slope";
    public static final String BIAS_ATTR = "BIAS";
    public static final String BIAS_UNCY_ATTR = "UNCYB";

	// new constants tied to point features
	public static final String SHORELINE_ID_ATTR = "shoreline_id";
	public static final String SEGMENT_ID_ATTR = "segment_id";

	public static final boolean DEFAULT_MHW_VALUE = false;
	public static final double DEFAULT_BIAS = 0.0d;
	public static final double DEFAULT_BIAS_UNCY = 0.0d;
    
    // Per http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf (Page 7), need value less that -10^38
    public static final double  SHAPEFILE_NODATA = -1e39;
    
    public static final double EARTH_RADIUS = 6371000.0d;

    public static enum Orientation {
        SHOREWARD("shoreward", 1),
        SEAWARD("seaward", -1),
        UNKNOWN("unknown", 0);
        
        private final String orientation;
        /* Sign is negative to indicate erosion, positive for accretion */
        private final int sign;
        
        Orientation(String value, int sign) {
            this.orientation = value;
            this.sign = sign;
        }
        
        public String getValue() {
            return orientation;
        }
        
        public int getSign() {
            return sign;
        }
        
        public static Orientation fromAttr(String value) {
            if ("shoreward".equalsIgnoreCase(value)) {
                return SHOREWARD;
            }
            else if ("seaward".equalsIgnoreCase(value)) {
                return SEAWARD;
            }
            else {
                return UNKNOWN;
            }
        }
    }
}
