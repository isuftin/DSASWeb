package gov.usgs.cida.coastalhazards.service.util;

import gov.usgs.cida.coastalhazards.shoreline.exception.LidarFileFormatException;
import gov.usgs.cida.owsutils.commons.communication.RequestResponse;
import gov.usgs.cida.owsutils.commons.io.FileHelper;
import gov.usgs.cida.owsutils.commons.io.exception.ShapefileFormatException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class ImportUtil {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ImportUtil.class);

	/**
	 *
	 * @param request
	 * @param defaultFileParam
	 * @param workDir
	 * @param overwrite
	 * @return
	 * @throws IOException
	 * @throws FileUploadException
	 * @throws ShapefileFormatException
	 * @throws LidarFileFormatException
	 */
	public static File saveShorelineFileFromRequest(HttpServletRequest request, String defaultFileParam, String workDir, boolean overwrite) throws IOException, FileUploadException, ShapefileFormatException, LidarFileFormatException {
		String filenameParam = defaultFileParam;
		String fnReqParam = request.getParameter("filename.param");
		String extension = ".shp";
		if (StringUtils.isNotBlank(fnReqParam)) {
			filenameParam = fnReqParam;
		}
		LOGGER.debug("Filename parameter set to: {}", filenameParam);
		LOGGER.debug("Cleaning file name.\nWas: {}", filenameParam);
		String zipFileName = cleanFileName(request.getParameter(filenameParam));
		LOGGER.debug("Is: {}", zipFileName);
		if (filenameParam.equals(zipFileName)) {
			LOGGER.debug("(No change)");
		}
		String shorelineFileName = zipFileName.substring(0, zipFileName.lastIndexOf("."));
		File saveDirectory = new File(workDir + File.separator + shorelineFileName);
		if (!saveDirectory.exists()) {
			FileUtils.forceMkdir(saveDirectory);
		}
		if (overwrite) {
			try {
				FileUtils.cleanDirectory(saveDirectory);
			} catch (IOException ex) {
				LOGGER.debug("Could not clean save directory at " + saveDirectory.getAbsolutePath(), ex);
			}
			LOGGER.debug("File already existed on server. Deleted before re-saving.");
		}
		File shorelineZipFile = new File(saveDirectory, zipFileName);
		LOGGER.debug("Temporary file set to {}", shorelineZipFile.getAbsolutePath());
		try {
			RequestResponse.saveFileFromRequest(request, shorelineZipFile, filenameParam);
			LOGGER.debug("Shoreline saved");
			FileHelper.flattenZipFile(shorelineZipFile.getAbsolutePath());
			LOGGER.debug("Shoreline zip structure flattened");

			try {
				LidarFileUtils.validateLidarFileZip(shorelineZipFile);
				LOGGER.debug("Lidar file verified");
				extension = ".csv";
			} catch (LidarFileFormatException ex) {
				LOGGER.debug("failed lidar validation, try shapefile");
				FileHelper.validateShapefileZip(shorelineZipFile);
				LOGGER.debug("Shapefile verified");
				extension = ".shp";
			}
			gov.usgs.cida.utilities.file.FileHelper.unzipFile(saveDirectory.getAbsolutePath(), shorelineZipFile);
			LOGGER.debug("Shoreline unzipped");
			if (shorelineZipFile.delete()) {
				LOGGER.debug("Deleted zipped shapefile");
			} else {
				LOGGER.debug("Could not delete shoreline zip at {}", shorelineZipFile.getAbsolutePath());
			}
			Collection<File> shapeFileParts = FileUtils.listFiles(saveDirectory, HiddenFileFilter.VISIBLE, null);
			for (File file : shapeFileParts) {
				String oldFilename = file.getName();
				String newFilename = shorelineFileName + "." + FilenameUtils.getExtension(file.getName());
				gov.usgs.cida.utilities.file.FileHelper.renameFile(file, newFilename);
				LOGGER.debug("Renamed {} to {}", oldFilename, newFilename);
			}
		} catch (FileUploadException | IOException ex) {
			FileUtils.deleteQuietly(saveDirectory);
			throw ex;
		}
		return new File(saveDirectory, shorelineFileName + extension);
	}

	/**
	 *
	 * @param input
	 * @return
	 */
	public static String cleanFileName(String input) {
		String updated = input;
		if (input.substring(0, 1).matches("[0-9]")) {
			updated = "_" + input;
		}
		char[] inputArr = updated.toCharArray();
		for (int cInd = 0; cInd < inputArr.length; cInd++) {
			if (!Character.isLetterOrDigit(inputArr[cInd]) && !(inputArr[cInd] == '.')) {
				inputArr[cInd] = '_';
			}
		}
		return String.valueOf(inputArr);
	}

	private ImportUtil() {
	}

}
