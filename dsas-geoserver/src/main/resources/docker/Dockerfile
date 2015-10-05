FROM tomcat:7-jre8

MAINTAINER Ivan Suftin <isuftin@usgs.gov>

VOLUME ["/heapdumps", "/data"]

COPY setenv.sh /usr/local/tomcat/bin/setenv.sh

COPY download-geoserver.sh /download-geoserver.sh

COPY tomcat-users.xml /usr/local/tomcat/conf/tomcat-users.xml

RUN apt-get update && \
    apt-get install -y libxml2-utils && \
    /download-geoserver.sh