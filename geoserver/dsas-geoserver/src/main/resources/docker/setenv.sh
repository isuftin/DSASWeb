CONFIG_FILE="/usr/local/tomcat/conf/context.xml"

export CATALINA_OPTS="$CATALINA_OPTS -DGEOSERVER_DATA_DIR=/data"
export CATALINA_OPTS="$CATALINA_OPTS -server"
export CATALINA_OPTS="$CATALINA_OPTS -XX:MaxPermSize=1024m"
export CATALINA_OPTS="$CATALINA_OPTS -Xmx1024m"
export CATALINA_OPTS="$CATALINA_OPTS -Xms1024m"
export CATALINA_OPTS="$CATALINA_OPTS -XX:+HeapDumpOnOutOfMemoryError"
export CATALINA_OPTS="$CATALINA_OPTS -XX:+CMSClassUnloadingEnabled"
export CATALINA_OPTS="$CATALINA_OPTS -XX:HeapDumpPath=/heapdumps"
export CATALINA_OPTS="$CATALINA_OPTS -XX:SoftRefLRUPolicyMSPerMB=36000"
export CATALINA_OPTS="$CATALINA_OPTS -XX:+UseParallelGC"
export CATALINA_OPTS="$CATALINA_OPTS -Djava.awt.headless=true"

sed -i -e "s/DB_HOSTNAME/${DSAS_DB_PORT_5432_TCP_ADDR:-localhost}/" $CONFIG_FILE

sleep 1

cp -R /tmp/data /