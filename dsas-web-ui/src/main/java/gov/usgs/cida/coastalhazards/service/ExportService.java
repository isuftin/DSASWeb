package gov.usgs.cida.coastalhazards.service;

import gov.usgs.cida.coastalhazards.service.util.Property;
import gov.usgs.cida.coastalhazards.service.util.PropertyUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import sun.misc.BASE64Decoder;

/**
 *
 * @author isuftin
 */
public class ExportService extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String filename = request.getParameter("filename");
		String data = request.getParameter("data");
		String type = request.getParameter("type");
		String encoding = StringUtils.isNotBlank(request.getParameter("encoding")) && request.getParameter("encoding").toLowerCase().equals("utf-8") ? request.getParameter("encoding") : "base64";
		String shortName = request.getParameter("shortName");
		String url = request.getRequestURL().toString();

		if (StringUtils.isBlank(filename)) {
			response.sendError(500, "'filename' element was empty.");
			return;
		}

		if (url.toLowerCase().contains("squiggle")) {
			String layer = request.getParameter("layer");
			String workspaceNS = request.getParameter("workspaceNS");
			String[] output = request.getParameterValues("output");
			if (StringUtils.isBlank(layer) || StringUtils.isBlank(workspaceNS) || output.length == 0) {
				response.sendError(500, "Either 'layer', 'layerNS' or 'output' elements were empty.");
				return;
			}

			String workspace = layer.split(":")[0];
			String cchn52Endpoint = PropertyUtil.getProperty(Property.N52_ENDPOINT);
			String gsEndpoint = PropertyUtil.getProperty(Property.GEOSERVER_ENDPOINT);

			if (gsEndpoint.endsWith("/")) {
				gsEndpoint = gsEndpoint.substring(0, gsEndpoint.length() - 1);
			}

			String wpsRequest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
					+ "<wps:Execute xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" service=\"WPS\" version=\"1.0.0\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsExecute_request.xsd\">"
					+ "<ows:Identifier>org.n52.wps.server.r.DSAS_squigglePlot</ows:Identifier>"
					+ "<wps:DataInputs>"
					+ "<wps:Input>"
					+ "<ows:Identifier>input</ows:Identifier>"
					+ "<wps:Reference mimeType=\"text/xml\" xlink:href=\"" + gsEndpoint + "/" + workspace + "/wfs\" method=\"POST\">"
					+ "<wps:Body>"
					+ "<wfs:GetFeature service=\"WFS\" xmlns:" + workspace + "=\"" + workspaceNS + "\"  version=\"1.1.0\" outputFormat=\"GML2\" xmlns:ogc=\"http://www.opengis.net/ogc\">"
					+ "<wfs:Query typeName=\"" + layer + "\"/>"
					+ "</wfs:GetFeature>"
					+ "</wps:Body>"
					+ "</wps:Reference>"
					+ "</wps:Input>"
					+ "<wps:Input>"
					+ "<ows:Identifier>shortName</ows:Identifier>"
					+ "<wps:Data>"
					+ "<wps:LiteralData>" + shortName + "</wps:LiteralData>"
					+ "</wps:Data>"
					+ "</wps:Input>"
					+ "</wps:DataInputs>"
					+ "<wps:ResponseForm>"
					+ "<wps:RawDataOutput mimeType=\"" + type + "\" encoding=\"" + encoding + "\">"
					+ "<ows:Identifier>output</ows:Identifier>"
					+ "</wps:RawDataOutput>"
					+ "</wps:ResponseForm>"
					+ "</wps:Execute>";
			HttpUriRequest req = new HttpPost(cchn52Endpoint + "/WebProcessingService");
			HttpClient client = new DefaultHttpClient();
			req.addHeader("Content-Type", "text/xml");
			if (!StringUtils.isBlank(wpsRequest) && req instanceof HttpEntityEnclosingRequestBase) {
				StringEntity contentEntity = new StringEntity(wpsRequest);
				((HttpEntityEnclosingRequestBase) req).setEntity(contentEntity);
			}
			HttpResponse resp = client.execute(req);
			StatusLine statusLine = resp.getStatusLine();

			if (statusLine.getStatusCode() != 200) {
				response.sendError(statusLine.getStatusCode(), statusLine.getReasonPhrase());
				return;
			}
			data = IOUtils.toString(resp.getEntity().getContent(), "UTF-8");
			if (data.contains("ExceptionReport")) {
				response.sendError(500, data);
				return;
			}
		}

		if (StringUtils.isBlank(data)) {
			response.sendError(500, "Either 'filename' or 'data' elements were empty.");
			return;
		}

		StringBuilder sb = new StringBuilder(data);
		byte[] dataByteArr;
		if ("base64".equalsIgnoreCase(encoding)) {
			dataByteArr = new BASE64Decoder().decodeBuffer(data.toString());
		} else {
			dataByteArr = data.getBytes(encoding);
		}
		int length = dataByteArr.length;

		OutputStream out = null;
		InputStream in = null;
		try {
			response.setContentType("application/octet-stream");
			response.setContentLength(length);
			response.setHeader("Content-Disposition", "attachment;filename=" + filename);
			response.setHeader("Pragma", "no-cache");
			response.setHeader("Expires", "0");

			in = new ByteArrayInputStream(dataByteArr);
			out = response.getOutputStream();
			IOUtils.copy(in, out);
			out.flush();
			out.close();
		} finally {
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(in);
		}
	}

	// <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Returns a short description of the servlet.
	 *
	 * @return a String containing servlet description
	 */
	@Override
	public String getServletInfo() {
		return "Exports posted data via a file back to the client";
	}// </editor-fold>
}
