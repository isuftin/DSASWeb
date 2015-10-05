package gov.usgs.cida.dsas.wps;

import gov.usgs.cida.dsas.util.UTMFinder;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 *
 * @author tkunicki
 */

@DescribeProcess(
    title = "UTMZoneCount",
    description = "For a given feature collection return the number of intersecting UTM zones",
    version = "1.0.0")
public class UTMZoneCountProcess implements GeoServerProcess {
    
    public UTMZoneCountProcess() {
    }
    
    @DescribeResult(name = "count", description = "Number of intersecting UTM zones")
    public int execute(
            @DescribeParameter(name="features", description="features to test for UTM zone intersection", min = 1, max = 1)
                    FeatureCollection features
            ) throws Exception
    {
        return new Process(features).execute();
    }
    
    public class Process {
        
        private final FeatureCollection featureCollection;
        
        Process(FeatureCollection featureCollection) {
            this.featureCollection = featureCollection;
        }
        
        public Integer execute() throws Exception {
            return UTMFinder.findUTMZoneCRSCount(featureCollection);
        }
    }
}
