FROM rserve:latest

MAINTAINER Jordan Walker <jiwalker@usgs.gov>

USER root
RUN apt-get update && \
    apt-get install -y --force-yes git libssl1.0.0 libssl-dev libcurl3 libcurl3-gnutls libcurl4-openssl-dev libxml2-dev

RUN install.r devtools
RUN R -e 'library(devtools); install_github("USGS-R/hazarditems");'

USER rserve