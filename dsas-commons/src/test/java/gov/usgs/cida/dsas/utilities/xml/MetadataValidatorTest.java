package gov.usgs.cida.dsas.utilities.xml;

import gov.usgs.cida.dsas.metadata.MetadataValidator;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.xml.xpath.XPathExpressionException;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class MetadataValidatorTest {

    @Test
    public void testValidateFGDC() throws IOException, XPathExpressionException, URISyntaxException {
        URL meta = getClass().getClassLoader().getResource(
                "gov/usgs/cida/coastalhazards/metadata/OR_transects_LT.shp.FGDCclean.xml");
        MetadataValidator validator = new MetadataValidator(new File(meta.toURI()));
        assertTrue(validator.validateFGDC());
    }
    
}
