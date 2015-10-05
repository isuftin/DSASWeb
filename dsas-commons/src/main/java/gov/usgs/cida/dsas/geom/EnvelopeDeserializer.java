package gov.usgs.cida.dsas.geom;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import java.lang.reflect.Type;

public class EnvelopeDeserializer implements JsonDeserializer<Envelope> {
    
    @Override
    public Envelope deserialize(JsonElement json, Type type,
            JsonDeserializationContext context) throws JsonParseException {
        JsonArray bbox = json.getAsJsonArray();
        Coordinate coordA = new Coordinate(bbox.get(0).getAsDouble(), bbox.get(1).getAsDouble());
        Coordinate coordB = new Coordinate(bbox.get(2).getAsDouble(), bbox.get(3).getAsDouble());
        Envelope envelope = new Envelope(coordA, coordB);

        return envelope;
    }
}