#!/bin/bash
# Zagon porazdeljene simulacije (MPJ Express).
#
# Uporaba: ./run_distributed.sh <stevilo_procesov> [opcije]
# Primer:  ./run_distributed.sh 4 --particles 1000 --cycles 2000 --ui false
set -e
cd "$(dirname "$0")"
source ./setup_env.sh

if [ -z "$MPJ_HOME" ]; then
    echo "NAPAKA: MPJ Express ni najden."
    echo "Namestite ga in nastavite: export MPJ_HOME=/pot/do/mpj"
    exit 1
fi

if [[ ! "$1" =~ ^[0-9]+$ ]]; then
    echo "Uporaba: ./run_distributed.sh <stevilo_procesov> [opcije]"
    echo "Primer:  ./run_distributed.sh 4 --particles 1000 --cycles 2000"
    exit 1
fi
NP=$1
shift

mvn -q compile

"$MPJ_HOME/bin/mpjrun.sh" -np "$NP" \
  -cp "target/classes:$MPJ_HOME/lib/mpj.jar" \
  com.example.chargedparticles.DistributedRunner "$@"
