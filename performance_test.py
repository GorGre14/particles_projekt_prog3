#!/usr/bin/env python3
"""
ChargedParticles Performance Testing Script
Conducts comprehensive performance analysis across Sequential, Parallel, and Distributed modes

Test Configuration (as per assignment requirements):
- Test 1: Fixed 3000 particles, cycles starting at 500 and incrementing by 500
- Test 2: Fixed 10000 cycles, particles starting at 500 and incrementing by 500
- Each configuration runs 3 times, stops when runtime exceeds 3 minutes
"""

import subprocess
import time
import csv
import os
import signal
import sys
from typing import List, Dict, Tuple
import statistics

# Configuration
JAVA_HOME = "/opt/homebrew/opt/openjdk@21"
JAVA_PATH = f"{JAVA_HOME}/bin/java"
PROJECT_DIR = "/Users/gregorantonaz/Desktop/programiranje_3_projekt/ChargedParticles"
# Use the shaded JAR file instead of classes directory
JAR_FILE = f"{PROJECT_DIR}/target/ChargedParticles-1.0-SNAPSHOT.jar"
CLASSPATH = JAR_FILE
MAIN_CLASS = "com.example.chargedparticles.SimulationRunner"

# Test configurations - ORIGINAL ASSIGNMENT REQUIREMENTS
# Test 1: Fixed particles, increasing cycles
FIXED_PARTICLES_TEST = {
    "particles": 3000,
    "starting_cycles": 500,
    "cycle_increment": 500,
    "max_cycles": 10000,
    "runs_per_config": 3,
    "max_runtime_seconds": 180
}

# Test 2: Fixed cycles, increasing particles  
FIXED_CYCLES_TEST = {
    "cycles": 10000,
    "starting_particles": 500,
    "particle_increment": 500,
    "max_particles": 5000,
    "runs_per_config": 3,
    "max_runtime_seconds": 180
}

class PerformanceTest:
    def __init__(self):
        self.worker_process = None
        self.results = []
        
    def setup_environment(self):
        """Set up Java environment"""
        os.environ["JAVA_HOME"] = JAVA_HOME
        os.environ["PATH"] = f"{JAVA_PATH}:{os.environ.get('PATH', '')}"
        os.chdir(PROJECT_DIR)
        
        # Check if JAR file exists
        if not os.path.exists(JAR_FILE):
            print(f"ERROR: JAR file not found: {JAR_FILE}")
            print("Please run 'mvn clean package' first to build the project.")
            sys.exit(1)
        
    def start_worker(self):
        """Start 4 distributed worker nodes"""
        # First kill any existing workers
        subprocess.run(["pkill", "-f", "role worker"], capture_output=True)
        time.sleep(1)
        
        print("Starting 4 worker nodes...")
        self.worker_processes = []
        
        for i in range(4):
            cmd = [JAVA_PATH, "-cp", CLASSPATH, MAIN_CLASS, "--role", "worker"]
            worker = subprocess.Popen(cmd, stdout=subprocess.PIPE, 
                                    stderr=subprocess.PIPE, text=True)
            self.worker_processes.append(worker)
            
            # Check if worker started successfully
            time.sleep(0.5)
            if worker.poll() is not None:
                stdout, stderr = worker.communicate()
                print(f"Worker {i+1} failed to start!")
                print(f"STDOUT: {stdout}")
                print(f"STDERR: {stderr}")
                return False
                
        print("All 4 workers started, waiting for initialization...")
        time.sleep(4)  # Wait for all workers to initialize
        return True
        
    def stop_worker(self):
        """Stop all distributed worker nodes"""
        if hasattr(self, 'worker_processes') and self.worker_processes:
            print("Stopping all 4 worker nodes...")
            for worker in self.worker_processes:
                worker.terminate()
                try:
                    worker.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    worker.kill()
            self.worker_processes = []
            
        # Additional cleanup
        subprocess.run(["pkill", "-f", "role worker"], capture_output=True)
        
    def run_simulation(self, mode: str, particles: int, cycles: int) -> float:
        """Run a single simulation and extract runtime"""
        if mode == "distributed":
            if not hasattr(self, 'worker_processes') or not self.worker_processes:
                if not self.start_worker():
                    print("Failed to start workers, skipping distributed test")
                    return None
            cmd = [JAVA_PATH, "-cp", CLASSPATH, MAIN_CLASS, 
                  "--mode", "distributed", "--role", "master", "--workers", "4",
                  "--particles", str(particles), "--cycles", str(cycles), "--ui", "false"]
        else:
            cmd = [JAVA_PATH, "-cp", CLASSPATH, MAIN_CLASS,
                  "--mode", mode, "--particles", str(particles), 
                  "--cycles", str(cycles), "--ui", "false"]
                  
        try:
            # Use adequate timeout for distributed mode
            timeout = 300
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
            
            if result.returncode != 0:
                print(f"\nError running {mode} mode: {result.stderr}")
                if mode == "distributed":
                    print(f"STDOUT: {result.stdout}")
                return None
                
            # Extract elapsed time from output
            for line in result.stdout.split('\n'):
                if "Elapsed time:" in line:
                    time_str = line.split("Elapsed time:")[1].strip().split()[0]
                    return float(time_str)
                    
            print(f"\nCould not find elapsed time in output for {mode} mode")
            if mode == "distributed":
                print(f"STDOUT: {result.stdout}")
            return None
            
        except subprocess.TimeoutExpired:
            print(f"Timeout running {mode} mode with {particles} particles, {cycles} cycles")
            return None
        except Exception as e:
            print(f"Exception running {mode} mode: {e}")
            return None
            
    def run_test_series(self, test_name: str, mode: str, particles_list: List[int], 
                       cycles_list: List[int], runs_per_config: int):
        """Run a series of tests for given parameters"""
        print(f"\n=== {test_name} - {mode.upper()} MODE ===")
        
        for particles in particles_list:
            for cycles in cycles_list:
                times = []
                print(f"Testing {particles} particles, {cycles} cycles... ", end="", flush=True)
                
                for run in range(runs_per_config):
                    runtime = self.run_simulation(mode, particles, cycles)
                    if runtime is not None:
                        times.append(runtime)
                    else:
                        print(f"Failed run {run+1}")
                        
                if len(times) > 0:
                    avg_time = statistics.mean(times)
                    std_dev = statistics.stdev(times) if len(times) > 1 else 0
                    print(f"avg: {avg_time:.3f}s (±{std_dev:.3f})")
                    
                    result = {
                        'test_type': test_name,
                        'mode': mode,
                        'particles': particles,
                        'cycles': cycles,
                        'run_times': times,
                        'average_time': avg_time,
                        'std_dev': std_dev
                    }
                    self.results.append(result)
                else:
                    print("All runs failed!")
                    
    def fixed_particles_test(self):
        """Test 1: Fixed particles (3000), increasing cycles"""
        print("\n" + "=" * 50)
        print("TEST 1: Fixed Particles, Increasing Cycles")
        print(f"Particles: {FIXED_PARTICLES_TEST['particles']}")
        print(f"Starting cycles: {FIXED_PARTICLES_TEST['starting_cycles']}")
        print(f"Cycle increment: {FIXED_PARTICLES_TEST['cycle_increment']}")
        print("=" * 50)
        
        particles = FIXED_PARTICLES_TEST["particles"]
        runs = FIXED_PARTICLES_TEST["runs_per_config"]
        max_runtime = FIXED_PARTICLES_TEST["max_runtime_seconds"]
        
        for mode in ["distributed"]:
            print(f"\n=== Testing {mode.upper()} mode ===")
            cycles = FIXED_PARTICLES_TEST["starting_cycles"]
            
            while cycles <= FIXED_PARTICLES_TEST["max_cycles"]:
                times = []
                print(f"Testing {particles} particles, {cycles} cycles... ", end="", flush=True)
                
                for run in range(runs):
                    runtime = self.run_simulation(mode, particles, cycles)
                    if runtime is not None:
                        times.append(runtime)
                    else:
                        print(f"Failed run {run+1}")
                        
                if len(times) > 0:
                    avg_time = statistics.mean(times)
                    std_dev = statistics.stdev(times) if len(times) > 1 else 0
                    print(f"avg: {avg_time:.3f}s (±{std_dev:.3f})")
                    
                    result = {
                        'test_type': 'Fixed_Particles',
                        'mode': mode,
                        'particles': particles,
                        'cycles': cycles,
                        'run_times': times,
                        'average_time': avg_time,
                        'std_dev': std_dev
                    }
                    self.results.append(result)
                    
                    # Stop if average runtime exceeds threshold
                    if avg_time > max_runtime:
                        print(f"Stopping {mode} mode - runtime exceeded {max_runtime}s")
                        break
                else:
                    print("All runs failed! Stopping this mode.")
                    break
                    
                cycles += FIXED_PARTICLES_TEST["cycle_increment"]
                
            if mode == "distributed":
                self.stop_worker()
                time.sleep(2)
                
    def fixed_cycles_test(self):
        """Test 2: Fixed cycles (10000), increasing particles"""
        print("\n" + "=" * 50)
        print("TEST 2: Fixed Cycles, Increasing Particles")
        print(f"Cycles: {FIXED_CYCLES_TEST['cycles']}")
        print(f"Starting particles: {FIXED_CYCLES_TEST['starting_particles']}")
        print(f"Particle increment: {FIXED_CYCLES_TEST['particle_increment']}")
        print("=" * 50)
        
        cycles = FIXED_CYCLES_TEST["cycles"]
        runs = FIXED_CYCLES_TEST["runs_per_config"]
        max_runtime = FIXED_CYCLES_TEST["max_runtime_seconds"]
        
        for mode in ["distributed"]:
            print(f"\n=== Testing {mode.upper()} mode ===")
            particles = FIXED_CYCLES_TEST["starting_particles"]
            
            while particles <= FIXED_CYCLES_TEST["max_particles"]:
                times = []
                print(f"Testing {particles} particles, {cycles} cycles... ", end="", flush=True)
                
                for run in range(runs):
                    runtime = self.run_simulation(mode, particles, cycles)
                    if runtime is not None:
                        times.append(runtime)
                    else:
                        print(f"Failed run {run+1}")
                        
                if len(times) > 0:
                    avg_time = statistics.mean(times)
                    std_dev = statistics.stdev(times) if len(times) > 1 else 0
                    print(f"avg: {avg_time:.3f}s (±{std_dev:.3f})")
                    
                    result = {
                        'test_type': 'Fixed_Cycles',
                        'mode': mode,
                        'particles': particles,
                        'cycles': cycles,
                        'run_times': times,
                        'average_time': avg_time,
                        'std_dev': std_dev
                    }
                    self.results.append(result)
                    
                    # Stop if average runtime exceeds threshold
                    if avg_time > max_runtime:
                        print(f"Stopping {mode} mode - runtime exceeded {max_runtime}s")
                        break
                else:
                    print("All runs failed! Stopping this mode.")
                    break
                    
                particles += FIXED_CYCLES_TEST["particle_increment"]
                
            if mode == "distributed":
                self.stop_worker()
                time.sleep(2)
                
    def save_results(self):
        """Append new distributed results to existing CSV files"""
        # Fixed Particles Test Results - append only new distributed results
        fixed_particles_results = [r for r in self.results if r['test_type'] == 'Fixed_Particles']
        if fixed_particles_results:
            with open('fixed_particles_results.csv', 'a', newline='') as f:
                writer = csv.writer(f)
                for result in fixed_particles_results:
                    times = result['run_times'] + [None] * (3 - len(result['run_times']))
                    writer.writerow([
                        result['mode'], result['particles'], result['cycles'],
                        times[0], times[1], times[2],
                        result['average_time'], result['std_dev']
                    ])
                
        # Fixed Cycles Test Results - append only new distributed results
        fixed_cycles_results = [r for r in self.results if r['test_type'] == 'Fixed_Cycles']
        if fixed_cycles_results:
            with open('fixed_cycles_results.csv', 'a', newline='') as f:
                writer = csv.writer(f)
                for result in fixed_cycles_results:
                    times = result['run_times'] + [None] * (3 - len(result['run_times']))
                    writer.writerow([
                        result['mode'], result['particles'], result['cycles'],
                        times[0], times[1], times[2],
                        result['average_time'], result['std_dev']
                    ])
                
        print(f"\nNew distributed results appended to:")
        print(f"- fixed_particles_results.csv ({len(fixed_particles_results)} records)")
        print(f"- fixed_cycles_results.csv ({len(fixed_cycles_results)} records)")
        
    def cleanup(self):
        """Cleanup resources"""
        self.stop_worker()
        
    def run_all_tests(self):
        """Run complete performance test suite"""
        print("ChargedParticles Performance Testing Suite")
        print("=" * 50)
        print("\nTest Configuration (as per assignment requirements):")
        print("\nTest 1 - Fixed Particles:")
        print(f"  - Particles: {FIXED_PARTICLES_TEST['particles']} (fixed)")
        print(f"  - Starting cycles: {FIXED_PARTICLES_TEST['starting_cycles']}")
        print(f"  - Increment: {FIXED_PARTICLES_TEST['cycle_increment']} cycles")
        print(f"  - Max runtime: {FIXED_PARTICLES_TEST['max_runtime_seconds']}s")
        print("\nTest 2 - Fixed Cycles:")
        print(f"  - Cycles: {FIXED_CYCLES_TEST['cycles']} (fixed)")
        print(f"  - Starting particles: {FIXED_CYCLES_TEST['starting_particles']}")
        print(f"  - Increment: {FIXED_CYCLES_TEST['particle_increment']} particles")
        print(f"  - Max runtime: {FIXED_CYCLES_TEST['max_runtime_seconds']}s")
        print(f"\nRuns per configuration: {FIXED_PARTICLES_TEST['runs_per_config']}")
        print(f"Timeout per simulation: 300 seconds")
        print("Testing mode: Distributed only (Sequential and Parallel already completed)")
        print("=" * 50)
        
        try:
            self.setup_environment()
            self.fixed_particles_test()
            self.fixed_cycles_test()
            self.save_results()
            
            print("\n" + "=" * 50)
            print("Performance testing completed successfully!")
            
        except KeyboardInterrupt:
            print("\nTesting interrupted by user")
        except Exception as e:
            print(f"\nError during testing: {e}")
        finally:
            self.cleanup()

def main():
    tester = PerformanceTest()
    tester.run_all_tests()

if __name__ == "__main__":
    main()