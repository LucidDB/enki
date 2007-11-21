# Define variables needed by runtime scripts.  This script is meant to be
# sourced from other scripts, not executed directly.

if [ -z "$CLASSPATH" ]; then
    CLASSPATH_ARG="-cp `cat classpath.gen`"
else
    CLASSPATH_ARG="-cp `cat classpath.gen`:$CLASSPATH"
fi

BASE_JAVA_ARGS="-ea -esa $CLASSPATH_ARG \
  -Djava.util.logging.config.file=trace/EnkiTrace.properties"
