package gov.usgs.cida.utilities.communication;

import gov.usgs.cida.utilities.file.FileHelper;
import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author isuftin
 */
public class FormUploadHandler {

    public static File saveFileFromRequest(HttpServletRequest request, String filenameParameter, File destinationFile) throws FileUploadException, IOException {
        if (StringUtils.isBlank(filenameParameter)) {
            throw new IllegalArgumentException();
        }
        if (ServletFileUpload.isMultipartContent(request)) {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            FileItemIterator iter;
            iter = upload.getItemIterator(request);
            while (iter.hasNext()) {
                FileItemStream item = iter.next();
                String name = item.getFieldName();
                if (filenameParameter.toLowerCase().equals(name.toLowerCase())) {
                    FileHelper.saveFileFromInputStream(item.openStream(), destinationFile);
                    break;
                }
            }
        } else {
            FileHelper.saveFileFromInputStream(request.getInputStream(), destinationFile);
        }
        return destinationFile;
    }
    
}
