package gov.usgs.cida.dsas.util;

import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import gov.usgs.cida.dsas.exceptions.PoorlyDefinedBaselineException;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class GeomAsserts {

	/**
	 * Throws an exception if the baseline crosses any of the shorelines.
	 * 
	 * Because the transects are cast in only one direction from a given baseline
	 * segment, there cannot be any crosses between the two
	 * TODO: remove requirement for prepared geometry and use the STRtree so we
	 * don't have to create two shoreline indexes
	 * @param shorelines
	 * @param baselines 
	 */
    public static void assertBaselinesDoNotCrossShorelines(PreparedGeometry shorelines, MultiLineString baselines) {
        if (shorelines.intersects(baselines)) {
            throw new PoorlyDefinedBaselineException("Baselines cannot intersect shorelines");
        }
    }

}
