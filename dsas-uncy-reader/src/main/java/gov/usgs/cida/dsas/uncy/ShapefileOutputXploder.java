package gov.usgs.cida.dsas.uncy;

import com.vividsolutions.jts.geom.Point;
import gov.usgs.cida.utilities.features.Constants;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class ShapefileOutputXploder extends Xploder {
	
	public static final String OUTPUT_FILENAME_PARAM = "outputFilename";
	private static final Logger LOGGER = LoggerFactory.getLogger(ShapefileOutputXploder.class);
	public static final String PTS_SUFFIX = "_pts";
	private final String outputFileName;
	private File outputFile;
	
	public ShapefileOutputXploder(Map<String, String> config) throws IOException {
		
		if (config == null) {
			throw new NullPointerException("Configuration map for ShapefileOutputExploder may not be null");
		}
		
		String[] requiredConfigs = new String[] {
			UNCERTAINTY_COLUMN_PARAM,
			INPUT_FILENAME_PARAM
		};
		
		for (String requiredConfig : requiredConfigs) {
			if (!config.containsKey(requiredConfig)) {
				throw new IllegalArgumentException(String.format("Configuration map for ShapefileOutputExploder must include parameter %s", requiredConfig));
			}
			if (StringUtils.isBlank(config.get(requiredConfig))) {
				throw new IllegalArgumentException(String.format("Configuration map for ShapefileOutputExploder must include value for parameter %s", requiredConfig));
			}
		}
		
		this.outputFileName = config.get(OUTPUT_FILENAME_PARAM);
		this.uncyColumnName = config.get(UNCERTAINTY_COLUMN_PARAM);
		this.inputFileName = config.get(INPUT_FILENAME_PARAM);
	}
	
	@Override
	FeatureWriter<SimpleFeatureType, SimpleFeature> createFeatureWriter(Transaction tx) throws IOException {
		// read input to get attributes
		SimpleFeatureType sourceSchema = readSourceSchema(inputFileName);

		// duplicate input schema, except replace geometry with Point
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName(sourceSchema.getName() + PTS_SUFFIX);
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
		String shpExtension = ".shp";
		String _outputFileName = StringUtils.isNotBlank(this.outputFileName) ? 
				this.outputFileName : 
				inputFileName + PTS_SUFFIX + shpExtension;
		if (!_outputFileName.endsWith(shpExtension)) {
			_outputFileName += shpExtension;
		}
		setOutputFile(new File(_outputFileName));

		Map<String, Serializable> connect = new HashMap<>();
		connect.put("url", getOutputFile().toURI().toURL());
		connect.put("create spatial index", Boolean.TRUE);

		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		ShapefileDataStore outputStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(connect);

		outputStore.createSchema(outputFeatureType);
		FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter = outputStore.getFeatureWriterAppend(tx);
		
		LOGGER.info("Will write {}", getOutputFile().getAbsolutePath());
		
		return featureWriter;
	}

	public File getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}
	
}
