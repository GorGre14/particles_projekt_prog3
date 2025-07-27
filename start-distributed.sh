#!/bin/bash

# Helper script for starting ChargedParticles in distributed mode
# Usage: ./start-distributed.sh [worker|master] [num_workers]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="$SCRIPT_DIR/target/ChargedParticles-1.0-SNAPSHOT.jar"
MAIN_CLASS="com.example.chargedparticles.SimulationRunner"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
DEFAULT_WORKERS=2
ROLE="master"
NUM_WORKERS=$DEFAULT_WORKERS

# Parse arguments
if [ $# -ge 1 ]; then
    ROLE="$1"
fi

if [ $# -ge 2 ]; then
    NUM_WORKERS="$2"
fi

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if Java is available
check_java() {
    export JAVA_HOME=/opt/homebrew/Cellar/openjdk/24.0.1/libexec/openjdk.jdk/Contents/Home
    if [ ! -d "$JAVA_HOME" ]; then
        print_error "Java is not installed at $JAVA_HOME"
        exit 1
    fi
    
    java_version=$($JAVA_HOME/bin/java -version 2>&1 | head -1)
    print_info "Using Java: $java_version"
}

# Function to build the project if needed
check_build() {
    if [ ! -f "$JAR_FILE" ]; then
        print_warning "JAR file not found. Attempting to build..."
        if command -v mvn &> /dev/null; then
            print_info "Building with Maven..."
            cd "$SCRIPT_DIR"
            export JAVA_HOME=/opt/homebrew/Cellar/openjdk/24.0.1/libexec/openjdk.jdk/Contents/Home
            mvn clean package -q
            if [ $? -ne 0 ]; then
                print_error "Build failed"
                exit 1
            fi
        else
            print_error "JAR file not found and Maven is not available"
            print_error "Please run: mvn clean package"
            exit 1
        fi
    fi
}

# Function to check if RMI registry is running
check_registry() {
    netstat -tln 2>/dev/null | grep -q ":1099 " || lsof -i :1099 &>/dev/null
    if [ $? -ne 0 ]; then
        print_warning "RMI registry (port 1099) doesn't seem to be running"
        print_info "It will be created automatically when the first worker starts"
    else
        print_info "RMI registry is running on port 1099"
    fi
}

# Function to start worker node
start_worker() {
    print_info "Starting worker node..."
    print_info "Registry: localhost:1099"
    print_info "Press Ctrl+C to shutdown"
    echo
    
    export JAVA_HOME=/opt/homebrew/Cellar/openjdk/24.0.1/libexec/openjdk.jdk/Contents/Home
    $JAVA_HOME/bin/java -cp "$JAR_FILE" "$MAIN_CLASS" --role worker
}

# Function to start master node
start_master() {
    print_info "Starting master node..."
    print_info "Expected workers: $NUM_WORKERS"
    print_info "Registry: localhost:1099"
    echo
    print_warning "Make sure $NUM_WORKERS worker nodes are running before starting simulation"
    echo
    
    export JAVA_HOME=/opt/homebrew/Cellar/openjdk/24.0.1/libexec/openjdk.jdk/Contents/Home
    $JAVA_HOME/bin/java -cp "$JAR_FILE" "$MAIN_CLASS" \
        --mode distributed \
        --role master \
        --workers "$NUM_WORKERS" \
        --particles 500 \
        --cycles 100 \
        --ui true
}

# Function to display usage
show_usage() {
    echo "ChargedParticles Distributed Mode Helper"
    echo
    echo "Usage: $0 [worker|master] [num_workers]"
    echo
    echo "Arguments:"
    echo "  worker               Start as worker node"
    echo "  master [num_workers] Start as master node (default: $DEFAULT_WORKERS workers)"
    echo
    echo "Examples:"
    echo "  $0 worker            # Start worker node"
    echo "  $0 master            # Start master expecting $DEFAULT_WORKERS workers"
    echo "  $0 master 3          # Start master expecting 3 workers"
    echo
    echo "Setup Instructions:"
    echo "1. Open multiple terminals"
    echo "2. Start worker nodes: $0 worker"
    echo "3. Start master node:  $0 master [num_workers]"
    echo
    echo "Network Setup:"
    echo "- All nodes communicate via RMI on localhost:1099"
    echo "- For multi-machine setup, modify DistributedConfig.java"
}

# Main execution
case "$ROLE" in
    "worker")
        print_info "=== ChargedParticles Distributed Worker ==="
        check_java
        check_build
        check_registry
        start_worker
        ;;
    "master")
        print_info "=== ChargedParticles Distributed Master ==="
        check_java
        check_build
        check_registry
        start_master
        ;;
    "help"|"-h"|"--help")
        show_usage
        ;;
    *)
        print_error "Invalid role: $ROLE"
        echo
        show_usage
        exit 1
        ;;
esac