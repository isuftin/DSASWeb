package gov.usgs.cida.owsutils.commons.shapefile.utils;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.shapefile.shp.ShapefileException;

/**
 *
 * @author isuftin
 */
public class XploderMultiLineHandler extends MultiLineZHandler {

	public XploderMultiLineHandler(ShapeType type, GeometryFactory gf) throws ShapefileException {
		super(type, gf);
	}

	@Override
	public Object read(ByteBuffer buffer, ShapeType type, boolean flatGeometry) {
		if (type == ShapeType.NULL) {
			return geometryFactory.createMultiLineString(null);
		}
		int dimensions = (shapeType == ShapeType.ARCZ && !flatGeometry) ? 3 : 2;
		// read bounding box (not needed)
		buffer.position(buffer.position() + 4 * 8);

		int numParts = buffer.getInt();
		int numPoints = buffer.getInt(); // total number of points

		int[] partOffsets = new int[numParts];

		// points = new Coordinate[numPoints];
		for (int i = 0; i < numParts; i++) {
			partOffsets[i] = buffer.getInt();
		}
		// read the first two coordinates and start building the coordinate
		// sequences
		CoordinateSequence[] lines = new CoordinateSequence[numParts];
		int finish, start;
		int length;
		boolean clonePoint;
		final DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();
		for (int part = 0; part < numParts; part++) {
			start = partOffsets[part];

			if (part == (numParts - 1)) {
				finish = numPoints;
			} else {
				finish = partOffsets[part + 1];
			}

			length = finish - start;
			int xyLength = length;
			if (length == 1) {
				length = 2;
				clonePoint = true;
			} else {
				clonePoint = false;
			}

			CoordinateSequence cs = geometryFactory.getCoordinateSequenceFactory().create(length, dimensions);
			double[] xy = new double[xyLength * 2];
			doubleBuffer.get(xy);
			for (int i = 0; i < xyLength; i++) {
				cs.setOrdinate(i, 0, xy[i * 2]);
				cs.setOrdinate(i, 1, xy[i * 2 + 1]);
			}

			if (clonePoint) {
				cs.setOrdinate(1, 0, cs.getOrdinate(0, 0));
				cs.setOrdinate(1, 1, cs.getOrdinate(0, 1));
			}

			lines[part] = cs;
		}

		// Prepare line strings and return the multilinestring
		LineString[] lineStrings = new LineString[numParts];
		for (int part = 0; part < numParts; part++) {
			lineStrings[part] = geometryFactory.createLineString(lines[part]);
		}

		return geometryFactory.createMultiLineString(lineStrings);
	}

}
