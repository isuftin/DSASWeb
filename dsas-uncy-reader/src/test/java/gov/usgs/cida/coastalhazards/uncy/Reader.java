package gov.usgs.cida.coastalhazards.uncy;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jtsexample.geom.ExtendedCoordinate;
import gov.usgs.cida.owsutils.commons.shapefile.utils.MultiLineZHandler;
import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.Query;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.shapefile.shp.ShapefileReader.Record;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.Hints;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;

/** Read a shoreline shapefile and the associated uncertainty map.
 * 
 * @author rhayes
 *
 */
public class Reader {

	private int locateField(DbaseFileHeader hdr, String nm, Class<?> expected) {
		int idx = -1;
		
		for (int x = 0; x < hdr.getNumFields(); x++) {
			String fnm = hdr.getFieldName(x);
			if (nm.equalsIgnoreCase(fnm)) {
				idx = x;
			}
		}
		if (idx < 0) {
			throw new RuntimeException("did not find column named UNCY");
		}
		
		Class<?> idClass = hdr.getFieldClass(idx);		
		if ( ! expected.isAssignableFrom(idClass)) {
			throw new RuntimeException("Actual class " + idClass + " is not assignable to expected " + expected);
		}

		return idx;
	}
	
	public Map<Integer,Double> readUncyFromDBF(String fn) throws Exception {
		
		ShpFiles shpFile = new ShpFiles(fn);
		Charset charset = Charset.defaultCharset();
		
		DbaseFileReader rdr = new DbaseFileReader(shpFile, false, charset);
		
		DbaseFileHeader hdr = rdr.getHeader();
		System.out.println("Header: " + hdr);
		
		int uncyIdx = locateField(hdr, "uncy", Double.class);
		int idIdx = locateField(hdr, "id", Number.class);
		
		Map<Integer,Double> value = new HashMap<Integer,Double>();
		
		while (rdr.hasNext()) {
			Object[] ff = rdr.readEntry();
			
			// System.out.printf("%s\n", ff[2]);
			Integer i = ((Number)ff[idIdx]).intValue();
			Double d = (Double)ff[uncyIdx];
			
			value.put(i, d);
		}
		
		return value;
	}
	
	public void processM(String fn, Map<Integer,Double> uncyMap) throws Exception {

		File file = new File(fn);

		ShpFiles shpFile = new ShpFiles(file);
		CoordinateSequenceFactory x = com.vividsolutions.jtsexample.geom.ExtendedCoordinateSequenceFactory.instance();
		GeometryFactory gf = new GeometryFactory(x);

		ShapefileReader rdr = new ShapefileReader(shpFile,false, false, gf);
		rdr.setHandler(new MultiLineZHandler(ShapeType.ARCM, gf));

		Charset charset = Charset.defaultCharset();		
		DbaseFileReader dbf = new DbaseFileReader(shpFile, false, charset);
		DbaseFileHeader hdr = dbf.getHeader();

		int dfltUncyIdx = locateField(hdr, "uncy", Double.class);
		
		int shpCt = 0;
		
		while (rdr.hasNext()) { 
			Record rec = rdr.nextRecord();
			
			Object[] ff = dbf.readEntry();
			Double defaultUncy = (Double)ff[dfltUncyIdx];
			
			Object thing = rec.shape();
			if (shpCt < 100) {
				System.out.printf("%s\n", rec);
				System.out.printf("shape %s\n", thing);
			}
						
			MultiLineString mls = (MultiLineString) thing;
			for (int g = 0; g < mls.getNumGeometries(); g++) {
				Geometry geom = mls.getGeometryN(g);
				
				LineString ls = (LineString) geom;
				CoordinateSequence cs = ls.getCoordinateSequence();
				
				if (shpCt < 100) {
					System.out.printf("Geom %d: %s\n", g, cs);
				}
				
				int ptCt = 0;
				for (int i = 0; i < cs.size(); i++) {
					ptCt ++;
					
					double uncy = defaultUncy;
					
					Coordinate coord = cs.getCoordinate(i);
					ExtendedCoordinate eCoord = (ExtendedCoordinate) coord;
					
					Point p = ls.getPointN(i);
					ExtendedCoordinate eCoord2 = (ExtendedCoordinate) p.getCoordinate();

					double md = cs.getOrdinate(i, 3);
					if ( ! Double.isNaN(md)) {
						int mi = (int)md;
						
						uncy = uncyMap.get(mi);
					}
					
					if (shpCt < 100) {
						if (ptCt < 10) {
							System.out.printf("\tX %f Y %f M %f uncy %f\n", cs.getX(i), cs.getY(i), cs.getOrdinate(i, 3), uncy);
						}
					}
				}
			}
			
			shpCt ++;
		}

	}

	public void read(String fn) throws Exception {

		File file = new File(fn);

		Map connect = new HashMap();
		connect.put("url", file.toURL());

		DataStore dataStore = DataStoreFinder.getDataStore(connect);

		String[] typeNames = dataStore.getTypeNames();

		System.out.println("Type names:");
		for (String tn : typeNames) {
			System.out.println(tn);
		}

		String typeName = typeNames[0];

		System.out.println("Reading content " + typeName);

		SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);

		Query q = new Query();
		CoordinateSequenceFactory x = com.vividsolutions.jtsexample.geom.ExtendedCoordinateSequenceFactory.instance();
		GeometryFactory gf = new GeometryFactory(x);

		Hints hints = new Hints(org.geotools.factory.Hints.JTS_GEOMETRY_FACTORY, gf);
		q.setHints(hints);
		
		SimpleFeatureCollection collection = featureSource.getFeatures(q);

		SimpleFeatureIterator iterator = collection.features();
		
		try {
			while (iterator.hasNext()) {
				Feature feature = iterator.next();
				GeometryAttribute sourceGeometry = feature.getDefaultGeometryProperty();
				System.out.printf("Geometry %s\n", sourceGeometry);
			}
		} finally {
			iterator.close();
		}
	}

	// This loses -- it produces Extended points, but with m=0.0.
	public void read_lose263(String fn) throws Exception {

		File file = new File(fn);

		Map connect = new HashMap();
		connect.put("url", file.toURL());

		DataStore dataStore = DataStoreFinder.getDataStore(connect);

		String[] typeNames = dataStore.getTypeNames();

		System.out.println("Type names:");
		for (String tn : typeNames) {
			System.out.println(tn);
		}

		String typeName = typeNames[0];

		System.out.println("Reading content " + typeName);

		SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);

		Query q = new Query();
		CoordinateSequenceFactory x = com.vividsolutions.jtsexample.geom.ExtendedCoordinateSequenceFactory.instance();
		GeometryFactory gf = new GeometryFactory(x);

		Hints hints = new Hints(org.geotools.factory.Hints.JTS_GEOMETRY_FACTORY, gf);
		q.setHints(hints);
		
		SimpleFeatureCollection collection = featureSource.getFeatures(q);

		SimpleFeatureIterator iterator = collection.features();
		
		try {
			while (iterator.hasNext()) {
				Feature feature = iterator.next();
				GeometryAttribute sourceGeometry = feature.getDefaultGeometryProperty();
				System.out.printf("Geometry %s\n", sourceGeometry);
			}
		} finally {
			iterator.close();
		}
	}

	public boolean rd = false;
	
	public static void main(String[] args) throws Exception {
		for (String fn : args) {
			Reader ego = new Reader();
			
			if (ego.rd) {
				ego.read(fn+".shp");
			} else {
				Map<Integer,Double> uncyMap = ego.readUncyFromDBF(fn + "_uncertainty.dbf");
				System.out.printf("Got map size %d\n", uncyMap.size());
			
				ego.processM(fn+".shp", uncyMap);
			}
		}
	}

}
