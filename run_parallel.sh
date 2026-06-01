#!/bin/bash
# Zagon vzporedne simulacije
# Uporaba: ./run_parallel.sh [--particles N] [--cycles N] [--ui true|false]

# Avtomatsko zaznavanje JAVA_HOME na macOS, ce ni nastavljen
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home" ]; then
        export JAVA_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
fi

mvn -q compile exec:java \
  -Dexec.mainClass="com.example.chargedparticles.SimulationRunner" \
  -Dexec.args="--mode parallel $*"
