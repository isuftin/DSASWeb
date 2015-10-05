package gov.usgs.cida.dsas.geom;

import gov.usgs.cida.dsas.geom.GeometryDeserializer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jiwalker
 */
public class GeometryDeserializerTest {
    
    public GeometryDeserializerTest() {
    }

    /**
     * Test of deserialize method, of class GeometryDeserializer.
     */
    @Test
    public void testDeserializeJsonArray() {
        JsonArray json = new JsonArray();
        JsonPrimitive x = new JsonPrimitive(1.23);
        JsonPrimitive y = new JsonPrimitive(-45.6);
        json.add(x);
        json.add(y);
        GeometryDeserializer instance = new GeometryDeserializer();
        Geometry expResult = new GeometryFactory().createPoint(new Coordinate(x.getAsDouble(), y.getAsDouble()));
        
        Geometry result = instance.deserialize(json, null, null);
        assertEquals(expResult, result);
    }
    
    /**
     * Test of deserialize method, of class GeometryDeserializer.
     * this wants an object rather than an array
     */
    @Test
    public void testDeserializeJsonObject() {
        JsonObject jsonObj = new JsonObject();
        JsonArray jsonArr = new JsonArray();
        JsonPrimitive x = new JsonPrimitive(1.23);
        JsonPrimitive y = new JsonPrimitive(-45.6);
        jsonArr.add(x);
        jsonArr.add(y);
        jsonObj.add("type", new JsonPrimitive("Point"));
        jsonObj.add("coordinates", jsonArr);
        GeometryDeserializer instance = new GeometryDeserializer();
        Geometry expResult = new GeometryFactory().createPoint(new Coordinate(x.getAsDouble(), y.getAsDouble()));
        
        Geometry result = instance.deserialize(jsonObj, null, null);
        assertEquals(expResult, result);
    }
}
