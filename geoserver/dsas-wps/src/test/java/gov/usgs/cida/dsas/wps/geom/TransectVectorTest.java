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
import com.vividsolutions.jts.geom.Point;
import gov.usgs.cida.dsas.utilities.features.Constants.Orientation;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class TransectVectorTest {
    
    private static GeometryFactory factory = new GeometryFactory();
    
    @Test
    public void testAngles() {
        Coordinate a = new Coordinate(0.0, 0.0);
        Transect east = new Transect(a, 0.0, Orientation.UNKNOWN, 0, "0", 0, null);
        east.setLength(1.0);
        Transect north = new Transect(a, Angle.PI_OVER_2, Orientation.UNKNOWN, 0, "0", 0, null);
        north.setLength(1.0);
        Transect west = new Transect(a, Math.PI, Orientation.UNKNOWN, 0, "0", 0, null);
        west.setLength(1.0);
        Transect south = new Transect(a, 3 * Angle.PI_OVER_2, Orientation.UNKNOWN, 0, "0", 0, null);
        south.setLength(1.0);
        assertEquals(east.getLineString().getEndPoint().getX(), 1.0, 0.00001);
        assertEquals(north.getLineString().getEndPoint().getY(), 1.0, 0.00001);
        assertEquals(west.getLineString().getEndPoint().getX(), -1.0, 0.00001);
        assertEquals(south.getLineString().getEndPoint().getY(), -1.0, 0.00001);
    }   
    
    /**
     * Test of getLineOfLength method, of class VectorCoordAngle.
     */
    @Test
    public void testGetLineOfLength() {
        Coordinate a = new Coordinate(0.0, 0.0);
        Coordinate b = new Coordinate(0.0, 1.0);
        Transect instance = new Transect(a, Angle.PI_OVER_2, Orientation.UNKNOWN, 0, "0", 0, null);
        instance.setLength(1.0);
        LineString expResult = factory.createLineString(new Coordinate[] {a, b});
        LineString result = instance.getLineString();
        assertEquals(expResult.getCoordinateN(0).x, result.getCoordinateN(0).x, 0.00001);
        assertEquals(expResult.getCoordinateN(0).y, result.getCoordinateN(0).y, 0.00001);
        assertEquals(expResult.getCoordinateN(1).x, result.getCoordinateN(1).x, 0.00001);
        assertEquals(expResult.getCoordinateN(1).y, result.getCoordinateN(1).y, 0.00001);
    }

    /**
     * Test of getOriginPoint method, of class VectorCoordAngle.
     */
    @Test
    public void testGetOriginPoint() {
        Coordinate a = new Coordinate(0.0, 0.0);
        Transect instance = new Transect(a, Angle.PI_OVER_2, Orientation.UNKNOWN, 0, "0", 0, null);
        Point result = instance.getOriginPoint();
        Point expResult = factory.createPoint(a);
        assertEquals(expResult.getX(), result.getX(), 0.00001);
        assertEquals(expResult.getY(), result.getY(), 0.00001);
    }

    /**
     * Test of rotate180Deg method, of class VectorCoordAngle.
     */
    @Test
    public void testRotate180Deg() {
        Coordinate a = new Coordinate(0.0, 0.0);
        Coordinate b = new Coordinate(0.0, -1.0);
        Transect instance = new Transect(a, Angle.PI_OVER_2, Orientation.UNKNOWN, 0, "0", 0, null);
        instance.setLength(1.0);
        instance.rotate180Deg();
        LineString expResult = factory.createLineString(new Coordinate[] {a, b});
        LineString result = instance.getLineString();
        assertEquals(expResult.getCoordinateN(0).x, result.getCoordinateN(0).x, 0.00001);
        assertEquals(expResult.getCoordinateN(0).y, result.getCoordinateN(0).y, 0.00001);
        assertEquals(expResult.getCoordinateN(1).x, result.getCoordinateN(1).x, 0.00001);
        assertEquals(expResult.getCoordinateN(1).y, result.getCoordinateN(1).y, 0.00001);
    }

    /**
     * Test of generatePerpendicularVector method, of class VectorCoordAngle.
     */
    @Test
    public void testGeneratePerpendicularVector() {
        Coordinate origin = new Coordinate(0.0, 0.0);
        Coordinate b = new Coordinate(1.0, 0.0);
        LineSegment segment = new LineSegment(origin, b);
        Transect expResult = new Transect(origin, Angle.PI_OVER_2, Orientation.UNKNOWN, 0, "0", 99, null);
        Transect result = Transect.generatePerpendicularVector(origin, segment, Orientation.UNKNOWN, 0, "0", 99, Angle.COUNTERCLOCKWISE);
        assertEquals(expResult.getOriginCoord().x, result.getOriginCoord().x, 0.00001);
        assertEquals(expResult.getOriginCoord().y, result.getOriginCoord().y, 0.00001);
        assertEquals(expResult.getAngle(), result.getAngle(), 0.00001);
        assertEquals(expResult.getBaselineDistance(), result.getBaselineDistance(), 0.00001);
    }
}
