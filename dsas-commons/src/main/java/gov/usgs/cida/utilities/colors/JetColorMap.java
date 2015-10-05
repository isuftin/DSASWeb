package gov.usgs.cida.utilities.colors;

import java.awt.Color;

/**
 * Moved from ResultsRasterProcess
 * 
 * @author tkunicki
 */
public class JetColorMap implements ColorMap<Number> {

    public final static Color CLAMP_MIN = new Color(0f, 0f, 0.5f);
    public final static Color CLAMP_MAX = new Color(0.5f, 0f, 0f);

    public final AttributeRange range;

    public JetColorMap(AttributeRange range) {
        this.range = range;
    }

    @Override
    public Color valueToColor(Number value) {
        double coef = ((value.doubleValue() - range.min) / range.extent);
        if (coef < 0) {
            return CLAMP_MIN;
        } else if (coef > 1) {
            return CLAMP_MAX;
        } else {
            coef *= 4d;
            float r = (float) Math.min(coef - 1.5, -coef + 4.5);
            float g = (float) Math.min(coef - 0.5, -coef + 3.5);
            float b = (float) Math.min(coef + 0.5, -coef + 2.5);
            return new Color(
                    r > 1f ? 1f : r < 0f ? 0f : r,
                    g > 1f ? 1f : g < 0f ? 0f : g,
                    b > 1f ? 1f : b < 0f ? 0f : b);
        }
    }
}
