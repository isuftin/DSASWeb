#!/bin/bash
set -e

cat liquibase.properties | envsubst > tmp.properties
mv tmp.properties liquibase.properties

exec "$@"
