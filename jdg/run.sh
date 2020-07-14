#!/bin/bash

set -xe

JDG_DIR=$(dirname $(realpath -e ${BASH_SOURCE}))
JDG_CONFIG=jdg-config.yaml
JDG_CLEAN=true
JDG_CUSTOM_CONFIG=${JDG_CUSTOM_CONFIG:-true}
#JDG_IMAGE=infinispan/server
JDG_IMAGE=infinispan/server-native
#JDG_IMAGE=datagrid/datagrid-8-rhel8
JDG_LOCAL_PORT=11222
PFX_FILE=../certs/datagrid.pfx
PFX_PASSWORD=changeit

trap clean EXIT

function clean() {
    if [ "$JDG_CLEAN" = "true" ] ; then
	rm -f $JDG_KEYSTORE $JDG_CONFIG
    fi
}

[ -f "$JDG_KEYSTORE" ] && rm -f $JDG_KEYSTORE

cat > $JDG_CONFIG << EOF
---
#
# https://github.com/infinispan/infinispan-images
#
keystore:
  password: $PFX_PASSWORD
  path: /user-config/server.pfx
  type: PFX
  alias: 1
EOF

[ "$JDG_CUSTOM_CONFIG" = "true" ] && CUSTOM_CONFIG="-e CONFIG_PATH="/user-config/config.yaml""

podman run -ti --rm -p $JDG_LOCAL_PORT:11222 \
    --name datagrid \
     $CUSTOM_CONFIG \
    -e IDENTITIES_PATH=/user-config/identies.yaml \
    -v $(pwd)/identities.yaml:/user-config/identies.yaml:z,ro \
    -v ${JDG_DIR}/$PFX_FILE:/user-config/server.pfx:ro,z \
    -v ${JDG_DIR}/$JDG_CONFIG:/user-config/config.yaml:ro,z \
    $JDG_IMAGE
