# Razred: DistributedSimulation (`DistributedSimulation.java`)

## Vloga v simulaciji
Porazdeljena različica z **MPJ Express** (MPI za Javo). Vsak proces hrani kopijo stanja **vseh** delcev, računa pa sile in nove pozicije samo za **svoj odsek**. Na koncu cikla kolektivna operacija `Allgatherv` združi odseke, tako da imajo vsi procesi spet celotno stanje.

V nasprotju z vzporednim načinom si procesi **ne delijo pomnilnika**, zato je treba stanje ob vsakem ciklu eksplicitno prenesti. Ta prenos je cena porazdeljenega modela.

## Razdelitev delcev med procese
Prvih `n mod size` procesov dobi en delec več, da je delitev čim bolj enakomerna:

```java
private int startIndexOf(int r) {
    int base = n / size;
    int remainder = n % size;
    return (r < remainder) ? r * (base + 1)
                           : remainder * (base + 1) + (r - remainder) * base;
}
```

Ker `startIndexOf(size)` vrne `n`, je konec območja procesa `r` kar `startIndexOf(r + 1)` — meje in tabele za `Allgatherv` se tako izračunajo z eno samo pomožno metodo.

Primer z 10 delci in 3 procesi:

| Rank | Delci | Število |
|---|---|---|
| 0 | [0, 4) | 4 |
| 1 | [4, 7) | 3 |
| 2 | [7, 10) | 3 |

## Algoritem enega cikla
1. **Faza 1** — vsak proces izračuna sile za **svoje** delce (potrebuje podatke **vseh** delcev, ki jih že ima).
2. **Faza 2** — vsak proces posodobi pozicije in hitrosti **samo svojih** delcev.
3. **Faza 3** — `Allgatherv`: vsi procesi si izmenjajo posodobljene odseke.

## Allgatherv
> [!IMPORTANT]
> `Allgatherv` = "vsi zberejo vse, različne količine". Vsak proces pošlje svoj del in prejme dele vseh ostalih. Po klicu ima **vsak** proces celotno posodobljeno stanje.

| Parameter | Pomen |
|---|---|
| `sendOffset` | začetek mojih podatkov v polju |
| `sendCount` | koliko doublov pošiljam |
| `recvCounts[]` | koliko doublov pričakujem od vsakega procesa |
| `displacements[]` | kje v sprejemnem polju se začnejo podatki vsakega procesa |

```
Pred Allgatherv:
  Rank 0: [nov  | star | star]
  Rank 1: [star | nov  | star]
  Rank 2: [star | star | nov ]

Po Allgatherv:
  Rank 0..2: [nov | nov | nov]
```

> [!NOTE]
> **Zakaj ne Scatter + Gather?**
> Pri Scatter/Gather bi celoten rezultat imel samo rank 0 in bi ga moral znova razposlati (Broadcast). Z `Allgatherv` **vsi** procesi hkrati prejmejo celotno stanje — ena operacija namesto dveh.

> [!NOTE]
> **Zakaj potrebuje vsak proces vse delce?**
> Ker je sila na delec $i$ vsota sil **vseh** ostalih $n-1$ delcev. To je inherentna lastnost N-body problema.

## Cena komunikacije
Vsak proces prenese $6 \cdot 8 \cdot n = 48n$ bajtov na cikel. Pri $n = 2000$ je to 96 kB na proces, in to v **vsakem** ciklu.

Ker komunikacija raste kot $O(n)$, računanje pa kot $O(n^2)$, zaostanek za vzporedno različico z večanjem problema **pada**:

| $n$ | Vzporedna [s] | Porazdeljena $P=10$ [s] | Porazdeljena počasnejša za |
|---|---|---|---|
| 500 | 0,161 | 0,608 | 278 % |
| 1000 | 0,467 | 0,798 | 71 % |
| 2000 | 1,453 | 2,386 | 64 % |
| 3000 | 4,159 | 5,416 | 30 % |

Pri $n = 500$ je porazdeljena različica z 10 procesi celo **počasnejša od sekvenčne** (pohitritev $0{,}72\times$): računanja na cikel je tako malo, da 1000 klicev `Allgatherv` prevlada nad celotnim časom.

> [!IMPORTANT]
> **Kdaj se porazdeljeni model izplača?** Ko procesi tečejo na **različnih računalnikih** — takrat sta na voljo skupni pomnilnik in skupno število jeder več strojev. Znotraj enega računalnika te prednosti ni, zato je komunikacija čisti režijski strošek in je vzporedna različica pričakovano hitrejša.

## Koda razreda

```java
package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;
import mpi.MPI;
import mpi.MPIException;

import java.util.List;

/**
 * Porazdeljena simulacija z MPJ Express (MPI za Javo).
 *
 * Vsak proces hrani kopijo stanja vseh delcev, izracuna pa sile in nove
 * pozicije samo za svoj odsek. Na koncu cikla kolektivna operacija Allgatherv
 * zdruzi odseke, tako da imajo vsi procesi spet celotno stanje.
 *
 * V nasprotju z vzporednim nacinom si procesi ne delijo pomnilnika, zato je
 * treba stanje ob vsakem ciklu eksplicitno prenesti. Ta prenos je cena
 * porazdeljenega modela.
 */
public class DistributedSimulation extends AbstractSimulation {

    private final int rank;         // rang tega procesa
    private final int size;         // skupno stevilo procesov
    private final int myStart;      // prvi delec tega procesa (vkljucno)
    private final int myEnd;        // zadnji delec tega procesa (izkljucno)

    // Opis odsekov za Allgatherv: koliko doublov poslje in kam jih vpise vsak proces.
    private final int[] recvCounts;
    private final int[] displacements;

    public DistributedSimulation(List<Particle> particles, SimulationParameters params,
                                 int rank, int size) {
        super(particles, params);
        this.rank = rank;
        this.size = size;
        this.myStart = startIndexOf(rank);
        this.myEnd = startIndexOf(rank + 1);

        this.recvCounts = new int[size];
        this.displacements = new int[size];
        for (int r = 0; r < size; r++) {
            int start = startIndexOf(r);
            recvCounts[r] = (startIndexOf(r + 1) - start) * ForceKernel.FIELDS_PER_PARTICLE;
            displacements[r] = start * ForceKernel.FIELDS_PER_PARTICLE;
        }
    }

    /**
     * Zacetni indeks delcev za proces r. Prvih (n mod size) procesov dobi
     * en delec vec, da je delitev cim bolj enakomerna.
     * Za r == size vrne n, zato je konec odseka r kar startIndexOf(r + 1).
     */
    private int startIndexOf(int r) {
        int base = n / size;
        int remainder = n % size;
        return (r < remainder) ? r * (base + 1)
                               : remainder * (base + 1) + (r - remainder) * base;
    }

    @Override
    public void performOneCycle() {
        // FAZA 1 + 2: izracun sil in novih pozicij za lasten odsek.
        ForceKernel.computeForces(state, n, myStart, myEnd, forces, params);
        ForceKernel.integrate(state, myStart, myEnd, forces);

        // FAZA 3: vsak proces poslje svoj odsek in prejme odseke vseh ostalih.
        int sendOffset = myStart * ForceKernel.FIELDS_PER_PARTICLE;
        int sendCount = (myEnd - myStart) * ForceKernel.FIELDS_PER_PARTICLE;
        try {
            MPI.COMM_WORLD.Allgatherv(
                    state, sendOffset, sendCount, MPI.DOUBLE,
                    state, 0, recvCounts, displacements, MPI.DOUBLE);
        } catch (MPIException e) {
            throw new IllegalStateException("Napaka pri MPI komunikaciji", e);
        }
    }

    @Override
    public String getDescription() {
        return "Porazdeljena (" + size + " procesov)";
    }

    public int getRank() {
        return rank;
    }

    public int getMyStart() {
        return myStart;
    }

    public int getMyEnd() {
        return myEnd;
    }
}
```
