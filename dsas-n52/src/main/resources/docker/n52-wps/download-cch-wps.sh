#!/bin/bash

# Pull down the latest snapshot
group="gov.usgs.cida.coastalhazards"
artifact="coastal-hazards-n52"
nexusUrl="http://cida.usgs.gov/maven/service/local/artifact/maven/"
wpsArtifact="?r=cida-public-snapshots&g=${group}&a=${artifact}&v=LATEST&e=war"
wpsFileLocation="/wps.war"
finalWarLocation="/usr/local/tomcat/webapps/wps.war"
remoteSHA1=$(curl "${nexusUrl}resolve${wpsArtifact}" | xmllint --xpath "string(//sha1)" -)

# Check to see if we already have the N52 artifact and if so, verify that it's the same
# file as the remote version
if [ -f $finalWarLocation ]; then
    echo "${finalWarLocation} already exists"
    existingSHA1=$(sha1sum $finalWarLocation | cut -d ' ' -f 1)
    if [ $remoteSHA1 == $existingSHA1 ]; then
        # This is the same file as remote. We're done
        exit 0
    fi
    echo "${finalWarLocation} differs from remote version. Remote version will be downloaded"
fi

echo "Getting artifact from CIDA Nexus"
wget "${nexusUrl}redirect${wpsArtifact}" -O $wpsFileLocation

echo "Validating download"
localSHA1=$(sha1sum $wpsFileLocation | cut -d ' ' -f 1)
if [ $remoteSHA1 == $localSHA1  ]; then
    mv $wpsFileLocation $finalWarLocation
else
    echo "Remote file SHA1 differs from downloaded version."
    exit 1
fi