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
- **Explosions**: Pixel-art explosion animations play on bullet hits and enemy/player deaths.
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
- **Explosionen**: Pixel-Art-Explosionsanimationen bei Treffern und Schiffs-/Spielertod.
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
│   ├── EmbeddedTextures.java            # PNG decoder (8-bit RGBA only)
│   ├── components/
│   │   ├── PlayerComponent.java         # Player movement, firing, i-frames
│   │   ├── EnemyComponent.java          # Enemy AI: entry, formation, diving
│   │   ├── BulletComponent.java         # Projectile movement
│   │   ├── PickupComponent.java         # Collectible behavior
│   │   └── ExplosionComponent.java      # Animated sprite strip playback
│   └── tools/
│       └── ExplosionEditor.java         # Standalone explosion sprite editor
└── src/main/resources/                  # Resources
```

---

## 🎆 Explosion Editor / Explosions-Editor

A standalone tool for authoring procedural pixel-art explosions and exporting them as animated sprite sheets.

Explosions are already integrated in-game via `ExplosionComponent`. Use this editor to create custom explosion effects.

### 🇬🇧 Running the Editor (English)

```bash
mvn javafx:run -Pexplosions
```

### Features
- **Live 60fps preview** with nearest-neighbor rendering (pixel-art style)
- **Parameter controls**: Duration, frame count, frame size (32/48/64/96), particle count, size over lifetime, color gradient (core/mid/smoke), gravity, outward velocity, drag
- **Layer toggles**: Fire, Sparks, Smoke, Shockwave
- **Presets**: Hit (tiny sparks), Ship (medium fire+debris), Tough (fatter orange), Boss (big multi-layer), Plasma (cyan/magenta), Sparks
- **Playback controls**: Play, Pause, Restart, frame scrubbing, loop toggle
- **Seed control**: Reproducible explosions via seed field + Randomize button

### Exporting
1. **Sprite Sheet**: Click "Sprite Sheet exportieren..." → saves PNG + JSON sidecar
2. **Individual Frames**: Click "Einzelframes exportieren..." → saves numbered PNGs + JSON

Exported PNGs are **8-bit RGBA, non-interlaced** (color type 6), compatible with the game's `EmbeddedTextures.decodePng()`.

### In-Game Integration
Explosions are played via `ExplosionComponent`. To add custom sheets, embed them in `EmbeddedTextures` with keys like `explosion_custom.png` and spawn via:
```java
spawn("explosion", new SpawnData(x, y).put("variant", "custom"));
```

### 🇩🇪 Editor starten (Deutsch)

```bash
mvn javafx:run -Pexplosions
```

### Funktionen
- **Live 60fps Vorschau** mit Nearest-Neighbor Rendering (Pixel-Art Stil)
- **Parameter**: Dauer, Frame-Anzahl, Frame-Größe, Partikelzahl, Größe über Lebenszeit, Farbverlauf (Kern/Mitte/Rauch), Gravity, Outward-Velocity, Drag
- **Layer-Toggles**: Feuer, Funken, Rauch, Schockwelle
- **Presets**: Treffer, Schiff, Tough, Boss, Plasma, Funken
- **Playback**: Play, Pause, Restart, Frame-Scrubbing, Loop

### Exportieren
1. **Sprite Sheet**: "Sprite Sheet exportieren..." → PNG + JSON Metadaten
2. **Einzelframes**: "Einzelframes exportieren..." → nummerierte PNGs + JSON

Exportierte PNGs sind **8-bit RGBA, nicht-interlaced** — kompatibel mit `EmbeddedTextures.decodePng()`.

---

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

## Credits / Danksagungen

### Pixel Art Assets
The pixel art sprites used in this game are from the **Space Shooter Pack** by **RGS_Dev (Raphael Gonçalves)**.

- **Asset Pack**: [Space Shooter Pack - Pixel Art 2D by RGS_Dev](https://rgsdev.itch.io/space-shooter-pack-pixel-art-2d-by-rgsdev)
- **License**: Free to use in games. Do not resell or redistribute as an asset pack.

Die Pixel-Art-Sprites in diesem Spiel stammen aus dem **Space Shooter Pack** von **RGS_Dev (Raphael Gonçalves)**.

---

*DeltaBlade is an original game built with FXGL and pixel art assets by RGS_Dev.*
