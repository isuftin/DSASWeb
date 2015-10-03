package gov.usgs.cida.coastalhazards.service.util;

/**
 * An enum of all application properties used throughout the application.
 *
 * This is fed as a parameter to PropertyUtil
 *
 * @author isuftin
 */
public enum Property {

	DIRECTORIES_BASE("coastal-hazards.files.directory.base"),
	DIRECTORIES_UPLOAD("coastal-hazards.files.directory.upload"),
	DIRECTORIES_WORK("coastal-hazards.files.directory.work"),
	GEOSERVER_ENDPOINT("coastal-hazards.geoserver.endpoint"),
	GEOSERVER_USERNAME("coastal-hazards.geoserver.username"),
	GEOSERVER_PASSWORD("coastal-hazards.geoserver.password"),
	GEOSERVER_DATA_DIRECTORY("coastal-hazards.geoserver.datadir"),
	GEOSERVER_DEFAULT_PUBLISHED_WORKSPACE("coastal-hazards.workspace.published"),
	GEOSERVER_DEFAULT_UPLOAD_WORKSPACE("coastal-hazards.default.upload.workspace"),
	GEOSERVER_DEFAULT_UPLOAD_STORENAME("coastal-hazards.default.upload.storename"),
	GEOSERVER_LAYER_PROJECTION_POLICY("coastal-hazards.projection.policy"),
	CSW_INTERNAL_ENDPOINT("coastal-hazards.csw.internal.endpoint"),
	DEFAULT_OVERWRITE_LAYER("coastal-hazards.filename.param"),
	DEFAULT_SRS("coastal-hazards.default.srs"),
	FILE_UPLOAD_USE_CRS_FAILOVER("coastal-hazards.use.crs.failover"),
	N52_ENDPOINT("coastal-hazards.n52.endpoint"),
	FILE_UPLOAD_MAX_SIZE("coastal-hazards.files.upload.max-size"),
	FILE_UPLOAD_FILENAME_PARAM("coastal-hazards.filename.param"),
	JDBC_NAME("jndi.jdbc.name"),
	DB_SCHEMA_NAME("coastal-hazards.schema.name"),
	DEVELOPMENT("development");

	private final String key;

	Property(String key) {
		this.key = key;
	}

	public String getKey() {
		return this.key;
	}

}
