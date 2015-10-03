package gov.usgs.cida.coastalhazards.shoreline.file;

import gov.usgs.cida.coastalhazards.shoreline.exception.ShorelineFileFormatException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import org.geotools.feature.SchemaException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author isuftin
 */
public interface IShorelineFile {

	/**
	 * Saves a zip file, unpacks it to application work directory of the same
	 * name.
	 *
	 * Unzips the file and returns the directory where file unzipped to
	 *
	 * @param file
	 * @return directory where file unzipped to
	 * @throws java.io.IOException
	 */
	public File saveZipFile(File file) throws IOException;

	/**
	 * Sets the directory that contains the Shoreline File set
	 *
	 * @param directory
	 * @return token to directory
	 * @throws java.io.FileNotFoundException If directory is not found
	 */
	public String setDirectory(File directory) throws IOException;

	/**
	 * Uses a token to retrieve a the working directory for this ShorelineFile
	 *
	 * @param token
	 * @return
	 */
	public File getDirectory(String token);

	/**
	 * Gets the projection EPSG of the shoreline file
	 *
	 * @return
	 */
	public String getEPSGCode();

	/**
	 * Gets columns and column types from shoreline file
	 *
	 * @return
	 * @throws java.io.IOException can happen if files under ShorelineFile are
	 * missing or invalid
	 */
	public String[] getColumns() throws IOException;

	/**
	 * Convenience method to accept a servlet request to import to database
	 *
	 * @param request
	 * @return
	 * @throws
	 * gov.usgs.cida.coastalhazards.shoreline.exception.ShorelineFileFormatException
	 * @throws java.sql.SQLException
	 * @throws javax.naming.NamingException
	 * @throws java.text.ParseException
	 * @throws java.io.IOException
	 * @throws org.geotools.feature.SchemaException
	 * @throws org.opengis.referencing.operation.TransformException
	 * @throws org.opengis.referencing.FactoryException
	 */
	public String importToDatabase(HttpServletRequest request) throws ShorelineFileFormatException, SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException;

	/**
	 * Imports the shoreline file into the database
	 *
	 * @param columns
	 * @return
	 * @throws ShorelineFileFormatException
	 * @throws SQLException
	 * @throws NamingException
	 * @throws NoSuchElementException
	 * @throws ParseException
	 * @throws IOException
	 * @throws SchemaException
	 * @throws TransformException
	 * @throws FactoryException
	 */
	public String importToDatabase(Map<String, String> columns) throws ShorelineFileFormatException, SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException;

	/**
	 * Imports the view as a layer in Geoserver
	 *
	 * @param viewName
	 * @throws IOException
	 */
	public void importToGeoserver(String viewName) throws IOException;

	/**
	 * Checks if underlying files exist in the file system
	 *
	 * @return
	 */
	public boolean exists();

	/**
	 * Returns the name of the workspace this shoreline file operates under
	 *
	 * @return
	 */
	public String getWorkspace();

	/**
	 * Deletes own files in the file system and removes parent directory
	 *
	 * @return whether parent directory has been removed
	 */
	public boolean clear();

	@Override
	public boolean equals(Object obj);

	@Override
	public int hashCode();

}
