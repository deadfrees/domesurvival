# DomeSurvival — Lanos root-transform hotfix

## Причина фиолетово-чёрных иконок
Предыдущий вариант физически умножал координаты каждого `elements[].from/to` внутри vanilla JSON модели.
После увеличения часть координат стала выходить за допустимый для vanilla JSON диапазон `[-16, 32]`.
Из-за этого модель не загружалась целиком, а Minecraft показывал missing-model texture.

## Исправление
- возвращена геометрия самых первых рабочих `lanos.zip` / `lanosold.zip`;
- геометрия остаётся в допустимых координатах vanilla JSON;
- масштаб x2.44541485 теперь применяется через Forge root `transform` на стадии bake;
- X/Y/Z масштабируются одинаково;
- низ исходной геометрии перенесён на Y=0 до масштабирования;
- модель центрирована по Z перед root-scale;
- конечная длина = 7 блоков;
- ширина ≈ 3.76 блока;
- высота ≈ 2.42 / 2.45 блока;
- item display scale компенсирован, чтобы иконка помещалась в Creative/инвентаре;
- registry ID не меняются.

## Установка
```powershell
cd C:\domesurvival
Expand-Archive `
  -Path "$env:USERPROFILE\Downloads\domesurvival_lanos_root_transform_hotfix.zip" `
  -DestinationPath "C:\domesurvival" `
  -Force

.\CLEANUP_OLD_LANOS_RENDER_FILES.ps1
.\gradlew.bat clean build
.\gradlew.bat runClient
```

## Проверка
```mcfunction
/give @s domesurvival:lanos_decorative
/give @s domesurvival:lanos_abandoned
```

Проверить:
1. обе иконки во вкладке Dome Survival не фиолетово-чёрные;
2. обе машины отображаются после установки;
3. длина около 7 блоков;
4. пропорции по высоте сохранены;
5. колёса/низ модели стоят на поверхности без левитации;
6. поворот north/east/south/west корректен.
