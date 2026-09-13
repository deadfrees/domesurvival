# Energy pipe Tier 1 — проверка оформления

Дата: 2026-09-13. Scope: `domesurvival:basic_energy_pipe`, сохранение существующей формы. Tier 2/3 проверяются как соседи; их ресурсы не перерабатываются.

## Модель и исходники

- Blender 5.2.1 LTS запускается из терминала через `blender-launcher.exe`; исходники создаются и проверяются через `bpy`.
- `verify_energy_pipe_game_shape.py`: PASS для трёх masters, 36 исходных элементов, координат вершин и направлений граней.
- `verify_tier1_style.mjs`: геометрия core/arm, multipart selectors/rotations, UV, текстуры 16×16, семь item contexts, 17 элементов / 88 quads предмета.
- JSON + multipart / baked models; никаких новых BER, сетевых пакетов, tick hooks или gameplay-классов в production.
- Редактируемый Blockbench-файл содержит встроенные текстуры и `texture=null` на отсутствующих гранях. Его импорт через UI не проверялся.

## Minecraft

Forge 1.20.1, полный локальный dev-набор модов, отдельный fresh superflat world в `run/energy-pipe-visual`. Тестовый Java source set подключён только init script и не входит в production JAR.

[Протокол полного прогона](../dev/energy_pipes/runtime_tier1_release/checks.txt): **RESULT PASS, failures=0**.

Проверены фактические BlockItem placement/player break, изолированный узел, вертикаль, угол, T, cross, шесть направлений, смешанные Tier, подключение к настоящим Energy Buffer. В реальной FE-сети приёмник получает до 256 FE за тик, суммарная энергия сохраняется. DISABLED останавливает поток, AUTO возобновляет; разрыв останавливает, установка трубы восстанавливает передачу. Все 64 baked states Tier 1 содержат ожидаемое число quads и разрешённые sprites без missingno.

Игровые снимки:

- [Линии крупнее](../dev/energy_pipes/runtime_tier1_release/01_tier1_close.png), [узлы и вертикаль](../dev/energy_pipes/runtime_tier1_release/02_junctions.png).
- [Подключение к машинам](../dev/energy_pipes/runtime_tier1_release/03_machine_connection.png).
- [Правая рука](../dev/energy_pipes/runtime_tier1_release/04_first_person.png), [левая рука](../dev/energy_pipes/runtime_tier1_release/10_first_person_left.png), [creative inventory](../dev/energy_pipes/runtime_tier1_release/05_inventory.png).
- [Предмет на земле](../dev/energy_pipes/runtime_tier1_release/07_ground_and_frame.png).

Первые попытки со снимками меню паузы исключены: стенд получил `pauseOnLostFocus=false`. Проверка inventory учитывает автоматическую замену экрана на creative inventory. Снимки третьего лица из полного прогона непригодны из-за близкой камеры; отдельный обзор предмета использует свободное место. После полного прогона угол `fixed` изменён с 180° на 45°, чтобы в рамке была видна длина секции; эта правка не затрагивает мировой рендер или FE.

Отдельный предметный прогон также завершён **PASS, failures=0**: [протокол](../dev/energy_pipes/runtime_tier1_item_review/checks.txt), [third person — правая рука](../dev/energy_pipes/runtime_tier1_item_review/01_third_person_right.png), [левая рука](../dev/energy_pipes/runtime_tier1_item_review/02_third_person_left.png), [окончательный ракурс в рамке](../dev/energy_pipes/runtime_tier1_item_review/03_fixed_frame.png). Эти три снимка просмотрены: предмет находится у соответствующей руки и читается в рамке как секция трубы.

## Производительность

Пять параллельных линий по 20 basic pipes; рядом находятся дополнительные узлы и тестовые машины. Одинаковые мир, камера и разрешение 1280×800, FOV 60, render distance 6, VSync отключён. Before pack восстанавливает ровно пять исходных JSON; после перезагрузки ресурсов — 120 тиков прогрева и 200 тиков измерения. Счётчик использует интервалы `RenderTickEvent.END`, поэтому это оценка времени кадров клиентского цикла, а не GPU profiler.

| Вариант | Выборка | Среднее, мс | Медиана, мс | P95, мс |
|---|---:|---:|---:|---:|
| Исходный | 6 973 | 1.4244 | 1.1259 | 1.4043 |
| Новый Tier 1 | 6 861 | 1.4474 | 1.1431 | 1.4463 |

Разница среднего +0.023 мс (+1.6%). Это один последовательный before/after замер в dev-клиенте; он не доказывает универсального отсутствия регрессии. Геометрический бюджет мировых труб одинаков. В стенде у десяти концов по одному отводу: 8 540 исходных quads на 100 труб, без учёта дополнительных примеров. У 100 секций с двумя отводами было бы 8 800.

[CSV](../dev/energy_pipes/runtime_tier1_release/frame_times.csv), [before](../dev/energy_pipes/runtime_tier1_release/08_stress_before.png), [after](../dev/energy_pipes/runtime_tier1_release/09_stress_after.png).

## Логи и границы проверки

Финальная команда `gradlew -PdomeFullDev=true build --offline --console=plain` завершилась **BUILD SUCCESSFUL** после изменения fixed transform. `verify_tier1_jar.ps1` сверяет содержимое 11 ресурсов труб с исходниками по SHA-256 и отсутствие тестового probe в production JAR; [результат и хеш JAR](../dev/energy_pipes/tier1_jar_checks.json).

В полном прогоне нет предупреждений/ошибок модели или текстур энерготруб. В общем dev-наборе остаются сообщения сторонних модов: отсутствующие модели pie у Farmer's Delight/Brewin' and Chewin', текстуры некоторых EnderIO conduits и CustomNPCs, предупреждения shader uniforms MorePlayerModels. Это существующие внешние visual issues; они не исправлялись в рамках труб. Общий latest.log не объявляется полностью чистым.

В процессе проверки появились изменения рабочего дерева вне труб. Они отражены отдельно в `tier1_static_checks.json`; сравнение всего дерева с начальным снимком больше не имеет PASS. Production JAR собирается из текущего рабочего дерева, поэтому содержит также его прочие существующие изменения. Коммит оформления ограничен явно перечисленными файлами энергетических труб.

## Воспроизведение

Сначала генератор greybox, затем `style_energy_pipe_tier1.py` через Blender CLI, затем `node dev/energy_pipes/verify_tier1_style.mjs`. Повторный greybox без стадии style возвращает серые masters. Static verifier также экспортирует Blockbench.

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17.0.12'
$env:PATH=$env:JAVA_HOME+'\bin;'+$env:PATH
.\gradlew.bat -PdomeFullDev=true -I dev/energy_pipes/energy_pipe_test.init.gradle runClient --offline --console=plain
.\gradlew.bat -PdomeFullDev=true -PenergyPipeHandReview=true -I dev/energy_pipes/energy_pipe_test.init.gradle runClient --offline --console=plain
.\gradlew.bat -PdomeFullDev=true build --offline --console=plain
```

Стенд создаёт новый тестовый мир при каждом запуске. Протоколы дописываются; для отдельного нового набора evidence следует предварительно выбрать новый каталог OUT. Обычная сборка выполняется без init script.
