#!/bin/bash

export APP_NS=${APP_NS:-"datagrid"}

trap clean EXIT
function clean() {
    [ -e "$DIR/datagrid.crt.pem" ] && rm -f $DIR/datagrid.crt.pem
    [ -e "$DIR/datagrid.key.pem" ] && rm -f $DIR/datagrid.key.pem
}

set -e

DIR=$(dirname $(realpath -e $BASH_SOURCE))
BASE_DIR=$(realpath -e ${DIR}/..)

oc get project $APP_NS > /dev/null 2>&1 || oc new-project $APP_NS --description='Red Hat Datagrid' --display-name='Red Hat Datagrid'

export PASS=changeit

bash $BASE_DIR/utils/create-secret.sh datagrid-identities $DIR/identities.yaml

bash $BASE_DIR/utils/create-secret-keystore.sh datagrid-keystore $BASE_DIR/certs/datagrid.pfx

oc -n $APP_NS get subscription infinispan || \
    sed "s|namespace: datagrid|namespace: $APP_NS| ; s| - datagrid| - $APP_NS|" $DIR/infinispan-subscription.yaml | \
    oc --as system:admin -n $APP_NS apply -f -

for i in {0..9} ; do
    echo "Waiting for subscription activation ($i)"
    oc -n $APP_NS get csv -o custom-columns="Name:.metadata.name,Phase:.status.phase" --no-headers | awk '/infinispan/ { print $2; }' | grep Succeeded && break
    sleep 10
done

sed '
	s|\(^[^#]\s\+\)image: .*|\1image: "quay.io/infinispan/server-native:11.0.1.Final"|
	s|^#||
	s|512Mi|256Mi|
	s|quarkus-test|quarkus|
	s|replicas: 1|replicas: 2|
    ' \
    $DIR/infinispan-instance.yaml \
| oc -n $APP_NS apply -f -

sleep 1

function fix_privileges() {
    local role_name=manage-route

    oc -n $APP_NS --as system:admin adm policy who-can list route | grep system:serviceaccount:$APP_NS:infinispan-operator && return 0
    oc -n $APP_NS create role $role_name --verb=get,list,watch,create,delete  --resource=route -o yaml --dry-run=client | oc apply -f -
    oc -n $APP_NS create rolebinding infinipan-${role_name} --role $role_name --serviceaccount=$APP_NS:infinispan-operator -o yaml --dry-run=client | oc apply -f -
}

fix_privileges

oc -n $APP_NS get infinispan quarkus -o jsonpath='{ .spec.image }' | grep -i -- '-native' && NATIVE=true

function fix_native_statefulset() {
    oc -n $APP_NS patch statefulset quarkus -p '
      { "spec":
        { "template":
          { "spec":
            { "containers":
              [ {
                "name": "infinispan",
                "env":
                 [ { "name": "JAVA_OPTIONS", "value": "" } ]
              } ]
            }
          }
        }
      }
    '
}

if [ "$NATIVE" = "true" ] ; then
    sleep 5
    for i in {0..9} ; do
	echo "Waiting for datagrid instance creation ($i)"
	fix_native_statefulset && break
	sleep 20
    done
    oc -n $APP_NS delete pod quarkus-0 || true
fi
