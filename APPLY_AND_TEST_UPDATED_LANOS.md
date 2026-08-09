# DomeSurvival — updated Lanos models

Этот patch заменяет только ресурсы двух декоративных машин:

- `domesurvival:lanos_decorative`
- `domesurvival:lanos_abandoned`

Использованы новые модели из `lanos(2).zip` и `lanosold(2).zip`.

## Важно

- registry ID не менялись;
- Java-регистрация блоков и Creative-вкладка не менялись;
- геометрия, UV и текстуры взяты из новых моделей;
- конечный размер в мире сохраняется 3 × 7 × 2.5 блока;
- координаты внутри vanilla JSON нормализованы и восстановлены Forge root transform `scale = 3.5`, чтобы модель корректно запекалась;
- положение по Y не корректировалось сверх того, что задано в новой модели: её нижняя точка остаётся на поверхности.

## Установка

```powershell
cd C:\domesurvival
Expand-Archive `
  -Path "$env:USERPROFILE\Downloads\domesurvival_lanos_updated_v2_patch.zip" `
  -DestinationPath "C:\domesurvival" `
  -Force
.\gradlew.bat clean build
.\gradlew.bat runClient
```

## Проверка

```mcfunction
/give @s domesurvival:lanos_decorative
/give @s domesurvival:lanos_abandoned
```

Старые `.obj/.mtl` файлы, если они остались от экспериментальных патчей, больше не используются. Их наличие не влияет на рендер, потому что новые block model JSON на них не ссылаются.
