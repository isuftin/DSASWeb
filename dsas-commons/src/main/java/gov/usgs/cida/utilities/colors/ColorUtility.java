package gov.usgs.cida.utilities.colors;

import java.awt.Color;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class ColorUtility {

    public static String toHex(Color color) {
        return String.format("#%06X", (0xFFFFFF & color.getRGB()));
    }
    
    public static String toHexLowercase(Color color) {
        return toHex(color).toLowerCase();
    }
    
    public static Color fromHex(String hex) {
        int hashIdx = hex.indexOf("#");
        
        String justHex = (hashIdx >= 0) ? hex.substring(hashIdx + 1) : hex;
        int hexIntValue = Integer.parseInt(justHex, 16);
        Color color = new Color(hexIntValue);
        
        return color;
    }
}
