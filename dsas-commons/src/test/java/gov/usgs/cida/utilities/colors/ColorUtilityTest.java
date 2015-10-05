package gov.usgs.cida.utilities.colors;

import java.awt.Color;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 *
 * @author jiwalker
 */
public class ColorUtilityTest {
    
    public ColorUtilityTest() {
    }

    /**
     * Test of toHex method, of class ColorUtility.
     */
    @Test
    public void testToHex() {
        Color color = Color.RED;
        String expResult = "#FF0000";
        String result = ColorUtility.toHex(color);
        assertThat(expResult, is(equalTo(result)));
    }

    /**
     * Test of toHexLowercase method, of class ColorUtility.
     */
    @Test
    public void testToHexLowercase() {
        Color color = Color.BLUE;
        String expResult = "#0000ff";
        String result = ColorUtility.toHexLowercase(color);
        assertThat(expResult, is(equalTo(result)));
    }

    /**
     * Test of fromHex method, of class ColorUtility.
     */
    @Test
    public void testFromHex() {
        String hex = "#00ff00";
        Color expResult = Color.GREEN;
        Color result = ColorUtility.fromHex(hex);
        assertThat(expResult, is(equalTo(result)));
    }
    
}
