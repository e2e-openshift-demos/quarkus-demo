#!/bin/bash

set -e

#DEBUG=echo

RESOURCE=secret

function usage() {
    echo "$BASH_SOURCE: <APP_NS=application namespace> $BASH_SOURCE <$RESOURCE name> <tls-cert> <tls-key>"
    exit 1
}

RESOURCE_NAME=${1:-$SECRET_NAME}

[ -z "$APP_NS" -o -z "$RESOURCE_NAME" ] && usage

[ "$APP_NS" = "CURRENT" ] || NS="-n $APP_NS"

shift

TLS_CERT=$1
TLS_KEY=$2

[ -z "$TLS_CERT" -o -z "$TLS_KEY" ] && usage

if oc $NS get $RESOURCE $RESOURCE_NAME 2> /dev/null 1>&2 ; then
    $DEBUG \
    oc $NS delete $RESOURCE $RESOURCE_NAME
fi

$DEBUG \
oc $NS create $RESOURCE tls $RESOURCE_NAME --cert=$TLS_CERT --key=$TLS_KEY

if [ -z "$VERBOSE" ] ; then
    cmd=get
else
    cmd=describe
fi
oc $NS $cmd $RESOURCE $RESOURCE_NAME
