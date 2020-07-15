#!/bin/bash

set -e

#DEBUG=echo

RESOURCE=configmap

function usage() {
    echo "$BASH_SOURCE: <APP_NS=application namespace> [CONFIG_FILE_NAME=config_name] $BASH_SOURCE <config map name> <config1> [config2[config3[..]]]"
    exit 1
}

RESOURCE_NAME=${1:-$RESOURCE_NAME}

[ -z "$APP_NS" -o -z "$RESOURCE_NAME" ] && usage

[ "$APP_NS" = "CURRENT" ] || NS="-n $APP_NS"

shift

OTHER_FILES="$*"

[ -z "$OTHER_FILES" ] && usage

if oc $NS get $RESOURCE $RESOURCE_NAME 2> /dev/null 1>&2 ; then
    $DEBUG \
    oc $NS delete $RESOURCE $RESOURCE_NAME
fi

$DEBUG \
oc $NS create $RESOURCE $RESOURCE_NAME --from-file=$([ -z "$CONFIG_FILE_NAME" ] || echo "${CONFIG_FILE_NAME}=")$OTHER_FILES
#oc $NS create $RESOURCE $RESOURCE_NAME $(for f in $OTHER_FILES ; do echo " --from-file=$f" ; done)

if [ -z "$VERBOSE" ] ; then
    cmd=get
else
    cmd=describe
fi
oc $NS $cmd $RESOURCE $RESOURCE_NAME
