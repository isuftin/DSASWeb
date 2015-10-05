package gov.usgs.cida.coastalhazards.uncy;

import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import gov.usgs.cida.owsutils.commons.shapefile.utils.IterableShapefileReader;
import gov.usgs.cida.owsutils.commons.shapefile.utils.PointIterator;
import gov.usgs.cida.owsutils.commons.shapefile.utils.ShapeAndAttributes;
import gov.usgs.cida.owsutils.commons.shapefile.utils.XploderMultiLineHandler;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.files.ShpFileType;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.shapefile.shp.ShapefileException;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Write a copy of the input shapefile, with lines exploded to their constituent
 * points and with the M columns used to look up the point-by-point uncertainty
 * (if available).
 *
 * @author rhayes
 *
 */
public class Xploder {

	public static final String PTS_SUFFIX = "_pts";
	private static final Logger logger = LoggerFactory.getLogger(Xploder.class);
	private static final GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

	public static int locateField(DbaseFileHeader hdr, String nm) {
		int idx = -1;

		for (int x = 0; x < hdr.getNumFields(); x++) {
			String fnm = hdr.getFieldName(x);
			if (nm.equalsIgnoreCase(fnm)) {
				idx = x;
			}
		}

		return idx;
	}

	private static SimpleFeatureType readSourceSchema(String fn) throws MalformedURLException, IOException {
		File fin = new File(fn + ".shp");
		logger.debug("Reading source schema from {}", fin);

		Map<String, Serializable> connect = new HashMap<>();
		connect.put("url", fin.toURI().toURL());

		DataStore inputStore = DataStoreFinder.getDataStore(connect);

		String[] typeNames = inputStore.getTypeNames();
		String typeName = typeNames[0];

		SimpleFeatureSource featureSource = inputStore.getFeatureSource(typeName);
		SimpleFeatureType sourceSchema = featureSource.getSchema();

		// this might kill the source schema.
		inputStore.dispose();

		logger.debug("Source schema is {}", sourceSchema);

		return sourceSchema;
	}

	private static String shapefileNames(ShpFiles shp) {
		StringBuilder sb = new StringBuilder();

		Map<ShpFileType, String> m = shp.getFileNames();
		for (Map.Entry<ShpFileType, String> me : m.entrySet()) {
			sb.append(me.getKey()).append("\t").append(me.getValue()).append("\n");
		}

		return sb.toString();
	}

	public static void main(String[] args) throws Exception {
		for (String fn : args) {
			Xploder ego = new Xploder();
			ego.explode(fn);
		}
	}

	private int geomIdx = -1;
	private FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter;
	private String uncyColumnName = "uncy";
	private Class<?> uncyColumnClassType = Double.class;
	private DbaseFileHeader dbfHdr;
	private Transaction tx;
	private int uncertaintyIdIdx;

	public Xploder() {
		this("uncy");
	}

	public Xploder(String uncyColumnName) {
		if (StringUtils.isNotBlank(uncyColumnName)) {
			this.uncyColumnName = uncyColumnName;
		}
	}

	public int processShape(ShapeAndAttributes sap, int segmentId) throws IOException {

		Double uncertainty = ((Number) sap.row.read(uncertaintyIdIdx)).doubleValue();

		int ptCt = 0;
		MultiLineString shape = (MultiLineString) sap.record.shape();
		int recordNum = sap.record.number;
		int numGeom = shape.getNumGeometries();

		for (int segmentIndex = 0; segmentIndex < numGeom; segmentIndex++) {
			Geometry geometry = shape.getGeometryN(segmentIndex);

			PointIterator pIterator = new PointIterator(geometry);
			while (pIterator.hasNext()) {
				Point p = pIterator.next();

				// write new point-thing-with-uncertainty
				writePoint(p, sap.row, uncertainty, recordNum, segmentId);

				ptCt++;

			}
		}

		return ptCt;

	}

	public void writePoint(Point p, DbaseFileReader.Row row, double uncy, int recordId, int segmentId) throws IOException {

		SimpleFeature writeFeature = featureWriter.next();

		// geometry field is first, otherwise we lose.
		Point np = geometryFactory.createPoint(p.getCoordinate());
		writeFeature.setAttribute(0, np);

		// copy them other attributes over, replacing uncy
		int i;
		for (i = 0; i < dbfHdr.getNumFields(); i++) {
			Object value;
			if (i == uncertaintyIdIdx) {
				value = uncy;
			} else {
				value = row.read(i);
			}
			writeFeature.setAttribute(i + 1, value);
		}
		// Add record attribute
		writeFeature.setAttribute(i + 1, recordId);
		writeFeature.setAttribute(i + 2, segmentId);

		featureWriter.write();
	}

	private File initWriter(String fn) throws IOException {
		// read input to get attributes
		SimpleFeatureType sourceSchema = readSourceSchema(fn);

		// duplicate input schema, except replace geometry with Point
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName(sourceSchema.getName() + PTS_SUFFIX);
		typeBuilder.setCRS(sourceSchema.getCoordinateReferenceSystem());

		geomIdx = -1;
		// dfltUncyIdx = -1;
		int idx = 0;
		for (AttributeDescriptor ad : sourceSchema.getAttributeDescriptors()) {
			AttributeType at = ad.getType();
			if (at instanceof GeometryType) {
				typeBuilder.add(ad.getLocalName(), Point.class);
				geomIdx = idx;
			} else {
				typeBuilder.add(ad.getLocalName(), ad.getType().getBinding());
			}
			idx++;
		}
		typeBuilder.add("recordId", Integer.class);
		typeBuilder.add("segmentId", Integer.class);
		SimpleFeatureType outputFeatureType = typeBuilder.buildFeatureType();

		logger.debug("Output feature type is {}", outputFeatureType);

		File fout = new File(fn + PTS_SUFFIX + ".shp");

		Map<String, Serializable> connect = new HashMap<>();
		connect.put("url", fout.toURI().toURL());
		connect.put("create spatial index", Boolean.TRUE);

		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		ShapefileDataStore outputStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(connect);

		outputStore.createSchema(outputFeatureType);

		featureWriter = outputStore.getFeatureWriterAppend(tx);

		logger.info("Will write {}", fout.getAbsolutePath());

		return fout;
	}

	public File explode(String fn) throws IOException  {
		File ptFile;

		try (IterableShapefileReader rdr = initReader(fn)) {
			logger.debug("Input files from {}\n{}", fn, shapefileNames(rdr.getShpFiles()));
			tx = new DefaultTransaction("create");
			ptFile = initWriter(fn);

			// Too bad that the reader classes don't expose the ShpFiles.
			int shpCt = 0;
			int ptTotal = 0;

			if (geomIdx != 0) {
				throw new RuntimeException("This program only supports input that has the geometry as attribute 0");
			}
			
			for (ShapeAndAttributes saa : rdr) {
				int ptCt = processShape(saa, shpCt + 1);
				logger.debug("Wrote {} points for shape {}", ptCt, saa.record.toString());
				ptTotal += ptCt;
				shpCt++;
			}

			tx.commit();

			logger.info("Wrote {} points in {} shapes", ptTotal, shpCt);
		} finally {
			if (null != featureWriter) {
				try {
					featureWriter.close();
				} catch (IOException ex) {
					logger.info("Could not close feature writer", ex);
				}
			}
		}
		return ptFile;
	}

	protected IterableShapefileReader initReader(String fn) throws ShapefileException  {
		CoordinateSequenceFactory csf = com.vividsolutions.jtsexample.geom.ExtendedCoordinateSequenceFactory.instance();
		GeometryFactory gf = new GeometryFactory(csf);
		XploderMultiLineHandler mlh = new XploderMultiLineHandler(ShapeType.ARCM, gf);
		IterableShapefileReader rdr = new IterableShapefileReader(fn, mlh);

		dbfHdr = rdr.getDbfHeader();
		uncertaintyIdIdx = locateField(dbfHdr, uncyColumnName);
		return rdr;
	}
}
