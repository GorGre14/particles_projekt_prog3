# Simulacija naelektrenih delcev

Simulacija N naelektrenih delcev v 2D prostoru, izvedena na tri nacine:
**sekvencno**, **vzporedno** (niti, skupni pomnilnik) in **porazdeljeno**
(MPI prek MPJ Express).

Vsak cikel simulacije ima dve fazi:

1. izracun sile med vsemi pari delcev - `O(n^2)`,
2. posodobitev hitrosti in pozicij - `O(n)`.

Vsi trije nacini uporabljajo **isto racunsko jedro** (`ForceKernel`) nad **isto
podatkovno postavitvijo**, zato izmerjene razlike izvirajo izkljucno iz nacina
delitve dela, ne iz razlik v izvedbi fizike.

## Zahteve

| Orodje | Razlicica |
|---|---|
| JDK | 17 ali novejsi |
| Maven | 3.6 ali novejsi |

**MPJ Express 0.44 je prilozen projektu** v mapi `lib/mpj`, zato dodatna namestitev
ni potrebna. Skripta `setup_env.sh` poisce `JAVA_HOME` in `MPJ_HOME` samodejno in
poskrbi tudi za izvrsljivost MPJ skript (zip arhiv lahko izgubi pravice).

Ce imate svojo namestitev MPJ Express, jo lahko vsilite z:

```bash
export MPJ_HOME=/pot/do/mpj
```

## Zagon

```bash
./run_sequential.sh                  # sekvencno, z graficnim vmesnikom
./run_parallel.sh                    # vzporedno
./run_distributed.sh 4               # porazdeljeno, 4 MPI procesi
```

Opcije so pri vseh treh skriptah enake:

```
--particles <N>               stevilo delcev (privzeto 400)
--cycles <N>                  stevilo ciklov (privzeto 1000)
--ui <true|false>             graficni vmesnik (privzeto true)
--window <W> <H>              velikost okna (privzeto 800 600)
--bounds <x1> <x2> <y1> <y2>  meje prostora (privzeto 0 800 0 600)
--fps <N>                     hitrost izrisa (privzeto 60)
```

Primer meritve brez izrisa:

```bash
./run_distributed.sh 4 --particles 2000 --cycles 1000 --ui false
```

## Graficni vmesnik

V oknu nastavite stevilo delcev in ciklov, izberete nacin in kliknete **Start**.

Sekvencni in vzporedni nacin teceta v istem procesu. Porazdeljeni nacin
potrebuje vec MPI procesov, zato ga vmesnik zazene s skripto
`run_distributed.sh`; ta odpre **novo okno**, v katerem tece porazdeljena
simulacija. Ob izbiri porazdeljenega nacina se pokaze se polje **Procesi** za
stevilo MPI procesov.

## Meritve

```bash
./benchmark.sh        # 3 ponovitve na nastavitev (privzeto)
./benchmark.sh 1      # hitri pregled
```

Skripta izmeri vse tri nacine pri vec velikostih problema, izpise tabelo in
shrani rezultate v `benchmark_results.csv`. Rezultati so opisani v
`report_gregor_antonaz.pdf`.

## Preverjanje pravilnosti

Delci se generirajo iz fiksnega seeda (42), zato vsi nacini startajo iz enakega
zacetnega stanja in morajo dati enak rezultat:

```bash
./run_sequential.sh    --particles 500 --cycles 300 --ui false
./run_parallel.sh      --particles 500 --cycles 300 --ui false
./run_distributed.sh 4 --particles 500 --cycles 300 --ui false
```

Izpisane pozicije zadnjih petih delcev se morajo v vseh treh primerih ujemati.

## Struktura kode

```
src/main/java/com/example/chargedparticles/
├── SimulationRunner.java              vstopna tocka (sekvencno, vzporedno)
│                                      + skupna simulacijska zanka
├── DistributedRunner.java             vstopna tocka za MPI
├── model/
│   └── Particle.java                  delec: pozicija, hitrost, naboj, masa
├── simulation/
│   ├── ForceKernel.java               izracun sil in integracija (skupno jedro)
│   ├── Simulation.java                vmesnik
│   ├── AbstractSimulation.java        skupno stanje vseh nacinov
│   ├── SequentialSimulation.java      ena nit
│   ├── ParallelSimulation.java        thread pool
│   ├── DistributedSimulation.java     MPI, Allgatherv
│   ├── SimulationFactory.java         izbira nacina
│   ├── SimulationMode.java            enum nacinov
│   └── SimulationParameters.java      parametri + branje ukazne vrstice
└── ui/
    └── SimulationUI.java              graficni vmesnik
```

Ostale datoteke v korenu projekta:

```
pom.xml                  Maven konfiguracija
setup_env.sh             skupna priprava okolja (JAVA_HOME, MPJ_HOME)
run_sequential.sh        zagon sekvencnega nacina
run_parallel.sh          zagon vzporednega nacina
run_distributed.sh       zagon porazdeljenega nacina (MPI)
benchmark.sh             vse meritve iz porocila
benchmark_results.csv    izmerjeni casi
report_gregor_antonaz.*  porocilo (LaTeX vir + PDF)
acmart.cls               ACM predloga za prevajanje porocila
ACM-Reference-Format.bst slog za literaturo
lib/mpj/                 prilozena knjiznica MPJ Express 0.44
```
