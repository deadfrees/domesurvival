#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/build/core-test/classes"
rm -rf "$OUT"
mkdir -p "$OUT"

mapfile -t MAIN < <(find "$ROOT/src/main/java/com/wasted/domesurvival/core" -name '*.java' -print | sort)
mapfile -t TEST < <(find "$ROOT/src/test/java/com/wasted/domesurvival/core" -name '*.java' -print | sort)

# The host JDK may be newer; --release 17 guarantees Java 17 language/API target and classfile 61.
javac --release 17 -encoding UTF-8 -d "$OUT" "${MAIN[@]}" "${TEST[@]}"
java -ea -cp "$OUT" com.wasted.domesurvival.core.dome.DomeGeometrySelfTest

CLASS="$OUT/com/wasted/domesurvival/core/dome/DomeSpec.class"
MAJOR="$(javap -verbose "$CLASS" | awk '/major version/ {print $3; exit}')"
if [[ "$MAJOR" != "61" ]]; then
  echo "ERROR: expected Java 17 classfile major 61, got $MAJOR" >&2
  exit 1
fi

echo "Java compatibility: OK (classfile major $MAJOR = Java 17)"
