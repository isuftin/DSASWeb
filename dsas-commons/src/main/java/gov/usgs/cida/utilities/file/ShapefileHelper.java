package gov.usgs.cida.utilities.file;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.referencing.CRS;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class ShapefileHelper extends FileHelper {

	private static org.slf4j.Logger log = LoggerFactory.getLogger(ShapefileHelper.class);

	public static BoundingBox getBoundingBoxFromShapefile(File shapefileZip) throws IOException, TransformException, FactoryException {
		return getBoundingBoxFromShapefile(shapefileZip, CRS.decode("EPSG:4326"));
	}

	public static BoundingBox getBoundingBoxFromShapefile(File shapefileZip, CoordinateReferenceSystem crs) throws IOException, TransformException, FactoryException {
		File tempShapeFile = null;
		File tempDir = null;
		ShapefileDataStore ds = null;
		try {
			tempDir = Files.createTempDirectory("temp-shapefile-dir").toFile();
			tempShapeFile = Files.createTempFile("tempshapefile", ".zip").toFile();
			tempShapeFile.deleteOnExit();
			tempDir.deleteOnExit();
			FileUtils.copyFile(shapefileZip, tempShapeFile);
			FileHelper.unzipFile(tempDir.getAbsolutePath(), tempShapeFile);

			File[] prjFile = tempDir.listFiles((File dir, String name) -> name.endsWith("prj"));
			if (prjFile.length < 1) {
				throw new IOException("Projection file not found within zip.");
			} else if (prjFile.length > 1) {
				throw new IOException("Multiple projection files found within zip.");
			}
			
			File[] shpFile = tempDir.listFiles((File dir, String name) -> name.endsWith("shp"));
			if (shpFile.length < 1) {
				throw new IOException("Shapefile not found within zip");
			} else if (shpFile.length > 1) {
				throw new IOException("Multiple shapefiles found within zip");
			}

			URL shapefileUrl = shpFile[0].toURI().toURL();
			Map<String, URL> urlMap = new HashMap<>();
			urlMap.put("url", shapefileUrl);

			ds = (ShapefileDataStore) DataStoreFinder.getDataStore(urlMap);
			return ds
					.getFeatureSource()
					.getBounds()
					.transform(crs, true);
		} finally {
			FileUtils.deleteQuietly(tempDir);
			FileUtils.deleteQuietly(tempShapeFile);
			if (ds != null) {
				ds.dispose();
			}
		}
	}

}
