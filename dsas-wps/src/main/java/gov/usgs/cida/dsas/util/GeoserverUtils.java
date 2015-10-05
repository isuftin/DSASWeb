package gov.usgs.cida.dsas.util;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.wps.gs.ImportProcess;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.ProcessException;
import org.geotools.util.DefaultProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.util.ProgressListener;

/**
 * A collection of utility methods to help in dealing with GeoServer layers
 *
 * @author isuftin
 */
public class GeoserverUtils {

	protected static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(GeoserverUtils.class);
	private Catalog catalog;

	public GeoserverUtils(Catalog catalog) {
		this.catalog = catalog;
	}

	public WorkspaceInfo getWorkspaceByName(String workspace) {
		WorkspaceInfo ws;
		ws = catalog.getWorkspaceByName(workspace);
		if (ws == null) {
			throw new ProcessException("Could not find workspace " + workspace);
		}
		return ws;
	}

	/**
	 * Attempts to retrieve a DataStoreInfo from the catalog using the provided
	 * workspace and name
	 *
	 * @param workspace
	 * @param store
	 * @return
	 */
	public DataStoreInfo getDataStoreByName(String workspace, String store) {
		DataStoreInfo ds = catalog.getDataStoreByName(workspace, store);
		if (ds == null) {
			throw new ProcessException("Could not find store " + store + " in workspace " + workspace);
		}
		return ds;
	}

	/**
	 * Automatically converts a DataStoreInfo into a DataAccess object,
	 * including a listener (if provided)
	 *
	 * @see DataStoreInfo#getDataStore(org.opengis.util.ProgressListener)
	 * @param store
	 * @param listener
	 * @return
	 */
	public DataAccess<? extends FeatureType, ? extends Feature> getDataAccess(DataStoreInfo store, ProgressListener listener) {
		DataAccess<? extends FeatureType, ? extends Feature> da;
		try {
			da = store.getDataStore(listener == null ? new DefaultProgressListener() : listener);
		} catch (IOException ioe) {
			throw new ProcessException(ioe);
		}
		return da;
	}

	public FeatureSource<? extends FeatureType, ? extends Feature> getFeatureSource(DataStoreInfo store, String layer, ProgressListener listener) {
		DataAccess<? extends FeatureType, ? extends Feature> dataAccess;
		try {
			dataAccess = store.getDataStore(listener == null ? new DefaultProgressListener() : listener);
		} catch (IOException ioe) {
			throw new ProcessException(ioe);
		}
		return getFeatureSource(dataAccess, layer);
	}

	/**
	 * Attempts to get the FeatureSource, provided DataAccess and a layer name
	 *
	 * @see DataAccess#getFeatureSource(org.opengis.feature.type.Name)
	 * @param dataAccess
	 * @param layer
	 * @return
	 */
	public FeatureSource<? extends FeatureType, ? extends Feature> getFeatureSource(DataAccess<? extends FeatureType, ? extends Feature> dataAccess, String layer) {
		FeatureSource<? extends FeatureType, ? extends Feature> featureSource;
		try {
			featureSource = dataAccess.getFeatureSource(new NameImpl(layer));
		} catch (IOException ioe) {
			throw new ProcessException(ioe);
		}
		return featureSource;
	}

	public FeatureCollection<? extends FeatureType, ? extends Feature> getFeatureCollection(FeatureSource<? extends FeatureType, ? extends Feature> featureSource) {
		FeatureCollection<? extends FeatureType, ? extends Feature> fc;
		try {
			fc = featureSource.getFeatures();
		} catch (IOException ex) {
			throw new ProcessException(ex);
		}
		return fc;
	}

	/**
	 * Replaces a layer on Geoserver with a provided feature collection. In case of failure, will try to replace original
	 * files to original location
	 *
	 * @param collection FeatureCollection to replace layer with
	 * @param layer Layer name to replace
	 * @param dataStore Store in which layer to replace sits
	 * @param workspace Workspace in which store that needs layer replacement
	 * sits
	 * @param importProc Import process used to replace the layer
	 * @return Location of new layer in format: "workspaceName:layerName"
	 */
	public String replaceLayer(FeatureCollection<SimpleFeatureType, SimpleFeature> collection, String layer, DataStoreInfo dataStore, WorkspaceInfo workspace, ImportProcess importProc) {
		// TODO- Once files are back in original location, try to re-import them into the catalog to get as close as
		// possible to this function not having consequence on fail
		String result = null;
		LayerInfo layerByName = catalog.getLayerByName(workspace.getName() + ':' + layer);
		new CascadeDeleteVisitor(catalog).visit(layerByName);
		File tempLocation = Files.createTempDir();
		File diskDirectory = null;
		tempLocation.deleteOnExit();

		try {
			diskDirectory = new File(dataStore.getDataStore(new DefaultProgressListener()).getInfo().getSource());
			Collection<File> listFiles = FileUtils.listFiles(diskDirectory, new PrefixFileFilter(layerByName.getName()), null);

			// I don't want to delete these files right away. I'd like to save them in case we get into any problems, 
			// I can move them back
			for (File file : listFiles) {
				Files.move(file, new File(tempLocation, file.getName()));
			}
		} catch (IOException ex) {
			LOGGER.log(Level.WARNING, "While trying to replace layer, file could not be moved  to " + tempLocation.getAbsolutePath(), ex);

			// If diskDirectory is null, that means that we haven't even tried to move files yet, so disregard the 
			// trying to move the files back to their original location
			if (diskDirectory != null) {
				// Try to move all files back 
				Collection<File> listFiles = FileUtils.listFiles(tempLocation, new PrefixFileFilter(layerByName.getName()), null);
				for (File file : listFiles) {
					try {
						Files.move(file, new File(tempLocation, diskDirectory.getName()));
					} catch (IOException ex1) {
						LOGGER.log(Level.WARNING,
								"While trying to recover from file moving, failed to move file back from " + file.getAbsolutePath() + " to " + diskDirectory.getAbsolutePath(),
								ex1);
					}
				}
			}
			throw new ProcessException(ex);
		}

		// Files have been moved out of the store location so save the store + workspace
		catalog.save(dataStore);
		catalog.save(workspace);
		
		// Try to import the incoming layer 
		LayerImportUtil importer = new LayerImportUtil(catalog, importProc);
		try {
			result = importer.importLayer((SimpleFeatureCollection) collection, workspace.getName(), dataStore.getName(), layer, collection.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem(), ProjectionPolicy.REPROJECT_TO_DECLARED);
		} catch (Exception e) {
			// Something went wrong. Let's try to get those files in that temp location back into their original spot
			LOGGER.log(Level.WARNING, "Import process failed. Will attempt to restore files back to store location {0}", diskDirectory.getAbsolutePath());

			Collection<File> listFiles = FileUtils.listFiles(tempLocation, new PrefixFileFilter(layerByName.getName()), null);
			int moveFailCount = 0;
			for (File file : listFiles) {
				try {
					Files.move(file, new File(diskDirectory, file.getName()));
				} catch (IOException ex) {
					LOGGER.log(Level.WARNING,
							file.getAbsolutePath() + " could not be moved  to " + diskDirectory.getAbsolutePath(),
							ex);
					moveFailCount++;
				}
			}

			if (moveFailCount > 0) {
				LOGGER.log(Level.WARNING,
						"{0} files failed to be restored back to original directory. Files will be available at {1} until application shutdown.",
						new Object[]{moveFailCount, tempLocation.getAbsolutePath()});
			}

			throw e;
		}

		// Files were imported successfully. No longer need to have the temporarily held files
		try {
			FileUtils.deleteDirectory(tempLocation);
		} catch (IOException ex) {
			LOGGER.log(
					Level.WARNING,
					tempLocation.getAbsolutePath() + " could not be deleted. Will try again on system exit or can be deleted manually.",
					ex);
		}

		return result;
	}

}
