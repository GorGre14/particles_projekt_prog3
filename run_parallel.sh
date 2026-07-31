#!/bin/bash
# Zagon vzporedne simulacije.
#
# Uporaba: ./run_parallel.sh [opcije]
# Primer:  ./run_parallel.sh --particles 1000 --cycles 2000 --ui false
set -e
cd "$(dirname "$0")"
source ./setup_env.sh

mvn -q compile exec:java \
  -Dexec.mainClass="com.example.chargedparticles.SimulationRunner" \
  -Dexec.args="--mode parallel $*"
