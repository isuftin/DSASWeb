FROM kartoza/postgis:9.4-2.1

MAINTAINER Ivan Suftin <isuftin@usgs.gov>

COPY init-database.txt /init-database.txt

RUN sed -i '80rinit-database.txt' /start-postgis.sh