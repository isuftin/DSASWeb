package gov.usgs.cida.utilities.colors;

/**
 * Moved from ResultsRasterProcess
 * 
 * @author tkunicki
 */
public class AttributeRange {

    public final double min;
    public final double max;
    public final double extent;

    public AttributeRange(double min, double max) {
        this.min = min;
        this.max = max;
        this.extent = max - min;
    }

    @Override
    public String toString() {
        return new StringBuilder("range=[").append(min).append(':').append(max).append(']').toString();
    }

    public AttributeRange zeroInflect(boolean invert) {
        double absOfMax = max < 0 ? 0 - max : max;
        double absOfMin = min < 0 ? 0 - min : min;
        double maxAbs = absOfMax > absOfMin ? absOfMax : absOfMin;
        return invert
                ? new AttributeRange(maxAbs, 0 - maxAbs)
                : new AttributeRange(0 - maxAbs, maxAbs);
    }
}
