package gov.usgs.cida.dsas.util;

import gov.usgs.cida.dsas.util.BaselineDistanceAccumulator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.PrecisionModel;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class BaselineDistanceAccumulatorTest {

    private final static double EPS = 1e-15;
        
    private GeometryFactory gf;
    
    @Before
    public void setup() {
        gf = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
    }
    
    @Test
    public void Test_getStartLineSegment() {
        LineString lineString = gf.createLineString(new Coordinate[] {
            new Coordinate(0, 0),
            new Coordinate(1, 0),
            new Coordinate(2, 0),
            new Coordinate(3, 0),
            new Coordinate(4, 0),
            new Coordinate(5, 0),
            new Coordinate(6, 0),
            new Coordinate(7, 0),
            new Coordinate(8, 0),
            new Coordinate(9, 0),
        });
        LineSegment start = BaselineDistanceAccumulator.getStartLineSegment(lineString);
        assertNotNull(start);
        assertEquals(lineString.getCoordinateN(0), start.getCoordinate(0));
        assertEquals(lineString.getCoordinateN(1), start.getCoordinate(1));
    }
    
    @Test
    public void Test_getEndLineSegment() {
        LineString lineString = gf.createLineString(new Coordinate[] {
            new Coordinate(0, 0),
            new Coordinate(1, 0),
            new Coordinate(2, 0),
            new Coordinate(3, 0),
            new Coordinate(4, 0),
            new Coordinate(5, 0),
            new Coordinate(6, 0),
            new Coordinate(7, 0),
            new Coordinate(8, 0),
            new Coordinate(9, 0),
        });
        LineSegment end = BaselineDistanceAccumulator.getEndLineSegment(lineString);
        assertNotNull(end);
        assertEquals(lineString.getCoordinateN(8), end.getCoordinate(0));
        assertEquals(lineString.getCoordinateN(9), end.getCoordinate(1));
    }
    
    @Test
    public void Test_getMinimumProjectedDistance() {
        LineSegment start;
        LineSegment end;
        double distance;
        
        end = new LineSegment(0, 0, 1, 0);
        start = new LineSegment(1, 0, 2, 0);
        distance = BaselineDistanceAccumulator.getMinimumProjectedDistance(end, start);
        assertEquals("abutting segments along same line", 0, distance, EPS);
        
        end = new LineSegment(0, 0, 1, 0);
        start = new LineSegment(3, 0, 4, 0);
        distance = BaselineDistanceAccumulator.getMinimumProjectedDistance(end, start);
        assertEquals("segments along same line (discontinuous)", 2, distance, EPS);
        
        end = new LineSegment(0, 1, 1, 1);
        start = new LineSegment(1, 1, 2, 1);
        distance = BaselineDistanceAccumulator.getMinimumProjectedDistance(end, start);
        assertEquals("segments parallel to same line (abutting along projection)", 0, distance, EPS);
        
        end = new LineSegment(0, 1, 1, 1);
        start = new LineSegment(3, 1, 4, 1);
        distance = BaselineDistanceAccumulator.getMinimumProjectedDistance(end, start);
        assertEquals("segments parallel to same line (discontinuous along projection)", 2, distance, EPS);
        
        end = new LineSegment(0, 0, 1, 0);
        start = new LineSegment(1, 0, 1, 1);
        distance = BaselineDistanceAccumulator.getMinimumProjectedDistance(end, start);
        assertEquals("segments perpendicular to each other (abutting at end)", 0, distance, EPS);
        
        end = new LineSegment(0, 0, 1, 0);
        start = new LineSegment(1, 2, 1, 4);
        distance = BaselineDistanceAccumulator.getMinimumProjectedDistance(end, start);
        assertEquals("segments perpendicular to each other (discontinuos at end)", 0, distance, EPS);
        
        end = new LineSegment(0, 0, 1, 0);
        start = new LineSegment(2, 2, 2, 3);
        distance = BaselineDistanceAccumulator.getMinimumProjectedDistance(end, start);
        assertEquals("segments perpendicular to each other (offset 1 in x, 2 in y)", 1, distance, EPS);
        
        end = new LineSegment(-1, 0, 0, 0);
        start = new LineSegment(2, 2, 6, 4);
        distance = BaselineDistanceAccumulator.getMinimumProjectedDistance(end, start);
        assertEquals("segments 45 to each other", 2, distance, EPS);
    }
}
