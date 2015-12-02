package gov.usgs.cida.dsas.uncy;

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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
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
		super(config);
		
		this.outputFileName = config.get(OUTPUT_FILENAME_PARAM);
	}
	
	@Override
	FeatureWriter<SimpleFeatureType, SimpleFeature> createFeatureWriter(Transaction tx) throws IOException {
		SimpleFeatureType outputFeatureType = createOutputFeatureType();
		
		String shpExtension = ".shp";
		String _outputFileName = StringUtils.isNotBlank(this.outputFileName) ? 
				this.outputFileName : 
				shapeFiles.getTypeName() + PTS_SUFFIX + shpExtension;
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
