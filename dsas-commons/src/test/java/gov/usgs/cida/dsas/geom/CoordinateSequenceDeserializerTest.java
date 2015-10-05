package gov.usgs.cida.dsas.geom;

import gov.usgs.cida.dsas.geom.CoordinateSequenceDeserializer;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequenceFactory;
import java.lang.reflect.Type;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jiwalker
 */
public class CoordinateSequenceDeserializerTest {

    /**
     * Test of deserialize method, of class CoordinateSequenceDeserializer.
     */
    @Test
    public void testDeserialize() {
        JsonArray json = new JsonArray();
        JsonPrimitive x = new JsonPrimitive(1.23);
        JsonPrimitive y = new JsonPrimitive(-45.6);
        json.add(x);
        json.add(y);
        CoordinateSequenceDeserializer instance = new CoordinateSequenceDeserializer();
        CoordinateSequenceFactory coordFactory = CoordinateArraySequenceFactory.instance();
        Coordinate coord = new Coordinate(x.getAsDouble(), y.getAsDouble());
        CoordinateSequence coordSeq = coordFactory.create(new Coordinate[]{coord});
        CoordinateSequence result = instance.deserialize(json, null, null);
        assertEquals(coordSeq.getCoordinate(0), result.getCoordinate(0));
    }
    
}
