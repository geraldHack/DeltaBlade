# DeltaBlade

**Original arcade shooter game** inspired by classic space shooters like Galaga. Built with Java 21 and [FXGL](https://github.com/AlmasB/FXGL) game engine.

![Java](https://img.shields.io/badge/Java-21-orange)
![FXGL](https://img.shields.io/badge/FXGL-21.1-blue)
![License](https://img.shields.io/badge/License-MIT-green)

---

## 🇬🇧 English

### Requirements
- **JDK 21** or higher
- **Maven 3.8+**

### How to Run
```bash
mvn javafx:run
```

The game automatically disables Metal rendering on macOS for compatibility. If you still experience rendering issues, you can try:
```bash
mvn javafx:run -Djavafx.options="-Dprism.order=sw"
```

Or alternatively:
```bash
mvn compile exec:java
```

### How to Play
- **Move**: Arrow keys (← →) or A/D
- **Fire**: Space or X
- **Restart**: R (after Game Over)

### Game Features

#### Wave System
- **Squad Entry**: Enemies fly in along curved paths in squads, then settle into formation
- **Squad Combo Bonus**: Destroy an entire incoming squad before they settle = bonus points!
- **Diving Attacks**: After settling, enemies may dive toward the player

#### Combat
- **Limited Ammo**: Start with 5 simultaneous bullets. Slots free when bullets hit or leave screen.
- **Weapon Upgrades**: Collect golden pickups (W) to upgrade:
  - Grade 1: Single shot
  - Grade 2: Double shot
  - Grade 3: Triple shot
- **Extra Ammo**: Collect cyan pickups (+) to increase capacity (max 12)
- **Death Penalty**: Getting hit drops weapon grade by 1 (minimum grade 1)

#### Enemies
- **Basic (Red)**: Standard enemies, 1 hit
- **Fast (Green)**: Quick and agile, 1 hit
- **Tough (Purple)**: Armored, takes 3 hits

#### Survival
- **Lives**: 3 lives total
- **Invulnerability**: Brief i-frames after getting hit

---

## 🇩🇪 Deutsch

### Voraussetzungen
- **JDK 21** oder höher
- **Maven 3.8+**

### Starten
```bash
mvn javafx:run
```

Das Spiel deaktiviert automatisch Metal-Rendering auf macOS für bessere Kompatibilität. Bei Rendering-Problemen:
```bash
mvn javafx:run -Djavafx.options="-Dprism.order=sw"
```

Oder alternativ:
```bash
mvn compile exec:java
```

### Steuerung
- **Bewegen**: Pfeiltasten (← →) oder A/D
- **Schießen**: Leertaste oder X
- **Neustart**: R (nach Game Over)

### Spielmechaniken

#### Wellen-System
- **Squad-Einflug**: Feinde fliegen in Gruppen entlang Kurvenpfaden ein und formieren sich dann
- **Squad-Combo-Bonus**: Zerstöre eine ganze einfliegende Gruppe bevor sie sich formiert = Bonuspunkte!
- **Sturzangriffe**: Nach der Formierung können Feinde auf den Spieler zustürzen

#### Kampf
- **Begrenzte Munition**: Start mit 5 gleichzeitigen Schüssen. Plätze werden frei bei Treffer oder Bildschirmrand.
- **Waffen-Upgrades**: Sammle goldene Pickups (W):
  - Stufe 1: Einzelschuss
  - Stufe 2: Doppelschuss
  - Stufe 3: Dreifachschuss
- **Extra Munition**: Cyan Pickups (+) erhöhen Kapazität (max. 12)
- **Todesstrafe**: Bei Treffer sinkt Waffenstufe um 1 (Minimum Stufe 1)

#### Feinde
- **Basis (Rot)**: Standard-Feinde, 1 Treffer
- **Schnell (Grün)**: Flink und wendig, 1 Treffer
- **Zäh (Lila)**: Gepanzert, braucht 3 Treffer

#### Überleben
- **Leben**: 3 Leben insgesamt
- **Unverwundbarkeit**: Kurze Schutzzeit nach Treffer

---

## Project Structure

```
DeltaBlade/
├── pom.xml                              # Maven build configuration (FXGL 21.1)
├── src/main/java/deltablade/
│   ├── DeltaBladeApp.java               # Main FXGL GameApplication
│   ├── DeltaBladeFactory.java           # Entity factory (spawning)
│   ├── WaveManager.java                 # Squad spawning and combo system
│   ├── EntityType.java                  # Entity type enum
│   ├── GameVars.java                    # Game variables/constants
│   └── components/
│       ├── PlayerComponent.java         # Player movement, firing, i-frames
│       ├── EnemyComponent.java          # Enemy AI: entry, formation, diving
│       ├── BulletComponent.java         # Projectile movement
│       └── PickupComponent.java         # Collectible behavior
└── src/main/resources/                  # Resources (empty for now)
```

## Building

```bash
# Compile only
mvn compile

# Quiet compile (no output on success)
mvn -q compile

# Package as JAR
mvn package

# Clean build
mvn clean install
```

## Architecture

Built with FXGL's Entity-Component-System:

- **GameApplication**: Main game class extending FXGL's `GameApplication`
- **WaveManager**: Controls squad spawning, entry paths, and combo bonuses
- **EntityFactory**: Creates game entities (player, enemies, bullets, pickups)
- **Components**: Modular behavior attached to entities
  - `PlayerComponent`: Movement, firing, invulnerability frames
  - `EnemyComponent`: State machine (ENTERING → FORMATION → DIVING)
  - `BulletComponent`: Projectile physics
  - `PickupComponent`: Collectible animation and effects

### Enemy States
1. **ENTERING**: Flying along curved path toward formation slot
2. **FORMATION**: Hovering in place, may randomly dive
3. **DIVING**: Attacking toward player position

### Extensibility
The architecture is ready for future features:
- Shop system between waves
- Shield power-ups
- Boss enemies
- Scoop mechanic for bonus collection
- Sound effects and music

---

## Technologies

- **[FXGL](https://github.com/AlmasB/FXGL)** - JavaFX Game Library by Almas Baimagambetov
- **JavaFX 21** - Graphics and UI
- **Java 21** - Language

---

*DeltaBlade is an original game. All graphics are procedurally generated using JavaFX shapes.*
