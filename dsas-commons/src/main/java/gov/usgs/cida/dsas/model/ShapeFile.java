package gov.usgs.cida.dsas.model;

//import gov.usgs.cida.dsas.model.DSASProcess;  // possible circular dependency?
import gov.usgs.cida.owsutils.commons.shapefile.utils.ProjectionUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.referencing.FactoryException;

/**
 * @author isuftin
 * OLD - new ShapeFile moved to UI under ...dsas.shapefile
 */
@Deprecated
public abstract class ShapeFile implements AutoCloseable {

    private DataStore ds = null;
    private File shapefileLocation = null;
    private DbaseFileReader dbfReader;
    //  protected DSASProcess process = null;
    static final String SHP = "shp";
    static final String SHX = "shx";
    static final String DBF = "dbf";
    static final String PRJ = "prj";
    static final String FBX = "fbx";
    static final String SBX = "sbx";
    static final String AIH = "aih";
    static final String IXS = "ixs";
    static final String MXS = "mxs";
    static final String ATX = "atx";
    static final String SHP_XML = "shp.xml";
    static final String CPG = "cpg";
    static final String CST = "cst";
    static final String CSV = "csv";
    static final String[] REQUIRED_FILES = new String[]{SHP, SHX, DBF}; // ShorelineShapefile and PDB files
    static final String[] OPTIONAL_FILES = new String[]{PRJ, FBX, SBX, AIH, IXS, MXS, ATX, SHP_XML, CPG, CST, CSV};

    public ShapeFile(File shapefileLocation) throws IOException {
        this.shapefileLocation = shapefileLocation;
        if (this.shapefileLocation.isFile()) {
            this.shapefileLocation = this.shapefileLocation.getParentFile();
        }

        validate();

        Map<String, Object> map = new HashMap<>(1);
        map.put("url", this.shapefileLocation.toURI().toURL());
        this.ds = DataStoreFinder.getDataStore(map);
        this.dbfReader = new DbaseFileReader(FileUtils.openInputStream(getDbfFile()).getChannel(), false, Charset.forName("UTF-8"));
    }

    public List<File> getRequiredFiles() {
        Collection<File> requiredFiles = FileUtils.listFiles(this.shapefileLocation, REQUIRED_FILES, false);
        return new ArrayList<>(requiredFiles);
    }

    public List<File> getOptionalFiles() {
        Collection<File> optionalFiles = FileUtils.listFiles(this.shapefileLocation, OPTIONAL_FILES, false);
        return new ArrayList<>(optionalFiles);
    }

    public boolean validate() throws IOException {
        if (!this.shapefileLocation.exists()) {
            throw new IOException(MessageFormat.format("File location at {0} does not exist.", shapefileLocation));
        }

        if (this.shapefileLocation.isFile() && this.shapefileLocation.getName().endsWith(".zip")) {
            throw new IOException("Shapefile may not be a zip file. Shapefile must point to a directory.");
        }

        List<File> requiredFiles = getRequiredFiles();

        if (requiredFiles.size() < 3) { //3 wont work for pdb types...
            throw new IOException("Shapefile does not meet content requirements for a shapefile (.shp, .shx, .dbf). Zip files qty: " + requiredFiles.size());
        }

        return true;
    }

    // this is needed to support the LIdar file as it does not have a shp file which prevents using the emptyArgMethod below
    public String getEPSGCode(File prjFile) throws IOException, FactoryException {
        String epsg = null;

        epsg = ProjectionUtils.getDeclaredEPSGFromPrj(prjFile);

        return epsg;
    }
    
    //only for files that have an shp file ie not Lidar
    public String getEPSGCode() throws IOException {

        SimpleFeatureSource featureSource = null;
        featureSource = ds.getFeatureSource(ds.getTypeNames()[0]);

        return featureSource.getBounds().getCoordinateReferenceSystem().getName().toString();
    }

    @Override
    public void close() throws Exception {
        if (this.dbfReader != null) {
            this.dbfReader.close();
        }
        if (this.ds != null) {
            this.ds.dispose();
        }

    }

    private File getDbfFile() throws FileNotFoundException, IOException {
        String[] DbfType = new String[]{DBF};
        Collection<File> dbfFiles = FileUtils.listFiles(this.shapefileLocation, DbfType, true);
        if (1 != dbfFiles.size() | dbfFiles.isEmpty()) {
            throw new FileNotFoundException("Unable to get dbf file. Missing from location:" + this.shapefileLocation + " or more than one found. Size: " + dbfFiles.size());
        }
        File dbfFile = dbfFiles.iterator().next();
        if (null == dbfFile | !dbfFile.canRead() | !dbfFile.exists() | !dbfFile.isFile()) {
            throw new IOException(MessageFormat.format("Unable to read dbfFile found in zip.", dbfFile));
        }

        return dbfFile;
    }

    /**
     * @return A list of attribute names found in the DBF file
     */
    public List<String> getDbfColumnNames() {
        List<String> names = new ArrayList<String>();
        int n = this.getDbaseFileHeader().getNumFields();
        for (int i = 0; i < n; i++) {
            names.add(this.getDbaseFileHeader().getFieldName(i));
        }

        return names;
    }

    public DbaseFileHeader getDbaseFileHeader() {
        return dbfReader.getHeader();
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

}
//    @Override
//    public abstract void setDSASProcess(DSASProcess process);
//    protected void updateProcessInformation(String string) {
//        if (this.process != null) {
//            this.process.addProcessInformation(string);
//        }
//    }
//    @Override
//    public boolean clear() {
//        boolean success = true;
//        Iterator<File> iterator = this.fileMap.values().iterator();
//        while (iterator.hasNext()) {
//            File parentDirectory = iterator.next().getParentFile();
//            success = FileUtils.deleteQuietly(parentDirectory);
//        }
//        if (success) {
//            this.fileMap.clear();
//        }
//        return success;
//    }

