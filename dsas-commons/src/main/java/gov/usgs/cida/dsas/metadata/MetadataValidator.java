package gov.usgs.cida.dsas.metadata;

import java.io.File;
import gov.usgs.cida.dsas.utilities.xml.XMLUtils;
import java.io.IOException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class MetadataValidator {
    
    private File metadataFile;
    
    private static final String TITLE_XPATH = "/metadata/idinfo/citation/citeinfo/title";
    private static final String ABSTRACT_XPATH = "/metadata/idinfo/descript/abstract";
    
    public MetadataValidator(File metadataFile) {
        this.metadataFile = metadataFile;
    }
    
    public boolean validateFGDC() throws IOException, XPathExpressionException {
        String metadata = IOUtils.toString(metadataFile.toURI().toURL());
        boolean valid = false;
        if (XMLUtils.createBooleanUsingXPathExpression(TITLE_XPATH, metadata) 
                && XMLUtils.createBooleanUsingXPathExpression(ABSTRACT_XPATH, metadata))  {
            valid = true;
        }
        return valid;
    }
    
}
