package gov.usgs.cida.dsas.shoreline.file;

import gov.usgs.cida.dsas.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.dsas.dao.shoreline.ShorelineFileDAO;
import gov.usgs.cida.dsas.featureType.file.FeatureType;
import gov.usgs.cida.dsas.model.DSASProcess;
import gov.usgs.cida.dsas.featureTypeFile.exception.LidarFileFormatException;
import gov.usgs.cida.dsas.featureTypeFile.exception.ShorelineFileFormatException;
import gov.usgs.cida.dsas.service.util.LidarFileUtils;
import gov.usgs.cida.dsas.utilities.features.Constants;
import gov.usgs.cida.owsutils.commons.io.FileHelper;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.naming.NamingException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.geotools.feature.SchemaException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class ShorelineLidarFile extends ShorelineFile {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ShorelineLidarFile.class);
	private static final String[] REQ_FILES = new String[]{CSV, PRJ};
	private static final String[] OPTIONAL_FILES = new String[]{null};
	private static final String[] FILE_PARTS = new String[]{
		PRJ,
		CSV
	};
	private static final String[] EXPECTED_COLUMNS = new String[]{Constants.DB_DATE_ATTR, Constants.UNCY_ATTR, Constants.MHW_ATTR};

	/**
	 * 
	 * @param featureTypeFileLocation
	 * @param gsHandler
	 * @param dao ShorelineFileDAO
	 */
	public ShorelineLidarFile(File featureTypeFileLocation, GeoserverDAO gsHandler, ShorelineFileDAO dao) {
		this(featureTypeFileLocation,gsHandler, dao, null);
	}
	
	public ShorelineLidarFile(File featureTypeFileLocation, GeoserverDAO gsHandler, ShorelineFileDAO dao, DSASProcess process) {
		init(featureTypeFileLocation, gsHandler, dao,  process);
	}
	
	//set up the work structures
	private void init(File featureTypeFileLocation, GeoserverDAO gsHandler, ShorelineFileDAO dao,  DSASProcess process) {
		this.featureTypeExplodedZipFileLocation = featureTypeFileLocation;
		this.geoserverHandler = gsHandler;
		this.dao = dao;
		this.fileMap = new HashMap<>(FILE_PARTS.length);
		updateFileMapWithDirFile(featureTypeFileLocation, FILE_PARTS);
		if (process != null)
			setDSASProcess(process);
		this.type = FeatureType.SHORELINE_LIDAR;
	}
	
		/**
	 * A lidar file has csv files, a prj file, and NO shp files.
	 *
	 * returns true if a structurally correct lidar zip file is found
	 *
	 * @param lidarZipFile
	 * @throws
	 * gov.usgs.cida.dsas.featureTypeFile.exception.LidarFileFormatException
	 * @throws IOException
	 */
	public static void validate(File lidarZipFile) throws LidarFileFormatException, IOException {

			File[] csvfiles = FileHelper.listFiles(lidarZipFile, (new String[]{CSV}), false).toArray(new File[0]);
			if (csvfiles.length == 0 || csvfiles.length > 1) {
				throw new LidarFileFormatException("Lidar archive needs to contain one csv file");
			}
			File[] prjfiles = FileHelper.listFiles(lidarZipFile, (new String[]{PRJ}), false).toArray(new File[0]);
			if (prjfiles.length == 0 || prjfiles.length > 1) {
				throw new LidarFileFormatException("Lidar archive needs to contain one prj file");
			}
			File[] shpfiles = FileHelper.listFiles(lidarZipFile, (new String[]{SHP}), false).toArray(new File[0]);
			if (shpfiles.length != 0) {
				throw new LidarFileFormatException("Lidar archive cannot contain an shp file");
			}

			LOGGER.debug("File {} validated as Lidar file", lidarZipFile.getAbsolutePath());
	}	
	
	@Override
	public String getEPSGCode() {
		String epsg = null;
		String errorMsg = "Unable to retrieve epsg code from prj file.";
		try {
			epsg = LidarFileUtils.getEPSGCode(fileMap.get(PRJ));
		} catch (IOException ex) {
			LOGGER.error(errorMsg, ex);
			throw new RuntimeException(errorMsg, ex);
		} catch (FactoryException ex) {
			throw new RuntimeException(errorMsg, ex);
		}
		return epsg;
	}

	@Override
	public String importToDatabase(Map<String, String> columns, String workspace) throws ShorelineFileFormatException, SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException {
		return dao.importToDatabase(fileMap.get(CSV), columns, workspace, getEPSGCode());
	}

	@Override
	public String[] getColumns() {
		String[] result = Arrays.copyOf(EXPECTED_COLUMNS, EXPECTED_COLUMNS.length);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ShorelineLidarFile)) {
			return false;
		}
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}
	
	@Override
	public List<File> getRequiredFiles() {
		Collection<File> requiredFiles = FileUtils.listFiles(this.featureTypeExplodedZipFileLocation, REQ_FILES, false);
		return new ArrayList<>(requiredFiles);
	}

	@Override
	public List<File> getOptionalFiles() {
		Collection<File> optFiles = FileUtils.listFiles(this.featureTypeExplodedZipFileLocation, OPTIONAL_FILES, false);
		return new ArrayList<>(optFiles);
	}
}
