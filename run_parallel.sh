#!/bin/bash
# Zagon vzporedne simulacije
# Uporaba: ./run_parallel.sh [--particles N] [--cycles N] [--ui true|false]

mvn -q compile exec:java \
  -Dexec.mainClass="com.example.chargedparticles.SimulationRunner" \
  -Dexec.args="--mode parallel $*"
