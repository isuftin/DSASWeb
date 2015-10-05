package gov.usgs.cida.dsas.geom;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import java.lang.reflect.Type;

public class GeometryDeserializer implements JsonDeserializer<Geometry> {

    @Override
    public Geometry deserialize(JsonElement json, Type typeofT,
            JsonDeserializationContext context) throws JsonParseException {
        Geometry geom = null;
        if (json.isJsonArray()) {
            JsonArray coordinates = json.getAsJsonArray();
            GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
            Coordinate coord = new Coordinate(coordinates.get(0).getAsDouble(), coordinates.get(1).getAsDouble());
            geom = geometryFactory.createPoint(coord);
        } else if (json.isJsonObject()) {
            String type = json.getAsJsonObject().get("type").getAsString();

            if ("Point".equalsIgnoreCase(type)) {
                JsonArray coordinates = json.getAsJsonObject().getAsJsonArray("coordinates");
                GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
                Coordinate coord = new Coordinate(coordinates.get(0).getAsDouble(), coordinates.get(1).getAsDouble());
                geom = geometryFactory.createPoint(coord);
            }
        }
        return geom;
    }
}