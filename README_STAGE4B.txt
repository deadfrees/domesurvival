DOME SURVIVAL — STAGE 4B V2
Two ambient CustomNPCs

NPC 1
Name: iVAN
Title: Служба безопасности купола
Tag: domesurvival_ambient_security
Fixed position: -534.938 62 -664.466
Skin: domesurvival:textures/npc/dome_security_officer.png

NPC 2
Name: maneogflow
Title: Экспедиционный корпус
Tag: domesurvival_ambient_expedition
Fixed position: -508.950 62 -596.588
Skin: domesurvival:textures/npc/expedition_soldier.png

Positions were updated from in-game coordinates supplied before first launch.

CHANGES FROM THE FIRST DRAFT
- Uses the names/roles intended for the two NPCs.
- Adds persistent NPC tags for later automatic management.
- Cancels the normal interaction event so an ambient right-click does not open
  an unrelated default CustomNPCs interaction.
- Adds a short per-player click cooldown.
- Avoids immediately repeating the same ambient phrase to the same player.
- No tick() handler: zero continuous script polling cost.
- All unofficial-port API setters are guarded so one unsupported cosmetic/AI
  setter cannot break the whole NPC script.
- Installer backs up old world-script copies and verifies SHA-256 after copy.
- Installer automatically uses run\saves\WASTED_TEST when no path is supplied.

INSTALL
From C:\domesurvival:

  .\INSTALL_AMBIENT_NPC_SCRIPTS.bat

Or for another world:

  .\INSTALL_AMBIENT_NPC_SCRIPTS.bat "C:\path\to\world"

CUSTOMNPCS SETUP
1. Enter the world with CustomNPCs loaded.
2. Create two normal CustomNPCs with the NPC Wand.
3. For the first NPC link:
     ambient_security_officer.js
4. For the second NPC link:
     ambient_expedition_soldier.js
5. Save/Apply or reset each NPC script once.
6. init() moves each NPC to the intended fixed map position.

TEST
- Both NPCs stand on either side of Joseph.
- Names are visible.
- Right-click gives one private ambient phrase.
- Rapid spam-clicks are ignored.
- Repeated right-clicks should not immediately repeat the same phrase.
- Leaving and re-entering the world keeps both NPCs and their positions.
- Joseph's GUI/project logic remains unchanged.

NOTE
This V2 keeps the proven CustomNPCs-script approach used by the project and does
not add a tick handler. Automatic NPC creation can be moved into the Java
CustomNPCs integration later, after these exact two NPCs are visually approved.
