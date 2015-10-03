package gov.usgs.cida.coastalhazards.shoreline.file;

import gov.usgs.cida.coastalhazards.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.coastalhazards.dao.shoreline.ShorelineFileDAO;
import gov.usgs.cida.coastalhazards.service.util.Property;
import gov.usgs.cida.coastalhazards.service.util.PropertyUtil;
import gov.usgs.cida.coastalhazards.shoreline.exception.ShorelineFileFormatException;
import gov.usgs.cida.owsutils.commons.shapefile.utils.IterableShapefileReader;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.naming.NamingException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
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
	private static final String[] requiredFiles = new String[]{SHP, SHX, DBF, PRJ};
	private static final String[] fileParts = new String[]{
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

	public static void validate(File zipFile) throws ShorelineFileFormatException, IOException {
		ZipFile zFile = new ZipFile(zipFile);
		Enumeration<? extends ZipEntry> entries = zFile.entries();
		List<String> extensions = new ArrayList<>(zFile.size());
		List<String> requiredFilesList = Arrays.asList(requiredFiles);
		while (entries.hasMoreElements()) {
			ZipEntry ze = entries.nextElement();
			extensions.add(FilenameUtils.getExtension(ze.getName()));
		}
		if (!extensions.containsAll(requiredFilesList)) {
			throw new ShorelineFileFormatException("Missing mandatory files within shapefile. One of: .shp, .shx, .dbf");
		}
		if (Collections.frequency(extensions, SHP) > 1
				|| Collections.frequency(extensions, SHX) > 1
				|| Collections.frequency(extensions, DBF) > 1) {
			throw new ShorelineFileFormatException("Found more than one shapefile in archive. Can only stage one shape file at a time.");
		}
	}

	public ShorelineShapefile(GeoserverDAO gsHandler, ShorelineFileDAO dao, String workspace) {
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
	public String[] getColumns() throws IOException {
		String[] headers = null;
		File dbfFile = this.fileMap.get(DBF);

		// getDbfHeader uses the .shp file 
		if (null == dbfFile || !dbfFile.exists() || !dbfFile.isFile() || !dbfFile.canRead()) {
			throw new IOException(MessageFormat.format("DBF file at {0} not readable", dbfFile));
		}

		try (IterableShapefileReader reader = new IterableShapefileReader(dbfFile)) {
			DbaseFileHeader dbfHeader = reader.getDbfHeader();
			int fieldCount = dbfHeader.getNumFields();
			headers = new String[fieldCount];
			for (int headerIndex = 0; headerIndex < fieldCount; headerIndex++) {
				headers[headerIndex] = dbfHeader.getFieldName(headerIndex);
			}
		} catch (Exception ex) {
			LOGGER.warn("Error on closing IterableShapefileReader", ex);
		}
		return headers;
	}

	@Override
	public String importToDatabase(Map<String, String> columns) throws ShorelineFileFormatException, SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException {
		String projection = getEPSGCode();
		File shpFile = fileMap.get(SHP);
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
