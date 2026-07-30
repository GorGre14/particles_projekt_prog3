#!/bin/bash
# Meritve zmogljivosti vseh treh nacinov simulacije.
#
# Uporaba: ./benchmark.sh [stevilo_ponovitev]
#
# Vsako nastavitev pozene veckrat in obdrzi najkrajsi cas; rezultate zapise
# v benchmark_results.csv in jih izpise v obliki tabele.
set -e
cd "$(dirname "$0")"
source ./setup_env.sh

REPS=${1:-3}
OUT=benchmark_results.csv
CP="target/classes:$MPJ_HOME/lib/mpj.jar"

# Konfiguracije: "delci cikli"
TEST1="2000 250|2000 500|2000 1000|2000 2000"     # fiksni delci, razlicni cikli
TEST2="500 1000|1000 1000|2000 1000|3000 1000"    # fiksni cikli, razlicni delci

echo "Prevajanje ..."
mvn -q clean compile

# Izlusci cas izvajanja iz izpisa simulacije.
extract_time() {
    grep "Cas izvajanja" | awk '{print $(NF-1)}'
}

# run_once <nacin> <delci> <cikli>  ->  cas v sekundah
run_once() {
    local mode=$1 particles=$2 cycles=$3
    case "$mode" in
        sequential|parallel)
            java -cp "$CP" com.example.chargedparticles.SimulationRunner \
                --mode "$mode" --particles "$particles" --cycles "$cycles" --ui false \
                2>/dev/null | extract_time
            ;;
        mpi*)
            local np=${mode#mpi}
            "$MPJ_HOME/bin/mpjrun.sh" -np "$np" -cp "$CP" \
                com.example.chargedparticles.DistributedRunner \
                --particles "$particles" --cycles "$cycles" --ui false \
                2>/dev/null | extract_time
            ;;
    esac
}

# best_of <nacin> <delci> <cikli>  ->  najkrajsi cas iz REPS ponovitev
best_of() {
    local best=""
    for ((r = 1; r <= REPS; r++)); do
        local t
        t=$(run_once "$1" "$2" "$3")
        if [ -n "$t" ] && { [ -z "$best" ] || (( $(echo "$t < $best" | bc -l) )); }; then
            best=$t
        fi
        sleep 1   # kratek premor med ponovitvami
    done
    echo "$best"
}

MODES="sequential parallel mpi4 mpi10"

echo "test,particles,cycles,mode,seconds" > "$OUT"

run_test() {
    local name=$1 configs=$2
    echo
    printf '%s\n' "=== $name (najkrajsi cas od $REPS ponovitev) ==="
    printf '%8s %8s %12s %12s %12s %12s\n' "Delci" "Cikli" "Sekvencna" "Vzporedna" "MPI P=4" "MPI P=10"
    local IFS='|'
    for cfg in $configs; do
        unset IFS
        set -- $cfg
        local particles=$1 cycles=$2
        local times=()
        for mode in $MODES; do
            local t
            t=$(best_of "$mode" "$particles" "$cycles")
            times+=("$t")
            echo "$name,$particles,$cycles,$mode,$t" >> "$OUT"
        done
        printf '%8s %8s %12s %12s %12s %12s\n' \
            "$particles" "$cycles" "${times[0]}" "${times[1]}" "${times[2]}" "${times[3]}"
        IFS='|'
    done
    unset IFS
}

run_test "Test 1 - 2000 delcev / razlicno stevilo ciklov" "$TEST1"
run_test "Test 2 - 1000 ciklov / razlicno stevilo delcev" "$TEST2"

echo
echo "Rezultati shranjeni v $OUT"
