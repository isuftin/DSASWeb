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
package gov.usgs.cida.dsas.wps.geom;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import gov.usgs.cida.dsas.exceptions.AttributeNotANumberException;
import gov.usgs.cida.dsas.util.CRSUtils;
import gov.usgs.cida.dsas.utilities.features.AttributeGetter;
import static gov.usgs.cida.dsas.utilities.features.Constants.AVG_SLOPE_ATTR;
import static gov.usgs.cida.dsas.utilities.features.Constants.BASELINE_DIST_ATTR;
import static gov.usgs.cida.dsas.utilities.features.Constants.BASELINE_ID_ATTR;
import static gov.usgs.cida.dsas.utilities.features.Constants.BASELINE_ORIENTATION_ATTR;
import static gov.usgs.cida.dsas.utilities.features.Constants.BIAS_ATTR;
import static gov.usgs.cida.dsas.utilities.features.Constants.BIAS_UNCY_ATTR;
import gov.usgs.cida.dsas.utilities.features.Constants.Orientation;
import static gov.usgs.cida.dsas.utilities.features.Constants.TRANSECT_ID_ATTR;
import static java.lang.Double.NaN;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Transect.
 * 
 * Creating this class because I'm having a hard time wrapping my head around
 * polar vs. cartesian arithmetic. Holding a cartesian coord with a polar angle
 * seemed to be my best way around it.
 * 
 * Currently seaward orientation is default
 * 
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class Transect {

    private Coordinate cartesianCoord;
    private double angle; // radians
    private double length; // meters
    private Orientation orientation; // is transect built off of seaward or shoreward baseline
    private int transectId;
    private String baselineId;
    private double baselineDistance;
    private ProxyDatumBias bias;
    private static final GeometryFactory gf;

    static {
        gf = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
    }
    
    /**
     * Generate a Zero Vector with given attributes
     * 
     * Length is set afterwards by calculating intersections
     * @param coord xy coordinates of vector
     * @param angle angle of the vector
     * @param orientation orientation to shoreline
     * @param transectId id for this transect
     * @param baselineId id of baseline this projects from
     * @param baselineDistance distance along baseline of this vector
     * @param bias proxy datum bias to retain with this transect
     */
    Transect(Coordinate coord, double angle, Orientation orientation, int transectId, String baselineId, double baselineDistance, ProxyDatumBias bias) {
        this.cartesianCoord = coord;
        this.angle = angle;
        this.length = 0.0;
        this.orientation = orientation;
        this.transectId = transectId;
        this.baselineId = baselineId;
        this.baselineDistance = baselineDistance;
        this.bias = bias;
    }
    
    Transect(double x, double y, double angle, Orientation orientation, int transectId, String baselineId, double baselineDistance, ProxyDatumBias bias) {
        this(new Coordinate(x, y), angle, orientation, transectId, baselineId, baselineDistance, bias);
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }
    
    public void setBaselineDistance(double dist) {
        this.baselineDistance = dist;
    }
    
    public LineString getLineString() {
        double rise = length * Math.sin(angle);
        double run = length * Math.cos(angle);
        Coordinate endpoint = new Coordinate(cartesianCoord.x + run, cartesianCoord.y + rise);
        LineString newLineString = gf.createLineString(new Coordinate[]{cartesianCoord, endpoint});
        return newLineString;
    }

    public Coordinate getOriginCoord() {
        return cartesianCoord;
    }
    
    public Point getOriginPoint() {
        return gf.createPoint(cartesianCoord);
    }
    
    /**
     * 
     * @return Angle in radians
     */
    public double getAngle() {
        return angle;
    }
    
    public Orientation getOrientation() {
        return orientation;
    }
    
    public int getId() {
        return transectId;
    }

    public double getBaselineDistance() {
        return baselineDistance;
    }

    public ProxyDatumBias getBias() {
        return bias;
    }

    public void setBias(ProxyDatumBias bias) {
        this.bias = bias;
    }

    public void rotate180Deg() {
        angle += Math.PI;
    }
    
    public Transect subTransect(double startDistance, double length) {
        Transect subTransect = new Transect(cartesianCoord, angle, orientation, transectId, baselineId, baselineDistance, bias);
        subTransect.setLength(startDistance);
        subTransect.cartesianCoord = subTransect.getLineString().getEndPoint().getCoordinate();
        subTransect.setLength(length);
        return subTransect;
    }
    
    public static Transect generatePerpendicularVector(Coordinate origin,
            LineSegment segment,
            Orientation orientation,
            int transectId,
            String baselineId,
            double baselineDistance,
            int direction) {
        // Don't worry about the bias for the vector transect, add it when trimming
        ProxyDatumBias bias = null;
        double angle;
        switch (direction) {
            case Angle.CLOCKWISE:
                angle = segment.angle() - Angle.PI_OVER_2;
                break;
            case Angle.COUNTERCLOCKWISE:
                angle = segment.angle() + Angle.PI_OVER_2;
                break;
            default:
                throw new IllegalStateException("Must be either clockwise or counterclockwise");
        }
        return new Transect(origin, angle, orientation, transectId, baselineId, baselineDistance, bias);
    }
    
	public static Transect fromFeature(SimpleFeature feature) {
		MultiLineString lines = CRSUtils.getMultilineFromFeature(feature);
		LineString line = (LineString)lines.getGeometryN(0);  // ignore more than one for now (shouldn't happen)
		LineSegment segment = new LineSegment(line.getStartPoint().getCoordinate(),
				line.getEndPoint().getCoordinate());
		AttributeGetter getter = new AttributeGetter(feature.getFeatureType());
		String orientVal = (String)getter.getValue(BASELINE_ORIENTATION_ATTR, feature);
		Orientation orient = Orientation.fromAttr(orientVal);
		int id = (Integer)getter.getValue(TRANSECT_ID_ATTR, feature);
		String baselineId = (String)getter.getValue(BASELINE_ID_ATTR, feature);
		Double baseDist;
		try {
			baseDist = getter.getDoubleValue(BASELINE_DIST_ATTR, feature);
		} catch (AttributeNotANumberException e) {
			baseDist = NaN;
		}
		ProxyDatumBias bias = ProxyDatumBias.fromFeature(feature);

		Transect transect = new Transect(segment.p0, segment.angle(), orient, id, baselineId, baseDist, bias);
		transect.length = segment.p0.distance(segment.p1);

		return transect;
	}
    
    public boolean equals(Transect b) {
        if (this.cartesianCoord.equals2D(b.getOriginCoord()) 
                && this.angle == b.angle
                && this.length == b.length) {
            return true;
        }
        else {
            return false;
        }
    }
    
	public static SimpleFeatureType buildFeatureType(CoordinateReferenceSystem crs) {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("Transects");
		builder.add("geom", LineString.class, crs);
		builder.add(TRANSECT_ID_ATTR, Integer.class);
		builder.add(BASELINE_ORIENTATION_ATTR, String.class);
		builder.add(BASELINE_ID_ATTR, String.class);
		builder.add(BASELINE_DIST_ATTR, Double.class);
		builder.add(AVG_SLOPE_ATTR, Double.class);
		builder.add(BIAS_ATTR, Double.class);
		builder.add(BIAS_UNCY_ATTR, Double.class);
		return builder.buildFeatureType();
	}

    public SimpleFeature createFeature(SimpleFeatureType type) {
        LineString line = this.getLineString();
        Double avgSlope = null;
        Double biasVal = null;
        Double biasUncy = null;
        if (bias != null) {
            avgSlope = bias.getAvgSlope();
            biasVal = bias.getBias();
            biasUncy = bias.getUncyb();
        }
        SimpleFeature feature = SimpleFeatureBuilder.build(type,
                new Object[]{line,transectId, orientation.getValue(), baselineId, baselineDistance, avgSlope, biasVal, biasUncy}, null);
        return feature;
    }
}
