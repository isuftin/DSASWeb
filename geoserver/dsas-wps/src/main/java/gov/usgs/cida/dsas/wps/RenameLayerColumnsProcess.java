package gov.usgs.cida.dsas.wps;

import gov.usgs.cida.dsas.util.GeoserverUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.gs.ImportProcess;
import org.geotools.data.DataAccess;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.geotools.feature.type.AttributeDescriptorImpl;
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
import org.opengis.feature.type.Name;

/**
 *
 * @author isuftin
 */
@DescribeProcess(
		title = "Rename Layer Columns",
		description = "Given a layer and a set of column name to column name mappings, will rename a set of columns",
		version = "1.0.0")
public class RenameLayerColumnsProcess implements GeoServerProcess {

	private Catalog catalog;
	private final ImportProcess importProcess;

	public RenameLayerColumnsProcess(ImportProcess importer, Catalog catalog) {
		this.catalog = catalog;
		this.importProcess = importer;
	}

	@DescribeResult(name = "layerName", description = "Name of the new featuretype, with workspace")
	public String execute(
			@DescribeParameter(name = "layer", min = 1, description = "Input Layer To Append Columns To") String layer,
			@DescribeParameter(name = "workspace", min = 1, description = "Workspace in which layer resides") String workspace,
			@DescribeParameter(name = "store", min = 1, description = "Store in which layer resides") String store,
			@DescribeParameter(name = "column", min = 1, max = Integer.MAX_VALUE, description = "Original Column Name|New Column Name") String[] columns)
			throws ProcessException {

		GeoserverUtils gsUtils = new GeoserverUtils(catalog);
		WorkspaceInfo ws = gsUtils.getWorkspaceByName(workspace);
		DataStoreInfo ds = gsUtils.getDataStoreByName(ws.getName(), store);
		DataAccess<? extends FeatureType, ? extends Feature> da = gsUtils.getDataAccess(ds, null);
		FeatureSource<? extends FeatureType, ? extends Feature> featureSource = gsUtils.getFeatureSource(da, layer);
		FeatureType featureType = featureSource.getSchema();
		List<AttributeDescriptor> attributeList = new ArrayList(featureType.getDescriptors());
		List<SimpleFeature> sfList = new ArrayList<>();
		FeatureCollection<? extends FeatureType, ? extends Feature> featureCollection = gsUtils.getFeatureCollection(featureSource);

		Map<String, String> columnNameMap = new HashMap<>();
		for (String column : columns) {
			String[] columnArr = column.split("\\|");
			String originalColumnName = columnArr[0];
			String newColumnName = columnArr[1];
			columnNameMap.put(originalColumnName, newColumnName);
		}
		AttributeDescriptor attributeDescriptor;
		int length = attributeList.size();

		for (int i = 0; i < length; i++) {
			attributeDescriptor = attributeList.get(i);
			String attributeName = attributeDescriptor.getName().toString();
			if (columnNameMap.containsKey(attributeName)) {
				AttributeType type = attributeDescriptor.getType();
				Name newName = new NameImpl(columnNameMap.get(attributeName));
				int minOccurs = attributeDescriptor.getMinOccurs();
				int maxOccurs = attributeDescriptor.getMaxOccurs();
				boolean isNillable = attributeDescriptor.isNillable();
				Object defaultValue = attributeDescriptor.getDefaultValue();
				AttributeDescriptor renamedAttributeDescriptor = new AttributeDescriptorImpl(type, newName, minOccurs, maxOccurs, isNillable, defaultValue);
				attributeList.set(i, renamedAttributeDescriptor);
			}
		}

		SimpleFeatureType newFeatureType = new SimpleFeatureTypeImpl(
				featureType.getName(),
				attributeList,
				featureType.getGeometryDescriptor(),
				featureType.isAbstract(),
				featureType.getRestrictions(),
				featureType.getSuper(),
				featureType.getDescription());

		SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(newFeatureType);

		FeatureIterator<? extends Feature> features = null;
		try {
			features = featureCollection.features();
			while (features.hasNext()) {
				SimpleFeature feature = (SimpleFeature) features.next();
				SimpleFeature newFeature = SimpleFeatureBuilder.retype(feature, sfb);
				List<Object> oldAttributes = feature.getAttributes();
				List<Object> newAttributes = newFeature.getAttributes();
				// If the feature type contains attributes in which the original 
				// feature does not have a value for, 
				// the value in the resulting feature is set to null.
				// Need to copy it back from the original feature
				for (int aInd = 0; aInd < newAttributes.size(); aInd++) {
					Object oldAttribute = oldAttributes.get(aInd);
					Object newAttribute = newAttributes.get(aInd);

					if (newAttribute == null && oldAttribute != null) {
						newFeature.setAttribute(aInd, oldAttribute);
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
