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
import org.slf4j.LoggerFactory;

/**
 *
 * @author smlarson
 */
public class FeatureTypeFileFactory {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ShapefileResource.class);
//	FeatureTypeFile ftf = null;

	private static final Integer DEFAULT_MAX_FILE_SIZE = Integer.MAX_VALUE;
	private static final File BASE_DIRECTORY = new File(PropertyUtil.getProperty(Property.DIRECTORIES_BASE, FileUtils.getTempDirectory().getAbsolutePath()));
	private static final File UPLOAD_DIRECTORY = new File(BASE_DIRECTORY, PropertyUtil.getProperty(Property.DIRECTORIES_UPLOAD));
	private static final File WORK_DIRECTORY = new File(BASE_DIRECTORY, PropertyUtil.getProperty(Property.DIRECTORIES_WORK));

	/**
	 *
	 * @param zipFile The uploaded zip file.
	 * @return FeatureTypeFile
	 */
	private FeatureTypeFile createFeatureTypeFile(File zipFile, FeatureType type) throws IOException, FeatureTypeFileException {

		performGenericValidation(zipFile);

//		//get zip name. verify name exists. if it doesn't use a default from the props file. 
//		String zipName = getZipName(zipFile);
//
//		//clean the zip name (?dont do if you have used the default as that should comply with rules)
//		String cleanedZipName = cleanFileName(zipName);
//
//		//create an empty zip file with the upload directory path.
//		File newZip = new File(UPLOAD_DIRECTORY, cleanedZipName);
//
//		//check to see if the cleaned zip already exists, attempt delete if it does before next step
//		if (newZip.exists()) {
//			FileUtils.deleteQuietly(newZip);
//		}
//
//		//copy the contents of the passed in zip into the new clean zip with our directory path
//		FileUtils.copyFile(zipFile, newZip);
//
//		//rename the zip contents to be the same as the cleaned zip name keeping the .ext intact
//		File dirToContents = renameZipFileContents(newZip);
//		
		//flatten the zip. It's expected that all the contents of the zip are in the same root file.
		FileHelper.flattenZipFile(zipFile);

		//rename the zip contents to be the same as the zip's name keeping the .ext intact
		File dirToContents = renameZipFileContents(zipFile);

		//validate the zip which will determine which FeatureType sub-class it is 
		FeatureType typed = performTypeValidation(dirToContents, type);

		//create the GeoserverDao with the known user and pwd
		GeoserverDAO geoserverHandler = getGeoserverDao();

		//create the correct sub-class using the exploded directory path as part of the constructor
		FeatureTypeFile result = createTypeFile(dirToContents, typed, geoserverHandler);

		return result;
	}

	/**
	 *
	 * @param zipFileInputStream The input stream of the uploaded zip file.
	 * Assumes the zip name has been cleaned prior to the request.
	 * @param type
	 * @return FeatureTypeFile
	 */
	public FeatureTypeFile createFeatureTypeFile(InputStream zipFileInputStream, FeatureType type) throws IOException, FeatureTypeFileException {
		//Since an inputstream is sent, there is no need to clean the zip name (replaced numeric prefixes, non-alpha, non-numerics with underscore) as was done 
		File copiedZip = Files.createTempFile(UPLOAD_DIRECTORY.toPath(), null, ".zip").toFile();
		IOUtils.copyLarge(zipFileInputStream, new FileOutputStream(copiedZip));

		return createFeatureTypeFile(copiedZip, type);
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

//	// may not need this method - believe it may be done prior to the request coming in as a file stream.
//	protected String cleanFileName(String input) {
//		String updated = input;
//
//		// Test the first character and if numeric, prepend with underscore
//		if (input.substring(0, 1).matches("[0-9]")) {
//			updated = "_" + input;
//		}
//
//		// Test the rest of the characters and replace anything that's not a 
//		// letter, digit or period with an underscore
//		char[] inputArr = updated.toCharArray();
//		for (int cInd = 0; cInd < inputArr.length; cInd++) {
//			if (!Character.isLetterOrDigit(inputArr[cInd]) && !(inputArr[cInd] == '.')) {
//				inputArr[cInd] = '_';
//			}
//		}
//		return String.valueOf(inputArr);
//	}
//	public String getZipName(File zipFile){
//		String filenameParam = PropertyUtil.getProperty(Property.FILE_UPLOAD_FILENAME_PARAM);
//		String fnReqParam = "getNameFromUserRequest"; //request.getParameter(FILENAME_PARAM); // this was the previous way of doing this
//		if (StringUtils.isNotBlank(fnReqParam)) {
//			filenameParam = fnReqParam;
//		}
//		return filenameParam;
//	}
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

	public FeatureType performTypeValidation(File zipFile, FeatureType type) throws FeatureTypeFileException, IOException {

		FeatureType result = FeatureType.OTHER;
		switch (type) {
			case PDB:
				if (isValidPdbShapeType(zipFile))
					{result = type;}
				break;
			case SHORELINE:
				result = determineShorelineType(zipFile);  //determines the specific file type and validates -refactor this in Feb
				break;
			case OTHER: //this should get hit if any of the other processes set the type to OTHER ie comes in as pdb type but is not valid, result would remain as OTHER and the default case would occur.
			default:
				FileUtils.deleteQuietly(zipFile);
				throw new IOException("zip file is not valid.");
		}
		return result;
	}

	protected FeatureType determineShorelineType(File zipFile) {
		FeatureType type = FeatureType.OTHER;
		try {
			ShorelineLidarFile.validate(zipFile);
			LOGGER.debug("Lidar file verified");
			type = FeatureType.SHORELINE_LIDAR;
		} catch (LidarFileFormatException | IOException ex) {
			LOGGER.info("Failed lidar validation, try shapefile", ex);
			try {
				ShorelineShapefile.validate(zipFile);
				LOGGER.debug("Shapefile verified");
				type = FeatureType.SHORELINE_SHAPE;
			} catch (ShorelineFileFormatException | IOException ex1) {
				LOGGER.info("Failed shapefile validation", ex1);
			}
		}
		return type;
	}

	protected boolean isValidPdbShapeType(File zip) throws FeatureTypeFileException {
		boolean result = false;
		try {
			PdbFile.validate(zip);
			LOGGER.debug("PdbFile verified");
			result = true;
		} catch (ShapefileException ex) {
			LOGGER.info("Failed PdbFile validation.", ex);
			result = false;
			throw new FeatureTypeFileException("File has failed Pdb validation.");
		}
		return result;
	}

	protected boolean isValidShorelineShapeType(File zip) {
		boolean result = false;
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

	private FeatureTypeFile createTypeFile(File explodedZipDir, FeatureType type, GeoserverDAO geoserverHandler) throws IOException, FeatureTypeFileException {
		FeatureTypeFile result = null;

//		if (type == FeatureType.SHORELINE_SHAPE) {
//			new ShorelineShapefile(geoserverHandler, new ShorelineShapefileDAO());
//		} else if (type == FeatureType.SHORELINE_LIDAR) {
//			new ShorelineShapefile(geoserverHandler, new ShorelineLidarFileDAO());
//		} else if (type == FeatureType.PDB) {
//			result = new PdbFile(explodedZipDir, geoserverHandler, new PdbDAO());
//			result.setType(type);
//		}
		switch (type){
			case SHORELINE_SHAPE:
				new ShorelineShapefile(geoserverHandler, new ShorelineShapefileDAO());  //add the result = after the refactor of the Shoreline to descend from FeatureTypeFile
				break;
			case SHORELINE_LIDAR:
				new ShorelineShapefile(geoserverHandler, new ShorelineLidarFileDAO());
				break;
			case PDB:
				result = new PdbFile(explodedZipDir, geoserverHandler, new PdbDAO());
				break;
			case OTHER:
			default:
				LOGGER.error("Failed to create FeatureTypeFile of assumed type: "+ type.name());
				throw new FeatureTypeFileException("Unable to create instantiate FeatureTypeFile with zip.");
		}
		
		return result;
	}
}
