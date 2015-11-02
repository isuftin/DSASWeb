package gov.usgs.cida.dsas.dao.shoreline;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTReader;

import gov.usgs.cida.dsas.service.util.LidarFileUtils;
import gov.usgs.cida.dsas.service.util.Property;
import gov.usgs.cida.dsas.service.util.PropertyUtil;
import gov.usgs.cida.dsas.shoreline.exception.LidarFileFormatException;
import gov.usgs.cida.dsas.shoreline.exception.ShorelineFileFormatException;
import gov.usgs.cida.utilities.features.Constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.naming.NamingException;

import org.apache.commons.io.FilenameUtils;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class ShorelineLidarFileDAO extends ShorelineFileDAO {

	private static final Logger log = LoggerFactory.getLogger(ShorelineLidarFileDAO.class);
	
	private static final int BATCH_SIZE = 5000;
	
	private CoordinateReferenceSystem targetCRS;
	
	public ShorelineLidarFileDAO() {
		this.JNDI_NAME = PropertyUtil.getProperty(Property.JDBC_NAME);
		// Workaround for lat/lon vs lon/lat in reprojection
		CRSAuthorityFactory   factory = CRS.getAuthorityFactory(true);
		try {
			this.targetCRS = factory.createCoordinateReferenceSystem("EPSG:4326");
		} catch (FactoryException ex) {
			// Something really bad is happening
			this.targetCRS = null;
			log.error("Unable to get CRS for WGS84");
		}
	}

	@Override
	public String importToDatabase(File shorelineFile, Map<String, String> columns, String workspace, String EPSGCode) throws ShorelineFileFormatException, SQLException, NamingException, NoSuchElementException, ParseException, IOException, SchemaException, TransformException, FactoryException {
		SimpleDateFormat dtFormat = new SimpleDateFormat("MM/dd/yyyy");
		String baseFileName = FilenameUtils.getBaseName(shorelineFile.getName());
		CoordinateReferenceSystem sourceCRS = CRS.decode(EPSGCode);
		MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, false);
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
		WKTReader reader = new WKTReader(geometryFactory);
		
		List<double[]> xyUncies = new ArrayList<>(BATCH_SIZE * 2);
		
		String viewName;
		try (
				Connection connection = getConnection();
				BufferedReader br = new BufferedReader(new FileReader(shorelineFile))) {
			String line;
			int row = 0;
			long prevShorelineId = -1;
			int prevSegmentId = -1;
			HashMap<String, Long> shorelineDateToIdMap = new HashMap<>();
			while ((line = br.readLine()) != null) {
				row++;

				// use comma as separator
				String[] point = line.split(",");

				//validation
				try {
					if (row == 1) {
						LidarFileUtils.validateHeaderRow(point);
						continue;
					} else {
						LidarFileUtils.validateDataRow(point);
					}
				} catch (LidarFileFormatException ex) {
					throw new ShorelineFileFormatException(ex.getMessage());
				}

				//shorline id
				long shorelineId;
				String shorelineDate = point[4];
				if (!shorelineDateToIdMap.keySet().contains(shorelineDate)) { //if we have not used this shoreline date yet, go ahead create new shoreline record
					shorelineId = insertToShorelinesTable(
							connection,
							workspace,
							dtFormat.parse(shorelineDate),
							true, //lidar always has MHW = true 
							shorelineFile.getName(),
							baseFileName,
							null,
							Constants.MHW_ATTR);
					shorelineDateToIdMap.put(shorelineDate, shorelineId);
				} else {
					shorelineId = shorelineDateToIdMap.get(shorelineDate);
				}

				int segmentId = Integer.valueOf(point[0]);
				Double originalX = Double.valueOf(point[1]);
				Double originalY = Double.valueOf(point[2]);
				Double uncy = Double.valueOf(point[3]);
				
				Point pt;
				try {
					pt = (Point) reader.read("POINT (" + originalX + " " + originalY + ")");
				} catch (com.vividsolutions.jts.io.ParseException ex) {
					throw new TransformException(ex.getMessage());
				}
				Geometry transformedPt = JTS.transform(pt, transform);
				Double reprojectedX = transformedPt.getCentroid().getX();
				Double reprojectedY = transformedPt.getCentroid().getY();
				// first data row
				if (row == 2) {
					prevShorelineId = shorelineId;
					prevSegmentId = segmentId;
				}
				if (prevShorelineId == shorelineId && prevSegmentId == segmentId && xyUncies.size() < BATCH_SIZE) {
					xyUncies.add(new double[] {reprojectedX, reprojectedY, uncy});
				} else {
					insertPointsIntoShorelinePointsTable(
						connection,
						prevShorelineId,
						prevSegmentId,
						xyUncies.toArray(new double[xyUncies.size()][])
						);
					xyUncies.clear();
					xyUncies.add(new double[] {reprojectedX, reprojectedY, uncy});
					prevShorelineId = shorelineId;
					prevSegmentId = segmentId;
				}
				
			}
			if (xyUncies.size() > 0) {
				insertPointsIntoShorelinePointsTable(
							connection,
							prevShorelineId,
							prevSegmentId,
							xyUncies.toArray(new double[xyUncies.size()][])
							);
			}
			viewName = createViewAgainstWorkspace(connection, workspace);
		}
		return viewName;
	}

}
