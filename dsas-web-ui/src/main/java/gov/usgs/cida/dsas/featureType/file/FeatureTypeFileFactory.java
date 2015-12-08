package gov.usgs.cida.dsas.featureType.file;

import gov.usgs.cida.dsas.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.dsas.dao.pdb.PdbDAO;
import gov.usgs.cida.dsas.dao.shoreline.ShorelineLidarFileDAO;
import gov.usgs.cida.dsas.dao.shoreline.ShorelineShapefileDAO;
import gov.usgs.cida.dsas.pdb.file.PdbFile;
import gov.usgs.cida.dsas.rest.service.shapefile.ShapefileResource;
import gov.usgs.cida.dsas.featureTypeFile.exception.FeatureTypeFileException;
import gov.usgs.cida.dsas.featureTypeFile.exception.LidarFileFormatException;
import gov.usgs.cida.dsas.featureTypeFile.exception.ShapefileException;
import gov.usgs.cida.dsas.featureTypeFile.exception.ShorelineFileFormatException;
import gov.usgs.cida.dsas.shoreline.file.ShorelineFile;
import gov.usgs.cida.dsas.shoreline.file.ShorelineLidarFile;
import gov.usgs.cida.dsas.shoreline.file.ShorelineShapefile;
import gov.usgs.cida.dsas.utilities.properties.Property;
import gov.usgs.cida.dsas.utilities.properties.PropertyUtil;
import gov.usgs.cida.owsutils.commons.io.FileHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.MessageFormat;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hsqldb.types.Charset;
import org.slf4j.LoggerFactory;

/**
 *
 * @author smlarson
 */
public class FeatureTypeFileFactory {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ShapefileResource.class);
	FeatureTypeFile ftf = null;

	private static final Integer DEFAULT_MAX_FILE_SIZE = Integer.MAX_VALUE;  
	private static final File BASE_DIRECTORY = new File(PropertyUtil.getProperty(Property.DIRECTORIES_BASE, FileUtils.getTempDirectory().getAbsolutePath()));
	private static final File UPLOAD_DIRECTORY = new File(BASE_DIRECTORY, PropertyUtil.getProperty(Property.DIRECTORIES_UPLOAD));
	private static final File WORK_DIRECTORY = new File(BASE_DIRECTORY, PropertyUtil.getProperty(Property.DIRECTORIES_WORK));

	/**
	 *
	 * @param zipFile The uploaded zip file.
	 * @return FeatureTypeFile
	 */
	private FeatureTypeFile createFeatureTypeFile(File zipFile) throws IOException, FeatureTypeFileException {
		performGenericValidation(zipFile);

		//get zip name. verify name exists. if it doesn't use a default from the props file. 
		String zipName = getZipName(zipFile);

		//clean the zip name (?dont do if you have used the default as that should comply with rules)
		String cleanedZipName = cleanFileName(zipName);

		//create an empty zip file with the upload directory path.
		File newZip = new File(UPLOAD_DIRECTORY, cleanedZipName);

		//check to see if the cleaned zip already exists, attempt delete if it does before next step
		if (newZip.exists()) {
			FileUtils.deleteQuietly(newZip);
		}

		//copy the contents of the passed in zip into the new clean zip with our directory path
		FileUtils.copyFile(zipFile, newZip);

		//rename the zip contents to be the same as the cleaned zip name keeping the .ext intact
		File dirToContents = renameZipFileContents(newZip);
		
		
		//flatten the zip. It's expected that all the contents of the zip are in the same root file.
		FileHelper.flattenZipFile(zipFile);

		//validate the zip which will determine which FeatureType sub-class it is 
		FeatureType type = performTypeValidation(zipFile);

		//create the GeoserverDao with the known user and pwd
		GeoserverDAO geoserverHandler = getGeoserverDao();

		//create the correct sub-class using the exploded directory path as part of the constructor
		FeatureTypeFile result = createTypeFile(zipFile, type, geoserverHandler);

		return result;
	}

	/**
	 *
	 * @param zipFileInputStream The input stream of the uploaded zip file.
	 * Assumes the zip name has been cleaned prior to the request.
	 * @return FeatureTypeFile
	 */
	public FeatureTypeFile createFeatureTypeFile(InputStream zipFileInputStream) throws IOException, FeatureTypeFileException {
		//Since an inputstream is sent, its assumed that the client has already cleaned the zip name (replaced numeric prefixes, non-alpha, non-numerics with underscore) 
		File copiedZip = null;
		copiedZip = Files.createTempFile(UPLOAD_DIRECTORY.toPath(), null, ".zip").toFile();
		IOUtils.copyLarge(zipFileInputStream, new FileOutputStream(copiedZip));

		return createFeatureTypeFile(copiedZip);
	}

	private void performGenericValidation(File zipFile) throws FeatureTypeFileException, IOException {
		// general validation for all types
		if (zipFile == null || !zipFile.exists() || !FileHelper.isZipFile(zipFile)) {
			throw new FeatureTypeFileException("An error occurred attempting to validate the file");
		} else if (zipFile.length() > getMaxFileSize()) {
			throw new FeatureTypeFileException(MessageFormat.format("File maximum size: {0}", getMaxFileSize()));
		}
	}

	protected Integer getMaxFileSize() {
		Integer maxFSize = DEFAULT_MAX_FILE_SIZE;
		String mfsJndiProp = PropertyUtil.getProperty(Property.FILE_UPLOAD_MAX_SIZE);
		if (StringUtils.isNotBlank(mfsJndiProp)) {
			maxFSize = Integer.parseInt(mfsJndiProp);
		}
		return maxFSize;

	}

	// may not need this method - believe it may be done prior to the request coming in as a file stream.
	protected String cleanFileName(String input) {
		String updated = input;

		// Test the first character and if numeric, prepend with underscore
		if (input.substring(0, 1).matches("[0-9]")) {
			updated = "_" + input;
		}

		// Test the rest of the characters and replace anything that's not a 
		// letter, digit or period with an underscore
		char[] inputArr = updated.toCharArray();
		for (int cInd = 0; cInd < inputArr.length; cInd++) {
			if (!Character.isLetterOrDigit(inputArr[cInd]) && !(inputArr[cInd] == '.')) {
				inputArr[cInd] = '_';
			}
		}
		return String.valueOf(inputArr);
	}

	public String getZipName(File zipFile){
		String filenameParam = PropertyUtil.getProperty(Property.FILE_UPLOAD_FILENAME_PARAM);
		String fnReqParam = "getNameFromUserRequest"; //request.getParameter(FILENAME_PARAM); // this was the previous way of doing this
		if (StringUtils.isNotBlank(fnReqParam)) {
			filenameParam = fnReqParam;
		}
		return filenameParam;
	}
			
	//takes the zip file cleaned name and replaces the names of the files in it with the cleaned name plus the files extension. Returns the path to the exploded renamed contents.
	public File renameZipFileContents(File zipFile) throws IOException {
		File workLocation = createWorkLocationForZip(zipFile);
		FileHelper.unzipFile(workLocation.getAbsolutePath(), zipFile);
		FileHelper.renameDirectoryContents(workLocation);
		return workLocation;
	}

	private File createWorkLocationForZip(File zipFile) throws IOException {
		String fileName = FilenameUtils.getBaseName(zipFile.getName());
		File fileWorkDirectory = new File(WORK_DIRECTORY, fileName);
		if (fileWorkDirectory.exists()) {
			try {
				FileUtils.cleanDirectory(fileWorkDirectory);
			} catch (IOException ex) {
				LOGGER.debug("Could not clean work directory at " + fileWorkDirectory.getAbsolutePath(), ex);
			}
		}
		FileUtils.forceMkdir(fileWorkDirectory);
		return fileWorkDirectory;
	}

	public FeatureType performTypeValidation(File zipFile) throws FeatureTypeFileException {

		FeatureType result = FeatureType.OTHER;
		if (isValidPdbShapeType(zipFile)) {
			result = FeatureType.PDB;
		} else if (isValidShorelineLidarType(zipFile)) {
			result = FeatureType.LIDAR;
		} else if (isValidShorelineShapeType(zipFile)) {
			result = FeatureType.SHAPEFILE;
		} else {
			FileUtils.deleteQuietly(zipFile);
			throw new FeatureTypeFileException("Zip file is not valid type.");
		}
		return result;
	}

	protected boolean isValidPdbShapeType(File zip) {
		boolean result = false;
		FeatureType type = FeatureType.OTHER;
		try {
			PdbFile.validate(zip);
			LOGGER.debug("PdbFile verified");
			result = true;
		} catch (ShapefileException ex) {
			LOGGER.info("Failed PdbFile validation.", ex);
			result = false;
		}
		return result;
	}

	protected boolean isValidShorelineShapeType(File zip) {
		boolean result = false;
		FeatureType type = FeatureType.OTHER;
		try {
			ShorelineShapefile.validate(zip);
			LOGGER.debug("ShorelineShapefile verified");
			result = true;
		} catch (ShorelineFileFormatException | IOException ex) {
			LOGGER.info("Failed ShorelineShapefile validation.", ex);
			result = false;
		}
		return result;
	}

	protected boolean isValidShorelineLidarType(File zip) {
		boolean result = false;
		FeatureType type = FeatureType.OTHER;
		try {
			ShorelineLidarFile.validate(zip);
			LOGGER.debug("Lidar file verified");
			result = true;
		} catch (LidarFileFormatException | IOException ex) {
			LOGGER.info("Failed lidar validation.", ex);
			result = false;
		}
		return result;
	}

	private GeoserverDAO getGeoserverDao() {
		String geoserverEndpoint = PropertyUtil.getProperty(Property.GEOSERVER_ENDPOINT);
		String geoserverUsername = PropertyUtil.getProperty(Property.GEOSERVER_USERNAME);
		String geoserverPassword = PropertyUtil.getProperty(Property.GEOSERVER_PASSWORD);
		GeoserverDAO geoserverHandler = new GeoserverDAO(geoserverEndpoint, geoserverUsername, geoserverPassword);

		return geoserverHandler;
	}

	private FeatureTypeFile createTypeFile(File zipFile, FeatureType type, GeoserverDAO geoserverHandler) throws IOException {
		FeatureTypeFile result = null;

		if (type == FeatureType.SHAPEFILE) {
			new ShorelineShapefile(geoserverHandler, new ShorelineShapefileDAO());
		} else if (type == FeatureType.LIDAR) {
			new ShorelineShapefile(geoserverHandler, new ShorelineLidarFileDAO());
		} else if (type == FeatureType.PDB) {
			new PdbFile(zipFile, geoserverHandler, new PdbDAO());  // constructor takes the path to the exploded zip TODO TODO!! consolidate on constructors ...setDirectory places it in a token too
		}
//		switch (type){
//			case : FeatureType.SHAPEFILE
//			result = new ShorelineShapefile(geoserverHandler, new ShorelineShapefileDAO());
//			break;

		return result;
	}
}
