package gov.usgs.cida.utilities.communication;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class CSWHandler {
    
    private String url;
    
    public CSWHandler(String url) {
        this.url = url;
    }
    
    public HttpResponse sendRequest(String contentType, String content) throws FileNotFoundException, IOException {
        HttpPost post;
        HttpClient httpClient = new DefaultHttpClient();

        post = new HttpPost(url);
        AbstractHttpEntity entity = new StringEntity(content);
        
        post.setEntity(entity);
        HttpResponse response = httpClient.execute(post);

        return response;
    }
}
