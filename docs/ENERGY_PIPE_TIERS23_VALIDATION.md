# Energy pipe Tier 2/3 — проверка переноса оформления

Дата: 2026-09-13. Продолжение после принятого Tier 1. Производственные изменения ограничены 10 JSON и 12 новыми PNG для `reinforced_energy_pipe` и `high_voltage_energy_pipe`. Форма остаётся как в игре; Tier 1, Java и другие ассеты не изменены относительно снимка перед этим этапом.

## Статические проверки

[tiers23_static_checks.json](../dev/energy_pipes/tiers23_static_checks.json): PASS моделей и границ изменений. Проверяются все три уровня: core/arm geometry, UV, текстуры, multipart selectors/rotations, семь item contexts и 88 quads предмета. PNG 16×16, исходные JSON Tier 2/3 сохранены отдельно для сравнения. Встроенные текстуры и явные отсутствующие грани сохранены в Blockbench; UI-импорт Blockbench не проверялся.

[game_shape_verification.json](../dev/energy_pipes/game_shape_verification.json): PASS после сохранения текстурированных masters через `bpy`; проверены 36 исходных элементов трёх Blender-файлов, координаты и направления граней.

## Стенд Minecraft

Изолированный fresh superflat world в `run/energy-pipe-visual`, полный dev-набор Forge 1.20.1. Новый тестовый `EnergyPipeFamilyProbe` подключается только при `-PenergyPipeFamily=true` через init script. Старый Tier 1 probe при этом не активируется.

Стенд содержит 100 труб Tier 2 и 100 Tier 3, прямые линии трёх уровней, отдельные узлы/отводы/машины. Для каждого старшего Tier построена независимая FE-сеть с двумя настоящими Adamantium Energy Buffer, чтобы предел машины 4096 FE/t позволял проверить предел трубы 1024 или 4096 FE/t. Начальная энергия каждой сети — 2 000 000 FE.

Проверки включают фактическую установку и разрушение BlockItem, одиночный узел, вертикаль, угол, T, cross, шесть направлений, подключение к машине; все 54 упорядоченные пары Tier/направления; все 192 baked states. В FE-сетях проверяются точная передача за тик и сохранение суммы энергии, DISABLED/AUTO, разрыв/восстановление и временное включение basic-трубы как ограничения 256 FE/t.

Обзор предметов включает GUI, ground, fixed, first/third person для обеих рук. Камера третьего лица находится в свободном месте, чтобы соседние трубы не придвигали её внутрь модели игрока.

Итог [игрового протокола](../dev/energy_pipes/runtime_tiers23/checks.txt): **RESULT PASS, failures=0**. Пройдены все перечисленные проверки. Игровые снимки просмотрены: форма стыков сохранена, маркировка различима, предметы отображаются у нужной руки.

- [Три уровня в линиях](../dev/energy_pipes/runtime_tiers23/05_family_lines.png).
- [Узлы Tier 2](../dev/energy_pipes/runtime_tiers23/02_tier2_junctions.png), [узлы Tier 3](../dev/energy_pipes/runtime_tiers23/03_tier3_junctions.png), [машины](../dev/energy_pipes/runtime_tiers23/04_machine_connection.png).
- [Инвентарь](../dev/energy_pipes/runtime_tiers23/07_inventory.png), [земля и рамки](../dev/energy_pipes/runtime_tiers23/06_ground_and_frames.png).
- Tier 2: [first right](../dev/energy_pipes/runtime_tiers23/tier2_first_right.png), [first left](../dev/energy_pipes/runtime_tiers23/tier2_first_left.png), [third right](../dev/energy_pipes/runtime_tiers23/tier2_third_right.png), [third left](../dev/energy_pipes/runtime_tiers23/tier2_third_left.png).
- Tier 3: [first right](../dev/energy_pipes/runtime_tiers23/tier3_first_right.png), [first left](../dev/energy_pipes/runtime_tiers23/tier3_first_left.png), [third right](../dev/energy_pipes/runtime_tiers23/tier3_third_right.png), [third left](../dev/energy_pipes/runtime_tiers23/tier3_third_left.png).

## Производительность и логи

Одинаковые мир и камера, 1280×800, FOV 60, render distance 6, VSync отключён. Before pack заменяет только 10 исходных JSON Tier 2/3. После каждого resource reload — 120 тиков прогрева и 200 тиков измерения. Дополнительные узлы и машины присутствуют в обоих вариантах.

| Вариант | Выборка кадров | Среднее, мс | Медиана, мс | P95, мс |
|---|---:|---:|---:|---:|
| Исходные Tier 2/3 | 6 610 | 1.4954 | 1.1843 | 1.5528 |
| Новое оформление | 6 346 | 1.5564 | 1.2245 | 1.6253 |

Среднее +0.061 мс (+4.1%). Это одна последовательная пара замеров интервалов `RenderTickEvent.END` в dev-клиенте, без GPU profiler; фоновые процессы, прогрев и движение облаков не изолированы. Результат не является универсальной гарантией FPS. Число граней труб в мире осталось прежним: 17 080 исходных quads у 200 труб в десяти линиях с открытыми концами, без дополнительных примеров. Новых tick/render hooks в production нет.

[CSV](../dev/energy_pipes/runtime_tiers23/frame_times.csv), [before](../dev/energy_pipes/runtime_tiers23/08_stress_before.png), [after](../dev/energy_pipes/runtime_tiers23/09_stress_after.png).

В журнале нет ошибок/предупреждений моделей или текстур энерготруб. Общий dev-набор продолжает выдавать сторонние сообщения Farmer's Delight/Brewin' and Chewin', EnderIO, CustomNPCs, Immersive Engineering и MorePlayerModels, аналогичные прошлому этапу. Они не исправлялись в рамках energy pipes; общий latest.log не объявляется полностью чистым.

## Итоговая сборка

`gradlew -PdomeFullDev=true build --offline --console=plain`: **BUILD SUCCESSFUL**, 43 секунды. Проверка JAR: **PASS** — все 33 игровых ресурса трёх Tier побайтно совпадают с исходниками по SHA-256, оба тестовых probe отсутствуют. [Отчёт и хеш](../dev/energy_pipes/family_jar_checks.json). JAR: `build/libs/domesurvival-0.2.0.jar`.

JAR собирается из текущего рабочего дерева и содержит его прочие существующие изменения. Коммиты этого этапа ограничены ассетами энергетических труб, их исходниками, инструментами и доказательствами проверки.

## Воспроизведение

```powershell
& 'C:\Users\deadfrees\AppData\Local\Microsoft\WindowsApps\blender-launcher.exe' --background --factory-startup --python-exit-code 1 --python 'C:\domesurvival\source_assets\blender\scripts\style_energy_pipe_tiers23.py'
node dev/energy_pipes/verify_tiers23_style.mjs
$env:JAVA_HOME='C:\Program Files\Java\jdk-17.0.12'
$env:PATH=$env:JAVA_HOME+'\bin;'+$env:PATH
.\gradlew.bat -PdomeFullDev=true -PenergyPipeFamily=true -I dev/energy_pipes/energy_pipe_test.init.gradle runClient --offline --console=plain
.\gradlew.bat -PdomeFullDev=true build --offline --console=plain
powershell.exe -NoProfile -ExecutionPolicy Bypass -File dev/energy_pipes/verify_tier1_jar.ps1 -AllTiers
```

Для повторного независимого evidence меняется каталог OUT, поскольку протоколы дописываются. Обычная сборка выполняется без init script. Скрипт `style_energy_pipe_tier1.py` сохраняет возможность самостоятельного запуска, но импорт общих функций теперь не запускает его автоматически.
