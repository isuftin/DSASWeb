package gov.usgs.cida.dsas.shoreline.file;

import gov.usgs.cida.dsas.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.dsas.dao.shoreline.ShorelineFileDAO;
import gov.usgs.cida.dsas.featureType.file.FeatureType;
import gov.usgs.cida.dsas.featureTypeFile.exception.ShapefileException;
import gov.usgs.cida.dsas.model.DSASProcess;
import gov.usgs.cida.dsas.utilities.properties.Property;
import gov.usgs.cida.dsas.utilities.properties.PropertyUtil;
import gov.usgs.cida.dsas.featureTypeFile.exception.ShorelineFileFormatException;
import gov.usgs.cida.dsas.service.util.ShapeFileUtil;
import gov.usgs.cida.owsutils.commons.shapefile.utils.IterableShapefileReader;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.naming.NamingException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.feature.SchemaException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class ShorelineShapefile extends ShorelineFile {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ShorelineShapefile.class);
	private static final String[] REQ_FILES = new String[]{SHP, SHX, DBF, PRJ};
	private static final String[] OPTIONAL_FILES = new String[]{SHP_XML};
	private static final String[] FILE_PARTS = new String[]{
		SHP,
		SHX,
		DBF,
		PRJ,
		FBX,
		SBX,
		AIH,
		IXS,
		MXS,
		ATX,
		CST,
		SHP_XML,
		CPG};

	public static void validate(File zipFileDir) throws ShorelineFileFormatException, IOException {
		
			//		ZipFile zFile = new ZipFile(zipFile);
//		Enumeration<? extends ZipEntry> entries = zFile.entries();
//		List<String> extensions = new ArrayList<>(zFile.size());
//		List<String> requiredFilesList = Arrays.asList(REQ_FILES);
//		while (entries.hasMoreElements()) {
//			ZipEntry ze = entries.nextElement();
//			extensions.add(FilenameUtils.getExtension(ze.getName()));
//		}
//		if (!extensions.containsAll(requiredFilesList)) {
//			throw new ShorelineFileFormatException("Missing mandatory files within shapefile. One of: .shp, .shx, .dbf");
//		}
//		if (Collections.frequency(extensions, SHP) > 1
//				|| Collections.frequency(extensions, SHX) > 1
//				|| Collections.frequency(extensions, DBF) > 1) {
//			throw new ShorelineFileFormatException("Found more than one shapefile in archive. Can only stage one shape file at a time.");
//		}
		try {		
		ShapeFileUtil.isValidShapefile(zipFileDir);
		} catch (ShapefileException ex) {
			throw new ShorelineFileFormatException(ex.getMessage());
		}
	}

	public ShorelineShapefile(File featureTypeFileLocation, GeoserverDAO gsHandler, ShorelineFileDAO dao) {
		this(featureTypeFileLocation, gsHandler, dao, null);
	}

	public ShorelineShapefile(File featureTypeFileLocation, GeoserverDAO gsHandler, ShorelineFileDAO dao, DSASProcess process) {
		init(featureTypeFileLocation, gsHandler, dao, process);
//		this.process = process;
//		this.baseDirectory = new File(PropertyUtil.getProperty(Property.DIRECTORIES_BASE, System.getProperty("java.io.tmpdir")));
//		this.uploadDirectory = new File(baseDirectory, PropertyUtil.getProperty(Property.DIRECTORIES_UPLOAD));
//		this.workDirectory = new File(baseDirectory, PropertyUtil.getProperty(Property.DIRECTORIES_WORK));
//		this.geoserverHandler = gsHandler;
//		this.dao = dao;
//		this.fileMap = new HashMap<>(fileParts.length);
////		this.workspace = workspace;
//		if (this.process != null) {
//			this.dao.setDSASProcess(process);
//		}
	}

	private void init(File featureTypeFileLocation, GeoserverDAO gsHandler, ShorelineFileDAO dao, DSASProcess process) {
		this.featureTypeExplodedZipFileLocation = featureTypeFileLocation;
		this.geoserverHandler = gsHandler;
		this.dao = dao;
		this.fileMap = new HashMap<>(FILE_PARTS.length);
		updateFileMapWithDirFile(featureTypeFileLocation, FILE_PARTS);
		if (process != null) {
			setDSASProcess(process);
		}
		this.type = FeatureType.SHORELINE_LIDAR;
	}
//	@Override
//	public String setDirectory(File directory) throws IOException {
//		String fileToken = super.setDirectory(directory);
//		updateFileMapWithDirFile(directory, fileParts);
//		return fileToken;
//	}

	@Override
	public String getEPSGCode() {
		String epsg = null;
		String errorMsg = "Unable to retrieve epsg code from prj file.";
		try {
			epsg = ShapeFileUtil.getEPSGCode(this.fileMap.get(SHP));
		} catch (IOException ex) {
			LOGGER.error(errorMsg, ex);
			throw new RuntimeException(errorMsg, ex); //current code, IShorelineFile and Servlet is not throwing any exception
		}
		return epsg;
	}

	@Override
	public List<File> getRequiredFiles() {
		Collection<File> requiredFiles = FileUtils.listFiles(this.featureTypeExplodedZipFileLocation, REQ_FILES, false);
		return new ArrayList<>(requiredFiles);
	}

	@Override
	public List<File> getOptionalFiles() {
		Collection<File> requiredFiles = FileUtils.listFiles(this.featureTypeExplodedZipFileLocation, OPTIONAL_FILES, false);
		return new ArrayList<>(requiredFiles);
	}

	@Override
	public String[] getColumns() throws IOException {
//		String[] headers = null;
		List<String> dbfColumns = ShapeFileUtil.getDbfColumnNames(this.featureTypeExplodedZipFileLocation);
		String[] names = dbfColumns.toArray(new String[dbfColumns.size()]);
		return names;
//		File dbfFile = this.fileMap.get(DBF);
//
//		// getDbfHeader uses the .shp file 
//		if (null == dbfFile || !dbfFile.exists() || !dbfFile.isFile() || !dbfFile.canRead()) {
//			throw new IOException(MessageFormat.format("DBF file at {0} not readable", dbfFile));
//		}
//
//		try (IterableShapefileReader reader = new IterableShapefileReader(new ShpFiles(dbfFile))) {
//			DbaseFileHeader dbfHeader = reader.getDbfHeader();
//			int fieldCount = dbfHeader.getNumFields();
//			headers = new String[fieldCount];
//			for (int headerIndex = 0; headerIndex < fieldCount; headerIndex++) {
//				headers[headerIndex] = dbfHeader.getFieldName(headerIndex);
//			}
//		} catch (Exception ex) {
//			LOGGER.warn("Error on closing IterableShapefileReader", ex);
//		}
//		return headers;
	}

	@Override
	public String importToDatabase(Map<String, String> columns, String workspace) throws ShorelineFileFormatException, SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException {
		updateProcessInformation("Getting EPSG Code");
		String projection = getEPSGCode();
		File shpFile = fileMap.get(SHP);
		updateProcessInformation("Importing to database");
		return dao.importToDatabase(shpFile, columns, workspace, projection);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ShorelineShapefile)) {
			return false;
		}
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

}
