# Bioincubator Progression V1.7

Trigger: `GENETIC_SAMPLES_RECOVERED`.

Machine inputs:
- FE
- purified water
- viable cryocapsule
- universal `domesurvival:nutrient_mix`; the amount is species-specific

Incubation:
- Chicken: 1 capsule + 2 nutrient mix + 800 mB + 40 FE/t × 1000 ticks
- Sheep: 1 capsule + 3 nutrient mix + 1200 mB + 50 FE/t × 1200 ticks
- Cow: 1 capsule + 3 nutrient mix + 1600 mB + 60 FE/t × 1400 ticks
- Damaged pig capsule remains locked.

Output is a baby vanilla animal in front of the machine. The guaranteed archive cache contains only
one capsule per stable species. The distribution ledger reserves these first copies, distributes the
remaining species catalogue before allowing any pairs, then requires a second specimen to be at least
500 horizontal blocks from the first.

Repair mode:
- 1 damaged module
- 1 biological-module repair kit
- 1 biogel
- 1 nutrient mix
- 1000 mB purified water
- 80 FE/t × 1800 ticks

The repaired result is a viable data-driven module of the same species.

FTB continuation:
1. Биоинкубатор
2. Первое дыхание

First successful incubation sets `FAUNA_RESTORATION_STARTED`, grants the technical advancement to all
online players, and grants it on later login as a dedicated-server/chunk-loader catch-up path.

## V1.7.1 installer/runtime corrections
- robust Chapter 7 insertion before the real top-level `quest_links:` field;
- preserves non-empty quest links and later chapter fields;
- uses DomeSurvival `MachineEnergyStorage.setEnergyStoredInternal/getEnergyStored` for NBT persistence.
