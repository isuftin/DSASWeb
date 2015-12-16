package gov.usgs.cida.dsas.wps;

import gov.usgs.cida.dsas.util.GeoserverUtils;
import gov.usgs.cida.dsas.util.LayerImportUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.gs.ImportProcess;
import org.geotools.data.DataAccess;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;

/**
 *
 * @author isuftin
 */
@DescribeProcess(
        title = "Append Columns To Layer",
description = "Append a set of columns to a given layer using provided data types and default values",
version = "1.0.0")
public class AppendColumnsToLayerProcess implements GeoServerProcess {

    private Catalog catalog;
    private LayerImportUtil importer;
    private ImportProcess importProcess;

    public AppendColumnsToLayerProcess(ImportProcess importer, Catalog catalog) {
        this.catalog = catalog;
        this.importer = new LayerImportUtil(catalog, importer);
        this.importProcess = importer;
    }

    @DescribeResult(name = "layerName", description = "Name of the new featuretype, with workspace")
    public String execute(
            @DescribeParameter(name = "layer", min = 1, description = "Input Layer To Append Columns To") String layer,
            @DescribeParameter(name = "workspace", min = 1, description = "Workspace in which layer resides") String workspace,
            @DescribeParameter(name = "store", min = 1, description = "Store in which layer resides") String store,
            @DescribeParameter(name = "column", min = 1, max = Integer.MAX_VALUE, description = "Column Name|Column Type|Column Description|Default Value") String[] columns)
            throws ProcessException {
        
        GeoserverUtils gsUtils = new GeoserverUtils(catalog);
        WorkspaceInfo ws = gsUtils.getWorkspaceByName(workspace);
        DataStoreInfo ds = gsUtils.getDataStoreByName(workspace, store);
        DataAccess<? extends FeatureType, ? extends Feature> dataAccess = gsUtils.getDataAccess(ds, null);
        FeatureSource<? extends FeatureType, ? extends Feature> featureSource = gsUtils.getFeatureSource(dataAccess, layer);
        FeatureCollection<? extends FeatureType, ? extends Feature> featureCollection = gsUtils.getFeatureCollection(featureSource);
        
        Map<String, String[]> newColumns = new HashMap<String, String[]>();
        for (String column : columns) {
            String[] columnAttributes = column.split("\\|");
            if (columnAttributes.length != 4) {
                throw new ProcessException("column input must have four attributes split by a pipe character: \"Column Name|Column Type|Column Description|Default Value\"");
            }

            String name = columnAttributes[0];
            String type = columnAttributes[1];
            String description = columnAttributes[2];
            String defaultValue = columnAttributes[3];
            newColumns.put(name, new String[]{type, description, defaultValue});
        }

        FeatureType ft = featureSource.getSchema();
        List<AttributeDescriptor> attributeList = new ArrayList(ft.getDescriptors());
        Set<String> colKeys = newColumns.keySet();

        for (String columnKey : colKeys) {
            String[] columnAttributes = newColumns.get(columnKey);
            String name = columnKey;
            if (ft.getDescriptor(name) == null) {
                char typeChar = columnAttributes[0].toLowerCase().charAt(0);
                String description = columnAttributes[1];

                /*
                 * Available types:
                 * <li>String</li>
                 * <li>Object - will return empty string</li>
                 * <li>Integer</li>
                 * <li>Double</li>
                 * <li>Long</li>
                 * <li>Short</li>
                 * <li>Float</li>
                 * <li>BigDecimal</li>
                 * <li>BigInteger</li>
                 * <li>Character</li>
                 * <li>Boolean</li> 
                 * <li>UUID</li>
                 * <li>Timestamp</li>
                 * <li>java.sql.Date</li>
                 * <li>java.sql.Time</li>
                 * <li>java.util.Date</li> 
                 * <li>JTS Geometries</li>
                 */
                AttributeTypeBuilder atb = new AttributeTypeBuilder();
                Object defaultValue = null;
                String defaultValueString = StringUtils.isBlank(columnAttributes[2]) ? "" : columnAttributes[2];
                switch (typeChar) {
                    case 's': {
                        atb.setBinding(String.class);
                        defaultValue = defaultValueString;
                        break;
                    }
                    case 'i': {
                        atb.setBinding(Integer.class);
                        try {
                            defaultValue = Integer.parseInt(defaultValueString);
                        } catch (NumberFormatException ex) {
                            // Ignore
                        }
                        break;
                    }
                    case 'l': {
                        atb.setBinding(Long.class);
                        try {
                            defaultValue = Long.parseLong(defaultValueString);
                        } catch (NumberFormatException ex) {
                            // Ignore
                        }
                        break;
                    }
                    case 'd': {
                        atb.setBinding(Double.class);
                        try {
                            defaultValue = Double.parseDouble(defaultValueString);
                        } catch (NumberFormatException ex) {
                            // Ignore
                        }
                        break;
                    }
                    case 'f': {
                        atb.setBinding(Float.class);
                        try {
                            defaultValue = Float.parseFloat(defaultValueString);
                        } catch (NumberFormatException ex) {
                            // Ignore
                        }
                        break;
                    }
                    case 'b': {
                        atb.setBinding(Boolean.class);
                        defaultValue = Boolean.parseBoolean(defaultValueString);
                        break;
                    }
                    default: {
                        throw new ProcessException("Invalid column type");
                    }
                }
                atb.setName(name);
                atb.setDescription(description);
                atb.setMinOccurs(0);
                atb.setMaxOccurs(1);
                atb.setNillable(true);
                atb.setIdentifiable(true);
                AttributeType atType = atb.buildType();
                atb.setDefaultValue(defaultValue);
                AttributeDescriptor descriptor = atb.buildDescriptor(new NameImpl(name), atType);

                attributeList.add(descriptor);
            }
        }

        SimpleFeatureType newFeatureType = new SimpleFeatureTypeImpl(
                ft.getName(),
                attributeList,
                ft.getGeometryDescriptor(),
                ft.isAbstract(),
                ft.getRestrictions(),
                ft.getSuper(),
                ft.getDescription());
        SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(newFeatureType);
        List<SimpleFeature> sfList = new ArrayList<SimpleFeature>();
        
        FeatureIterator<? extends Feature> features = null;
		try {
			features = featureCollection.features();
			while (features.hasNext()) {
				SimpleFeature feature = (SimpleFeature) features.next();
				SimpleFeature newFeature = SimpleFeatureBuilder.retype(feature, sfb);
				for (String columnKey : colKeys) {
					String name = columnKey;
					if (newFeature.getAttribute(new NameImpl(name)) == null) {
						newFeature.setAttribute(name, newFeature.getFeatureType().getDescriptor(name).getDefaultValue());
					}
				}
				sfList.add(newFeature);
			}
		} finally {
			if (null != features) {
				features.close();
			}
		}

        SimpleFeatureCollection collection = DataUtilities.collection(sfList);
        
        return gsUtils.replaceLayer(collection, layer, ds, ws, importProcess);
    }
}
