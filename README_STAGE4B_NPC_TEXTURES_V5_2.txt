DomeSurvival Stage 4B V5.2 — native UV texture correction

This patch replaces the problematic 4096x4096 nearest-neighbor skins with
native 64x64 Minecraft Java Classic/Steve UV textures.

Reason:
4096x4096 was only an enlargement of a 64x64 atlas. It added no real detail and
made UV/overlay mistakes much more visible. Native 64x64 is the correct
no-stretch source resolution for these reconstructed skins.

Fixes:
- maneogflow: eye band is no longer covered by the cap overlay
- maneogflow: tactical vest is more compact and proportional
- maneogflow: cap/mask/gloves/pants/boots wrap correctly
- iVan: sunglasses occupy only the eye band
- iVan: arm and pant stripes are aligned around the correct faces
- iVan: jacket/zipper/chest marks are kept inside the correct torso UV
- all mandatory base faces are fully opaque
- unused overlay pixels remain transparent

Resource paths are unchanged:
domesurvival:textures/npc/expedition_soldier.png
domesurvival:textures/npc/dome_security_officer.png
