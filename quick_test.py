#!/usr/bin/env python3
"""
Quick test script to verify all modes work correctly
Uses minimal particles/cycles for fast verification
"""

import subprocess
import time
import os

# Configuration
JAVA_HOME = "/opt/homebrew/opt/openjdk@21"
JAVA_PATH = f"{JAVA_HOME}/bin/java"
PROJECT_DIR = "/Users/gregorantonaz/Desktop/programiranje_3_projekt/ChargedParticles"
JAR_FILE = f"{PROJECT_DIR}/target/ChargedParticles-1.0-SNAPSHOT.jar"

def test_mode(mode, worker_process=None):
    """Test a single mode with minimal parameters"""
    print(f"\n{'='*50}")
    print(f"Testing {mode.upper()} mode...")
    print(f"{'='*50}")
    
    if mode == "distributed":
        # Start worker if needed
        if not worker_process:
            print("Starting worker node...")
            cmd = [JAVA_PATH, "-cp", JAR_FILE, "com.example.chargedparticles.SimulationRunner", "--role", "worker"]
            worker_process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            time.sleep(2)
        
        cmd = [JAVA_PATH, "-cp", JAR_FILE, "com.example.chargedparticles.SimulationRunner",
               "--mode", "distributed", "--role", "master", "--workers", "1",
               "--particles", "10", "--cycles", "5", "--ui", "false"]
    else:
        cmd = [JAVA_PATH, "-cp", JAR_FILE, "com.example.chargedparticles.SimulationRunner",
               "--mode", mode, "--particles", "10", "--cycles", "5", "--ui", "false"]
    
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=10)
        
        if result.returncode == 0:
            print("✅ SUCCESS!")
            # Extract key info from output
            for line in result.stdout.split('\n'):
                if "Elapsed time:" in line:
                    print(f"   {line.strip()}")
                elif "Initialized" in line:
                    print(f"   {line.strip()}")
        else:
            print("❌ FAILED!")
            print(f"Error: {result.stderr}")
            
    except subprocess.TimeoutExpired:
        print("❌ TIMEOUT!")
    except Exception as e:
        print(f"❌ Exception: {e}")
    
    return worker_process

def main():
    print("ChargedParticles Quick Test")
    print("="*50)
    
    # Change to project directory
    os.chdir(PROJECT_DIR)
    os.environ["JAVA_HOME"] = JAVA_HOME
    os.environ["PATH"] = f"{JAVA_PATH}:{os.environ.get('PATH', '')}"
    
    # Check JAR exists
    if not os.path.exists(JAR_FILE):
        print(f"ERROR: JAR file not found: {JAR_FILE}")
        print("Please run 'mvn clean package' first!")
        return
    
    worker_process = None
    
    try:
        # Test each mode
        test_mode("sequential")
        test_mode("parallel")
        worker_process = test_mode("distributed")
        
        print("\n" + "="*50)
        print("All modes tested! ✅")
        print("="*50)
        
    finally:
        # Cleanup
        if worker_process:
            print("\nStopping worker...")
            worker_process.terminate()
            worker_process.wait(timeout=5)

if __name__ == "__main__":
    main()