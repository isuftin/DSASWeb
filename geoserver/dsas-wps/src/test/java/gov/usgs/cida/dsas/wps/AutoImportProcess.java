//@url: http://cida-eros-stash.er.usgs.gov:7990/stash/projects/CIDAGS/repos/cida-geoserver/browse/cida-geoserver-wps/src/main/java/org/geoserver/wps/gs/ImportProcess.java?at=bcfc92d20a41bb8590a22e6e5adf3cb6c22eca5f
package gov.usgs.cida.dsas.wps;


/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

import com.google.common.io.Files;
import gov.usgs.cida.utilities.features.Constants;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.jxpath.ri.compiler.Constant;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.gs.ImportProcess;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Imports a feature collection into the GeoServer catalog
 * This process automates the creation of workspaces, stores, and layers given
 * that they do not yet exist.
 * TODO: Add coverage into execute properly, right now coverages will not work!
 * @author Andrea Aime - OpenGeo
 * 
 */
@DescribeProcess(title = "Import to Catalog", description = "Imports a feature collection into the catalog")
public class AutoImportProcess  extends ImportProcess{

    static final Logger LOGGER = Logging.getLogger(AutoImportProcess.class);

    private Catalog catalog;
	private GeoServerDataDirectory dataDir;
	
	public AutoImportProcess(Catalog catalog){
        super(catalog);
		this.catalog=catalog;
		this.dataDir = new GeoServerDataDirectory(
				Files.createTempDir()
		);
    }
    public AutoImportProcess(Catalog catalog, GeoServerDataDirectory dataDirectory) throws IOException {
        super(catalog);
		this.catalog=catalog;
		this.dataDir = dataDirectory;
    }

    @DescribeResult(name = "layerName", description = "Name of the new featuretype, with workspace")
    public String execute(
            @DescribeParameter(name = "features", description = "Input feature collection") SimpleFeatureCollection features,
            @DescribeParameter(name = "coverage", min = 0, description = "Input raster") GridCoverage2D coverage,
            @DescribeParameter(name = "workspace", min = 0, description = "Target workspace (default is the system default)") String workspace,
            @DescribeParameter(name = "store", min = 0, description = "Target store (default is the workspace default) - if parameter is provided and store does not exist, a best effort attempt will be made to create it") String store,
            @DescribeParameter(name = "name", min = 0, description = "Name of the new featuretype (default is the name of the features in the collection)") String name,
            @DescribeParameter(name = "srs", min = 0, description = "Target coordinate reference system (default is based on source when possible)") CoordinateReferenceSystem srs,
            @DescribeParameter(name = "srsHandling", min = 0, description = "Desired SRS handling (default is FORCE_DECLARED, others are REPROJECT_TO_DECLARED or NONE)") ProjectionPolicy srsHandling,
            @DescribeParameter(name = "styleName", min = 0, description = "Name of the style to be associated with the layer (default is a standard geometry-specific style)") String styleName)
            throws ProcessException {

        // first off, decide what is the target store
        WorkspaceInfo ws;
        if (workspace != null) {
            ws = catalog.getWorkspaceByName(workspace);
            if (ws == null) {
                throw new ProcessException("Could not find workspace " + workspace);
            }
        } else {
            ws = catalog.getDefaultWorkspace();
            if (ws == null) {
                throw new ProcessException(
                        "The catalog is empty, could not find a default workspace");
            }
        }

        // ok, find the target store
		DataStoreInfo storeInfo;
		if (store != null) {
			// TODO - This only works for file based data stores. This will not work for 
			// database-backed datastores
			storeInfo = catalog.getDataStoreByName(ws.getName(), store);
			if (storeInfo == null) {
				try {
					LOGGER.log(Level.INFO, "Store {0} not found. Will try to create", store);
					File dataRoot = dataDir.findWorkspaceDir(ws);
					if (dataRoot == null) {
						dataRoot = new File(dataDir.root() + File.separator + "workspaces" + File.separator + ws.getName());
						org.apache.commons.io.FileUtils.forceMkdir(dataRoot);
					}
					String dataDirLocation = dataRoot.getPath();
					LOGGER.log(Level.INFO, "GEOSERVER_DATA_DIR found @ {0}", dataDirLocation);
					File storeDirectory = new File(dataRoot, store);
					if (!storeDirectory.exists()) {
						LOGGER.log(Level.INFO, "Store directory @ {0} not found. Will try to create", storeDirectory.getPath());
						storeDirectory.mkdirs();
					}
					LOGGER.log(Level.INFO, "Store directory @ {0} created", storeDirectory.getPath());
					CatalogBuilder builder = new CatalogBuilder(catalog);
					storeInfo = builder.buildDataStore(store);
					
					try {
						storeInfo.getConnectionParameters().put("url", storeDirectory.toURI().toURL().toExternalForm());
					} catch (MalformedURLException ex) {
						storeInfo.getConnectionParameters().put("url", "file://" + storeDirectory.getPath());
					}
					catalog.add(storeInfo);
					LOGGER.info("Store created");
				} catch (IOException ex) {
					Logger.getLogger(AutoImportProcess.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		} else {
			storeInfo = catalog.getDefaultDataStore(ws);
			if (storeInfo == null) {
				throw new ProcessException("Could not find a default store in workspace "
						+ ws.getName());
			}
		}

        // check if the target layer and the target feature type are not
        // already there (this is a half-assed attempt as we don't have
        // an API telling us how the feature type name will be changed
        // by DataStore.createSchema(...), but better than fully importing
        // the data into the target store to find out we cannot create the layer...)
        String tentativeTargetName = null;
        if (name != null) {
            tentativeTargetName = ws.getName() + ":" + name;
        } else {
            tentativeTargetName = ws.getName() + ":" + features.getSchema().getTypeName();
        }
        if (catalog.getLayer(tentativeTargetName) != null) {
            throw new ProcessException("Target layer " + tentativeTargetName + " already exists");
        }

        // check the target style if any
        StyleInfo targetStyle = null;
        if (styleName != null) {
            targetStyle = catalog.getStyleByName(styleName);
            if (targetStyle == null) {
                throw new ProcessException("Could not find style " + styleName);
            }
        }

        // check the target crs
        String targetSRSCode = null;
        if (srs != null) {
            try {
                Integer code = CRS.lookupEpsgCode(srs, true);
                if (code == null) {
                    throw new WPSException("Could not find a EPSG code for " + srs);
                }
                targetSRSCode = "EPSG:" + code;
            } catch (Exception e) {
                throw new ProcessException("Could not lookup the EPSG code for the provided srs", e);
            }
        } else {
            // check we can extract a code from the original data
            GeometryDescriptor gd = features.getSchema().getGeometryDescriptor();
            if (gd == null) {
                // data is geometryless, we need a fake SRS
                targetSRSCode = "EPSG:4326";
                srsHandling = ProjectionPolicy.FORCE_DECLARED;
            } else {
                CoordinateReferenceSystem nativeCrs = gd.getCoordinateReferenceSystem();
                if (nativeCrs == null) {
                    throw new ProcessException("The original data has no native CRS, "
                            + "you need to specify the srs parameter");
                } else {
                    try {
                        Integer code = CRS.lookupEpsgCode(nativeCrs, true);
                        if (code == null) {
                            throw new ProcessException("Could not find an EPSG code for data "
                                    + "native spatial reference system: " + nativeCrs);
                        } else {
                            targetSRSCode = "EPSG:" + code;
                        }
                    } catch (Exception e) {
                        throw new ProcessException("Failed to loookup an official EPSG code for "
                                + "the source data native " + "spatial reference system", e);
                    }
                }
            }
        }

        // import the data into the target store
        SimpleFeatureType targetType;
        try {
            targetType = importDataIntoStore(features, name, storeInfo);
        } catch (IOException e) {
            throw new ProcessException("Failed to import data into the target store", e);
        }

        // now import the newly created layer into GeoServer
        try {
            CatalogBuilder cb = new CatalogBuilder(catalog);
            cb.setStore(storeInfo);

            // build the typeInfo and set CRS if necessary
            FeatureTypeInfo typeInfo = cb.buildFeatureType(targetType.getName());
            if (targetSRSCode != null) {
                typeInfo.setSRS(targetSRSCode);
				
				// The import process kills the native project and sets it to the 
				// target projection. The only time this doesn't happen is when a 
				// target SRS isn't passed into the import process. Of course, that's 
				// also not what we want since then we don't have a declared SRS. 
				// Typical requirements for us is to keep the native SRS and reproject
				// to declared.
				//
				// This fixes that, though I'm unsure if this 
				// is desired in every case. - I.S.
				typeInfo.setNativeCRS(features.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem());
            }
            if (srsHandling != null) {
                typeInfo.setProjectionPolicy(srsHandling);
            }
            // compute the bounds
            cb.setupBounds(typeInfo);

            // build the layer and set a style
            LayerInfo layerInfo = cb.buildLayer(typeInfo);
            if (targetStyle != null) {
                layerInfo.setDefaultStyle(targetStyle);
            }

            catalog.add(typeInfo);
            catalog.add(layerInfo);

            return typeInfo.getPrefixedName();
        } catch (Exception e) {
            throw new ProcessException(
                    "Failed to complete the import inside the GeoServer catalog", e);
        }
    }

    private SimpleFeatureType importDataIntoStore(SimpleFeatureCollection features, String name,
            DataStoreInfo storeInfo) throws IOException, ProcessException {
        SimpleFeatureType targetType;
        // grab the data store
        DataStore ds = (DataStore) storeInfo.getDataStore(null);

        // decide on the target ft name
        SimpleFeatureType sourceType = features.getSchema();
        if (name != null) {
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            tb.init(sourceType);
            tb.setName(name);
            sourceType = tb.buildFeatureType();
        }

        // create the schema
        ds.createSchema(sourceType);

        // try to get the target feature type (might have slightly different
        // name and structure)
        targetType = ds.getSchema(sourceType.getTypeName());
        if (targetType == null) {
            // ouch, the name was changed... we can only guess now...
            // try with the typical Oracle mangling
            targetType = ds.getSchema(sourceType.getTypeName().toUpperCase());
        }

        if (targetType == null) {
            throw new WPSException(
                    "The target schema was created, but with a name "
                            + "that we cannot relate to the one we provided the data store. Cannot proceeed further");
        } else {
            // check the layer is not already there
            String newLayerName = storeInfo.getWorkspace().getName() + ":"
                    + targetType.getTypeName();
            LayerInfo layer = catalog.getLayerByName(newLayerName);
            // todo: we should not really reach here and know beforehand what the targetType
            // name is, but if we do we should at least get a way to drop it
            if (layer != null) {
                throw new ProcessException("Target layer " + newLayerName
                        + " already exists in the catalog");
            }
        }

        // try to establish a mapping with old and new attributes. This is again
        // just guesswork until we have a geotools api that will give us the
        // exact mapping to be performed
        Map<String, String> mapping = buildAttributeMapping(sourceType, targetType);

        // start a transaction and fill the target with the input features
        Transaction t = new DefaultTransaction();
        SimpleFeatureStore fstore = (SimpleFeatureStore) ds.getFeatureSource(targetType
                .getTypeName());
        fstore.setTransaction(t);
        SimpleFeatureIterator fi = features.features();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(targetType);
        while (fi.hasNext()) {
            SimpleFeature source = fi.next();
            fb.reset();
            for (String sname : mapping.keySet()) {
                fb.set(mapping.get(sname), source.getAttribute(sname));
            }
            SimpleFeature target = fb.buildFeature(null);
            fstore.addFeatures(DataUtilities.collection(target));
        }
        t.commit();
        t.close();

        return targetType;
    }

    /**
     * Applies a set of heuristics to find which target attribute corresponds to a certain input
     * attribute
     * 
     * @param sourceType
     * @param targetType
     * @return
     */
    Map<String, String> buildAttributeMapping(SimpleFeatureType sourceType,
            SimpleFeatureType targetType) {
        // look for the typical manglings. For example, if the target is a
        // shapefile store it will move the geometry and name it the_geom

        // collect the source names
        Set<String> sourceNames = new HashSet<String>();
        for (AttributeDescriptor sd : sourceType.getAttributeDescriptors()) {
            sourceNames.add(sd.getLocalName());
        }

        // first check if we have been kissed by sheer luck and the names are
        // the same
        Map<String, String> result = new HashMap<String, String>();
        for (String name : sourceNames) {
            if (targetType.getDescriptor(name) != null) {
                result.put(name, name);
            }
        }
        sourceNames.removeAll(result.keySet());

        // then check for simple case difference (Oracle case)
        for (String name : sourceNames) {
            for (AttributeDescriptor td : targetType.getAttributeDescriptors()) {
                if (td.getLocalName().equalsIgnoreCase(name)) {
                    result.put(name, td.getLocalName());
                    break;
                }
            }
        }
        sourceNames.removeAll(result.keySet());

        // then check attribute names being cut (another Oracle case)
        for (String name : sourceNames) {
            String loName = name.toLowerCase();
            for (AttributeDescriptor td : targetType.getAttributeDescriptors()) {
                String tdName = td.getLocalName().toLowerCase();
                if (loName.startsWith(tdName)) {
                    result.put(name, td.getLocalName());
                    break;
                }
            }
        }
        sourceNames.removeAll(result.keySet());

        // consider the shapefile geometry descriptor mangling
        if (targetType.getGeometryDescriptor() != null
                && Constants.DEFAULT_GEOM_ATTR.equals(targetType.getGeometryDescriptor().getLocalName())
                && !Constants.DEFAULT_GEOM_ATTR.equalsIgnoreCase(sourceType.getGeometryDescriptor().getLocalName())) {
            result.put(sourceType.getGeometryDescriptor().getLocalName(), Constants.DEFAULT_GEOM_ATTR);
        }

        // and finally we return with as much as we can match
        if (!sourceNames.isEmpty()) {
            LOGGER.warning("Could not match the following attributes " + sourceNames
                    + " to the target feature type ones: " + targetType);
        }
        return result;
    }
}
