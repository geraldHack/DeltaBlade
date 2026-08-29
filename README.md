# DeltaBlade

**Original arcade shooter game** inspired by classic space shooters like Galaga. Built with Java 21 and [FXGL](https://github.com/AlmasB/FXGL) game engine.

![Java](https://img.shields.io/badge/Java-21-orange)
![FXGL](https://img.shields.io/badge/FXGL-17.3-blue)
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

Or alternatively:
```bash
mvn compile exec:java
```

### How to Play
- **Move**: Arrow keys (← →) or A/D
- **Fire**: Space or Ctrl
- **Restart**: R (after Game Over)

### Game Features
- **Limited Ammo**: You start with 5 simultaneous bullets. Ammo slots free up when bullets hit enemies or leave the screen.
- **Weapon Upgrades**: Collect golden pickups (W) to upgrade your weapon:
  - Grade 1: Single shot
  - Grade 2: Double shot
  - Grade 3: Triple shot
- **Extra Ammo**: Collect cyan pickups (+) to increase your ammo capacity (max 12).
- **Enemies**: Defeat all enemies in each wave to advance to the next level. Enemies get faster and tougher!
  - **Basic (Red)**: Standard enemies
  - **Fast (Green)**: Quick and agile
  - **Tough (Purple)**: Takes 3 hits to destroy
- **Lives**: You have 3 lives. Getting hit makes you briefly invulnerable.

---

## 🇩🇪 Deutsch

### Voraussetzungen
- **JDK 21** oder höher
- **Maven 3.8+**

### Starten
```bash
mvn javafx:run
```

Oder alternativ:
```bash
mvn compile exec:java
```

### Steuerung
- **Bewegen**: Pfeiltasten (← →) oder A/D
- **Schießen**: Leertaste oder Strg
- **Neustart**: R (nach Game Over)

### Spielmechaniken
- **Begrenzte Munition**: Du startest mit 5 gleichzeitigen Schüssen. Munitionsplätze werden frei, wenn Kugeln treffen oder den Bildschirm verlassen.
- **Waffen-Upgrades**: Sammle goldene Pickups (W) für bessere Waffen:
  - Stufe 1: Einzelschuss
  - Stufe 2: Doppelschuss
  - Stufe 3: Dreifachschuss
- **Extra Munition**: Sammle cyan Pickups (+) für mehr Munitionskapazität (max. 12).
- **Feinde**: Besiege alle Feinde in jeder Welle, um das nächste Level zu erreichen. Feinde werden schneller und stärker!
  - **Basis (Rot)**: Standard-Feinde
  - **Schnell (Grün)**: Flink und wendig
  - **Zäh (Lila)**: Braucht 3 Treffer
- **Leben**: Du hast 3 Leben. Nach einem Treffer bist du kurz unverwundbar.

---

## Project Structure

```
DeltaBlade/
├── pom.xml                              # Maven build configuration (FXGL 17.3)
├── src/main/java/deltablade/
│   ├── DeltaBladeApp.java               # Main FXGL GameApplication
│   ├── DeltaBladeFactory.java           # Entity factory (spawning)
│   ├── EntityType.java                  # Entity type enum
│   ├── GameVars.java                    # Game variables/constants
│   └── components/
│       ├── PlayerComponent.java         # Player logic
│       ├── EnemyComponent.java          # Enemy AI and behavior
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
- **EntityFactory**: Creates game entities (player, enemies, bullets, pickups)
- **Components**: Modular behavior attached to entities
  - `PlayerComponent`: Movement, firing, invulnerability
  - `EnemyComponent`: AI, hovering, diving, shooting
  - `BulletComponent`: Projectile physics
  - `PickupComponent`: Collectible animation and effects

### Extensibility
The architecture is ready for future features:
- Shop system between waves
- Shield power-ups
- Boss enemies
- More enemy types and formations
- Sound effects and music

---

## Technologies

- **[FXGL](https://github.com/AlmasB/FXGL)** - JavaFX Game Library by Almas Baimagambetov
- **JavaFX 21** - Graphics and UI
- **Java 21** - Language

---

*DeltaBlade is an original game. All graphics are procedurally generated using JavaFX shapes.*
