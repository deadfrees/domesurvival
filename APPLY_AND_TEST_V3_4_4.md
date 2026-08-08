DomeSurvival V3.4.4 - Apply and test
====================================

1. Close Minecraft.
2. Checkout feature/surface-hazards.
3. Extract this ZIP directly into C:\domesurvival with overwrite enabled.
4. Run: .\gradlew.bat clean build
5. Run: .\gradlew.bat runClient

Tests
-----
Acid rain:
  /weather rain
Expected: visibly green atmosphere and moderate fog both inside and outside the dome. Crossing the
glass should not substantially change the green tint or fog distance.

Acid thunderstorm:
  /weather thunder
Expected: stronger/darker green atmosphere than normal acid rain, with equal visual treatment inside
and outside the dome. Terrain should still remain readable.

Sandstorm:
  /weather clear
  /dome weather sandstorm start
Expected: the orange/brown colour cast inside the dome should match the outside colour cast. The dome
still protects from storm damage. Visibility remains longer inside than outside by design.
