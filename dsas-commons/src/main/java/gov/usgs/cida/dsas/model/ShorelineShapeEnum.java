package gov.usgs.cida.dsas.model;

import java.util.Arrays;


/**
 *
 * @author smlarson
 */
public enum ShorelineShapeEnum {
    shp, shx, dbf, prj;
        
    public static String[] getNames(Class<? extends Enum<?>> e)
    {
        return Arrays.stream(e.getEnumConstants()).map(Enum::name).toArray(String[]::new);
    }
}
