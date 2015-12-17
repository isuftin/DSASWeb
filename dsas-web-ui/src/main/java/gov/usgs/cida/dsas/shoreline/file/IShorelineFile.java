package gov.usgs.cida.dsas.shoreline.file;

import gov.usgs.cida.dsas.exceptions.AttributeNotANumberException;
import gov.usgs.cida.dsas.model.DSASProcess;
import gov.usgs.cida.dsas.featureTypeFile.exception.ShorelineFileFormatException;
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
	 * Sets the DSASProcess object. If present, functions that may want to update 
	 * it may do so
	 * 
	 * @param process 
	 */
	public void setDSASProcess(DSASProcess process);

	/**
	 * Gets the projection EPSG of the shoreline file
	 *
	 * @return
	 * @throws java.io.IOException
	 * @throws org.opengis.referencing.FactoryException
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
	 * gov.usgs.cida.dsas.featureTypeFile.exception.ShorelineFileFormatException
	 * @throws java.sql.SQLException
	 * @throws javax.naming.NamingException
	 * @throws java.text.ParseException
	 * @throws java.io.IOException
	 * @throws org.geotools.feature.SchemaException
	 * @throws org.opengis.referencing.operation.TransformException
	 * @throws org.opengis.referencing.FactoryException
	 */
	public String importToDatabase(HttpServletRequest request, String workspace) throws ShorelineFileFormatException, SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException, AttributeNotANumberException;

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
	public String importToDatabase(Map<String, String> columns, String workspace) throws ShorelineFileFormatException, SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException, AttributeNotANumberException;

	/**
	 * Imports the view as a layer in Geoserver
	 *
	 * @param viewName
	 * @throws IOException
	 */
	public void importToGeoserver(String viewName, String workspace) throws IOException;

	/**
	 * Checks if underlying files exist in the file system
	 *
	 * @return
	 */
	public boolean exists();

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
