#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

if [[ -x .jdk/21/bin/javac ]]; then
    export JAVA_HOME="$PWD/.jdk/21"
fi

if [[ -z "${JAVA_HOME:-}" || ! -x "$JAVA_HOME/bin/javac" ]]; then
    echo "JAVA_HOME must point to a JDK 21 installation." >&2
    exit 1
fi

export PATH="$JAVA_HOME/bin:$PATH"
bash ./tooling/verification/linux-configurator-tests.sh
exec bash ./gradlew clean verify packageLinuxDistribution --no-daemon --console=plain
