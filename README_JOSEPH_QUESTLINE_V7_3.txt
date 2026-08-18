Dome Survival — Joseph Questline V7.3 MULTIMOD + TEST SKIP
===========================================================

Requires local Joseph V7.2.

Cross-mod quest integration
---------------------------
Farmer's Delight:
- Cutting Board, Cooking Pot, Skillet
- Organic Compost, Rich Soil
- Tomato, Cabbage, Onion, Rice
- Stove/seeds in rewards

Brewin' And Chewin':
- Jerky during first controlled expedition
- Keg + Beer + Mead in the final emergency reserve
- Tankards / Mead in rewards

Mekanism:
- Basic / Advanced Control Circuits
- Infused / Reinforced Alloys
- Energy Tablet, Configurator, Steel Ingot

Immersive Engineering:
- Iron / Steel Components
- Electronic / Advanced Electronic Components
- Copper Wire, Steel Plates, Engineer's Hammer reward

Ender IO:
- Dark Steel Ingots in industrial stages

Exact names in GUI
------------------
The quest GUI no longer relies only on hardcoded labels. It creates an ItemStack
for each registry ID and reads getDisplayName(), so the requirement and reward
names follow the actual language currently loaded by the game. The hardcoded
name remains only as a fallback.

Test command
------------
/josephscript nextstage

Completes ONLY the currently active stage without consuming any quest items.
Then right-click Joseph once: pending reward is issued and the next stage opens.

The Stage 02 test skip fills its progress state but is NOT the command for testing
physical workshop construction. Test the workshop construction by handing Stage 02
resources normally if that mechanic is what you are validating.

Reset full logical quest progression:
/josephscript resetprogress

Install
-------
1. Extract over C:\domesurvival
2. Run:
   .\APPLY_JOSEPH_QUESTLINE_V7_3.bat
3. Launch:
   .\dev\RUN_DEV_FULL.bat
4. In WASTED_TEST:
   /josephscript apply

For rapid UI progression testing use /josephscript nextstage repeatedly, once per stage.
