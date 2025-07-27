#!/bin/bash

# Test script for distributed mode
cd "/Users/gregorantonaz/Desktop/programiranje_3_projekt/ChargedParticles"
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"

# Check if JAR file exists
if [ ! -f "target/ChargedParticles-1.0-SNAPSHOT.jar" ]; then
    echo "ERROR: JAR file not found!"
    echo "Please run 'mvn clean package' first to build the project."
    exit 1
fi

echo "Starting worker node in background..."
java -cp target/ChargedParticles-1.0-SNAPSHOT.jar com.example.chargedparticles.SimulationRunner --role worker > worker.log 2>&1 &
WORKER_PID=$!

echo "Waiting 3 seconds for worker to initialize..."
sleep 3

echo "Starting master node..."
java -cp target/ChargedParticles-1.0-SNAPSHOT.jar com.example.chargedparticles.SimulationRunner \
  --mode distributed --role master --workers 1 --particles 10 --cycles 3 --ui false

echo "Cleaning up..."
kill $WORKER_PID 2>/dev/null
wait $WORKER_PID 2>/dev/null

echo "Worker log:"
cat worker.log
rm -f worker.log