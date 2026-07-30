# Razred: Particle (`Particle.java`)

## Vloga v simulaciji
Podatkovni model delca v 2D prostoru. Vsak delec ima pozicijo `(x, y)`, hitrost `(vx, vy)`, naboj `charge` in maso `mass = 1.0`.

## Vloga v izvajanju
Med samim računanjem se ti objekti **ne uporabljajo**. Simulacija dela nad ravnim `double[]` poljem (glej `ForceKernel`), objekti `Particle` pa služijo:
- kot **začetno stanje**, iz katerega se zgradi ravno polje,
- kot **izhod** za izris in izpis, kamor se stanje prepiše z `syncToParticles()`.

## Deterministično generiranje
```java
public static Particle randomParticle(double minX, double maxX,
                                      double minY, double maxY, Random random)
```
Metoda sprejme **že ustvarjen** `Random` objekt, ne ustvarja svojega. Klicatelj ga ustvari s fiksnim seedom (42), zato so generirani delci vedno enaki.

> [!NOTE]
> **Zakaj je to pomembno?**
> Vsi trije načini startajo iz **enakega** začetnega stanja. To omogoča dvoje: pošteno primerjavo časov in preverjanje, da vsi načini dajo enak rezultat.

Lastnosti generiranega delca:

| Lastnost | Razpon |
|---|---|
| pozicija | znotraj podanih mej |
| hitrost | $[-1, 1]$ |
| naboj | $\pm[0{,}5,\ 1{,}5]$, predznak naključen |
| masa | 1.0 |

## Koda razreda

```java
package com.example.chargedparticles.model;

import java.util.Random;

/**
 * Razred za predstavitev delca v 2D prostoru.
 * Vsak delec ima pozicijo (x, y), hitrost (vx, vy), naboj in maso.
 */
public class Particle {

    private double x;      // X-koordinata delca
    private double y;      // Y-koordinata delca
    private double vx;     // Hitrost v smeri X
    private double vy;     // Hitrost v smeri Y
    private double charge; // Naboj delca (pozitiven ali negativen)
    private double mass = 1.0; // Masa delca

    /**
     * Konstruktor za delec.
     *
     * @param x      zacetna x koordinata
     * @param y      zacetna y koordinata
     * @param vx     zacetna hitrost v smeri x
     * @param vy     zacetna hitrost v smeri y
     * @param charge naboj delca (pozitiven ali negativen)
     */
    public Particle(double x, double y, double vx, double vy, double charge) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.charge = charge;
    }

    /**
     * Ustvari nakljucen delec z determinističnim seed-om.
     * Zagotavlja ponovljive rezultate simulacije.
     *
     * @param minX   minimalna x koordinata
     * @param maxX   maksimalna x koordinata
     * @param minY   minimalna y koordinata
     * @param maxY   maksimalna y koordinata
     * @param random Random objekt z dolocenim seed-om
     * @return nov delec z nakljucnimi lastnostmi
     */
    public static Particle randomParticle(double minX, double maxX, double minY, double maxY, Random random) {
        double x = minX + random.nextDouble() * (maxX - minX);
        double y = minY + random.nextDouble() * (maxY - minY);
        double vx = random.nextDouble() * 2.0 - 1.0;  // [-1, 1]
        double vy = random.nextDouble() * 2.0 - 1.0;  // [-1, 1]
        double charge = random.nextBoolean() ? 0.5 + random.nextDouble() : -(0.5 + random.nextDouble());
        return new Particle(x, y, vx, vy, charge);
    }

    // Getterji in setterji
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getVx() { return vx; }
    public void setVx(double vx) { this.vx = vx; }

    public double getVy() { return vy; }
    public void setVy(double vy) { this.vy = vy; }

    public double getCharge() { return charge; }
    public double getMass() { return mass; }

    @Override
    public String toString() {
        return String.format("Particle(x=%.2f, y=%.2f, vx=%.2f, vy=%.2f, charge=%.2f)",
                x, y, vx, vy, charge);
    }
}
```
