# Установка

Закройте runClient и распакуйте ZIP прямо в `C:\domesurvival` с заменой файлов.

```powershell
cd C:\domesurvival
.\gradlew.bat clean build
```

После `BUILD SUCCESSFUL`:

```powershell
.\gradlew.bat runClient
```

В игре:

```mcfunction
/give @s domesurvival:lanos_decorative
/give @s domesurvival:lanos_abandoned
```

Проверьте:
1. обе модели отображаются без missing texture;
2. ставятся в 4 направлениях;
3. игрок проходит сквозь них;
4. выделение охватывает приблизительно весь автомобиль;
5. при разрушении киркой выпадает соответствующий блок.
