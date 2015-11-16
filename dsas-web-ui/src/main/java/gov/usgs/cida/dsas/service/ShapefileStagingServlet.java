package gov.usgs.cida.dsas.service;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;

/**
 * Receives a shapefile from the client, reads the featuretype from it and sends
 * back a file token which will later be used to read in the shapefile
 *
 * @author isuftin
 */
@WebServlet(name = "ShapefileStagingServlet", urlPatterns = {"/service/shapefile/stage"})
public class ShapefileStagingServlet extends HttpServlet {
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ShapefileStagingServlet.class);
	private static final long serialVersionUID = 6616007726957388275L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(405);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(405);
	}

}
