FROM tomcat:7-jre8

MAINTAINER Ivan Suftin <isuftin@usgs.gov>

VOLUME ["/heapdumps", "/data"]

EXPOSE 8080

COPY setenv.sh /usr/local/tomcat/bin/setenv.sh

COPY download-geoserver.sh /download-geoserver.sh

COPY tomcat-users.xml /usr/local/tomcat/conf/tomcat-users.xml

COPY context.xml /usr/local/tomcat/conf/context.xml


RUN apt-get update && \
    apt-get install -y libxml2-utils && \
    chmod a+x /download-geoserver.sh && sleep 1 && /download-geoserver.sh