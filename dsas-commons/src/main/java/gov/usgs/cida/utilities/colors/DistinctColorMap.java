package gov.usgs.cida.utilities.colors;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class DistinctColorMap implements ColorMap<Number> {
    
    private AttributeRange range;
    
    private static final String[] colors = {"#ff0000", "#bf6c60", "#ffa640", "#a68500", "#86bf60", "#009952", "#007a99", "#0074d9", "#5630bf", "#f780ff", "#ff0066", "#ff8091", "#f20000", "#ff7340", "#bf9360", "#bfb960", "#44ff00", "#3df2b6", "#73cfe6", "#0066ff", "#9173e6", "#bf30a3", "#bf3069", "#a60000", "#a65b29", "#ffcc00", "#90d900", "#00d957", "#60bfac", "#0091d9", "#2200ff", "#b63df2", "#f279ba", "#a6293a"};


    public DistinctColorMap(AttributeRange range) {
        this.range = range;
    }
    
    /**
     * Reverse engineers this code
     * protected static final List<Map<String,Object>> bins;
     *  static {
     *      List<Map<String,Object>> binsResult = new ArrayList<Map<String,Object>>();
     *      for (int i=0; i<colors.length; i++) {
     *          List<Integer> years = new ArrayList<Integer>();
     *          int j=i;
     *          while(j<100) {
     *              years.add(j);
     *              j += colors.length;
     *          }
     *          Map<String, Object> binMap = new LinkedHashMap<String,Object>();
     *          binMap.put("years", years);
     *          binMap.put("color", colors[i]);
     *          binsResult.add(binMap);
     *      }
     *      bins = binsResult;
     *  }
     * @param value
     * @return 
     */
    @Override
    public Color valueToColor(Number value) {
        int yearOfCentury = value.intValue() % 100;
        int bin = yearOfCentury % colors.length;
        String hexValue = colors[bin];
        Color color = ColorUtility.fromHex(hexValue);
        return color;
    }

    
}
