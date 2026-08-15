#!/usr/bin/env bash
# Source this file before local test commands.
export DOMESURVIVAL_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
export DOMESURVIVAL_JAVA_RELEASE=17
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dfile.encoding=UTF-8"
echo "DomeSurvival Java target: ${DOMESURVIVAL_JAVA_RELEASE}"
java -version
