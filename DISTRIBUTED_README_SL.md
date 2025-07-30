# ChargedParticles - Implementacija porazdeljenega računalništva

## Pregled

Ta dokument opisuje implementacijo porazdeljenega računalništva za fizikalno simulacijo ChargedParticles. Porazdeljeni način omogoča izvajanje simulacije preko več procesov ali strojev z uporabo Java RMI (Remote Method Invocation).

## Arhitektura

### Arhitektura glavni-delavec
- **Glavno vozlišče**: koordinira simulacijo, razdeli delce med delovna vozlišča in združuje rezultate
- **Delovna vozlišča**: izračunavajo sile za dodeljene delce in poročajo rezultate glavnemu vozlišču
- **Komunikacija**: Java RMI za omrežno komunikacijo med vozlišči

### Ključne komponente

1. **ParticleState** - Serializabilna predstavitev delcev za omrežni prenos
2. **WorkerNode** - RMI vmesnik, ki definira operacije delovnega vozlišča
3. **WorkerNodeImpl** - Implementacija funkcionalnosti delovnega vozlišča
4. **MasterCoordinator** - Upravlja odkrivanje delovnih vozlišč in koordinacijo simulacije
5. **DistributedSimulation** - Glavni simulacijski razred, ki implementira vmesnik Simulation
6. **DistributedConfig** - Konfiguracijske konstante za omrežne nastavitve

## Hiter začetek

### Testiranje na enem stroju

1. **Izgradnja projekta**:
   ```bash
   mvn clean package
   ```

2. **Zagon delovnih vozlišč** (v ločenih terminalih):
   ```bash
   skripta ./start-distributed.sh worker
   skripta ./start-distributed.sh worker
   ```

3. **Zagon glavnega vozlišča**:
   ```bash
   skripta ./start-distributed.sh master 2
   ```

### Ročni zagon

1. **Zagon delovnih vozlišč**:
   ```bash
   java -cp target/classes com.example.chargedparticles.SimulationRunner --role worker
   ```

2. **Zagon glavnega vozlišča**:
   ```bash
   java -cp target/classes com.example.chargedparticles.SimulationRunner \
     --mode distributed --role master --workers 2 --particles 500 --cycles 100
   ```

## Konfiguracija

### Omrežne nastavitve (DistributedConfig.java)
- **Registry Host**: `localhost` (za testiranje na enem stroju)
- **Registry Port**: `1099` (RMI privzeti port)
- **Worker Discovery Timeout**: 10 sekund
- **Force Calculation Timeout**: 5 sekund
- **Position Update Timeout**: 2 sekundi
- **Maksimalno delovnih vozlišč**: 10
- **Minimalno delcev na delovno vozlišče**: 10

### Možnosti ukazne vrstice

#### Splošne možnosti
- `--mode distributed` - Omogoči porazdeljeni simulacijski način
- `--role master|worker` - Določi vlogo vozlišča (privzeto: master)
- `--workers <število>` - Število pričakovanih delovnih vozlišč (samo glavni, privzeto: 2)
- `--particles <število>` - Število delcev (privzeto: 400)
- `--cycles <število>` - Število simulacijskih ciklov (privzeto: 1000)
- `--ui true|false` - Vklopi/izklopi grafični vmesnik (GUI) (privzeto: true)

#### Primeri
```bash
# Zagon delovnega vozlišča
java -jar ChargedParticles.jar --role worker

# Zagon glavnega s 3 delovnimi vozlišči, 1000 delci, brez GUI
java -jar ChargedParticles.jar --mode distributed --role master --workers 3 --particles 1000 --ui false

# Zagon glavnega z GUI
java -jar ChargedParticles.jar --mode distributed --role master --workers 2 --particles 500
```

## Podrobnosti implementacije

### Strategija razdelitve delcev
- Delci so enakomerno razdeljeni med delovna vozlišča
- Vsako delovno vozlišče je odgovorno za izračun sil za svoje dodeljene delce
- Vsa delovna vozlišča prejmejo celotno stanje vseh delcev za izračun sil
- Rezultati so sinhronizirani po vsakem simulacijskem ciklu

### Proces simulacijskega cikla
1. **Glavno vozlišče** pretvori lokalne Particle objekte v serializabilne ParticleState objekte
2. **Glavno vozlišče** razdeli delce med razpoložljivimi delovnimi vozlišči
3. **Glavno vozlišče** inicializira vsa delovna vozlišča z njihovimi dodeljenimi delci
4. **Delovna vozlišča** vzporedno izračunavajo sile za dodeljene delce
5. **Glavno vozlišče** zbira rezultate izračuna sil od vseh delovnih vozlišč
6. **Delovna vozlišča** posodobijo položaje delcev na podlagi izračunanih sil
7. **Glavno vozlišče** zbira posodobitve položajev in jih uporabi na lokalne delce
8. **Ponovi** za naslednji simulacijski cikel

### Obravnavanje napak in odpornost na napake
- **Časovne omejitve povezave**: Vsi RMI klici imajo konfiguracijske časovne omejitve
- **Odkrivanje delovnih vozlišč**: Glavno vozlišče samodejno odkrije razpoložljiva delovna vozlišča v RMI registru
- **Eleganten fallback**: Vrne se na sekvenčno simulacijo, če porazdeljeni setup ne uspe
- **Preverjanje zdravja delovnih vozlišč**: Glavno vozlišče periodično preverja, ali so delovna vozlišča še vedno aktivna
- **Samodejna ponovna povezava**: Glavno vozlišče poskuša znova povezati z delovnimi vozlišči po napakah

### Deterministični rezultati
- Uporablja fiksno random seed (42L) za generiranje delcev
- Zagotavlja identično začetno razporeditev delcev v vseh simulacijskih načinih
- Vzdržuje konsistentno razporeditev izračuna sil
- Sinhrona komunikacija preprečuje race conditions

## Značilnosti zmogljivosti

### Pričakovane zmogljivosti
- **Mali podatkovni nizi** (< 500 delcev): Priporočen sekvenčni način zaradi omrežnih stroškov
- **Srednji podatkovni nizi** (500-2000 delcev): Porazdeljeni način kaže prednosti
- **Veliki podatkovni nizi** (> 2000 delcev): Pomembno pospeševanje z več delovnimi vozlišči

### Omrežni stroški
- Serializacija/deserializacija stanj delcev
- RMI komunikacijska latenca
- Sinhronizacijske bariere med simulacijskimi cikli

### Faktorji skalabilnosti
- Linearna pohitritev pričakovana z dodatnimi delovnimi vozlišči za velike količine delcev
- Omrežna pasovna širina lahko postane ozko grlo za zelo velike simulacije
- Poraba pomnilnika se povečuje s številom delcev na vsakem vozlišču

## Namestitev na več strojih

### Predpogoji
- Java 11+ nameščena na vseh strojih
- Omrežna povezljivost med stroji
- Ista verzija ChargedParticles na vseh strojih

### Koraki namestitve

1. **Izbira glavnega stroja**:
   - Ta stroj bo poganjal RMI register
   - Mora imeti dobro omrežno povezljivost z vsemi delovnimi vozlišči

2. **Zagon RMI registra** (na glavnem stroju):
   ```bash
   rmiregistry 1099
   ```

3. **Sprememba DistributedConfig** (če potrebno):
   ```java
   public static final String RMI_REGISTRY_HOST = "master-machine-ip";
   ```

4. **Zagon delovnih vozlišč** (na vsakem delovnem stroju):
   ```bash
   java -cp ChargedParticles.jar com.example.chargedparticles.SimulationRunner --role worker
   ```

5. **Zagon glavnega vozlišča**:
   ```bash
   java -cp ChargedParticles.jar com.example.chargedparticles.SimulationRunner \
     --mode distributed --role master --workers N
   ```

## GUI integracija

### Porazdeljeni način v GUI
- Na voljo v spustnem meniju simulacijskih načinov
- Glavno vozlišče prikazuje GUI z vizualizacijo delcev v realnem času
- Delovna vozlišča tečejo brez GUI (headless)
- Preklapljanje načinov v realnem času podprto med simulacijo

### GUI funkcionalnosti
- **Izbira načina**: Spustni meni vključuje možnost "Distributed simulation"
- **Prilagajanje parametrov**: Spreminjanje delcev, ciklov in drugih nastavitev
- **Gumbova Start/Reset**: Popoln nadzor nad življenjskim ciklom simulacije
- **Vizualizacija v realnem času**: Delci se posodabljajo v realnem času med simulacijo

## Razhroščevanje in odpravljanje težav

### Pogoste težave

1. **RMI register ni najden**:
   - Prepričajte se, da RMI register deluje na portu 1099
   - Delovna vozlišča samodejno ustvarijo register, če ni najden

2. **Delovna vozlišča niso odkrita**:
   - Preverite omrežno povezljivost
   - Preverite, ali so se delovna vozlišča uspešno registrirala
   - Povečajte časovno omejitev odkrivanja, če potrebno

3. **Napake časovnih omejitev**:
   - Povečajte vrednosti časovnih omejitev v DistributedConfig
   - Preverite omrežno latenco med vozlišči
   - Preverite, ali so delovna vozlišča odzivna

4. **Simulacija se vrne na sekvenčni način**:
   - Normalno obnašanje, ko porazdeljeni setup ne uspe
   - Preverite izhod konzole za specifična sporočila o napakah

### Nasveti za razhroščevanje

1. **Omogočite podrobno beleženje**:
   - Dodajte `-Djava.rmi.server.logCalls=true` k JVM argumentom
   - Spremljajte izhod konzole na vseh vozliščih

2. **Testirajte omrežno povezljivost**:
   ```bash
   telnet master-machine-ip 1099
   ```

3. **Preverite vsebino RMI registra**:
   ```bash
   java -cp . ListRegistry localhost 1099
   ```

4. **Spremljajte sistemske vire**:
   - Poraba procesorja na delovnih vozliščih med izračuni sil
   - Poraba pomnilnika za velike količine delcev
   - Omrežna pasovna širina med simulacijo

## Razvoj in razširitve

### Dodajanje novih funkcionalnosti

1. **Modeli sil po meri**:
   - Razširite ForceUtils z novimi metodami izračuna
   - Spremenite WorkerNodeImpl za uporabo novih tipov sil

2. **Uravnoteženje obremenitve**:
   - Implementirajte dinamično prerazporeditev delcev
   - Spremljajte zmogljivost delovnih vozlišč in prilagodite dodelitve

3. **Obstojnost**:
   - Dodajte funkcionalnost shranjevanja/nalaganja stanj simulacije
   - Implementirajte kontrolne točke za dolgoročne simulacije

### Smernice testiranja

1. **Testiranje enot**:
   - Testirajte serializacijo ParticleState
   - Preverite pravilnost izračuna sil
   - Validirajte logiko odkrivanja delovnih vozlišč

2. **Testiranje integracije**:
   - Primerjajte rezultate porazdeljenega proti sekvenčnemu
   - Testirajte z različnimi števili delovnih vozlišč
   - Preverite obravnavanje časovnih omejitev

3. **Testiranje zmogljivosti**:
   - Benchmark scaling-a s številom delovnih vozlišč
   - Izmerite omrežne stroške
   - Profilirajte vzorce porabe pomnilnika

## Akademski kontekst

Ta implementacija demonstrira več pomembnih konceptov porazdeljenih sistemov:

### Koncepti porazdeljenega računalništva
- **Remote Method Invocation (RMI)**: Komunikacija med procesi
- **Vzorec glavni-delavec**: Porazdeljena koordinacija nalog
- **Uravnoteženje obremenitve**: Enakomerna razporeditev računskega dela
- **Odpornost na napake**: Elegantno obravnavanje omrežnih napak
- **Sinhronizacija**: Koordiniranje vzporednih izračunov

### Analiza zmogljivosti
- **Skalabilnost**: Linearna pohitritev z dodatnimi delovnimi vozlišči
- **Omrežni stroški**: Vpliv serializacije in komunikacije
- **Distribucija obremenitve**: Uravnoteženje dela med heterogenimi vozlišči

### Izobraževalna vrednost
- Praktične izkušnje s porazdeljenimi sistemi
- Razumevanje izzivov omrežnega programiranja
- Izkušnje s tehnologijo Java RMI
- Tehnike optimizacije zmogljivosti

## Zaključek

Porazdeljena implementacija uspešno razširi simulacijo ChargedParticles za podporo izvajanja na več vozliščih, hkrati pa ohranja združljivost z obstoječimi sekvenčnimi in vzporednimi načini. Modularna zasnova omogoča enostavno razširitev in modifikacijo, kar jo naredi primerno za izobraževalne namene in nadaljnji razvoj.

Implementacija daje prednost:
- **Enostavnosti**: Uporaba Java RMI za preprosto porazdeljeno komunikacijo
- **Zanesljivosti**: Odpornost na napake in elegantno obravnavanje napak
- **Zmogljivosti**: Učinkovita razporeditev delcev in minimalni omrežni stroški
- **Vzdrževanju**: Čista ločitev skrbi in modularna zasnova

Za vprašanja ali težave si oglejte razdelek za odpravljanje težav ali preverite izhod konzole za podrobna sporočila o napakah.