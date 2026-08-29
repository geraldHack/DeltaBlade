# Sprite Attachment Issue

## Problem

The 8 PNG sprite attachments from Gerald's sprite sheets could not be extracted as files:

1. player.png — gold/blue fighter, 3 yellow thrusters
2. enemy_basic.png — red fighter, 2 red thrusters
3. enemy_fast.png — silver/purple sleek, 3 purple thrusters
4. enemy_tough.png — teal round ship, 2 green thrusters
5. pickup_weapon.png — grey blaster orange grip
6. bullet_player.png — cyan rocket arrow
7. pickup_ammo.png — green battery
8. bullet_enemy.png — magenta triangle arrow

## Investigation

The system indicated files were saved to `/workspace/sprites-out/`, but:
- Directory was empty when checked
- Read tool returned "File not found" for all 8 paths
- Searched all likely locations (`/cursor/`, `/opt/cursor/`, `/home/ubuntu/`, etc.)
- File attachments could not be passed to subagents (error: "Failed to read attachment")

## Per User Instructions

> "If you cannot get lossless bytes from attachments, STOP and say so in the PR body — do not invent ships."

The sprites cannot be replaced without access to the lossless PNG bytes.

## Resolution Options

1. **Commit sprites to repository**: Upload the 8 PNGs directly to `src/main/resources/assets/textures/`
2. **Provide Base64 strings**: Share the Base64-encoded PNG bytes directly in a follow-up message
3. **External URL**: Host the sprites at an accessible URL that can be fetched
