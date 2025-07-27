# ChargedParticles - Distributed Computing Implementation

## Overview

This document describes the distributed computing implementation for the ChargedParticles physics simulation. The distributed mode allows the simulation to run across multiple processes or machines using Java RMI (Remote Method Invocation).

## Architecture

### Master-Worker Architecture
- **Master Node**: Coordinates the simulation, distributes particles among workers, and aggregates results
- **Worker Nodes**: Calculate forces for assigned particles and report results back to master
- **Communication**: Java RMI for network communication between nodes

### Key Components

1. **ParticleState** - Serializable representation of particles for network transfer
2. **WorkerNode** - RMI interface defining worker operations
3. **WorkerNodeImpl** - Implementation of worker node functionality
4. **MasterCoordinator** - Manages worker discovery and simulation coordination
5. **DistributedSimulation** - Main simulation class implementing the Simulation interface
6. **DistributedConfig** - Configuration constants for network settings

## Quick Start

### Single Machine Testing

1. **Build the project**:
   ```bash
   mvn clean package
   ```

2. **Start worker nodes** (in separate terminals):
   ```bash
   ./start-distributed.sh worker
   ./start-distributed.sh worker
   ```

3. **Start master node**:
   ```bash
   ./start-distributed.sh master 2
   ```

### Manual Startup

1. **Start workers**:
   ```bash
   java -cp target/classes com.example.chargedparticles.SimulationRunner --role worker
   ```

2. **Start master**:
   ```bash
   java -cp target/classes com.example.chargedparticles.SimulationRunner \
     --mode distributed --role master --workers 2 --particles 500 --cycles 100
   ```

## Configuration

### Network Settings (DistributedConfig.java)
- **Registry Host**: `localhost` (for single-machine testing)
- **Registry Port**: `1099` (RMI default port)
- **Worker Discovery Timeout**: 10 seconds
- **Force Calculation Timeout**: 5 seconds
- **Position Update Timeout**: 2 seconds
- **Maximum Workers**: 10
- **Minimum Particles per Worker**: 10

### Command Line Options

#### General Options
- `--mode distributed` - Enable distributed simulation mode
- `--role master|worker` - Specify node role (default: master)
- `--workers <count>` - Number of expected workers (master only, default: 2)
- `--particles <count>` - Number of particles (default: 400)
- `--cycles <count>` - Number of simulation cycles (default: 1000)
- `--ui true|false` - Enable/disable GUI (default: true)

#### Examples
```bash
# Start worker node
java -jar ChargedParticles.jar --role worker

# Start master with 3 workers, 1000 particles, headless mode
java -jar ChargedParticles.jar --mode distributed --role master --workers 3 --particles 1000 --ui false

# Start master with GUI
java -jar ChargedParticles.jar --mode distributed --role master --workers 2 --particles 500
```

## Implementation Details

### Particle Distribution Strategy
- Particles are distributed evenly among worker nodes
- Each worker is responsible for calculating forces for its assigned particles
- All workers receive the complete particle state for force calculations
- Results are synchronized after each simulation cycle

### Simulation Cycle Process
1. **Master** converts local Particle objects to serializable ParticleState objects
2. **Master** partitions particles among available workers
3. **Master** initializes all workers with their assigned particles
4. **Workers** calculate forces for assigned particles in parallel
5. **Master** collects force calculation results from all workers
6. **Workers** update particle positions based on calculated forces
7. **Master** collects position updates and applies them to local particles
8. **Repeat** for next simulation cycle

### Error Handling and Fault Tolerance
- **Connection Timeouts**: All RMI calls have configurable timeouts
- **Worker Discovery**: Master automatically discovers available workers in RMI registry
- **Graceful Fallback**: Falls back to sequential simulation if distributed setup fails
- **Worker Health Checks**: Master periodically checks if workers are still alive
- **Automatic Reconnection**: Master attempts to reconnect to workers after failures

### Deterministic Results
- Uses fixed random seed (42L) for particle generation
- Ensures identical initial particle distribution across all simulation modes
- Maintains consistent force calculation ordering
- Synchronous communication prevents race conditions

## Performance Characteristics

### Expected Performance
- **Small Datasets** (< 500 particles): Sequential mode recommended due to network overhead
- **Medium Datasets** (500-2000 particles): Distributed mode shows benefits
- **Large Datasets** (> 2000 particles): Significant speedup with multiple workers

### Network Overhead
- Serialization/deserialization of particle states
- RMI communication latency
- Synchronization barriers between simulation cycles

### Scalability Factors
- Linear speedup expected with additional workers for large particle counts
- Network bandwidth becomes bottleneck for very large simulations
- Memory usage increases with particle count on each node

## Multi-Machine Deployment

### Prerequisites
- Java 11+ installed on all machines
- Network connectivity between machines
- Same version of ChargedParticles on all machines

### Setup Steps

1. **Choose Master Machine**:
   - This machine will run the RMI registry
   - Should have good network connectivity to all workers

2. **Start RMI Registry** (on master machine):
   ```bash
   rmiregistry 1099
   ```

3. **Modify DistributedConfig** (if needed):
   ```java
   public static final String RMI_REGISTRY_HOST = "master-machine-ip";
   ```

4. **Start Workers** (on each worker machine):
   ```bash
   java -cp ChargedParticles.jar com.example.chargedparticles.SimulationRunner --role worker
   ```

5. **Start Master**:
   ```bash
   java -cp ChargedParticles.jar com.example.chargedparticles.SimulationRunner \
     --mode distributed --role master --workers N
   ```

## GUI Integration

### Distributed Mode in GUI
- Available in simulation mode dropdown
- Master node shows GUI with real-time particle visualization
- Worker nodes run headless (no GUI)
- Real-time mode switching supported during simulation

### GUI Features
- **Mode Selection**: Dropdown includes "Distributed simulation" option
- **Parameter Adjustment**: Modify particles, cycles, and other settings
- **Start/Reset Buttons**: Full control over simulation lifecycle
- **Real-time Visualization**: Particles update in real-time during simulation

## Debugging and Troubleshooting

### Common Issues

1. **RMI Registry Not Found**:
   - Ensure RMI registry is running on port 1099
   - Workers automatically create registry if not found

2. **Workers Not Discovered**:
   - Check network connectivity
   - Verify workers are registering successfully
   - Increase discovery timeout if needed

3. **Timeout Errors**:
   - Increase timeout values in DistributedConfig
   - Check network latency between nodes
   - Verify worker nodes are responsive

4. **Simulation Falls Back to Sequential**:
   - Normal behavior when distributed setup fails
   - Check console output for specific error messages

### Debug Tips

1. **Enable Verbose Logging**:
   - Add `-Djava.rmi.server.logCalls=true` to JVM args
   - Monitor console output on all nodes

2. **Test Network Connectivity**:
   ```bash
   telnet master-machine-ip 1099
   ```

3. **Check RMI Registry Contents**:
   ```bash
   java -cp . ListRegistry localhost 1099
   ```

4. **Monitor System Resources**:
   - CPU usage on worker nodes during force calculations
   - Memory usage for large particle counts
   - Network bandwidth during simulation

## Development and Extension

### Adding New Features

1. **Custom Force Models**:
   - Extend ForceUtils with new calculation methods
   - Modify WorkerNodeImpl to use new force types

2. **Load Balancing**:
   - Implement dynamic particle redistribution
   - Monitor worker performance and adjust assignments

3. **Persistence**:
   - Add save/load functionality for simulation states
   - Implement checkpointing for long-running simulations

### Testing Guidelines

1. **Unit Testing**:
   - Test ParticleState serialization
   - Verify force calculation correctness
   - Validate worker discovery logic

2. **Integration Testing**:
   - Compare distributed vs sequential results
   - Test with various worker counts
   - Verify timeout handling

3. **Performance Testing**:
   - Benchmark scaling with worker count
   - Measure network overhead
   - Profile memory usage patterns

## Academic Context

This implementation demonstrates several important distributed systems concepts:

### Distributed Computing Concepts
- **Remote Method Invocation (RMI)**: Inter-process communication
- **Master-Worker Pattern**: Distributed task coordination
- **Load Balancing**: Even distribution of computational work
- **Fault Tolerance**: Graceful handling of network failures
- **Synchronization**: Coordinating parallel computations

### Performance Analysis
- **Scalability**: Linear speedup with additional workers
- **Network Overhead**: Impact of serialization and communication
- **Load Distribution**: Balancing work among heterogeneous nodes

### Educational Value
- Hands-on experience with distributed systems
- Understanding of network programming challenges
- Experience with Java RMI technology
- Performance optimization techniques

## Conclusion

The distributed implementation successfully extends the ChargedParticles simulation to support multi-node execution while maintaining compatibility with existing sequential and parallel modes. The modular design allows for easy extension and modification, making it suitable for educational purposes and further development.

The implementation prioritizes:
- **Simplicity**: Using Java RMI for straightforward distributed communication
- **Reliability**: Fault tolerance and graceful error handling
- **Performance**: Efficient particle distribution and minimal network overhead
- **Maintainability**: Clean separation of concerns and modular design

For questions or issues, refer to the troubleshooting section or examine the console output for detailed error messages.