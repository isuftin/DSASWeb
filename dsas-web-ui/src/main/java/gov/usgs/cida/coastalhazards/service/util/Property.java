package gov.usgs.cida.coastalhazards.service.util;

/**
 * An enum of all application properties used throughout the application.
 *
 * This is fed as a parameter to PropertyUtil
 *
 * @author isuftin
 */
public enum Property {

	DIRECTORIES_BASE("dsas.files.directory.base"),
	DIRECTORIES_UPLOAD("dsas.files.directory.upload"),
	DIRECTORIES_WORK("dsas.files.directory.work"),
	GEOSERVER_ENDPOINT("dsas.geoserver.endpoint"),
	GEOSERVER_USERNAME("dsas.geoserver.username"),
	GEOSERVER_PASSWORD("dsas.geoserver.password"),
	GEOSERVER_DATA_DIRECTORY("dsas.geoserver.datadir"),
	GEOSERVER_DEFAULT_PUBLISHED_WORKSPACE("dsas.workspace.published"),
	GEOSERVER_DEFAULT_UPLOAD_WORKSPACE("dsas.default.upload.workspace"),
	GEOSERVER_DEFAULT_UPLOAD_STORENAME("dsas.default.upload.storename"),
	GEOSERVER_LAYER_PROJECTION_POLICY("dsas.projection.policy"),
	CSW_INTERNAL_ENDPOINT("dsas.csw.internal.endpoint"),
	DEFAULT_OVERWRITE_LAYER("dsas.filename.param"),
	DEFAULT_SRS("dsas.default.srs"),
	FILE_UPLOAD_USE_CRS_FAILOVER("dsas.use.crs.failover"),
	N52_ENDPOINT("dsas.n52.endpoint"),
	FILE_UPLOAD_MAX_SIZE("dsas.files.upload.max-size"),
	FILE_UPLOAD_FILENAME_PARAM("dsas.filename.param"),
	JDBC_NAME("jndi.jdbc.name"),
	DB_SCHEMA_NAME("dsas.schema.name"),
	DEVELOPMENT("development");

	private final String key;

	Property(String key) {
		this.key = key;
	}

	public String getKey() {
		return this.key;
	}

}
