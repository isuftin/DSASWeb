package gov.usgs.cida.dsas.util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import gov.usgs.cida.dsas.exceptions.PoorlyDefinedBaselineException;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class BaselineDistanceAccumulator {
    
    public static final double EPS = 1.0d;
    
    private double accumulatedBaselineLength;
    private LineSegment previousBaselineEnd;
        
    public BaselineDistanceAccumulator() {
        this.accumulatedBaselineLength = 0;
        this.previousBaselineEnd = null;
    }
    
    public double accumulate(LineString line) {
        if (previousBaselineEnd != null) {
            accumulatedBaselineLength += getMinimumProjectedDistance(previousBaselineEnd, getStartLineSegment(line));
        }
        double baseDist = accumulatedBaselineLength;
        
        accumulatedBaselineLength += line.getLength();
        previousBaselineEnd = getEndLineSegment(line);
        
        return baseDist;
    }
    
    public double accumulateToPoint(LineString line, Point point) {
        for (int j=0; j<line.getNumPoints()-1; j++) {
            LineSegment segment = new LineSegment(line.getCoordinateN(j), line.getCoordinateN(j+1));
            Coordinate coord = point.getCoordinate();
            if (segment.distance(coord) < EPS) {
                accumulatedBaselineLength += (segment.segmentFraction(coord) * segment.getLength());
                return accumulatedBaselineLength; //done accumulating
            }
            else {
                accumulatedBaselineLength += segment.getLength();
            }
        }
        throw new PoorlyDefinedBaselineException("Baseline section does not contain transect origin");
    }
    
    public static LineSegment getStartLineSegment(LineString line) {
        return new LineSegment(
                line.getCoordinateN(0), line.getCoordinateN(1));
    }
    
    public static LineSegment getEndLineSegment(LineString line) {
        int lastIndex = line.getNumPoints() - 1;
        return new LineSegment(
                line.getCoordinateN(lastIndex - 1), line.getCoordinateN(lastIndex));
    }

    public static double getMinimumProjectedDistance(LineSegment previousEnd, LineSegment nextStart) {
        double startProjEnd = previousEnd.p1.distance(previousEnd.project(nextStart.p0));
        double endProjStart = nextStart.p0.distance(nextStart.project(previousEnd.p1));
        return startProjEnd < endProjStart ? startProjEnd : endProjStart;
    }
}
