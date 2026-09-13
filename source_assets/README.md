# DOMESURVIVAL source assets

`blockbench/machines/filter_regeneration_station.bbmodel` — редактируемый Java Block/Item master. PNG встроены в файл; внешние пути служат подсказкой редактору и не являются единственным источником текстур. Именованные cuboid соответствуют экспортированной статической модели. Скрытые грани имеют texture=null.

`baseline/filter_regeneration_station/` — оригинальные три JSON и GUI-класс из рабочего дерева до прототипа. Исходные PNG остаются в `src/main/resources/assets/domesurvival/textures/` без изменений.

Воспроизводимый конструктор: `node dev/visual_overhaul/build_filter_prototype.mjs`. Он создаёт master и три runtime JSON, не перезаписывает уже сохранённый baseline. Правки только в Blockbench не переживут повторный запуск конструктора: после ручного изменения либо переносите изменение в конструктор, либо экспортируйте JSON из master и не запускайте конструктор поверх ручной версии.

Active-вариант специально наследует основную модель и меняет `mesh`/`lamp`. Не дублируйте геометрию для анимации. Движения механических частей в этом образце нет: работают существующие animated atlas sprites только в active-модели. Blockstate с восемью facing/active-вариантами не изменён.

Материалы корпуса: `hopper/steel_body`, кромок: `hopper/steel_rim`; кассета/уплотнения/лампа используют `filter_regenerator_*`. Каждый кадр — 16×16; исходные полосы mesh_active и lamp_green_active имеют размер 16×128 и 16×96.

Импорт master в Blockbench отдельно не подтверждён UI-тестом. Валидность финальных JSON, реальная загрузка и отображение проверяются Minecraft probe; структура master соответствует java_block format 4.10 и сохраняет те же cuboid/UV/display.
