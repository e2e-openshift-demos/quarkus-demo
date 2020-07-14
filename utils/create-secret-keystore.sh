#!/bin/bash

set -e

#DEBUG=echo

RESOURCE=secret

function usage() {
    echo "$BASH_SOURCE: <APP_NS=application namespace> $BASH_SOURCE <$RESOURCE name> <trusted_store>"
    exit 1
}

RESOURCE_NAME=${1:-$SECRET_NAME}

[ -z "$APP_NS" -o -z "$RESOURCE_NAME" ] && usage

[ "$APP_NS" = "CURRENT" ] || NS="-n $APP_NS"

shift

OTHER_FILES=${1:-$TRUSTED_STORE}

[ -z "$OTHER_FILES" ] && usage

if oc $NS get $RESOURCE $RESOURCE_NAME 2> /dev/null 1>&2 ; then
    $DEBUG \
    oc $NS delete $RESOURCE $RESOURCE_NAME
fi

$DEBUG \
oc $NS create $RESOURCE generic $RESOURCE_NAME --from-file=keystore.p12=$OTHER_FILES --from-literal=password=changeit --from-literal=alias=1

if [ -z "$VERBOSE" ] ; then
    cmd=get
else
    cmd=describe
fi
oc $NS $cmd $RESOURCE $RESOURCE_NAME
