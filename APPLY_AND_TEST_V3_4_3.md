# Apply and test V3.4.3

1. Close Minecraft.
2. Switch to the surface hazards branch.
3. Extract this ZIP directly into `C:\domesurvival` with overwrite enabled.
4. Run `./gradlew.bat clean build`.
5. Run `./gradlew.bat runClient`.

## Test acid rain
```
/weather rain
```
Expected:
- green atmosphere is clearly visible;
- visibility remains close to clear weather;
- only a light toxic fog is present;
- effect is visible both inside and outside the dome.

## Test acid thunderstorm
```
/weather thunder
```
Expected:
- darker/stronger green atmosphere than rain;
- long visibility remains;
- fog is noticeable but not an opaque wall.

## Test sandstorm
```
/weather clear
/dome weather sandstorm start
```
Expected:
- orange/brown storm haze is obvious, not only individual particles;
- it is visible through the dome;
- outside visibility is heavily reduced;
- inside visibility is reduced less severely.

Stop:
```
/dome weather sandstorm stop
```

## Test lethal clear day
```
/weather clear
/time set noon
```
Expected:
- much longer daytime visibility than V3.4.2;
- a mild hot-air haze still remains;
- inside the dome, a subtle warm cast is visible;
- outside, the heat vignette remains active.
