#!/bin/sh

#
# Noiddroid Gradle wrapper startup script (POSIX).
# Based on the standard Gradle 8.x wrapper, distributed by Gradle under
# the Apache License, Version 2.0.
#

# Resolve the script location to find APP_HOME.
PRG="$0"
while [ -h "$PRG" ]; do
    ls=$(ls -ld "$PRG")
    link=${ls#*' -> '}
    case "$link" in
        /*) PRG="$link" ;;
        *)  PRG=$(dirname "$PRG")/"$link" ;;
    esac
done
APP_HOME=$(cd "$(dirname "$PRG")" >/dev/null 2>&1 && pwd -P)

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Find java.
if [ -n "$JAVA_HOME" ]; then
    if [ -x "$JAVA_HOME/jre/sh/java" ]; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    [ -x "$JAVACMD" ] || {
        echo "ERROR: JAVA_HOME set to invalid directory: $JAVA_HOME" >&2
        exit 1
    }
else
    JAVACMD="java"
    command -v java >/dev/null 2>&1 || {
        echo "ERROR: no 'java' command found; set JAVA_HOME." >&2
        exit 1
    }
fi

# Bump file-descriptor limit if possible.
MAX_FD=maximum
if ! uname | grep -qiE 'cygwin|darwin|msys|nonstop'; then
    case "$MAX_FD" in
        max*)
            MAX_FD=$(ulimit -H -n 2>/dev/null) || MAX_FD=1024
            ;;
    esac
    ulimit -n "$MAX_FD" 2>/dev/null || true
fi

# Run Gradle.
exec "$JAVACMD" \
    $DEFAULT_JVM_OPTS \
    $JAVA_OPTS \
    $GRADLE_OPTS \
    "-Dorg.gradle.appname=gradlew" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
