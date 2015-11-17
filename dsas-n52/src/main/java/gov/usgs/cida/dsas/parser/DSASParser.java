package gov.usgs.cida.dsas.parser;

import com.google.common.collect.Lists;
import gov.usgs.cida.dsas.exceptions.UnsupportedFeatureTypeException;
import gov.usgs.cida.dsas.wps.geom.Intersection;
import gov.usgs.cida.utilities.features.AttributeGetter;

import static gov.usgs.cida.utilities.features.Constants.BASELINE_DIST_ATTR;
import static gov.usgs.cida.utilities.features.Constants.BASELINE_ID_ATTR;
import static gov.usgs.cida.utilities.features.Constants.DATE_ATTR;
import static gov.usgs.cida.utilities.features.Constants.DISTANCE_ATTR;
import static gov.usgs.cida.utilities.features.Constants.EPR_ATTR;
import static gov.usgs.cida.utilities.features.Constants.LCI_ATTR;
import static gov.usgs.cida.utilities.features.Constants.LRR_ATTR;
import static gov.usgs.cida.utilities.features.Constants.NSM_ATTR;
import static gov.usgs.cida.utilities.features.Constants.SCE_ATTR;
import static gov.usgs.cida.utilities.features.Constants.TRANSECT_ID_ATTR;
import static gov.usgs.cida.utilities.features.Constants.UNCY_ATTR;
import static gov.usgs.cida.utilities.features.Constants.WCI_ATTR;
import static gov.usgs.cida.utilities.features.Constants.WLR_ATTR;
import static gov.usgs.cida.utilities.features.Constants.ECI_ATTR;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.datahandler.parser.AbstractParser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;

/**
 *
 * @author jiwalker
 */
public class DSASParser extends AbstractParser {
    
    private static List<String> rateColumns = Lists.newArrayList(BASELINE_DIST_ATTR, BASELINE_ID_ATTR, 
                        LRR_ATTR, LCI_ATTR, WLR_ATTR, WCI_ATTR, SCE_ATTR, NSM_ATTR, EPR_ATTR, ECI_ATTR);

    public DSASParser() {
        supportedIDataTypes.add(GenericFileDataBinding.class);
    }

    @Override
    public GenericFileDataBinding parse(InputStream input, String mimetype, String schema) {
        BufferedWriter buf = null;
        File xmlFile = null;
        GenericFileDataBinding fileBinding = null;
        try {
            File outfile = File.createTempFile(getClass().getSimpleName(), ".tsv");
            buf = new BufferedWriter(new FileWriter(outfile));

            xmlFile = File.createTempFile(getClass().getSimpleName(), ".xml");
            //FileUtils.copyInputStreamToFile(input, tempFile);
            consumeInputStreamToFile(input, xmlFile);
            FeatureCollection collection = new GMLStreamingFeatureCollection(xmlFile);
            FeatureType type = collection.getSchema();
            AttributeGetter getter = new AttributeGetter(type);

            if (getter.exists(TRANSECT_ID_ATTR)
                    && getter.exists(DISTANCE_ATTR)
                    && getter.exists(DATE_ATTR)
                    && getter.exists(UNCY_ATTR)) {
                Map<Integer, List<Intersection>> map = new TreeMap<>();
                FeatureIterator<SimpleFeature> features = collection.features();
                while (features.hasNext()) {
                    SimpleFeature feature = features.next();

                    Intersection intersection = new Intersection(feature, getter);
                    int transectId = intersection.getTransectId();
                    if (map.containsKey(transectId)) {
                        map.get(transectId).add(intersection);
                    } else {
                        List<Intersection> pointList = new LinkedList<>();
                        pointList.add(intersection);
                        map.put(transectId, pointList);
                    }
                }

                for (int key : map.keySet()) {
                    List<Intersection> points = map.get(key);
                    buf.write("# " + key);
                    buf.newLine();
                    for (Intersection p : points) {
                        buf.write(p.toString());
                        buf.newLine();
                    }
                }
            } else if (getter.exists(TRANSECT_ID_ATTR)
                    && getter.exists(rateColumns)) {
                FeatureIterator<SimpleFeature> features = collection.features();
                Map<Integer, SimpleFeature> featureMap = new TreeMap<>();

                buf.write(StringUtils.join(rateColumns, '\t'));
                buf.newLine();
                while (features.hasNext()) {
                    SimpleFeature feature = features.next();
                    int transectId = (Integer)getter.getValue(TRANSECT_ID_ATTR, feature);
                    featureMap.put(transectId, feature);
                }
                for (Integer id : featureMap.keySet()) {
                    SimpleFeature feature = featureMap.get(id);
                    List<String> values = Lists.newArrayListWithCapacity(rateColumns.size());
                    for (String column : rateColumns) {
                        if (column.equals(BASELINE_ID_ATTR)) {
                            String featureId = (String) getter.getValue(column, feature);
                            // relies on baselineId being featureId (Is that enforced anywhere?)
                            values.add(featureId.split("\\.")[1]);
                        }
                        else {
                            values.add(getter.getValue(column, feature).toString());
                        }
                    }
                    buf.write(StringUtils.join(values, '\t'));
                    buf.newLine();
                }
            } else {
                throw new UnsupportedFeatureTypeException("Feature must have match defined type");
            }
            buf.flush();
            IOUtils.closeQuietly(buf);
            fileBinding = new GenericFileDataBinding(new GenericFileData(outfile, "text/tsv"));
        } catch (IOException e) {
            throw new RuntimeException("Error creating temporary file", e);
        } catch (Exception e) {
            // if there is trouble parsing the feature collection (or it isn't one) just pass along the xml
            fileBinding = new GenericFileDataBinding(new GenericFileData(xmlFile, "text/xml"));
        } finally {
            IOUtils.closeQuietly(buf);
            return fileBinding;
        }
    }

    @Override
    public boolean isSupportedSchema(String schema) {
        return schema == null || super.isSupportedSchema(schema);
    }

    private void consumeInputStreamToFile(InputStream input, File file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String line;
        try {
            while (null != (line = reader.readLine())) {
                writer.write(line);
            }
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }
    }
}
