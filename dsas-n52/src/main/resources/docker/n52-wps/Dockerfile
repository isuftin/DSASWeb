FROM tomcat:8-jre8

MAINTAINER Ivan Suftin <isuftin@usgs.gov>

COPY tomcat-users.xml /usr/local/tomcat/conf/tomcat-users.xml

COPY context.xml /usr/local/tomcat/conf/context.xml

COPY download-dsas-wps.sh /download-dsas-wps.sh

COPY setenv.sh /usr/local/tomcat/bin/setenv.sh

COPY wps_config.xml /wps_config.xml

RUN apt-get update && \
   apt-get install -y libxml2-utils && \
   chmod a+x /download-dsas-wps.sh && sleep 1 && /download-dsas-wps.sh
   