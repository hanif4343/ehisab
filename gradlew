#!/bin/sh

##############################################################################
# Gradle start up script for UN*X
##############################################################################

# Attempt to set APP_HOME
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=$(ls -ld "$PRG")
    link=$(expr "$ls" : '.*-> \(.*\)$')
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=$(dirname "$PRG")"/$link"
    fi
done
APP_HOME=$(dirname "$PRG")
APP_HOME=$(cd "$APP_HOME" && pwd)

JAVA_OPTS="$JAVA_OPTS -Xmx64m -Xms64m"
GRADLE_OPTS="$GRADLE_OPTS -Dorg.gradle.daemon=false"

exec java $JAVA_OPTS \
  -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain "$@"
