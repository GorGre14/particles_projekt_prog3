#!/bin/bash
# Zagon sekvencne simulacije.
#
# Uporaba: ./run_sequential.sh [opcije]
# Primer:  ./run_sequential.sh --particles 1000 --cycles 2000 --ui false
set -e
cd "$(dirname "$0")"
source ./setup_env.sh

mvn -q compile exec:java \
  -Dexec.mainClass="com.example.chargedparticles.SimulationRunner" \
  -Dexec.args="--mode sequential $*"
