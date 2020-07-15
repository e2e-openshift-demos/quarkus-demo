#!/bin/bash

set -e

GITEA_NS=gpte-gitea
GITEA_DESC="Gitea Operator"
GITEA_DESC=""
GITEA_SERVER_NAME=gitea
GITEA_HOST=${GITEA_HOST:-${GITEA_SERVER_NAME}.apps-crc.testing}

oc project $GITEA_NS 1> /dev/null 2>& 1 || oc new-project $GITEA_NS 1> /dev/null 2>& 1

oc get clusterrole gitea-operator || \
    oc create --as system:admin -f https://raw.githubusercontent.com/wkulhanek/gitea-operator/master/deploy/cluster_role.yaml

oc -n $GITEA_NS get sa gitea-operator || \
    oc -n $GITEA_NS create sa gitea-operator

oc -n $GITEA_NS --as system:admin adm policy add-cluster-role-to-user gitea-operator -z gitea-operator

oc --as system:admin get crd giteas.gpte.opentlc.com || \
    oc -n $GITEA_NS create -f https://raw.githubusercontent.com/wkulhanek/gitea-operator/master/deploy/crds/gpte.opentlc.com_giteas_crd.yaml

oc -n $GITEA_NS get deploy gitea-operator || \
    oc -n $GITEA_NS create -f https://raw.githubusercontent.com/wkulhanek/gitea-operator/master/deploy/operator.yaml

cat | oc -n $GITEA_NS create --as system:admin -f - << EOF
apiVersion: gpte.opentlc.com/v1alpha1
kind: Gitea
metadata:
  name: $GITEA_SERVER_NAME
spec:
  postgresqlVolumeSize: 4Gi
  giteaVolumeSize: 4Gi
  giteaSsl: True
#  giteaServiceName: $GITEA_SERVER_NAME
  $([ -z "$GITEA_HOST" ] || echo "giteaRoute: $GITEA_HOST")
EOF
