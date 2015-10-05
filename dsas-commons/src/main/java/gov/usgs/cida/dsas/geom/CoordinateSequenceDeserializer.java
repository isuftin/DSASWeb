package gov.usgs.cida.dsas.geom;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequenceFactory;
import java.lang.reflect.Type;

public class CoordinateSequenceDeserializer implements JsonDeserializer<CoordinateSequence> {

    @Override
    public CoordinateSequence deserialize(JsonElement json, Type typeofT,
            JsonDeserializationContext context) throws JsonParseException {
        CoordinateSequence coordSeq = null;
        if (json.isJsonArray()) {
            JsonArray coordinates = json.getAsJsonArray();
            CoordinateSequenceFactory coordFactory = CoordinateArraySequenceFactory.instance();
            Coordinate coord = new Coordinate(coordinates.get(0).getAsDouble(), coordinates.get(1).getAsDouble());
            coordSeq = coordFactory.create(new Coordinate[]{coord});
        } 
        return coordSeq;
    }
}