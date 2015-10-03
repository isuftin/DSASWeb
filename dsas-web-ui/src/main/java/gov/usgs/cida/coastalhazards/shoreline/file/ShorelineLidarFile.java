package gov.usgs.cida.coastalhazards.shoreline.file;

import gov.usgs.cida.coastalhazards.service.util.Property;
import gov.usgs.cida.coastalhazards.service.util.PropertyUtil;
import gov.usgs.cida.coastalhazards.dao.shoreline.ShorelineFileDAO;
import gov.usgs.cida.coastalhazards.shoreline.exception.LidarFileFormatException;
import gov.usgs.cida.coastalhazards.shoreline.exception.ShorelineFileFormatException;
import gov.usgs.cida.owsutils.commons.io.FileHelper;
import gov.usgs.cida.coastalhazards.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.utilities.features.Constants;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import javax.naming.NamingException;
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
	private static final String[] fileParts = new String[]{
		PRJ,
		CSV
	};
	private static final String[] EXPECTED_COLUMNS = new String[]{Constants.DB_DATE_ATTR, Constants.UNCY_ATTR, Constants.MHW_ATTR};

	/**
	 * A lidar file has csv files, a prj file, and NO shp files.
	 *
	 * returns true if a structurally correct lidar zip file is found
	 *
	 * @param lidarZipFile
	 * @throws
	 * gov.usgs.cida.coastalhazards.shoreline.exception.LidarFileFormatException
	 * @throws IOException
	 */
	public static void validate(File lidarZipFile) throws LidarFileFormatException, IOException {
		File temporaryDirectory = new File(FileHelper.getTempDirectory(), UUID.randomUUID().toString() + "-deleteme");
		temporaryDirectory.deleteOnExit();
		
		try {
			if (!temporaryDirectory.mkdirs()) {
				throw new IOException("Could not create temporary directory (" + temporaryDirectory.getCanonicalPath() + ") for processing");
			}

			FileHelper.unzipFile(temporaryDirectory.getAbsolutePath(), lidarZipFile);

			File[] csvfiles = FileHelper.listFiles(temporaryDirectory, (new String[]{CSV}), false).toArray(new File[0]);
			if (csvfiles.length == 0 || csvfiles.length > 1) {
				throw new LidarFileFormatException("Lidar archive needs to contain one csv file");
			}
			File[] prjfiles = FileHelper.listFiles(temporaryDirectory, (new String[]{PRJ}), false).toArray(new File[0]);
			if (prjfiles.length == 0 || prjfiles.length > 1) {
				throw new LidarFileFormatException("Lidar archive needs to contain one prj file");
			}
			File[] shpfiles = FileHelper.listFiles(temporaryDirectory, (new String[]{SHP}), false).toArray(new File[0]);
			if (shpfiles.length != 0) {
				throw new LidarFileFormatException("Lidar archive cannot contain an shp file");
			}

			LOGGER.debug("File {} validated as Lidar file", lidarZipFile.getAbsolutePath());
		} finally {
			FileHelper.forceDelete(temporaryDirectory);
		}
	}

	/**
	 * 
	 * @param gsHandler
	 * @param dao
	 * @param workspace 
	 */
	public ShorelineLidarFile(GeoserverDAO gsHandler, ShorelineFileDAO dao, String workspace) {
		this.baseDirectory = new File(PropertyUtil.getProperty(Property.DIRECTORIES_BASE, System.getProperty("java.io.tmpdir")));
		this.uploadDirectory = new File(baseDirectory, PropertyUtil.getProperty(Property.DIRECTORIES_UPLOAD));
		this.workDirectory = new File(baseDirectory, PropertyUtil.getProperty(Property.DIRECTORIES_WORK));
		this.geoserverHandler = gsHandler;
		this.dao = dao;
		this.fileMap = new HashMap<>(fileParts.length);
		this.workspace = workspace;
	}

	@Override
	public String setDirectory(File directory) throws IOException {
		String fileToken = super.setDirectory(directory);
		updateFileMapWithDirFile(directory, fileParts);
		return fileToken;
	}

	@Override
	public String importToDatabase(Map<String, String> columns) throws ShorelineFileFormatException, SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException {
		return dao.importToDatabase(fileMap.get(CSV), columns, workspace, getEPSGCode());
	}

	@Override
	public String[] getColumns() {
		return Arrays.copyOf(EXPECTED_COLUMNS, EXPECTED_COLUMNS.length);
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

}
