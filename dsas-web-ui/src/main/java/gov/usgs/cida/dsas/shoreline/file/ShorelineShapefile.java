package gov.usgs.cida.dsas.shoreline.file;

import gov.usgs.cida.dsas.dao.geoserver.GeoserverDAO;
import gov.usgs.cida.dsas.dao.shoreline.ShorelineFileDAO;
import gov.usgs.cida.dsas.exceptions.AttributeNotANumberException;
import gov.usgs.cida.dsas.featureType.file.FeatureType;
import gov.usgs.cida.dsas.featureTypeFile.exception.ShapefileException;
import gov.usgs.cida.dsas.model.DSASProcess;
import gov.usgs.cida.dsas.featureTypeFile.exception.ShorelineFileFormatException;
import gov.usgs.cida.dsas.service.util.ShapeFileUtil;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
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
		List<String> dbfColumns = ShapeFileUtil.getDbfColumnNames(this.featureTypeExplodedZipFileLocation);
		String[] names = dbfColumns.toArray(new String[dbfColumns.size()]);
		return names;
	}

	@Override
	public String importToDatabase(Map<String, String> columns, String workspace) throws ShorelineFileFormatException, SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException, AttributeNotANumberException {
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
