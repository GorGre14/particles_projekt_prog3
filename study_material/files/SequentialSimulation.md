# Razred: SequentialSimulation (`SequentialSimulation.java`)

## Vloga v simulaciji
Najenostavnejša različica: **ena sama nit** obdela celotno območje `[0, n)`. Služi kot referenca (baseline), proti kateri merimo pohitritev ostalih dveh načinov.

## Algoritem
Celotna izvedba sta samo dva klica skupnega jedra:

```java
ForceKernel.computeForces(state, n, 0, n, forces, params);  // FAZA 1: O(n^2)
ForceKernel.integrate(state, 0, n, forces);                 // FAZA 2: O(n)
```

## Kompleksnost

| Faza | Kompleksnost |
|---|---|
| Faza 1 — izračun sil | $O(n^2)$ |
| Faza 2 — posodobitev | $O(n)$ |
| **Skupaj na cikel** | $O(n^2)$ |
| **Skupaj za simulacijo** | $O(n^2 \cdot C)$, kjer je $C$ število ciklov |

> [!NOTE]
> **Zakaj je razred tako kratek?**
> Ker je vsa fizika v `ForceKernel`. Sekvenčna različica samo določi, da eno samo območje `[0, n)` obdela ena nit. Vzporedna in porazdeljena različica kličeta **isti dve metodi**, le z drugimi mejami območja.

## Izmerjeno
Cena enega cikla pri $n = 2000$ je konstantna, ne glede na dolžino teka — 6,94 do 7,13 ms. To potrjuje, da sekvenčna različica nima režijskega stroška, ki bi rasel s številom ciklov.

## Koda razreda

```java
package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;
import java.util.List;

/**
 * Sekvencna simulacija: ena sama nit izracuna sile za vse delce.
 * Sluzi kot referenca za merjenje pohitritve ostalih dveh nacinov.
 */
public class SequentialSimulation extends AbstractSimulation {

    public SequentialSimulation(List<Particle> particles, SimulationParameters params) {
        super(particles, params);
    }

    @Override
    public void performOneCycle() {
        ForceKernel.computeForces(state, n, 0, n, forces, params);
        ForceKernel.integrate(state, 0, n, forces);
    }

    @Override
    public String getDescription() {
        return "Sekvencna";
    }
}
```
