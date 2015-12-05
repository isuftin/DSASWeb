package gov.usgs.cida.dsas.uncy;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import gov.usgs.cida.dsas.model.DSASProcess;
import gov.usgs.cida.dsas.model.IShapeFile;
import static gov.usgs.cida.dsas.uncy.ShapefileOutputXploder.PTS_SUFFIX;
import gov.usgs.cida.dsas.utilities.features.Constants;
import gov.usgs.cida.dsas.utilities.properties.Property;
import gov.usgs.cida.dsas.utilities.properties.PropertyUtil;
import gov.usgs.cida.owsutils.commons.io.FileHelper;
import gov.usgs.cida.owsutils.commons.shapefile.utils.IterableShapefileReader;
import gov.usgs.cida.owsutils.commons.shapefile.utils.ShapeAndAttributes;
import gov.usgs.cida.owsutils.commons.shapefile.utils.XploderMultiLineHandler;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
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
import org.geotools.geometry.jts.JTSFactoryFinder;
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
public abstract class Xploder implements AutoCloseable {

	public static final String UNCERTAINTY_COLUMN_PARAM = "uncertaintyColumnName";
	public static final String INPUT_FILENAME_PARAM = "inputFilename";
	public static final String OUTPUT_CRS_PARAM = "outputCrs"; // Should be WKT
	public static final String DSAS_PROCESS_PARAM = "dsasProcess"; // Should be WKT
	protected static final String DEFAULT_UNCY_COLUMN_NAME = "uncy";
	protected static final GeometryFactory GEOMETRY_FACTORY = JTSFactoryFinder.getGeometryFactory(null);
	protected static XploderMultiLineHandler shapeHandler = null;
	private static final Logger LOGGER = LoggerFactory.getLogger(Xploder.class);
	protected String uncyColumnName;
	protected SimpleFeatureType outputFeatureType;
	protected CoordinateReferenceSystem outputCRS = DefaultGeographicCRS.WGS84;
	protected DbaseFileHeader dbfHdr;
	protected int uncertaintyIdIdx;
	protected int segmentIdx;
	protected ShpFiles shapeFiles;
	protected CoordinateReferenceSystem sourceCRS;
	protected int geomIdx = -1;
	protected DSASProcess process = null;

	protected Xploder(Map<String, Object> config) throws IOException {
		if (config == null || config.isEmpty()) {
			throw new IllegalArgumentException("Configuration map for ShapefileOutputExploder may not be null or empty");
		}

		String[] requiredConfigs = new String[]{
			INPUT_FILENAME_PARAM,
			UNCERTAINTY_COLUMN_PARAM
		};

		for (String requiredConfig : requiredConfigs) {
			if (!config.containsKey(requiredConfig)) {
				throw new IllegalArgumentException(String.format("Configuration map for Xploder must include parameter %s", requiredConfig));
			}
		}

		String inputFileName = (String) config.get(INPUT_FILENAME_PARAM);
		File inputFile = new File(inputFileName);
		if (!inputFile.exists()) {
			throw new IOException(String.format("%s does not exist", inputFile.getAbsolutePath()));
		}

		// Ensure that we have a place to do work
		String baseDir = PropertyUtil.getProperty(Property.DIRECTORIES_BASE, FileUtils.getTempDirectoryPath() + "/DSASWeb");
		String workDir = PropertyUtil.getProperty(Property.DIRECTORIES_WORK, "/work");
		String inputFileWorkDir = String.format("%s%s/%s.%s", baseDir, workDir, inputFile.getName(), new Date().getTime());
		File inputFileTempWorkdir = new File(inputFileWorkDir);
		FileUtils.forceMkdir(new File(inputFileWorkDir));
		inputFileTempWorkdir.deleteOnExit();

		if (FileHelper.isZipFile(inputFile)) {
			FileHelper.unzipFile(inputFileTempWorkdir.getAbsolutePath(), inputFile);
		} else if (inputFile.isFile()) {
			File parentDirectory = inputFile.getParentFile();
			FileHelper.copyDirectory(parentDirectory, inputFileTempWorkdir);
		} else {
			FileHelper.copyDirectory(inputFile, inputFileTempWorkdir);
		}

		Collection<File> shapeFileColl = FileHelper.listFiles(inputFileTempWorkdir, IShapeFile.REQUIRED_FILES, false);
		if (shapeFileColl.isEmpty()) {
			throw new IOException(String.format("Shapefile is missing one or more required file types %s", String.join(",", IShapeFile.REQUIRED_FILES)));
		}
		shapeFiles = new ShpFiles(shapeFileColl.iterator().next());

		this.uncyColumnName = (String) config.get(UNCERTAINTY_COLUMN_PARAM);

		if (config.containsKey(OUTPUT_CRS_PARAM) && StringUtils.isNotBlank((String) config.get(OUTPUT_CRS_PARAM))) {
			try {
				outputCRS = ReferencingFactoryFinder.getCRSFactory(null).createFromWKT((String) config.get(OUTPUT_CRS_PARAM));
			} catch (FactoryException | FactoryRegistryException ex) {
				LOGGER.warn(String.format("Could not create output CRS. Output will default to %s", DefaultGeographicCRS.WGS84.getName().getCode()), ex);
			}
		}

		if (shapeHandler == null) {
			shapeHandler = new XploderMultiLineHandler(
					ShapeType.ARCM,
					new GeometryFactory(com.vividsolutions.jtsexample.geom.ExtendedCoordinateSequenceFactory.instance())
			);
		}

		if (config.containsKey(DSAS_PROCESS_PARAM) && config.containsValue(DSAS_PROCESS_PARAM)) {
			this.process = (DSASProcess) config.get(DSAS_PROCESS_PARAM);
		}

	}

	protected static int locateField(DbaseFileHeader fileHeader, String fieldName) {
		int fieldPositionIndex = -1;

		if (StringUtils.isNotBlank(fieldName)) {
			for (int headerIndex = 0; headerIndex < fileHeader.getNumFields(); headerIndex++) {
				String fileFieldName = fileHeader.getFieldName(headerIndex);
				if (fieldName.equalsIgnoreCase(fileFieldName)) {
					fieldPositionIndex = headerIndex;
				}
			}
		}

		return fieldPositionIndex;
	}

	CoordinateReferenceSystem getInputCrs() throws IOException {
		return readSourceSchema().getCoordinateReferenceSystem();
	}

	SimpleFeatureType readSourceSchema() throws MalformedURLException, IOException {
		DataStore inputStore = null;
		SimpleFeatureType sourceSchema = null;
		try {
			inputStore = DataStoreFinder.getDataStore(
					Collections.singletonMap("url", shapeFiles.getFileNames().get(ShpFileType.SHP))
			);

			String[] typeNames = inputStore.getTypeNames();
			SimpleFeatureSource featureSource = inputStore.getFeatureSource(typeNames[0]);
			sourceSchema = featureSource.getSchema();
		} finally {
			if (inputStore != null) {
				inputStore.dispose();
			}
		}
		return sourceSchema;
	}

	public abstract int processShape(ShapeAndAttributes sap, int segmentId, long recordId, FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter) throws IOException, MismatchedDimensionException, TransformException, FactoryException;

	public void writePoint(Point p, DbaseFileReader.Row row, double uncy, long recordId, int segmentId, FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter) throws IOException {

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
		SimpleFeatureType sourceSchema = readSourceSchema();

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
		outputFeatureType = typeBuilder.buildFeatureType();

		LOGGER.debug("Output feature type is {}", outputFeatureType);

		return outputFeatureType;
	}

	abstract FeatureWriter<SimpleFeatureType, SimpleFeature> createFeatureWriter(Transaction tx, String typeName) throws IOException;

	public abstract int explode() throws IOException;

	protected IterableShapefileReader initReader() throws ShapefileException, IOException {
		IterableShapefileReader shapefileReader = new IterableShapefileReader(shapeFiles, shapeHandler);

		dbfHdr = shapefileReader.getDbfHeader();
		uncertaintyIdIdx = locateField(dbfHdr, uncyColumnName);
		segmentIdx = locateField(dbfHdr, Constants.SEGMENT_ID_ATTR);
		sourceCRS = getInputCrs();

		return shapefileReader;
	}

	public void setOutputFeatureType(SimpleFeatureType outputFeatureType) {
		this.outputFeatureType = outputFeatureType;
	}

	@Override
	public void close() throws Exception {
		if (this.shapeFiles != null) {
			this.shapeFiles.dispose();
			this.shapeFiles.delete();
		}
	}

	/**
	 *
	 * @param string
	 */
	protected final void updateProcessInformation(String string) {
		if (this.process != null) {
			this.process.addProcessInformation(string);
		}
	}

}
