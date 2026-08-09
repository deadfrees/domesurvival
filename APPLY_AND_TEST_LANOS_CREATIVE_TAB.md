# DomeSurvival — Lanos model update + creative tab

## What this patch changes

- Replaces `lanos_decorative` with the new 7x3-block model from `lanos1.zip`.
- Replaces `lanos_abandoned` with the new 7x3-block model from `lanosold1.zip`.
- Keeps the same registry IDs, so already placed blocks/world saves remain compatible.
- Updates the selectable outline from the old ~1.5x2.9 footprint to the new 3x7 footprint.
- Keeps collision disabled: these vehicles are decorative and have zero tick/network cost.
- Adds a separate `Dome Survival` creative inventory tab.
- The tab automatically lists every registered item whose namespace is `domesurvival`.
  Therefore items added later by either developer appear automatically without editing the tab class.

## Install

Close Minecraft first.

```powershell
cd C:\domesurvival

Expand-Archive `
  -Path "$env:USERPROFILE\Downloads\domesurvival_lanos_models_and_creative_tab_update.zip" `
  -DestinationPath "C:\domesurvival" `
  -Force

.\gradlew.bat clean build
```

Expected result:

```text
BUILD SUCCESSFUL
```

Then:

```powershell
.\gradlew.bat runClient
```

## Test

Open Creative inventory. A separate tab named `Dome Survival` must be present.
It should include existing DomeSurvival blocks/items and both Lanos variants.

Commands:

```mcfunction
/give @s domesurvival:lanos_decorative
/give @s domesurvival:lanos_abandoned
```

Place both vehicles facing all four directions. The current models are 7 blocks long and 3 blocks wide.
They intentionally have no collision.

## Important for both developers

Do not manually add each future item to this creative tab. As long as its registry namespace is
`domesurvival`, the tab discovers it automatically from `ForgeRegistries.ITEMS`.
