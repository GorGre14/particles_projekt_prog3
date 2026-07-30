# Arhitektura sistema (Splošni pregled)

Diagram prikazuje odnose med ključnimi razredi. Uporabljena sta vzorca **Strategija (Strategy)** prek vmesnika `Simulation` in **Tovarna (Factory)** prek razreda `SimulationFactory`.

Ključna lastnost zasnove: vsi trije načini računajo z **istim jedrom** (`ForceKernel`) nad **istim stanjem** (`AbstractSimulation`) in tečejo v **isti zanki** (`SimulationRunner.runCycles`). Razlikujejo se samo v tem, kdo izračuna kateri odsek delcev.

```mermaid
classDiagram
    class SimulationRunner {
        +RANDOM_SEED = 42
        +main(args)
        +runCycles(simulation, particles, params, printResults)$
        +generateParticles(params)$
        +restartSimulation()$
        +stopSimulation()$
    }

    class DistributedRunner {
        +main(args)
    }

    class Simulation {
        <<interface>>
        +performOneCycle()
        +syncToParticles(particles)
        +getDescription()
        +shutdown()
    }

    class AbstractSimulation {
        <<abstract>>
        #int n
        #double[] state
        #double[] forces
        #SimulationParameters params
        +syncToParticles(particles)
    }

    class SequentialSimulation {
        +performOneCycle()
    }

    class ParallelSimulation {
        -ExecutorService executor
        -int numThreads
        -List~Callable~ tasks
        +performOneCycle()
        +shutdown()
    }

    class DistributedSimulation {
        -int rank
        -int size
        -int myStart
        -int myEnd
        -int[] recvCounts
        -int[] displacements
        -startIndexOf(r)
        +performOneCycle()
    }

    class ForceKernel {
        <<static>>
        +FIELDS_PER_PARTICLE = 6
        +computeForces(state, n, start, end, forces, params)$
        +integrate(state, start, end, forces)$
        +toFlatArray(particles)$
        +writeBack(data, particles)$
    }

    class SimulationFactory {
        +createSimulation(mode, particles, params)$ Simulation
    }

    class SimulationParameters {
        +fromArgs(args)$ SimulationParameters
    }

    class SimulationUI {
        -List~Particle~ particles
        -Timer renderTimer
        +paintComponent(g)
        -startDistributed()
    }

    class Particle {
        -double x, y
        -double vx, vy
        -double charge
        -double mass
        +randomParticle()$
    }

    AbstractSimulation ..|> Simulation : implementira
    SequentialSimulation --|> AbstractSimulation : deduje
    ParallelSimulation --|> AbstractSimulation : deduje
    DistributedSimulation --|> AbstractSimulation : deduje

    AbstractSimulation --> ForceKernel : stanje in prepis
    SequentialSimulation --> ForceKernel : isto jedro
    ParallelSimulation --> ForceKernel : isto jedro
    DistributedSimulation --> ForceKernel : isto jedro

    SimulationRunner --> Simulation : uporablja (Strategy)
    SimulationRunner --> SimulationUI : ustvari GUI
    SimulationRunner --> SimulationParameters : konfiguracija
    SimulationRunner --> Particle : seznam delcev

    DistributedRunner --> SimulationRunner : runCycles + generateParticles
    DistributedRunner --> DistributedSimulation : ustvari

    SimulationFactory ..> Simulation : ustvari
    SimulationMode --> SimulationFactory : doloca tip
    SimulationUI ..> DistributedRunner : zazene prek run_distributed.sh
    SimulationUI --> Particle : prikazuje
    ForceKernel --> Particle : pretvorba v ravno polje
```

## Opis modulov

- **Vstopni točki**: `SimulationRunner` (sekvenčni in vzporedni tek) ter `DistributedRunner` (porazdeljeni tek prek MPJ).
- **Skupno jedro**: `ForceKernel` vsebuje vso fiziko — Coulombovo silo, mejne sile in Eulerjevo integracijo. Uporabljajo ga vsi trije načini.
- **Skupno stanje**: `AbstractSimulation` hrani ravno polje delcev in polje sil. Ker je stanje enako pri vseh, se podrazredi razlikujejo samo po delitvi dela.
- **Strategija**: vmesnik `Simulation` določa pogodbo za en časovni korak.
- **Podatkovni model**: `Particle` predstavlja delec, `SimulationParameters` pa nastavitve in branje ukazne vrstice.

## Zakaj ta zasnova

> [!IMPORTANT]
> Primerjava izvedbenih modelov je smiselna **samo**, če vsi izvajajo isti algoritem. Če bi vsak način imel svojo kopijo fizike ali svojo podatkovno postavitev, bi izmerjena razmerja odražala te razlike in ne učinka paralelizacije. Z enim jedrom, enim stanjem in eno zanko merimo natanko to, kar želimo meriti.
