package gov.usgs.cida.coastalhazards.uncy;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiLineString;
// which Point class to use?
import com.vividsolutions.jts.geom.Point;


/** Write a copy of the input shapefile.
 * 
 * @author rhayes
 *
 */
public class Writer {

	public void copy(String fn) throws Exception {

		File fin = new File(fn+".shp");

		Map<String, Serializable> connect = new HashMap<String, Serializable> ();
		connect.put("url", fin.toURL());

		DataStore inputStore = DataStoreFinder.getDataStore(connect);

		File fout = new File(fn + "_copy.shp");
		connect.put("url", fout.toURL());
        connect.put("create spatial index", Boolean.TRUE);
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        ShapefileDataStore outputStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(connect);

		String[] typeNames = inputStore.getTypeNames();

		System.out.println("Type names:");
		for (String tn : typeNames) {
			System.out.println(tn);
		}

		String typeName = typeNames[0];

		System.out.println("Reading content " + typeName);

		SimpleFeatureSource featureSource = inputStore.getFeatureSource(typeName);
		SimpleFeatureType sourceSchema = featureSource.getSchema();
		// duplicate schema, except replace geometry with Point
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName(sourceSchema.getName());
		typeBuilder.setCRS(sourceSchema.getCoordinateReferenceSystem());
		
		int geomIdx = -1;
		int uncyIdx = -1;
		int idx = 0;
		for (AttributeDescriptor ad : sourceSchema.getAttributeDescriptors()) {
			AttributeType at = ad.getType();
			if (at instanceof GeometryType) {
				typeBuilder.add(ad.getLocalName(), Point.class);
				geomIdx = idx;
			} else {
				typeBuilder.add(ad.getLocalName(), ad.getType().getBinding());
				if ("uncy".equalsIgnoreCase(ad.getLocalName())) {
					uncyIdx = idx;
				}
			}
			idx++;
		}
		SimpleFeatureType outputFeatureType = typeBuilder.buildFeatureType();
		outputStore.createSchema(outputFeatureType);

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(outputStore.getSchema());

		SimpleFeatureCollection collection = featureSource.getFeatures();
		
		SimpleFeatureIterator iterator = collection.features();

        Transaction tx = new DefaultTransaction("create");
		FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter = outputStore.getFeatureWriterAppend(tx);

		int featureCt = 0;
		int pointCt = 0;
		
		try {
			while (iterator.hasNext()) {
				Feature feature = iterator.next();
				
				GeometryAttribute sourceGeometry = feature.getDefaultGeometryProperty();
				System.out.printf("feature identifier %s,  name %s, geometry type %s\n",
						feature.getIdentifier(), 
						feature.getName(),
						sourceGeometry.getType());
				
				SimpleFeature inputFeature = (SimpleFeature) feature;

				// explode this one multi-point feature to one Point feature per point
				
				// System.out.printf("Geometry %s\n", sourceGeometry);
				MultiLineString mls = (MultiLineString) sourceGeometry.getValue();
				for (Coordinate coord : mls.getCoordinates()) {
					Point newPoint = geometryFactory.createPoint(coord);
					
					SimpleFeature writeFeature = featureWriter.next();
					
					Collection<Property> fpp = inputFeature.getProperties();
					Property[] properties = fpp.toArray(new Property[fpp.size()]);
					for (int i = 0; i < properties.length; i++) {
						// TODO replace uncy
						// TODO Better test for geometry property

						if (i == geomIdx) {
							writeFeature.setAttribute(i, newPoint);
						} else if (i == uncyIdx) {
							// TODO replace uncy value with mapped, if possible
							writeFeature.setAttribute(i, properties[i].getValue());
						} else {
							writeFeature.setAttribute(i, properties[i].getValue());
						}
					}
					
					featureWriter.write();
					pointCt ++;					
				}
				featureCt ++;
			}
			
		} finally {
			iterator.close();
		}
		
		tx.commit();
		
		System.out.printf("Wrote %d points from %d features\n", pointCt, featureCt);
	}

	public static void main(String[] args) throws Exception {
		for (String fn : args) {
			Writer ego = new Writer();

			ego.copy(fn);
			
		}
	}

}
