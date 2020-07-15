#!/bin/bash

set -e

DIR=$(dirname $(realpath -e $BASH_SOURCE))
BASE_DIR=$(realpath -e ${DIR}/../../..)

export APP_NS=CURRENT

SECRET_FILE_NAME=ca.pem bash -x $BASE_DIR/utils/create-secret.sh custom-ca $BASE_DIR/certs/custom-ca.pem
SECRET_FILE_NAME=server.pfx bash -x $BASE_DIR/utils/create-secret.sh quarkus-demo-certs $BASE_DIR/certs/quarkus-demo.pfx
bash -x $BASE_DIR/utils/create-cm.sh quarkus-application-config $BASE_DIR/app/src/main/resources/application.properties
