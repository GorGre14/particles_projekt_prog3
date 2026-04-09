#!/bin/bash
# Zagon porazdeljene simulacije z MPJ Express
# Zahteva: MPJ Express mora biti namescen in MPJ_HOME nastavljen
#
# Uporaba: ./run_distributed.sh <stevilo_procesov> [--particles N] [--cycles N]
# Primer: ./run_distributed.sh 4 --particles 500 --cycles 1000

if [ -z "$MPJ_HOME" ]; then
    echo "NAPAKA: MPJ_HOME ni nastavljen."
    echo "Namestite MPJ Express in nastavite: export MPJ_HOME=/pot/do/mpj"
    exit 1
fi

if [ -z "$1" ]; then
    echo "Uporaba: ./run_distributed.sh <stevilo_procesov> [opcije]"
    echo "Primer: ./run_distributed.sh 4 --particles 500 --cycles 1000"
    exit 1
fi

NP=$1
shift

# Prevajanje
mvn -q compile

# Zagon z mpjrun
$MPJ_HOME/bin/mpjrun.sh -np $NP \
  -cp target/classes:$MPJ_HOME/lib/mpj.jar \
  com.example.chargedparticles.DistributedRunner "$@"
