package gov.usgs.cida.dsas.util;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.util.DefaultProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jndi.JndiTemplate;

/**
 *
 * @author isuftin
 */
public class GeoserverSweeperStartupListener implements InitializingBean {

	protected static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("gov.usgs.cida.dsas.util");
	private static final Long DEFAULT_MAX_LAYER_AGE = 2592000000l; // 30d
	private static final Long DEFAULT_RUN_EVER_MS = 3600000l; // 1h
	private static final Boolean DEFAULT_DELETE_EMPTY_STORES = Boolean.FALSE;
	private static final Boolean DEFAULT_DELETE_EMPTY_WORKSPACES = Boolean.FALSE;
	private static final String DEFAULT_READ_ONLY_WORKSPACES = "published";
	private Boolean deleteEmptyStores;
	private Boolean deleteEmptyWorkspaces;
	private Long maxAge;
	private Long runEveryMs;
	private Catalog catalog;
	private String[] readOnlyWorkspaces;
	private Thread sweeperThread;

	public GeoserverSweeperStartupListener(Catalog catalog) {
		this.catalog = catalog;
	}

	public void destroy() throws Exception {
		LOGGER.log(Level.INFO, "Sweeper thread is shutting down");
		this.sweeperThread.interrupt();
		this.sweeperThread.join(this.runEveryMs + 60000);
		LOGGER.log(Level.INFO, "Sweeper thread is shut down");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		JndiTemplate template = new JndiTemplate();

		try {
			this.maxAge = template.lookup("java:comp/env/dsas.geoserver.layer.age.maximum", Long.class);
		} catch (NamingException ex) {
			this.maxAge = DEFAULT_MAX_LAYER_AGE;
			LOGGER.log(Level.INFO, "Init parameter 'dsas.geoserver.layer.age.maximum' was not set. Maximum layer age set to {0}ms", this.maxAge);
		}

		try {
			this.runEveryMs = template.lookup("java:comp/env/dsas.geoserver.sweeper.run.period", Long.class);
		} catch (NamingException ex) {
			this.runEveryMs = DEFAULT_RUN_EVER_MS;
			LOGGER.log(Level.INFO, "Init parameter 'dsas.geoserver.sweeper.run.period' was not set. Sweeper will run every {0}ms", this.runEveryMs);
		}

		try {
			this.deleteEmptyStores = template.lookup("java:comp/env/dsas.geoserver.sweeper.stores.empty.delete", Boolean.class);
		} catch (NamingException ex) {
			this.deleteEmptyStores = DEFAULT_DELETE_EMPTY_STORES;
			LOGGER.log(Level.INFO, "Init parameter dsas.geoserver.sweeper.stores.empty.delete was not set. Empty stores set to be deleted: {0}", this.deleteEmptyStores);
		}

		try {
			this.deleteEmptyWorkspaces = template.lookup("java:comp/env/dsas.geoserver.sweeper.workspaces.empty.delete", Boolean.class);
		} catch (NamingException ex) {
			this.deleteEmptyWorkspaces = DEFAULT_DELETE_EMPTY_WORKSPACES;
			LOGGER.log(Level.INFO, "Init parameter dsas.geoserver.sweeper.workspaces.empty.delete was not set. Empty stores set to be deleted: {0}", this.deleteEmptyStores);
		}

		String roWorkspaces;
		try {
			roWorkspaces = template.lookup("java:comp/env/dsas.geoserver.sweeper.workspaces.read-only", String.class);
		} catch (NamingException ex) {
			roWorkspaces = DEFAULT_READ_ONLY_WORKSPACES;
			LOGGER.log(Level.INFO, "Init parameter dsas.geoserver.sweeper.workspaces.read-only was not set. Read only workspaces set to: {0}", roWorkspaces);
		}

		if (StringUtils.isNotBlank(roWorkspaces)) {
			this.readOnlyWorkspaces = roWorkspaces.split(",");
		}

		if (this.readOnlyWorkspaces.length != 0) {
			this.sweeperThread = new Thread(new Sweeper(this.catalog, this.maxAge, this.readOnlyWorkspaces, this.runEveryMs, this.deleteEmptyStores, this.deleteEmptyWorkspaces), "sweeper-thread");
			this.sweeperThread.start();
		} else {
			// Failsafe
			LOGGER.log(Level.INFO, "Because there were no workspaces set to read-only, sweeper will not run. If this is a mistake, set the parameter 'dsas.geoserver.sweeper.workspaces.read-only' to any workspace. The workspace does not need to actually exist.");
		}
	}

	private class Sweeper implements Runnable {

		private final Boolean deleteEmptyStores;
		private final Boolean deleteEmptyWorkspaces;
		private final Long maxAge;
		private final Long runEveryMs;
		private final String[] readOnlyWorkspaces;
		private final Catalog catalog;

		public Sweeper(Catalog catalog, Long maxAge, String[] readOnlyWorkspaces, Long runEveryMs, Boolean deleteEmptyStores, Boolean deleteEmptyWorkspaces) {
			this.catalog = catalog;
			this.maxAge = maxAge;
			this.readOnlyWorkspaces = readOnlyWorkspaces;
			this.runEveryMs = runEveryMs;
			this.deleteEmptyStores = deleteEmptyStores;
			this.deleteEmptyWorkspaces = deleteEmptyWorkspaces;
		}

		@Override
		public void run() {

			while (!Thread.interrupted()) {
				try {
					LOGGER.log(Level.FINE, "Running a layer sweep");
					Long currentTime = new Date().getTime();
					CatalogBuilder cBuilder = new CatalogBuilder(catalog);

					// Get a cleaned list of workspaces
					List<WorkspaceInfo> workspaceInfoList = catalog.getWorkspaces();
					Iterator<WorkspaceInfo> it = workspaceInfoList.iterator();
					while (it.hasNext()) {
						WorkspaceInfo wsInfo = it.next();
						if (java.util.Arrays.asList(readOnlyWorkspaces).contains(wsInfo.getName())) {
							it.remove();
						}
					}

					for (WorkspaceInfo wsInfo : workspaceInfoList) {
						List<DataStoreInfo> dsInfoList = catalog.getDataStoresByWorkspace(wsInfo);

						if (!dsInfoList.isEmpty()) {
							for (DataStoreInfo dsInfo : dsInfoList) {
								DataAccess<? extends FeatureType, ? extends Feature> da = dsInfo.getDataStore(new DefaultProgressListener());
								List<Name> resourceNames = da.getNames();
								if (!resourceNames.isEmpty()) {
									for (Name resourceName : resourceNames) {
										FeatureSource<? extends FeatureType, ? extends Feature> featureSource = da.getFeatureSource(resourceName);
										URI dataSource = featureSource.getDataStore().getInfo().getSource();
										if ("file".equals(dataSource.getScheme())) {
											File featureSourceFile = new File(dataSource);
											Long fileAge = featureSourceFile.lastModified();

											if (currentTime - fileAge > this.maxAge) {
												LayerInfo layerInfo = catalog.getLayerByName(resourceName);
												catalog.detach(layerInfo);
												catalog.remove(layerInfo);
												String filePrefix = featureSourceFile.getName().substring(0, featureSourceFile.getName().lastIndexOf("."));
												Collection<File> fileList = FileUtils.listFiles(featureSourceFile.getParentFile(), FileFilterUtils.prefixFileFilter(filePrefix), null);
												for (File file : fileList) {
													if (FileUtils.deleteQuietly(file)) {
														LOGGER.log(Level.INFO, "Expired layer file removed @ {0}", file.getPath());
													}
												}
											}
											// If the store is empty now, it will be deleted in the next pass
										} else {
											LOGGER.log(Level.INFO, "Source {0} is not a file", resourceName.toString());
										}
									}
								} else if (this.deleteEmptyStores) {
									LOGGER.log(Level.INFO, "Found empty store '{0}'. Store will be removed", dsInfo.getName());
									cBuilder.removeStore(dsInfo, true);
									// If the workspace is empty now, it will be deleted in the next pass
								}
							}
						} else if (this.deleteEmptyWorkspaces) {
							LOGGER.log(Level.INFO, "Found empty workspace '{0}'. Workspace will be removed", wsInfo.getName());
							cBuilder.removeWorkspace(wsInfo, true);
						}
					}

				} catch (Exception ex) {
					LOGGER.log(Level.WARNING, "An error has occurred during execution of sweep", ex);
				} finally {
					// Clean up
				}
				try {
					// TODO: Use ThreadPoolExecutor to do this - ( http://docs.oracle.com/javase/6/docs/api/java/util/concurrent/ThreadPoolExecutor.html ) 
					Thread.sleep(runEveryMs);
				} catch (InterruptedException ex) {
					LOGGER.log(Level.INFO, "Sweeper thread is shutting down");
				}
			}

		}
	}
}
