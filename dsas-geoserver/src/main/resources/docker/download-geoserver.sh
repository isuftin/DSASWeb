#!/bin/bash 

# Pull down the latest snapshot
group="gov.usgs.cida.coastalhazards"
artifact="coastal-hazards-geoserver"
nexusUrl="http://cida.usgs.gov/maven/service/local/artifact/maven/"
gsArtifact="?r=cida-public-snapshots&g=${group}&a=${artifact}&v=LATEST&e=war"
gsFileLocation="/coastal-hazards-geoserver.war"
finalWarLocation="/usr/local/tomcat/webapps/geoserver.war"
remoteSHA1=$(curl "${nexusUrl}resolve${gsArtifact}" | xmllint --xpath "string(//sha1)" -)

# Check to see if we already have a Geoserver artifact and if so, verify that it's the same
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
wget "${nexusUrl}redirect${gsArtifact}" -O $gsFileLocation

echo "Validating download"
localSHA1=$(sha1sum $gsFileLocation | cut -d ' ' -f 1)
if [ $remoteSHA1 == $localSHA1  ]; then 
    mv $gsFileLocation $finalWarLocation
else 
    echo "Remote file SHA1 differs from downloaded version."
    exit 1
fi