# Razred: ForceKernel (`ForceKernel.java`)

## Vloga v simulaciji
`ForceKernel` vsebuje **vso fiziko** simulacije. Uporabljajo ga **vsi trije načini** (sekvenčni, vzporedni, porazdeljeni), zato so enačbe zapisane na enem samem mestu in ne morejo razhajati med različicami.

To ni le vprašanje urejenosti kode. Ker vsi načini računajo z istim jedrom nad isto podatkovno postavitvijo, izmerjene razlike v času izvirajo **izključno iz načina delitve dela** in ne iz razlik v izvedbi fizike. Prav to omogoča pošteno primerjavo v poročilu.

## Podatkovna postavitev
Delci niso shranjeni kot objekti, ampak v **ravnem polju** `double[]`, po 6 vrednosti na delec:

```
[x, y, vx, vy, charge, mass]   ->   delec i se zacne na odmiku i * 6
```

Prednosti pred seznamom objektov:
- **Neprekinjen pomnilnik**: procesorjevo strojno preddobivanje (prefetcher) zazna linearen vzorec branja in podatke naloži vnaprej. Pri seznamu objektov bi morali slediti smernikom (pointer chasing), kar povzroča zgrešitve predpomnilnika.
- **Neposredna MPI komunikacija**: `MPI.DOUBLE` je primitiven tip, ki se kopira brez serializacije.

## Ključne metode

### 1. `computeForces(allData, n, start, end, forces, params)`
Izračuna sile za delce v območju `[start, end)` proti **vsem** `n` delcem, vključno z mejnimi silami.

- **Coulombova sila (modificirana)**:
  $$F = \frac{|c_1 \cdot c_2|}{d^2}$$
- **Preprečevanje eksplozije sil**: če se delca preveč približata, bi $d^2$ težil proti 0 in sila bi eksplodirala. Zato je razdalja omejena navzdol:
  ```java
  if (r2 < 1.0) r2 = 1.0;
  ```
- **"Spin" problema (obrnjena fizika)**:
  > [!IMPORTANT]
  > V tej simulaciji velja **obratno pravilo** kot v naravi:
  > - **Enaka naboja** ($+/+$ ali $-/-$) se **privlačita** (`sign = 1.0`)
  > - **Različna naboja** ($+/-$) se **odbijata** (`sign = -1.0`)
  ```java
  double sign = (c1 * c2 >= 0) ? 1.0 : -1.0;
  ```
- **Mejna sila**: aktivira se, ko je delec od roba oddaljen manj kot `BUFFER = 5.0`, in znaša $F = 10.0 / d^2$ (razdalja omejena navzdol na 0.1).

> [!IMPORTANT]
> **Globalno indeksiranje sil.** Rezultat za delec `i` gre **vedno** v `forces[i*2]` in `forces[i*2+1]`, ne glede na to, kateri izvajalec ga je izračunal. Ker so območja različnih izvajalcev disjunktna, lahko več niti hkrati piše v isto polje **brez zaklepanja**. Prav ta lastnost omogoča, da imajo vsi trije načini isto jedro.

### 2. `integrate(allData, start, end, forces)`
Posodobi hitrosti in pozicije za isto območje po Eulerjevi metodi: $a = F/m$, $v' = v + a$, $x' = x + v'$.

### 3. `toFlatArray(particles)` in `writeBack(data, particles)`
Pretvorbi med seznamom `Particle` objektov in ravnim poljem. `writeBack()` kličemo **samo** pred izrisom ali izpisom, nikoli v vroči zanki.

## Koda razreda

```java
package com.example.chargedparticles.simulation;

import com.example.chargedparticles.model.Particle;
import java.util.List;

/**
 * Skupno jedro za izracun sil nad ravnim poljem (flat double[]).
 *
 * Vse tri simulacije (sekvencna, vzporedna, porazdeljena) uporabljajo
 * TO ISTO jedro in TO ISTO podatkovno postavitev. S tem primerjava meri
 * ucinek paralelizacije in ne razlike v postavitvi podatkov.
 *
 * Postavitev: [x, y, vx, vy, charge, mass] -> 6 doublov na delec.
 * Polje sil je globalno indeksirano: forces[i*2], forces[i*2+1].
 * Zato lahko vec niti hkrati pise v disjunktne odseke istega polja.
 */
public final class ForceKernel {

    public static final int FIELDS_PER_PARTICLE = 6;

    private static final double BUFFER = 5.0;        // razdalja od meje, ki sprozi odboj
    private static final double REPEL_FACTOR = 10.0; // moc odbojne sile

    private ForceKernel() {
        // Samo staticne metode
    }

    /**
     * Pretvori seznam delcev v ravno polje.
     */
    public static double[] toFlatArray(List<Particle> particles) {
        int n = particles.size();
        double[] data = new double[n * FIELDS_PER_PARTICLE];
        for (int i = 0; i < n; i++) {
            Particle p = particles.get(i);
            int o = i * FIELDS_PER_PARTICLE;
            data[o]     = p.getX();
            data[o + 1] = p.getY();
            data[o + 2] = p.getVx();
            data[o + 3] = p.getVy();
            data[o + 4] = p.getCharge();
            data[o + 5] = p.getMass();
        }
        return data;
    }

    /**
     * Zapise podatke iz ravnega polja nazaj v seznam Particle objektov.
     * Klicemo samo za UI in za koncni izpis - ne v vroci zanki.
     */
    public static void writeBack(double[] data, List<Particle> particles) {
        int n = particles.size();
        for (int i = 0; i < n; i++) {
            int o = i * FIELDS_PER_PARTICLE;
            Particle p = particles.get(i);
            p.setX(data[o]);
            p.setY(data[o + 1]);
            p.setVx(data[o + 2]);
            p.setVy(data[o + 3]);
        }
    }

    /**
     * Izracuna sile za delce [start, end) proti vsem n delcem.
     *
     * Rezultat gre v forces[i*2] in forces[i*2+1] (globalni indeks i).
     * Ker so odseki disjunktni, sinhronizacija ni potrebna.
     *
     * @param state ravno polje z vsemi delci
     * @param n       skupno stevilo delcev
     * @param start   zacetni indeks (vkljucno)
     * @param end     koncni indeks (izkljucno)
     * @param forces  izhodno polje dolzine n*2
     * @param params  parametri simulacije (meje prostora)
     */
    public static void computeForces(double[] state, int n, int start, int end,
                                     double[] forces, SimulationParameters params) {
        final double minX = params.getMinX();
        final double maxX = params.getMaxX();
        final double minY = params.getMinY();
        final double maxY = params.getMaxY();

        for (int i = start; i < end; i++) {
            int oi = i * FIELDS_PER_PARTICLE;
            double x1 = state[oi];
            double y1 = state[oi + 1];
            double c1 = state[oi + 4];

            double fxSum = 0.0;
            double fySum = 0.0;

            // Sila od vseh ostalih delcev - O(n)
            for (int j = 0; j < n; j++) {
                if (j == i) continue; // Delec ne vpliva sam nase

                int oj = j * FIELDS_PER_PARTICLE;
                double dx = state[oj] - x1;
                double dy = state[oj + 1] - y1;
                double c2 = state[oj + 4];

                double r2 = dx * dx + dy * dy;
                if (r2 < 1.0) r2 = 1.0; // Prepreci eksplozijo sile
                double r = Math.sqrt(r2);

                double magnitude = Math.abs(c1 * c2) / r2;
                double sign = (c1 * c2 >= 0) ? 1.0 : -1.0;

                fxSum += sign * magnitude * (dx / r);
                fySum += sign * magnitude * (dy / r);
            }

            // Mejne sile (da delci ostanejo v oknu)
            if (x1 < minX + BUFFER) {
                double d = Math.max(0.1, x1 - minX);
                fxSum += REPEL_FACTOR / (d * d);
            }
            if (x1 > maxX - BUFFER) {
                double d = Math.max(0.1, maxX - x1);
                fxSum -= REPEL_FACTOR / (d * d);
            }
            if (y1 < minY + BUFFER) {
                double d = Math.max(0.1, y1 - minY);
                fySum += REPEL_FACTOR / (d * d);
            }
            if (y1 > maxY - BUFFER) {
                double d = Math.max(0.1, maxY - y1);
                fySum -= REPEL_FACTOR / (d * d);
            }

            forces[i * 2]     = fxSum;
            forces[i * 2 + 1] = fySum;
        }
    }

    /**
     * Posodobi hitrosti in pozicije za delce [start, end).
     * Pospesek a = F/m, nova hitrost v = v + a, nova pozicija x = x + v.
     */
    public static void integrate(double[] state, int start, int end, double[] forces) {
        for (int i = start; i < end; i++) {
            int o = i * FIELDS_PER_PARTICLE;

            double mass = state[o + 5];
            double newVx = state[o + 2] + forces[i * 2] / mass;
            double newVy = state[o + 3] + forces[i * 2 + 1] / mass;

            state[o]     += newVx;
            state[o + 1] += newVy;
            state[o + 2] = newVx;
            state[o + 3] = newVy;
        }
    }
}
```
