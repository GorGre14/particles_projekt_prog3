#!/usr/bin/env python3
"""
Debug distributed mode with visible output
"""

import subprocess
import threading
import time
import os

JAVA_HOME = "/opt/homebrew/opt/openjdk@21"
JAVA_PATH = f"{JAVA_HOME}/bin/java"
JAR_FILE = "target/ChargedParticles-1.0-SNAPSHOT.jar"

def stream_output(proc, name):
    """Stream output from process"""
    for line in iter(proc.stdout.readline, ''):
        if line:
            print(f"[{name}] {line.strip()}")

def test_distributed():
    # Kill any existing workers
    print("Killing existing processes...")
    subprocess.run(["pkill", "-f", "ChargedParticles"], capture_output=True)
    time.sleep(2)
    
    # Start worker with visible output
    print("\n=== Starting Worker ===")
    worker_cmd = [JAVA_PATH, "-cp", JAR_FILE, "com.example.chargedparticles.SimulationRunner", "--role", "worker"]
    worker = subprocess.Popen(worker_cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
    
    # Start thread to stream worker output
    worker_thread = threading.Thread(target=stream_output, args=(worker, "WORKER"))
    worker_thread.daemon = True
    worker_thread.start()
    
    # Wait for worker to initialize
    print("\nWaiting 5 seconds for worker to initialize...")
    time.sleep(5)
    
    # Run master with visible output
    print("\n=== Starting Master ===")
    master_cmd = [JAVA_PATH, "-cp", JAR_FILE, "com.example.chargedparticles.SimulationRunner",
                  "--mode", "distributed", "--role", "master", "--workers", "1",
                  "--particles", "10", "--cycles", "5", "--ui", "false"]
    
    print(f"Command: {' '.join(master_cmd)}")
    master = subprocess.Popen(master_cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
    
    # Stream master output
    for line in iter(master.stdout.readline, ''):
        if line:
            print(f"[MASTER] {line.strip()}")
    
    master.wait()
    print(f"\nMaster exit code: {master.returncode}")
    
    # Kill worker
    print("\nKilling worker...")
    worker.terminate()
    worker.wait()
    
if __name__ == "__main__":
    test_distributed()