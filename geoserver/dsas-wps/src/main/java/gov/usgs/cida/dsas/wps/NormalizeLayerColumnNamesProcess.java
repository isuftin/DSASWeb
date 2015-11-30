package gov.usgs.cida.dsas.wps;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import gov.usgs.cida.dsas.util.GeoserverUtils;
import gov.usgs.cida.utilities.features.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.gs.ImportProcess;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.opengis.feature.Feature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

@DescribeProcess(
		title = "Normalize Layer Column Names",
		description = "Given a layer, workspace, and store, the column names will be normalized to upper-case.",
		version = "1.0.0")
public class NormalizeLayerColumnNamesProcess implements GeoServerProcess {

	private final Catalog catalog;
	private final ImportProcess importProcess;
	/**
	 * Geoserver relies on case-sensitive attributes. We cannot reformat these
	 * attributes. In addition, we do not write SLDs against these attributes,
	 * so we don't need to care.
	 */
	public static final ImmutableSet<String> COLUMN_NAMES_TO_IGNORE = (new ImmutableSortedSet.Builder<>(String.CASE_INSENSITIVE_ORDER)
			.add(
					Constants.DEFAULT_GEOM_ATTR,
					"id"
			)).build();

	public NormalizeLayerColumnNamesProcess(ImportProcess importer, Catalog catalog) {
		this.catalog = catalog;
		this.importProcess = importer;
	}

	@DescribeResult(name = "columnMapping", description = "List of column renames in format: 'Original Column Name|New Column Name\nOriginal Column Name|New Column Name...'")

	public String execute(
			@DescribeParameter(
					name = "workspacePrefixedLayerName",
					min = 1,
					max = 1,
					description = "Input layer on which to normalize columns prefixed with the workspace in which layer resides. Example workspaceName:layerName") String prefixedLayerName
	) throws ProcessException {
		String workspace;
		String layer;
		String store;
		String renameColumnMappingReport = "";
		int attributeListSize;
		int renameColumnMappingSize;
		String[] workspaceAndLayer;
		GeoserverUtils gsUtils;
		LayerInfo layerInfo;
		ResourceInfo resourceInfo;
		DataStoreInfo storeInfo;
		DataAccess<? extends FeatureType, ? extends Feature> dataAccess;
		FeatureSource<? extends FeatureType, ? extends Feature> featureSource;
		FeatureType featureType;
		List<AttributeDescriptor> attributeList;
		List<String> renameColumnMapping;

		if (StringUtils.isBlank(prefixedLayerName)) {
			throw new ProcessException("workspacePrefixedLayerName may not be blank.");
		}

		workspaceAndLayer = prefixedLayerName.split(":");

		if (2 != workspaceAndLayer.length) {
			throw new ProcessException("workspacePrefixedLayerName could not be parsed. Must be in the format:  workspaceName:layerName");
		}
		workspace = workspaceAndLayer[0];
		layer = workspaceAndLayer[1];

		gsUtils = new GeoserverUtils(catalog);
		layerInfo = catalog.getLayerByName(prefixedLayerName);

		if (null == layerInfo) {
			throw new ProcessException("Layer " + prefixedLayerName + " could not be found.");
		}

		resourceInfo = layerInfo.getResource();
		if (null == resourceInfo) {
			throw new ProcessException("Layer " + prefixedLayerName + " resource could not be found.");
		}

		if (null == resourceInfo.getNativeCRS()) {
			throw new ProcessException("Layer " + prefixedLayerName + " native CRS could not be found.");
		}

		if (null == resourceInfo.getCRS()) {
			throw new ProcessException("Layer " + prefixedLayerName + " CRS could not be found.");
		}

		storeInfo = (DataStoreInfo) resourceInfo.getStore();
		store = storeInfo.getName();
		dataAccess = gsUtils.getDataAccess(storeInfo, null);
		featureSource = gsUtils.getFeatureSource(dataAccess, layer);
		featureType = featureSource.getSchema();
		attributeList = new ArrayList(featureType.getDescriptors());
		attributeListSize = attributeList.size();
		renameColumnMapping = new ArrayList<>(attributeListSize);
		
		for (int attributeListIndex = 0; attributeListIndex < attributeListSize; attributeListIndex++) {
			Name attributeName = attributeList.get(attributeListIndex).getName();
			if (null != attributeName) {
				String oldName = attributeName.toString();
				if (!COLUMN_NAMES_TO_IGNORE.contains(oldName)) {
					String newName = oldName.toUpperCase(Locale.ENGLISH);
					if (!newName.equals(oldName)) {
						String mapping = oldName + "|" + newName;
						renameColumnMapping.add(mapping);
					}
				}
			}
		}
		renameColumnMappingSize = renameColumnMapping.size();
		if (0 != renameColumnMappingSize) {
			// I now have a map of columns to rename
			RenameLayerColumnsProcess renameLayerProc = new RenameLayerColumnsProcess(importProcess, catalog);
			String[] renameColumnMappingArray = renameColumnMapping.toArray(new String[renameColumnMappingSize]);
			renameLayerProc.execute(layer, workspace, store, renameColumnMappingArray);
			renameColumnMappingReport = StringUtils.join(renameColumnMappingArray, ",");
		}

		return renameColumnMappingReport;
	}
}
