package gov.usgs.cida.dsas.wps;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 *
 * @author tkunicki
 */

@DescribeProcess(
    title = "NearestPointOnLine",
    description = "Finds nearest point on line(s) calculated in CRS of line(s).",
    version = "1.0.0")
public class NearestPointOnLineProcess implements GeoServerProcess {
    
    public NearestPointOnLineProcess() {

    }
    
    @DescribeResult(name = "point", description = "point (EWKT format)")
    public String execute(
            @DescribeParameter(name="lines", description="lines", min = 1, max = 1)
                    SimpleFeatureCollection features,
            @DescribeParameter(name="point", description="point (EWKT format)", min = 1, max = 1)
                    String pointEWKT
            ) throws Exception
    {
        return new Process(features, pointEWKT).execute();
    }
    
    public class Process {
        
        private final SimpleFeatureCollection featureCollection;
        private final String pointEWKT;
        
        Process(SimpleFeatureCollection featureCollection, String pointEWKT) {
            this.featureCollection = featureCollection;
            this.pointEWKT = pointEWKT;
        }
        
        public String execute() throws Exception {
            
            DirectPosition inputPointP = convertFromPointEWKT(pointEWKT);
            
            CoordinateReferenceSystem crsP = inputPointP.getCoordinateReferenceSystem();
            CoordinateReferenceSystem crsL = featureCollection.getSchema().getCoordinateReferenceSystem();
            
            MathTransform transformPtoL = CRS.findMathTransform(crsP, crsL, true);
            
            DirectPosition inputPointL = transformPtoL.transform(inputPointP, null);
            Coordinate intpuCoordinateL = new Coordinate(inputPointL.getOrdinate(0), inputPointL.getOrdinate(1));
            
            LineSegment closestSegment = null;
            double closestDistance = Double.MAX_VALUE;
            
            SimpleFeatureIterator featureIterator = null;
            try {
                featureIterator = featureCollection.features();
                while (featureIterator.hasNext()) {
                    SimpleFeature feature = featureIterator.next();
                    Object geometryAsObject = feature.getDefaultGeometry();
                    if (geometryAsObject instanceof LineString || geometryAsObject instanceof MultiLineString) {
                        Geometry geometry = (Geometry)geometryAsObject;
                        int gCount = geometry.getNumGeometries();
                        for (int gIndex = 0; gIndex < gCount; ++gIndex) {
                            LineString string = (LineString)geometry.getGeometryN(gIndex);
                            for (LineSegment currentSegment : CreateTransectsAndIntersectionsProcess.toLineSegments(string)) {
                                double currentDistance = currentSegment.distance(intpuCoordinateL);
                                if (currentDistance < closestDistance) {
                                    closestDistance = currentDistance;
                                    closestSegment = currentSegment;
                                }
                            }
                        }
                    }
                }
                if (closestSegment != null) {
                    Coordinate outputCoordinateL = closestSegment.closestPoint(intpuCoordinateL);
                    DirectPosition outputPointL = new DirectPosition2D(crsL, outputCoordinateL.x, outputCoordinateL.y);
                    DirectPosition outputPointP = transformPtoL.inverse().transform(outputPointL, null);
                    return new StringBuilder().
                            append("SRID=").
                            append(CRS.lookupEpsgCode(crsP, false)).
                            append(';').
                            append("POINT(").
                            append(outputPointP.getOrdinate(0)).
                            append(' ').
                            append(outputPointP.getOrdinate(1)).
                            append(")").toString();
                }
            } finally {
                if (featureIterator != null) {
                    featureIterator.close();
                }
            }
            // fallback
            return pointEWKT;
        }
    }

    private static Pattern PATTERN_POINT = Pattern.compile("POINT\\(([^\\)]+)\\)");
    static DirectPosition convertFromPointEWKT(String ewkt) {
        String[] split = ewkt.trim().toUpperCase().split(";");
        if (split.length != 2) {
            throw new RuntimeException("Invalid EWKT String \"" + ewkt + "\"");
        }
        CoordinateReferenceSystem crs = convertFromEWKT_SRID(split[0]);
        Matcher matcher = PATTERN_POINT.matcher(split[1]);
        if (!matcher.matches()) {
            throw new RuntimeException("Invalid POINT definition in EWKT String \"" + ewkt + "\"");
        }
        String[] splitCoordinate = matcher.group(1).split("\\s+");
        if (split.length != 2) {
            throw new RuntimeException("Invalid POINT definition in EWKT String \"" + ewkt + "\"");
        }
        double x;
        double y;
        try {
            x = Double.parseDouble(splitCoordinate[0]);
            y = Double.parseDouble(splitCoordinate[1]);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid POINT definition in EWKT String \"" + ewkt + "\"", e);
        }
        return new DirectPosition2D(crs, x, y);
    }
    
    static CoordinateReferenceSystem convertFromEWKT_SRID(String sridPart) {
        String[] split = sridPart.trim().split("=");
        if (split.length != 2 && split[0].equals("SRID")) {
            throw new RuntimeException("Invalid SRID component \"" + sridPart + "\"");
        }
        int srid;
        try {
            srid = Integer.parseInt(split[1].trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid SRID indentifier \"" + sridPart + "\"", e);
        }
        try {
            return CRS.decode("EPSG:" + srid);
        } catch (NoSuchAuthorityCodeException e) {
            throw new RuntimeException("Unknown SRID \"" + srid + "\"", e);
        } catch (FactoryException e) {
            throw new RuntimeException("Error converting SRID \"" + srid + "\"", e);
        }
    }
}
