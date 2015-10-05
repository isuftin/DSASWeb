package gov.usgs.cida.utilities.properties;

import gov.usgs.cida.config.DynamicReadOnlyProperties;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.naming.NamingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class JNDISingleton {
    
    private static final Logger LOG = LoggerFactory.getLogger(JNDISingleton.class);
    private static DynamicReadOnlyProperties props = null;
    
    public static DynamicReadOnlyProperties getInstance() {
        if (null == props) {
            try {
                URL propertiesFile = JNDISingleton.class.getClassLoader().getResource("application.properties");
                props = new DynamicReadOnlyProperties(new File(propertiesFile.toURI())).addJNDIContexts();
            } catch (NamingException e) {
                LOG.warn("Error occured during initProps()", e);
            } catch (IOException e) {
                LOG.warn("Could not get properties file");
            } catch (URISyntaxException e) {
                LOG.warn("URI for properties file invalid");
            } 
        }
        return props;
    }
}
