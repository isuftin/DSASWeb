package gov.usgs.cida.dsas.geom;

import gov.usgs.cida.dsas.geom.EnvelopeDeserializer;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequenceFactory;
import java.lang.reflect.Type;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jiwalker
 */
public class EnvelopeDeserializerTest {

    /**
     * Test of deserialize method, of class EnvelopeDeserializer.
     */
    @Test
    public void testDeserialize() {
        JsonArray json = new JsonArray();
        JsonPrimitive x1 = new JsonPrimitive(1.23);
        JsonPrimitive y1 = new JsonPrimitive(-45.6);
        JsonPrimitive x2 = new JsonPrimitive(78.9);
        JsonPrimitive y2 = new JsonPrimitive(-8.76);
        json.add(x1);
        json.add(y1);
        json.add(x2);
        json.add(y2);

        Coordinate coordA = new Coordinate(x1.getAsDouble(), y1.getAsDouble());
        Coordinate coordB = new Coordinate(x2.getAsDouble(), y2.getAsDouble());
        Envelope expResult = new Envelope(coordA, coordB);

        EnvelopeDeserializer instance = new EnvelopeDeserializer();
        Envelope result = instance.deserialize(json, null, null);
        assertEquals(expResult, result);
    }
    
}
