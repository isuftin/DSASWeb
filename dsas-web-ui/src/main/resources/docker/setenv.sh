CONFIG_FILE="/usr/local/tomcat/conf/context.xml"

sed -i -e "s/DSAS_GEOSERVER_HOSTNAME/${DSAS_GEOSERVER_PORT_8080_TCP_ADDR:-localhost}/" $CONFIG_FILE
sed -i -e "s/DSAS_N52_HOSTNAME/${DSAS_52N_PORT_8080_TCP_ADDR:-localhost}/" $CONFIG_FILE
sed -i -e "s/DB_HOSTNAME/${DSAS_DB_PORT_5432_TCP_ADDR:-localhost}/" $CONFIG_FILE