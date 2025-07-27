#!/usr/bin/env python3
"""
Simple test to debug distributed mode
"""

import subprocess
import time
import os

JAVA_HOME = "/opt/homebrew/opt/openjdk@21"
JAVA_PATH = f"{JAVA_HOME}/bin/java"
JAR_FILE = "target/ChargedParticles-1.0-SNAPSHOT.jar"

def test_distributed():
    # Kill any existing workers
    print("Killing existing workers...")
    subprocess.run(["pkill", "-f", "role worker"], capture_output=True)
    time.sleep(1)
    
    # Start worker
    print("Starting worker...")
    worker_cmd = [JAVA_PATH, "-cp", JAR_FILE, "com.example.chargedparticles.SimulationRunner", "--role", "worker"]
    worker = subprocess.Popen(worker_cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    
    # Give worker time to start
    print("Waiting for worker to initialize...")
    time.sleep(5)
    
    # Check if worker is still running
    if worker.poll() is not None:
        output = worker.stdout.read()
        print(f"Worker died! Output:\n{output}")
        return
    
    print("Worker is running. Starting master...")
    
    # Run master
    master_cmd = [JAVA_PATH, "-cp", JAR_FILE, "com.example.chargedparticles.SimulationRunner",
                  "--mode", "distributed", "--role", "master", "--workers", "1",
                  "--particles", "10", "--cycles", "5", "--ui", "false"]
    
    result = subprocess.run(master_cmd, capture_output=True, text=True, timeout=20)
    
    print(f"Master return code: {result.returncode}")
    print(f"Master stdout:\n{result.stdout}")
    if result.stderr:
        print(f"Master stderr:\n{result.stderr}")
    
    # Kill worker
    print("Killing worker...")
    worker.terminate()
    worker.wait()
    
if __name__ == "__main__":
    test_distributed()