DomeSurvival Stage 4B V5.1 - NPC texture hotfix

Fix:
V5 attempted to refresh CustomNPCs world script files during a texture-only
installation. On installations where that per-world script directory was not
present, Copy-Item could fail with DirectoryNotFoundException.

V5.1 does not touch world script directories at all.

It only installs and validates:
- maneogflow -> expedition_soldier.png
- iVan       -> dome_security_officer.png

Both texture resources are 4096x4096 PNG files.

Install over C:\domesurvival:
  Expand-Archive ...
  .\APPLY_STAGE4B_NPC_TEXTURES_HD_V5_1.bat
  .\dev\RUN_DEV_FULL.bat

If an already-created NPC still shows an old cached skin, open NPC Scripter and
use Reset Script / Apply once.
