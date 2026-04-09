#!/bin/bash
# Zagon sekvencne simulacije
# Uporaba: ./run_sequential.sh [--particles N] [--cycles N] [--ui true|false]

mvn -q compile exec:java \
  -Dexec.mainClass="com.example.chargedparticles.SimulationRunner" \
  -Dexec.args="--mode sequential $*"
