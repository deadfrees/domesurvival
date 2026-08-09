# DomeSurvival — Lanos OBJ hotfix

Причина фиолетово-чёрных моделей: новые Lanos имеют размер 7x3 блока и выходят далеко за допустимые координаты обычной vanilla JSON block model.

Исправление: геометрия перенесена в Forge WaveFront OBJ loader (`forge:obj`), который предназначен для нестандартных моделей. Registry ID блоков не меняются.

## Установка
1. Закрыть Minecraft.
2. Распаковать ZIP прямо в `C:\domesurvival` с заменой файлов.
3. `./gradlew.bat clean build`
4. `./gradlew.bat runClient`

## Проверка
```mcfunction
/give @s domesurvival:lanos_decorative
/give @s domesurvival:lanos_abandoned
```

Проверить иконки во вкладке Dome Survival, установку обеих машин и вращение по четырём сторонам.
