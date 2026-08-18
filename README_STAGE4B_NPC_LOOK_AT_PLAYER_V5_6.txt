DomeSurvival Stage 4B V5.6 — NPCs look at player

Changes from V5.5:
- Position remains hard-locked.
- NPC movement remains disabled.
- Fixed yaw=0 was removed.
- Each NPC now turns toward the nearest player within 48 blocks.
- On right click, the NPC immediately faces the interacting player.
- Target acquisition remains cancelled, so looking at the player does not turn
  into combat behavior.

maneogflow keeps the rap/music dialogue.
iVan keeps the single phrase: Player's Club.

Install:
  .\APPLY_STAGE4B_NPC_LOOK_AT_PLAYER_V5_6.bat
  .\dev\RUN_DEV_FULL.bat

Then Reset Script / Apply once on both existing NPCs.
