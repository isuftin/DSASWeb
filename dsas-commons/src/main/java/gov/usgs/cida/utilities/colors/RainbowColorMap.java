package gov.usgs.cida.utilities.colors;

import java.awt.Color;

/**
 * This class creates a rainbow color map.
 * This is accomplished by doing a linear mapping across the entire range.
 * At lower ends of the range (0 - 255) we move from Red to Yellow by increasing the Green value of RGB
 * The next range (256 - 511) we move from Yellow to Green by decreasing the Red value
 * The next range (512 - 767) we move from Green to Teal? by increasing the Blue value
 * The next range (768 - 1023) we move from Teal? to Blue by decreasing the Green value
 * The last range (1024 - 1151) we move from Blue to Purple by increasing the Red value
 * We only go to 127 for Red so that it is distinguishable from purple
 * 
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class RainbowColorMap implements ColorMap<Number> {

    private AttributeRange range;
    /* up 256 green + down 256 red + up 256 blue + down 256 green + up 127 red */
    private static final int FULL = 255;
    private static final int EMPTY = 0;
    
    private static final int RED = 0;
    private static final int RED_TO_YELLOW = RED + 256;
    private static final int YELLOW_TO_GREEN = RED_TO_YELLOW + 256;
    private static final int GREEN_TO_TEAL = YELLOW_TO_GREEN + 256;
    private static final int TEAL_TO_BLUE = GREEN_TO_TEAL + 256;
    private static final int BLUE_TO_PURPLE = TEAL_TO_BLUE + 127;
    private static final int MAX_VALUES = BLUE_TO_PURPLE;
    
    
    public RainbowColorMap(AttributeRange range) {
        this.range = range;
    }
    
    @Override
    public Color valueToColor(Number value) {
        Color color = new Color(EMPTY, EMPTY, EMPTY);
        
        // if there is only one value make it RED
        int index = 0;
        if (range.extent != 0.0d) {
            double coef = (value.doubleValue() - range.min) / range.extent;
            index = (int)Math.floor(coef * MAX_VALUES);
        }

        if (index >= RED && index < RED_TO_YELLOW) {
            int addToGreen = index - RED;
            int red = FULL;
            int green = EMPTY + addToGreen;
            int blue = EMPTY;
            color = new Color(red, green, blue);
        } else if (index >= RED_TO_YELLOW && index < YELLOW_TO_GREEN) {
            int removeFromRed = index - RED_TO_YELLOW;
            int red = FULL - removeFromRed;
            int green = FULL;
            int blue = EMPTY;
            color = new Color(red, green, blue);
        } else if (index >= YELLOW_TO_GREEN && index < GREEN_TO_TEAL) {
            int addToBlue = index - YELLOW_TO_GREEN;
            int red = EMPTY;
            int green = FULL;
            int blue = EMPTY + addToBlue;
            color = new Color(red, green, blue);
        } else if (index >= GREEN_TO_TEAL && index < TEAL_TO_BLUE) {
            int removeFromGreen = index - GREEN_TO_TEAL;
            int red = EMPTY;
            int green = FULL - removeFromGreen;
            int blue = FULL;
            color = new Color(red, green, blue);
        } else if (index >= TEAL_TO_BLUE && index <= BLUE_TO_PURPLE) {
            int addToRed = index - TEAL_TO_BLUE;
            int red = EMPTY + addToRed;
            int green = EMPTY;
            int blue = FULL;
            color = new Color(red, green, blue);
        }
        
        return color;
    }

}
