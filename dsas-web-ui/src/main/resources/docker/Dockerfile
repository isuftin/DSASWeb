FROM tomcat:8-jre8

MAINTAINER Ivan Suftin <isuftin@usgs.gov>

VOLUME ["/data"]

EXPOSE 8080

COPY setenv.sh /usr/local/tomcat/bin/setenv.sh

COPY download-postgresql-driver.sh /download-postgresql-driver.sh

COPY download-dsas-ui.sh /download-dsas-ui.sh

COPY tomcat-users.xml /usr/local/tomcat/conf/tomcat-users.xml

COPY context.xml /usr/local/tomcat/conf/context.xml

RUN apt-get update && \
    apt-get install -y libxml2-utils && \
    chmod a+x /download-postgresql-driver.sh && sleep 1 && /download-postgresql-driver.sh \
    chmod a+x /download-dsas-ui.sh && sleep 1 && /download-dsas-ui.sh