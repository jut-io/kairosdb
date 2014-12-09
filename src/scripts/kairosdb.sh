#!/bin/bash

# Find the location of the bin directory and change to the root of kairosdb
KAIROSDB_BIN_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$KAIROSDB_BIN_DIR/.."

KAIROSDB_ENV_FILE=${KAIROSDB_ENV_FILE:-"$KAIROSDB_BIN_DIR/kairosdb-env.sh"}

if [ -f "$KAIROSDB_ENV_FILE" ]; then
    . $KAIROSDB_ENV_FILE
fi

KAIROSDB_CONF_DIR=${KAIROSDB_CONF_DIR:-"conf"}
KAIROSDB_LOG_DIR=${KAIROSDB_LOG_DIR:-"log"}
KAIROSDB_LIB_DIR=${KAIROSDB_LIB_DIR:-"lib"}
KAIROSDB_PID_FILE=${KAIROSDB_PID_FILE:-"/var/run/kairosdb.pid"}
KAIROSDB_CONF_FILE=${KAIROSDB_CONF_FILE:-"$KAIROSDB_CONF_DIR/kairosdb.properties"}
KAIROSDB_LOG_FILE=${KAIROSDB_PID_FILE:-"$KAIROSDB_LOG_DIR/kairosdb.log"}


if [ ! -d "$KAIROSDB_LOG_DIR" ]; then
	mkdir "$KAIROSDB_LOG_DIR"
fi

if [ "$KAIROS_PID_FILE" = "" ]; then
	KAIROS_PID_FILE=/var/run/kairosdb.pid
fi


# Use JAVA_HOME if set, otherwise look for java in PATH
if [ -n "$JAVA_HOME" ]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA=java
fi

# Load up the classpath
CLASSPATH="conf/logging"
for jar in $KAIROSDB_LIB_DIR/*.jar; do
	CLASSPATH="$CLASSPATH:$jar"
done



if [ "$1" = "run" ] ; then
	shift
	exec "$JAVA" $JAVA_OPTS -cp $CLASSPATH org.kairosdb.core.Main -c run -p $KAIROSDB_CONF_FILE
elif [ "$1" = "start" ] ; then
	shift
	exec "$JAVA" $JAVA_OPTS -cp $CLASSPATH org.kairosdb.core.Main \
		-c start -p $KAIROSDB_CONF_FILE >> "$KAIROSDB_LOG_DIR/kairosdb.log" 2>&1 &
	echo $! > "$KAIROS_PID_FILE"
elif [ "$1" = "stop" ] ; then
	shift
	kill `cat $KAIROS_PID_FILE` > /dev/null 2>&1
	while kill -0 `cat $KAIROS_PID_FILE` > /dev/null 2>&1; do
		echo -n "."
		sleep 1;
	done
	rm $KAIROS_PID_FILE
elif [ "$1" = "export" ] ; then
	shift
	exec "$JAVA" $JAVA_OPTS -cp $CLASSPATH org.kairosdb.core.Main -c export -p $KAIROSDB_CONF_FILE $*
elif [ "$1" = "import" ] ; then
	shift
	exec "$JAVA" $JAVA_OPTS -cp $CLASSPATH org.kairosdb.core.Main -c import -p $KAIROSDB_CONF_FILE $*
else
	echo "Unrecognized command."
	exit 1
fi



