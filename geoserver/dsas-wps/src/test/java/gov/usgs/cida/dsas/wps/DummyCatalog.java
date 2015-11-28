/*
 * U.S.Geological Survey Software User Rights Notice
 * 
 * Copied from http://water.usgs.gov/software/help/notice/ on September 7, 2012.  
 * Please check webpage for updates.
 * 
 * Software and related material (data and (or) documentation), contained in or
 * furnished in connection with a software distribution, are made available by the
 * U.S. Geological Survey (USGS) to be used in the public interest and in the 
 * advancement of science. You may, without any fee or cost, use, copy, modify,
 * or distribute this software, and any derivative works thereof, and its supporting
 * documentation, subject to the following restrictions and understandings.
 * 
 * If you distribute copies or modifications of the software and related material,
 * make sure the recipients receive a copy of this notice and receive or can get a
 * copy of the original distribution. If the software and (or) related material
 * are modified and distributed, it must be made clear that the recipients do not
 * have the original and they must be informed of the extent of the modifications.
 * 
 * For example, modified files must include a prominent notice stating the 
 * modifications made, the author of the modifications, and the date the 
 * modifications were made. This restriction is necessary to guard against problems
 * introduced in the software by others, reflecting negatively on the reputation of the USGS.
 * 
 * The software is public property and you therefore have the right to the source code, if desired.
 * 
 * You may charge fees for distribution, warranties, and services provided in connection
 * with the software or derivative works thereof. The name USGS can be used in any
 * advertising or publicity to endorse or promote any products or commercial entity
 * using this software if specific written permission is obtained from the USGS.
 * 
 * The user agrees to appropriately acknowledge the authors and the USGS in publications
 * that result from the use of this software or in products that include this
 * software in whole or in part.
 * 
 * Because the software and related material are free (other than nominal materials
 * and handling fees) and provided "as is," the authors, the USGS, and the 
 * United States Government have made no warranty, express or implied, as to accuracy
 * or completeness and are not obligated to provide the user with any support, consulting,
 * training or assistance of any kind with regard to the use, operation, and performance
 * of this software nor to provide the user with any updates, revisions, new versions or "bug fixes".
 * 
 * The user assumes all risk for any damages whatsoever resulting from loss of use, data,
 * or profits arising in connection with the access, use, quality, or performance of this software.
 */

package gov.usgs.cida.dsas.wps;

import java.util.Collection;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.platform.GeoServerResourceLoader;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class DummyCatalog implements Catalog {
    
    public DummyCatalog() {
    }
    
    @Override
    public LayerInfo getLayerByName(String string) {
        return null;
    }

    @Override
    public CatalogFacade getFacade() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CatalogFactory getFactory() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void add(StoreInfo si) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<RuntimeException> validate(StoreInfo si, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove(StoreInfo si) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void save(StoreInfo si) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends StoreInfo> T detach(T t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends StoreInfo> T getStore(String string, Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends StoreInfo> T getStoreByName(String string, Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends StoreInfo> T getStoreByName(String string, String string1, Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends StoreInfo> T getStoreByName(WorkspaceInfo wi, String string, Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends StoreInfo> List<T> getStores(Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(WorkspaceInfo wi, Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(String string, Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DataStoreInfo getDataStore(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DataStoreInfo getDataStoreByName(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DataStoreInfo getDataStoreByName(String string, String string1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DataStoreInfo getDataStoreByName(WorkspaceInfo wi, String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<DataStoreInfo> getDataStoresByWorkspace(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<DataStoreInfo> getDataStoresByWorkspace(WorkspaceInfo wi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<DataStoreInfo> getDataStores() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo wi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDefaultDataStore(WorkspaceInfo wi, DataStoreInfo dsi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CoverageStoreInfo getCoverageStore(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CoverageStoreInfo getCoverageStoreByName(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CoverageStoreInfo getCoverageStoreByName(String string, String string1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CoverageStoreInfo getCoverageStoreByName(WorkspaceInfo wi, String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(WorkspaceInfo wi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<CoverageStoreInfo> getCoverageStores() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends ResourceInfo> T getResource(String string, Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(String string, String string1, Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(NamespaceInfo ni, String string, Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(Name name, Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(String string, Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void add(ResourceInfo ri) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<RuntimeException> validate(ResourceInfo ri, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove(ResourceInfo ri) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void save(ResourceInfo ri) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends ResourceInfo> T detach(T t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends ResourceInfo> List<T> getResources(Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(NamespaceInfo ni, Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(String string, Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends ResourceInfo> T getResourceByStore(StoreInfo si, String string, Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo si, Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FeatureTypeInfo getFeatureType(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(String string, String string1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(NamespaceInfo ni, String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(Name name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<FeatureTypeInfo> getFeatureTypes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<FeatureTypeInfo> getFeatureTypesByNamespace(NamespaceInfo ni) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByStore(DataStoreInfo dsi, String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByDataStore(DataStoreInfo dsi, String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<FeatureTypeInfo> getFeatureTypesByStore(DataStoreInfo dsi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<FeatureTypeInfo> getFeatureTypesByDataStore(DataStoreInfo dsi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CoverageInfo getCoverage(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CoverageInfo getCoverageByName(String string, String string1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CoverageInfo getCoverageByName(NamespaceInfo ni, String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CoverageInfo getCoverageByName(Name name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CoverageInfo getCoverageByName(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<CoverageInfo> getCoverages() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<CoverageInfo> getCoveragesByNamespace(NamespaceInfo ni) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CoverageInfo getCoverageByCoverageStore(CoverageStoreInfo csi, String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<CoverageInfo> getCoveragesByCoverageStore(CoverageStoreInfo csi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void add(LayerInfo li) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<RuntimeException> validate(LayerInfo li, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove(LayerInfo li) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void save(LayerInfo li) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public LayerInfo detach(LayerInfo li) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<CoverageInfo> getCoveragesByStore(CoverageStoreInfo csi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public LayerInfo getLayer(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public LayerInfo getLayerByName(Name name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<LayerInfo> getLayers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<LayerInfo> getLayers(ResourceInfo ri) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<LayerInfo> getLayers(StyleInfo si) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void add(MapInfo mi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove(MapInfo mi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void save(MapInfo mi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MapInfo detach(MapInfo mi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<MapInfo> getMaps() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MapInfo getMap(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MapInfo getMapByName(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void add(LayerGroupInfo lgi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<RuntimeException> validate(LayerGroupInfo lgi, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove(LayerGroupInfo lgi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void save(LayerGroupInfo lgi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public LayerGroupInfo detach(LayerGroupInfo lgi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<LayerGroupInfo> getLayerGroups() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo wi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public LayerGroupInfo getLayerGroup(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String string, String string1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo wi, String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void add(StyleInfo si) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<RuntimeException> validate(StyleInfo si, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove(StyleInfo si) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void save(StyleInfo si) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StyleInfo detach(StyleInfo si) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StyleInfo getStyle(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StyleInfo getStyleByName(String string, String string1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StyleInfo getStyleByName(WorkspaceInfo wi, String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StyleInfo getStyleByName(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<StyleInfo> getStyles() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<StyleInfo> getStylesByWorkspace(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo wi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void add(NamespaceInfo ni) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<RuntimeException> validate(NamespaceInfo ni, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove(NamespaceInfo ni) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void save(NamespaceInfo ni) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NamespaceInfo detach(NamespaceInfo ni) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NamespaceInfo getNamespace(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NamespaceInfo getNamespaceByPrefix(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NamespaceInfo getNamespaceByURI(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NamespaceInfo getDefaultNamespace() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDefaultNamespace(NamespaceInfo ni) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<NamespaceInfo> getNamespaces() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void add(WorkspaceInfo wi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<RuntimeException> validate(WorkspaceInfo wi, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove(WorkspaceInfo wi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void save(WorkspaceInfo wi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public WorkspaceInfo detach(WorkspaceInfo wi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDefaultWorkspace(WorkspaceInfo wi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<WorkspaceInfo> getWorkspaces() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public WorkspaceInfo getWorkspace(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public WorkspaceInfo getWorkspaceByName(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<CatalogListener> getListeners() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addListener(CatalogListener cl) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeListener(CatalogListener cl) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void fireAdded(CatalogInfo ci) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void fireModified(CatalogInfo ci, List<String> list, List list1, List list2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void firePostModified(CatalogInfo ci) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void fireRemoved(CatalogInfo ci) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ResourcePool getResourcePool() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setResourcePool(ResourcePool rp) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GeoServerResourceLoader getResourceLoader() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setResourceLoader(GeoServerResourceLoader gsrl) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void dispose() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void accept(CatalogVisitor cv) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeListeners(Class type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends CatalogInfo> int count(Class<T> type, Filter filter) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T extends CatalogInfo> T get(Class<T> type, Filter filter) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(Class<T> type, Filter filter) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(Class<T> type, Filter filter, Integer intgr, Integer intgr1, SortBy sortby) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
