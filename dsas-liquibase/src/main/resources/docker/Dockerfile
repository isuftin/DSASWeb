FROM kartoza/postgis:9.4-2.1

MAINTAINER Ivan Suftin <isuftin@usgs.gov>

COPY init-database.txt /init-database.txt

COPY run_liquibase.sh /run_liquibase.sh

COPY rerun_liquibase.sh /rerun_liquibase.sh

RUN chmod 755 /run_liquibase.sh
RUN chmod 755 /rerun_liquibase.sh

RUN echo 'host all all 0.0.0.0/0 md5' >> /etc/postgresql/9.4/main/pg_hba.conf

# Adds commands to create the dsas database
RUN sed -i '80rinit-database.txt' /start-postgis.sh

# Runs liquibase on running database to create DSAS schema
RUN sed -i '127rrun_liquibase.sh' /start-postgis.sh

# Required for JRE 8 - Java 8 is required to run the Liquibase JAR
RUN echo "deb http://http.debian.net/debian jessie-backports main" >> /etc/apt/sources.list

RUN apt-get update \
	&& apt-get install -y gettext libxml2-utils wget curl unzip openjdk-8-jre

ENV LIQUIBASE_HOME /opt/liquibase

RUN mkdir -p $LIQUIBASE_HOME/dsas_liquibase

ADD https://github.com/liquibase/liquibase/releases/download/liquibase-parent-3.4.2/liquibase-3.4.2-bin.tar.gz $LIQUIBASE_HOME/

ADD https://jdbc.postgresql.org/download/postgresql-9.4-1204.jdbc42.jar $LIQUIBASE_HOME/lib/

COPY liquibase.properties $LIQUIBASE_HOME/

COPY download-liquibase.sh $LIQUIBASE_HOME/

RUN tar -xzf $LIQUIBASE_HOME/liquibase-3.4.2-bin.tar.gz -C $LIQUIBASE_HOME/

RUN chmod 755 $LIQUIBASE_HOME/download-liquibase.sh \
    && sleep 1 \
	&& $LIQUIBASE_HOME/download-liquibase.sh