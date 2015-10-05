package gov.usgs.cida.utilities.colors;

import java.awt.Color;

/**
 * Moved from ResultsRasterProcess
 * 
 * @author tkunicki
 */
public interface ColorMap<T> {

    public Color valueToColor(T value);
    
}