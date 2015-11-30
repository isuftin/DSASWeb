package gov.usgs.cida.dsas.uncy;

import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import static gov.usgs.cida.dsas.uncy.ShapefileOutputXploder.PTS_SUFFIX;
import gov.usgs.cida.owsutils.commons.shapefile.utils.IterableShapefileReader;
import gov.usgs.cida.owsutils.commons.shapefile.utils.PointIterator;
import gov.usgs.cida.owsutils.commons.shapefile.utils.ShapeAndAttributes;
import gov.usgs.cida.owsutils.commons.shapefile.utils.XploderMultiLineHandler;
import gov.usgs.cida.utilities.features.Constants;
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
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.files.ShpFileType;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.shapefile.shp.ShapefileException;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Write a copy of the input shapefile, with lines exploded to their constituent
 * points and with the M columns used to look up the point-by-point uncertainty
 * (if available).
 *
 * @author rhayes, isuftin
 *
 */
public abstract class Xploder {

	public static final String UNCERTAINTY_COLUMN_PARAM = "uncertaintyColumnName";
	public static final String INPUT_FILENAME_PARAM = "inputFilename";
	public static final String OUTPUT_CRS_PARAM = "outputCrs"; // Should be WKT
	protected static final String DEFAULT_UNCY_COLUMN_NAME = "uncy";
	protected int geomIdx = -1;
	protected String inputFileName;
	protected String uncyColumnName;
	protected SimpleFeatureType outputFeatureType;
	protected CoordinateReferenceSystem outputCRS = DefaultGeographicCRS.WGS84;
	private static final Logger LOGGER = LoggerFactory.getLogger(Xploder.class);
	private static final GeometryFactory GEOMETRY_FACTORY = JTSFactoryFinder.getGeometryFactory(null);
	private DbaseFileHeader dbfHdr;
	private int uncertaintyIdIdx;
	private CoordinateReferenceSystem sourceCRS;

	protected Xploder(Map<String, String> config) {
		if (config == null || config.isEmpty()) {
			throw new IllegalArgumentException("Configuration map for ShapefileOutputExploder may not be null or empty");
		}
		
		String[] requiredConfigs = new String[] {
			INPUT_FILENAME_PARAM,
			UNCERTAINTY_COLUMN_PARAM
		};
		
		for (String requiredConfig : requiredConfigs) {
			if (!config.containsKey(requiredConfig)) {
				throw new IllegalArgumentException(String.format("Configuration map for Xploder must include parameter %s", requiredConfig));
			}
			if (StringUtils.isBlank(config.get(requiredConfig))) {
				throw new IllegalArgumentException(String.format("Configuration map for Xploder must include value for parameter %s", requiredConfig));
			}
		}

		this.inputFileName = config.get(INPUT_FILENAME_PARAM);
		this.uncyColumnName = config.get(UNCERTAINTY_COLUMN_PARAM);
		if (config.containsKey(OUTPUT_CRS_PARAM) && StringUtils.isNotBlank(config.get(OUTPUT_CRS_PARAM))) {
			try {
				outputCRS = ReferencingFactoryFinder.getCRSFactory(null).createFromWKT(config.get(OUTPUT_CRS_PARAM));
			} catch (FactoryException | FactoryRegistryException ex) {
				LOGGER.warn(String.format("Could not create output CRS. Output will default to %s", DefaultGeographicCRS.WGS84.getName().getCode()));
			}
		}
		
	}

	public static int locateField(DbaseFileHeader fileHeader, String fieldName) {
		int fieldPositionIndex = -1;

		for (int headerIndex = 0; headerIndex < fileHeader.getNumFields(); headerIndex++) {
			String fileFieldName = fileHeader.getFieldName(headerIndex);
			if (fieldName.equalsIgnoreCase(fileFieldName)) {
				fieldPositionIndex = headerIndex;
			}
		}

		return fieldPositionIndex;
	}

	static CoordinateReferenceSystem getInputCrs(String inputFilename) throws IOException {
		return readSourceSchema(inputFilename).getCoordinateReferenceSystem();
	}

	static SimpleFeatureType readSourceSchema(String inputFileName) throws MalformedURLException, IOException {
		File inputFile = new File(inputFileName + ".shp");
		LOGGER.debug("Reading source schema from {}", inputFile);

		DataStore inputStore = null;
		SimpleFeatureType sourceSchema = null;
		try {
			Map<String, Serializable> fileMap = new HashMap<>();
			fileMap.put("url", inputFile.toURI().toURL());
			inputStore = DataStoreFinder.getDataStore(fileMap);

			String[] typeNames = inputStore.getTypeNames();
			SimpleFeatureSource featureSource = inputStore.getFeatureSource(typeNames[0]);
			sourceSchema = featureSource.getSchema();

			LOGGER.debug("Source schema is {}", sourceSchema);
		} finally {
			if (inputStore != null) {
				inputStore.dispose();
			}
		}
		return sourceSchema;
	}

	private static String shapefileNames(ShpFiles shapefiles) {
		StringBuilder sb = new StringBuilder();

		Map<ShpFileType, String> m = shapefiles.getFileNames();
		m.entrySet().stream().forEach((me) -> {
			sb.append(me.getKey()).append("\t").append(me.getValue()).append("\n");
		});

		return sb.toString();
	}

	public int processShape(ShapeAndAttributes sap, int segmentId, FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter) throws IOException, MismatchedDimensionException, TransformException, FactoryException {
		Double uncertainty = ((Number) sap.row.read(uncertaintyIdIdx)).doubleValue();

		int ptCt = 0;
		MultiLineString shape = (MultiLineString) sap.record.shape();
		int recordNum = sap.record.number;
		int numGeom = shape.getNumGeometries();
		MathTransform mathTransform = CRS.findMathTransform(sourceCRS, outputCRS, true);
		for (int segmentIndex = 0; segmentIndex < numGeom; segmentIndex++) {
			Geometry geometry = JTS.transform(shape.getGeometryN(segmentIndex), mathTransform);
			PointIterator pIterator = new PointIterator(geometry);
			while (pIterator.hasNext()) {
				Point p = pIterator.next();

				// write new point-thing-with-uncertainty
				writePoint(p, sap.row, uncertainty, recordNum, segmentId, featureWriter);

				ptCt++;

			}
		}

		return ptCt;

	}

	public void writePoint(Point p, DbaseFileReader.Row row, double uncy, int recordId, int segmentId, FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter) throws IOException {

		SimpleFeature writeFeature = featureWriter.next();

		// geometry field is first, otherwise we lose.
		Point np = GEOMETRY_FACTORY.createPoint(p.getCoordinate());
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

	/**
	 * Will use the source schema name to set the output schema name
	 *
	 * @see Xploder#createOutputFeatureType(java.lang.String)
	 * @return
	 * @throws IOException
	 */
	protected SimpleFeatureType createOutputFeatureType() throws IOException {
		return createOutputFeatureType(null);
	}

	/**
	 *
	 * @param outputTypeName
	 * @return
	 * @throws IOException
	 */
	protected SimpleFeatureType createOutputFeatureType(String outputTypeName) throws IOException {
		// read input to get attributes
		SimpleFeatureType sourceSchema = readSourceSchema(inputFileName);

		// duplicate input schema, except replace geometry with Point
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		if (StringUtils.isBlank(outputTypeName)) {
			typeBuilder.setName(sourceSchema.getName() + PTS_SUFFIX);
		} else {
			typeBuilder.setName(outputTypeName);
		}
		typeBuilder.setCRS(sourceSchema.getCoordinateReferenceSystem());

		geomIdx = -1;
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
		typeBuilder.add(Constants.RECORD_ID_ATTR, Integer.class);
		typeBuilder.add(Constants.SEGMENT_ID_ATTR, Integer.class);
		SimpleFeatureType outputFeatureType = typeBuilder.buildFeatureType();

		LOGGER.debug("Output feature type is {}", outputFeatureType);

		return outputFeatureType;
	}

	abstract FeatureWriter<SimpleFeatureType, SimpleFeature> createFeatureWriter(Transaction tx) throws IOException;

	public int explode() throws IOException {
		int ptTotal = 0;
		try (IterableShapefileReader rdr = initReader(inputFileName);
				Transaction tx = new DefaultTransaction();
				FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter = createFeatureWriter(tx)) {
			LOGGER.debug("Input files from {}\n{}", inputFileName, shapefileNames(rdr.getShpFiles()));

			// Too bad that the reader classes don't expose the ShpFiles.
			int shpCt = 0;
			LOGGER.debug(geomIdx + "");
			if (geomIdx != 0) {
				throw new RuntimeException("This program only supports input that has the geometry as attribute 0");
			}

			for (ShapeAndAttributes saa : rdr) {
				int ptCt = processShape(saa, shpCt + 1, featureWriter);
				LOGGER.debug("Wrote {} points for shape {}", ptCt, saa.record.toString());
				ptTotal += ptCt;
				shpCt++;
			}

			tx.commit();
			LOGGER.info("Wrote {} points in {} shapes", ptTotal, shpCt);
		} catch (MismatchedDimensionException | TransformException | FactoryException ex) {
			throw new IOException(ex);
		}
		return ptTotal;
	}

	protected IterableShapefileReader initReader(String inputFilename) throws ShapefileException, IOException {
		CoordinateSequenceFactory csf = com.vividsolutions.jtsexample.geom.ExtendedCoordinateSequenceFactory.instance();
		GeometryFactory gf = new GeometryFactory(csf);
		XploderMultiLineHandler mlh = new XploderMultiLineHandler(ShapeType.ARCM, gf);
		IterableShapefileReader shapefileReader = new IterableShapefileReader(inputFilename, mlh);

		dbfHdr = shapefileReader.getDbfHeader();
		uncertaintyIdIdx = locateField(dbfHdr, uncyColumnName);
		sourceCRS = getInputCrs(inputFilename);
		
		return shapefileReader;
	}

	public void setOutputFeatureType(SimpleFeatureType outputFeatureType) {
		this.outputFeatureType = outputFeatureType;
	}
}
