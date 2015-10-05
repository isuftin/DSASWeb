/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gov.usgs.cida.dsas.util;

import com.vividsolutions.jts.geom.Coordinate;
import java.util.ArrayList;
import java.util.List;
import org.geotools.data.crs.ReprojectFeatureReader;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class UTMFinder {

    /* http://reference.mapinfo.com/common/docs/mapxtend-dev-web-none-eng/miaware/doc/guide/xmlapi/coordsys/systems.htm */
    private static final int UTM_EPSG_BASE = 32600;
    private static final int UTM_ESPG_OFFSET_S = 100;
    
    static final DefaultGeographicCRS UTM_GCRS = WGS84;
    static final int UTM_GCRS_LON = 0;
    static final int UTM_GCRS_LAT = 1;
    
    /**
     * Calculation is based on http://en.wikipedia.org/wiki/UTM_coordinate_system
     * 
     * @param featureCollection
     * @return UTM CRS
     */
    public static CoordinateReferenceSystem findUTMZoneCRSForCentroid(FeatureCollection featureCollection) throws NoSuchAuthorityCodeException, FactoryException, TransformException {
        return findUTMZoneCRSForCentroid(extractEnvelope(featureCollection));
    }
    
    public static CoordinateReferenceSystem findUTMZoneCRSForCentroid(ReferencedEnvelope envelope) throws NoSuchAuthorityCodeException, FactoryException, TransformException {
        Coordinate coordinate = envelope.transform(UTM_GCRS, true).centre();
        return findUTMZoneCRS(coordinate.x, coordinate.y);
    }
    
    public static List<CoordinateReferenceSystem> findUTMZoneCRS(FeatureCollection featureCollection) throws NoSuchAuthorityCodeException, FactoryException, TransformException {
        return findUTMZoneCRS(extractEnvelope(featureCollection));
    }
    
    public static List<CoordinateReferenceSystem> findUTMZoneCRS(ReferencedEnvelope envelope) throws NoSuchAuthorityCodeException, FactoryException, TransformException {
        List<CoordinateReferenceSystem> crsList = new ArrayList<CoordinateReferenceSystem>();
        envelope = envelope.transform(UTM_GCRS, true);
        double xMin = envelope.getMinX();
        // NOTE!  Don't use evelope max, min + width will handle date line crossing since we are subtracting
        double xMax = xMin + envelope.getWidth(); 
        int zoneMin = findUTMZone(xMin);
        int zoneMax = findUTMZone(xMax) + 1; // +1 since we want exclusive upperbound
        if (envelope.getMaxY() > 0) {
            for (int zoneIndex = zoneMin; zoneIndex < zoneMax; ++zoneIndex) {
                crsList.add(findUTMZoneCRS(zoneIndex, false));
            }
        }
        if (envelope.getMinY() < 0) {
            for (int zoneIndex = zoneMin; zoneIndex < zoneMax; ++zoneIndex) {
                crsList.add(findUTMZoneCRS(zoneIndex, true));
            }
        }
        return crsList;
    }
    
    public static CoordinateReferenceSystem findUTMZoneCRS(DirectPosition position) throws FactoryException, MismatchedDimensionException, TransformException {
        CRS.findMathTransform(position.getCoordinateReferenceSystem(), UTM_GCRS, true).transform(position, position);
        return findUTMZoneCRS(position.getOrdinate(UTM_GCRS_LON), position.getOrdinate(UTM_GCRS_LAT));
    }
    
    public static CoordinateReferenceSystem findUTMZoneCRS(double longitude, double latitude) throws NoSuchAuthorityCodeException, FactoryException {
        return CRS.decode("EPSG:" + findUTMZoneEPSGCode(longitude, latitude));
    }
    
    public static CoordinateReferenceSystem findUTMZoneCRS(int utmZone, boolean southernHemisphere) throws NoSuchAuthorityCodeException, FactoryException {
        return CRS.decode("EPSG:" + findUTMZoneEPSGCode(utmZone, southernHemisphere));
    }
    
    public static int findUTMZoneCRSCount(FeatureCollection featureCollection) throws NoSuchAuthorityCodeException, FactoryException, TransformException {
        return findUTMZoneCRSCount(extractEnvelope(featureCollection));
    }
    
    public static int findUTMZoneCRSCount(ReferencedEnvelope envelope) throws NoSuchAuthorityCodeException, FactoryException, TransformException {     
        envelope = envelope.transform(UTM_GCRS, true);
        double xMin = envelope.getMinX();
        // NOTE!  Don't use evelope max, min + width will handle date line crossing since we are subtracting
        double xMax = xMin + envelope.getWidth(); 
        int zoneCount = findUTMZone(xMax) - findUTMZone(xMin) + 1;
        if ((envelope.getMinY() > 0) != (envelope.getMaxY() > 0)) {
            zoneCount *= 2;
        }
        return zoneCount;
    }
    
    // private because we can't check/enforce WGS84 on longitude value.
    private static int findUTMZone(double longitude) {
        return (int)Math.ceil((180 + longitude) / 6);
    }
    
    // private because we can't check/enforce WGS84 on longitude value.
    private static int findUTMZoneEPSGCode(double longitude, double latitude) {
        return findUTMZoneEPSGCode(findUTMZone(longitude), latitude < 0);
    }
    
    private static int findUTMZoneEPSGCode(int utmZone, boolean southernHemisphere) {
        int epsgCode = UTM_EPSG_BASE + utmZone;
        if (southernHemisphere) {
            epsgCode += UTM_ESPG_OFFSET_S;
        }
        return  epsgCode;
    }
        
    private static ReferencedEnvelope extractEnvelope(FeatureCollection featureCollection) {
        ReferencedEnvelope envelope = featureCollection.getBounds();
        if (envelope.getCoordinateReferenceSystem() == null) {
            // A referenced envelope without and reference ?!   This appears to
            // be a bug in org.geotools.data.crs.ReprojectFeatureResults
            envelope = new ReferencedEnvelope(envelope, featureCollection.getSchema().getCoordinateReferenceSystem());
        }
        return envelope;
    }
    
}
