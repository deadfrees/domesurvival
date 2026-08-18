DomeSurvival Stage 4B V5.5 - Fixed NPC pose

Both ambient NPCs are hard-locked to their post.

maneogflow:
  position -508.950 62.0 -596.588
  yaw 0.0

iVan:
  position -534.938 62.0 -664.466
  yaw 0.0

Behavior:
- moving type is forced to Standing
- Return Home is disabled
- Stop On Interact is disabled
- target acquisition is cancelled
- exact position is restored every tick
- exact yaw is restored every tick
- interacting does not make either NPC turn toward the player
- V5.4 dialogue behavior is preserved

Textures are not changed.

Install:
  .\APPLY_STAGE4B_NPC_FIXED_POSE_V5_5.bat
  .\dev\RUN_DEV_FULL.bat

Then Reset Script / Apply once on both existing NPCs.
