package gov.usgs.cida.dsas.uncy;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import gov.usgs.cida.owsutils.commons.shapefile.utils.IterableShapefileReader;
import gov.usgs.cida.owsutils.commons.shapefile.utils.PointIterator;
import gov.usgs.cida.owsutils.commons.shapefile.utils.ShapeAndAttributes;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
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
	
	public ShapefileOutputXploder(Map<String, Object> config) throws IOException {
		super(config);
		
		this.outputFileName = (String) config.get(OUTPUT_FILENAME_PARAM);
	}
	
	@Override
	FeatureWriter<SimpleFeatureType, SimpleFeature> createFeatureWriter(Transaction tx, String typeName) throws IOException {
		SimpleFeatureType outputFeatureType = createOutputFeatureType();
		
		String shpExtension = ".shp";
		String _outputFileName = this.outputFileName;
		if (!_outputFileName.endsWith(shpExtension)) {
			_outputFileName += PTS_SUFFIX + shpExtension;
		}
		setOutputFile(new File(this.outputFileName));

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
	
	@Override
	public int explode() throws IOException {
		int ptTotal = 0;
		try (IterableShapefileReader rdr = initReader();
				Transaction tx = new DefaultTransaction();
				FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter = createFeatureWriter(tx, null)) {

			LOGGER.debug("Input files from {}\n{}",
					shapeFiles.getTypeName(),
					String.join(",",
							shapeFiles.getFileNames().values().toArray(new String[shapeFiles.getFileNames().size()])
					)
			);

			int shpCt = 0;
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
	
	@Override
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

	public File getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}
	
}
