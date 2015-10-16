FROM java:8

MAINTAINER Jordan Walker <jiwalker@usgs.gov>

ENV LIQUIBASE_HOME /opt/liquibase

RUN apt-get update \
	&& apt-get install -y gettext libxml2-utils

RUN mkdir $LIQUIBASE_HOME
ADD https://github.com/liquibase/liquibase/releases/download/liquibase-parent-3.4.1/liquibase-3.4.1-bin.tar.gz $LIQUIBASE_HOME/
ADD https://jdbc.postgresql.org/download/postgresql-9.4-1204.jdbc42.jar $LIQUIBASE_HOME/lib/
COPY liquibase.properties $LIQUIBASE_HOME/
COPY entrypoint.sh $LIQUIBASE_HOME/
COPY download-liquibase.sh $LIQUIBASE_HOME/

WORKDIR $LIQUIBASE_HOME
RUN tar -xzf liquibase-3.4.1-bin.tar.gz
RUN chmod 755 download-liquibase.sh \
	&& ./download-liquibase.sh \
	&& mkdir dsas_liquibase \
	&& cd dsas_liquibase \
	&& jar xf ../dsas-liquibase.jar
RUN chmod 755 entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]
CMD ["./liquibase", "--changeLogFile=dsas_liquibase/liquibase/changeLog.xml", "update"]
