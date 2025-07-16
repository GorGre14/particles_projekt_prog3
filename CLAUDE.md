# ChargedParticles Project - AI Agent Reference

## Project Overview
This is a Java-based physics simulation that models the behavior of charged particles in a 2D space. The project implements classical electrostatics with real-time visualization using JavaFX and Swing.

## Project Structure
```
ChargedParticles/
├── pom.xml                           # Maven build configuration
├── src/main/java/com/example/chargedparticles/
│   ├── SimulationRunner.java         # Main entry point and simulation control
│   ├── model/
│   │   └── Particle.java            # Particle data model
│   ├── simulation/
│   │   ├── SequentialSimulation.java # Core simulation engine
│   │   ├── ForceUtils.java          # Physics calculations
│   │   └── SimulationParameters.java # Configuration container
│   └── ui/
│       └── SimulationUI.java        # GUI components
└── src/main/resources/
    └── com/example/chargedparticles/
        └── hello-view.fxml          # FXML layout (unused)
```

## Core Components

### 1. SimulationRunner.java (Main Entry Point)
- **Purpose**: Application entry point, argument parsing, simulation lifecycle management
- **Key Methods**:
  - `main()`: Parses CLI arguments and initializes simulation
  - `restartSimulation()`: Restarts simulation with current parameters
- **Threading**: Uses separate thread for simulation execution
- **Command Line Args**: `--ui`, `--window`, `--particles`, `--cycles`, `--bounds`, `--fps`

### 2. Particle.java (Data Model)
- **Purpose**: Represents individual charged particles
- **Properties**: position (x,y), velocity (vx,vy), charge, mass
- **Key Methods**:
  - `randomParticle()`: Creates particle with random properties within bounds
  - Charge range: ±0.5 to ±1.5
  - Velocity range: ±1.0

### 3. SequentialSimulation.java (Physics Engine)
- **Purpose**: Implements single-threaded simulation algorithm
- **Algorithm**: Force calculation → velocity update → position update
- **Key Method**: `performOneCycle()` - executes one simulation timestep
- **Physics**: F=ma, v=v+a, x=x+v

### 4. ForceUtils.java (Physics Calculations)
- **Purpose**: Handles all force calculations
- **Methods**:
  - `computeParticleForce()`: Coulomb's law (F = |q1*q2|/r²)
  - `computeBoundaryForce()`: Repulsive forces near boundaries
- **Safety**: Prevents division by zero with minimum distance (1e-12)

### 5. SimulationUI.java (User Interface)
- **Purpose**: Real-time visualization and controls
- **Features**:
  - Color-coded particles (red=positive, blue=negative)
  - Control panel for parameter adjustment
  - FPS-based rendering with Timer
- **Layout**: BorderLayout with controls at top, simulation canvas in center

### 6. SimulationParameters.java (Configuration)
- **Purpose**: Container for all simulation parameters
- **Parameters**: UI enable/disable, window dimensions, particle count, cycles, boundaries, FPS

## Technical Details

### Build Configuration
- **Java Version**: 19
- **Build Tool**: Maven
- **Dependencies**: JavaFX Controls/FXML, JUnit 5
- **Main Class**: `com.example.chargedparticles.HelloApplication` (Note: This appears to be incorrect in pom.xml)

### Physics Model
- **Force Law**: Coulomb's law with simplified constant
- **Boundary Conditions**: Soft repulsive walls (buffer=5.0, repelFactor=10.0)
- **Integration**: Simple Euler integration
- **Units**: Arbitrary simulation units (no real-world scaling)

### Threading Model
- **Simulation Thread**: Separate thread for physics calculations
- **Rendering Thread**: Swing Timer for UI updates
- **Synchronization**: Basic thread interruption for restart functionality

## Code Conventions

### Language
- **Documentation**: Slovenian comments and variable names
- **Code Style**: Standard Java conventions
- **Naming**: Descriptive variable names in Slovenian

### Common Patterns
- Getter/setter methods for all properties
- Static factory methods (`randomParticle()`)
- Utility classes with static methods (`ForceUtils`)
- Swing event handling with anonymous inner classes

## Common Tasks and Commands

### Build and Run
```bash
mvn clean compile
mvn javafx:run
```

### Command Line Examples
```bash
# Run with custom parameters
java -jar target/ChargedParticles.jar --particles 200 --cycles 500 --fps 30

# Run without UI
java -jar target/ChargedParticles.jar --ui false --particles 1000 --cycles 100
```

### Testing
- **Framework**: JUnit 5 configured but no tests implemented
- **Manual Testing**: Visual inspection of particle behavior

## Known Issues and Limitations

1. **Main Class Mismatch**: pom.xml references `HelloApplication` but actual main is `SimulationRunner`
2. **No Parallel Processing**: Single-threaded simulation limits performance
3. **Fixed Time Step**: No adaptive time stepping for stability
4. **Memory Usage**: All particles stored in memory simultaneously
5. **No Persistence**: No save/load functionality for simulation states

## Extension Points

### Performance Improvements
- Implement parallel force calculations
- Add spatial partitioning for O(n²) to O(n log n) complexity
- Optimize rendering with double buffering

### Feature Additions
- Multiple particle types
- External force fields
- Collision detection
- Data export capabilities
- Parameter presets

### Architecture Improvements
- Separate physics engine from UI
- Plugin system for different force models
- Configuration file support
- Logging system

## Development Guidelines

### Before Making Changes
1. Understand the physics model being implemented
2. Test with small particle counts first
3. Verify boundary conditions work properly
4. Check thread safety for UI updates

### Common Debugging
- Use small particle counts (10-50) for debugging
- Monitor force calculations with print statements
- Verify particles stay within boundaries
- Check for NaN or infinite values in physics calculations

### Performance Considerations
- Particle count scales O(n²) for force calculations
- Rendering performance depends on particle count and FPS
- Memory usage grows linearly with particle count
- Thread switching overhead for very fast simulations

## Future AI Agent Instructions

When working on this project:
1. **Always test with small particle counts first** to verify behavior
2. **Preserve the physics model accuracy** - don't optimize at the expense of correctness
3. **Maintain thread safety** when modifying simulation or UI code
4. **Follow existing code patterns** and Slovenian commenting style
5. **Test boundary conditions** thoroughly after any physics changes
6. **Consider performance implications** of changes to the O(n²) force calculation loop

## Quick Reference

### Key Files to Modify for Common Tasks
- **Add new force types**: `ForceUtils.java`
- **Change particle properties**: `Particle.java`
- **Modify simulation algorithm**: `SequentialSimulation.java`
- **Add UI controls**: `SimulationUI.java`
- **Change startup behavior**: `SimulationRunner.java`

### Important Constants
- Minimum distance for force calculations: `1e-12`
- Boundary buffer distance: `5.0`
- Boundary repel factor: `10.0`
- Default particle mass: `1.0`
- Charge range: `±0.5` to `±1.5`
- Initial velocity range: `±1.0`