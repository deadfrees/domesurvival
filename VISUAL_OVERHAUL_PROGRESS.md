# DOMESURVIVAL — ход визуальной переработки

Объём текущего этапа: полный аудит, стиль и одна эталонная машина. Согласно заключительной части ТЗ массовое распространение начинается после совместной оценки образца. Ветка: `codex/visual-overhaul`.

## Сохранность исходного проекта

До работы в дереве было 188 изменённых/новых файлов. Они не откатывались и не включаются автоматически в коммиты визуального этапа. Локальный recovery diff: `dev/visual_overhaul/preexisting_changes.patch`; контрольный список хешей 2375 файлов `src`: `dev/visual_overhaul/preexisting_src_hashes.json`. Оригинальные модели и экран станции сохранены в `source_assets/baseline/filter_regeneration_station/`.

## Этапы

| Этап | Статус | Результат |
|---|---|---|
| 1. Аудит | Завершён статически | ASSET_AUDIT, SCALE_REPORT и два подробных аудита кода |
| 2. Стиль | Предложен на основе текущих материалов | VISUAL_STYLE_GUIDE, MACHINE_DESIGN_GUIDE, GUI_STYLE_GUIDE |
| 3. Эталон | Реализован | filter_regeneration_station: JSON, master, GUI |
| 4. Сравнение / проверка | В процессе | OLD сохранён, статическая проверка PASS, build PASS; выполняется Minecraft probe |
| 5. Остальные машины | Ожидает оценки образца | Приоритет: генератор, очиститель, электролизёр, наполнитель, пресс |
| 6. Трубы | Аудит выполнен, не изменены | 13 ID, сохранены все directional properties и режимы соединения |
| 7. Wearables | Аудит выполнен, не изменены | 8 предметов, Curios/vanilla armor |
| 8. GUI всего мода | Только экран эталона | Остальные Screen/menu не изменены |
| 9. Анимации | Только active-текстуры эталона | ANIMATION_PLAN описывает следующие этапы |
| 10. Финальная согласованность | Не начат | После принятия стиля и миграции |

## Итоги аудита A–J

A. **Машины:** 32 ID машин/устройств/связанных частей: copper_furnace; shaft_furnace и shaft_furnace_part; coke_oven и coke_oven_part; machine_stabilizer; coal_generator; water_purifier; oxygen_electrolyzer; oxygen_filler; bioincubator; sand_sieve; forming_press; filter_regeneration_station; energy_buffer, energy_buffer_titan, energy_buffer_adamantium, energy_buffer_creative; oxygen_complex_air_intake, oxygen_complex_filtration, oxygen_complex_compression, oxygen_complex_output; solar_panel_mk1/mk2/mk3; universal_tank; service_pass_through; airlock_gate; airlock_control_panel; copper_hopper, steel_hopper, desh_hopper. Legacy airlock_door/airlock_panel учтены отдельно. Подробные файлы/GUI/props — `dev/visual_overhaul/machine_code_audit.md`.

B. **Трубы:** oxygen_pipe, reinforced_oxygen_pipe, high_flow_oxygen_pipe; basic_energy_pipe, reinforced_energy_pipe, high_voltage_energy_pipe; basic_fluid_pipe, reinforced_fluid_pipe, high_pressure_fluid_pipe; copper_item_pipe, steel_item_pipe, desh_item_pipe, filtering_item_pipe.

C. **Wearables:** oxygen_mask; small/medium/large_oxygen_tank; surface_suit_helmet/chestplate/leggings/boots. Маска имеет отдельный Curios-слот, не является шлемом. Малый баллон намеренно крупнее среднего в текущей версии; изменение требует визуальной оценки, не автоматической «починки».

D. **GUI:** 21 собственный Screen: CoalGenerator, ShaftFurnace, CokeOven, WaterPurifier, OxygenElectrolyzer, OxygenFiller, Bioincubator, SandSieve, FormingPress, FilterRegeneration, EnergyBuffer, TitanEnergyBuffer, AdamantiumEnergyBuffer, CreativeEnergyBuffer, OxygenComplex, SolarPanel, UniversalTank, TieredHopper, ItemConnector, FilteringItemPipe, LanosTrunk. Дополнительно vanilla FurnaceScreen для copper_furnace, JEI, oxygen HUD, скорость FE, CustomNPCs, FancyMenu, FTB Quests, экраны загрузки/погоды. Полная таблица в `dev/visual_overhaul/ui_wearable_audit.md`.

E. **Анимации:** native BER шлюза и сита; атласные `.png.mcmeta` (20 metadata, не обязательно 20 видимых эффектов); частицы кислорода/пакетов/погоды; уровень содержимого бака; состояния GUI oxygen complex; vanilla движения экипировки и собственные hostile-model mixins. Intake/compression GUI содержит декоративное движение без корректного gating по работе; это отмечено как проблема. Полный план — ANIMATION_PLAN.

F. **Проблемы:** одинаковые orientable-корпуса с материалами генератора; неравномерные масштабы/плотность пикселей; неоднородные GUI; не все декоративные индикаторы отражают работу. Обнаружены 6 model-error записей из inverted bounds элемента 51 у ready-моделей двух металлургических печей и их наследников — это существующие дефекты, не изменения эталона. 223 model warning требуют контекстной оценки: многие относятся к намеренным multiblock/OBJ bounds. Missing parent/texture среди разрешённых активных JSON не обнаружено.

G. **Очередь:** сначала регенератор как образец; затем производственное семейство, порты/трубы, носимые предметы, общие GUI и анимации. Крупные OBJ и multiblock не упрощать без проверки фактического масштаба и gameplay shape.

H. **Эталон:** filter_regeneration_station — ограниченная сложность, существующий active/facing, отдельный GUI и подходящие оригинальные pixel-материалы.

I. **Рендер:** vanilla baked JSON + Forge multipart/OBJ/composite; 6 зарегистрированных BER; ModelPart/Curios для баллонов, собственный бинарный M40 mesh; vanilla armor; procedural GuiGraphics; GeckoLib не является используемым собственным renderer-путём мода.

J. **Без изменения механик:** геометрия/UV/display, палитра и реальные обозначения портов, наследование active-моделей, читаемость схемы GUI, сохранение item tooltips, ограничение декоративной анимации реальными синхронизированными состояниями.

## Воспроизводимость

1. `node dev/visual_overhaul/build_filter_prototype.mjs` — экспорт модели и portable Blockbench master; существующий baseline не перезаписывается.
2. `node dev/visual_overhaul/verify_prototype.mjs` — проверка всех хешей src, allowlist четырёх файлов, UV, геометрии, ссылок и восьми blockstate-вариантов.
3. Java 17, `gradlew.bat build --offline --console=plain` — обычная сборка.
4. `gradlew.bat -PdomeFullDev=true -PdomeVisualProbe=true -I dev/visual_overhaul/visual_test.init.gradle runClient --offline --console=plain` — отдельный тестовый клиент и fresh-world probe. Требуется существующий подготовленный FULL DEV cache. Инструмент находится вне production sourceSet и не должен попадать в release JAR.

Windows capture API дважды вернул `0x800706BE`; это не считается визуальным PASS. Minecraft probe сохраняет собственный framebuffer в `dev/visual_overhaul/runtime/` и пишет реальные assertions в `checks.txt`.
