FROM rserve:latest

MAINTAINER Jordan Walker <jiwalker@usgs.gov>

USER root
RUN apt-get update && \
	apt-get install -y --force-yes \
    #git libssl1.0.0=1.0.1k-3+deb8u1 libssl-dev libcurl3=7.38.0-4+deb8u2 libcurl3-gnutls libcurl4-openssl-dev=7.38.0-4+deb8u2 
    libxml2-dev

RUN install.r doSNOW

USER rserve
