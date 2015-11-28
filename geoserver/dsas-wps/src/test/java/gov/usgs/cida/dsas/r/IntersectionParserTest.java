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

package gov.usgs.cida.dsas.r;

import static org.junit.Assert.*;
import gov.usgs.cida.owsutils.commons.shapefile.utils.FeatureCollectionFromShp;
import gov.usgs.cida.utilities.features.AttributeGetter;
import gov.usgs.cida.utilities.features.Constants;
import gov.usgs.cida.dsas.wps.geom.Intersection;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 *  Fix this later
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class IntersectionParserTest {
    
    private URL legacyShapefile;
    private URL shapefile;
    
    @Before
    public void setupShape() throws IOException {
        legacyShapefile = IntersectionParserTest.class.getClassLoader()
                .getResource("gov/usgs/cida/coastalhazards/jersey/NewJerseyN_intersections.shp");

        shapefile = IntersectionParserTest.class.getClassLoader()
                .getResource("gov/usgs/cida/coastalhazards/Georgia_MHW_false_bias_test/Georgia_test_intersects.shp");
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void csvFromFeatureCollection() throws IOException, ParseException {
    	FeatureCollection<SimpleFeatureType, SimpleFeature> fc = FeatureCollectionFromShp.getFeatureCollectionFromShp(legacyShapefile);
        
        Map<Integer, List<Intersection>> map = new TreeMap<Integer, List<Intersection>>();
    	FeatureIterator<SimpleFeature> features = null;
		try {
			features = fc.features();
			while (features.hasNext()) {
				SimpleFeature feature = features.next();
				int transectId = (Integer)feature.getAttribute("TransectID");

				Intersection intersection = new Intersection(feature, new AttributeGetter(feature.getType()));
				
				assertFalse("Legacy shapefiles lacking MHW attribute defaults to false MHW flag", intersection.isMeanHighWater());
				assertTrue("Legacy shapefiles lacking " + Constants.BIAS_ATTR + " attribute defaults to 0", intersection.getBias() == 0.0d);
				assertTrue("Legacy shapefiles lacking " + Constants.BIAS_UNCY_ATTR + " attribute defaults to 0", intersection.getBiasUncertainty() == 0.0d);
				
				if (map.containsKey(transectId)) {
					map.get(transectId).add(intersection);
				}
				else {
					List<Intersection> pointList = new LinkedList<Intersection>();
					pointList.add(intersection);
					map.put(transectId, pointList);
				}
			}
		} finally {
			if (null != features) {
				features.close();
			}
		}
        
        for (int key : map.keySet()) {
            List<Intersection> points = map.get(key);
            for (Intersection p : points) {
            	assertExpectedToStringFormat(p);
            }
        }
    }

    @Test
    public void mhwValueTranslationFromIntersectionLayerTest() throws IOException {
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = FeatureCollectionFromShp.getFeatureCollectionFromShp(shapefile);
        
        Map<Integer, List<Intersection>> map = new TreeMap<>();
    	FeatureIterator<SimpleFeature> features = null;
		try {
			features = fc.features();
			boolean trueMhwExistsInShapefile = false;
			while (features.hasNext()) {
				SimpleFeature feature = features.next();
				Intersection intersection = new Intersection(feature, new AttributeGetter(feature.getType()));
				
				//ensure MHW property is read from transect are translated correctly
				if(feature.getAttribute(Constants.MHW_ATTR).toString().equalsIgnoreCase("true")) {
					assertTrue("When TRUE found in feature, property properly set", intersection.isMeanHighWater());
					trueMhwExistsInShapefile = true;
				}
				
				assertFalse("Bias is not default", intersection.getBias() == Constants.DEFAULT_BIAS);
				assertFalse("Bias uncertainty is not default", intersection.getBias() == Constants.DEFAULT_BIAS_UNCY);
			}
		} finally {
			if (null != features) {
				features.close();
			}
		}
		for (int key : map.keySet()) {
            List<Intersection> points = map.get(key);
            for (Intersection p : points) {
            	assertExpectedToStringFormat(p);
            }
        }
    }
    
    private void assertExpectedToStringFormat(Intersection p) {
    	String[] tabSeparatedParts = p.toString().split("\t");
    	assertTrue("Has 5 columns", tabSeparatedParts.length == 5);
    	assertEquals("Distance is in correct place", "" + p.getShiftedDistance(), tabSeparatedParts[1]);
    	assertEquals("Uncertainty is in correct place", "" + p.getUncertainty(), tabSeparatedParts[2]);
    	assertEquals("Bias is in correct place", "" + p.getBias(), tabSeparatedParts[3]);
    	assertEquals("Bias uncertainty is in correct place", "" + p.getBiasUncertainty(), tabSeparatedParts[4]);
    }
}
