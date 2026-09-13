# Переработка материалов энергетических и жидкостных труб

## Запрос и сохранённые свойства

Пользователь попросил сохранить существующие каркасы шести труб и улучшить детальность, плавность оттенков и ровность текстур после изучения аналогов из других модов. Эта версия заменяет материалы трёх энергетических и трёх жидкостных труб. Геометрия всех core/arm/inventory, наборы граней, multipart, хитбоксы, item display и игровой Java-код сохранены.

## Что изучено

Просмотрены увеличенные оригинальные PNG из установленных JAR для Minecraft 1.20.1. Точные имена файлов, версии, размеры и SHA-256 записаны в `dev/pipe_materials_v2/reference_manifest.json`. Локальная доска сравнения: `dev/pipe_materials_v2/references/reference_texture_study.png`. Исходные ресурсы сторонних модов используются только для анализа и не входят в DOMESURVIVAL.

| Образец | Размер исходного PNG | Наблюдение при визуальном разборе | Применение в DOMESURVIVAL |
|---|---|---|---|
| Mekanism 10.4.16.80: Universal Cable / Mechanical Pipe | 32×32 / 16×16 | Сильная иерархия: тёмная середина, внешний пояс и короткая цветовая маркировка | Компактная служебная табличка и локальный цветовой акцент |
| Ender IO 6.2.18: energy / pressurized fluid conduit | 64×64 | Несколько вложенных светлых и тёмных рамок; направленное освещение узких продольных поверхностей | Тонкая фаска по краю, углублённая панель, протяжённый мягкий блик |
| Thermal Dynamics 11.0.1.23: energy duct / fluid duct / connector | 32×32 / 16×16 / 32×32 | Последовательное изменение яркости металла поперёк трубы; затемнение у соединителей | Плавный поперечный градиент и тёмные швы на концах |
| Immersive Engineering 10.2.0-183: fluid pipe | 32×32 | Сдержанная металлическая палитра, многотонные края, читаемые заклёпки и стыки | Ровная сатиновая сталь, небольшие винты со шлицами и тонкая фактура |

Это дизайнерские выводы по просмотренным изображениям, а не утверждения авторов модов. Для идентификации типов и контекста использованы официальные источники: [Universal Cable](https://wiki.aidancbrady.com/wiki/Universal_Cable), [Mechanical Pipe](https://wiki.aidancbrady.com/wiki/Mechanical_Pipe), [Thermal Dynamics](https://teamcofh.com/docs/1.12/thermal-dynamics/), [Ender IO](https://www.enderio.com/) и [репозиторий Immersive Engineering](https://github.com/BluSunrize/ImmersiveEngineering). Страница Thermal описывает историческую версию 1.12; визуальный анализ выше выполнен по фактическим PNG установленной версии 1.20.1.

## Принятые решения

Фактура состоит из крупных спокойных поверхностей, непрерывных переходов яркости и небольших механических деталей. Случайные контрастные квадраты заменены узкими швами и равномерной микрофактурой. Углубления темнее корпуса; светлая кромка тонкая и расположена последовательно. На центральной табличке размещены маленькая молния либо капля, одна/две/три метки уровня и четыре винта. Янтарный цвет обозначает энергию, синий — жидкостную магистраль. Знаки статические и не показывают наличие жидкости или питание.

Уровень 1 использует нейтральную сталь, уровень 2 — более холодный оттенок, уровень 3 — глубокий графит со стальным синим оттенком. На высоковольтной трубе добавлена узкая янтарная полоса вдоль направляющей. Различия форм жидкостных фланцев и хомутов полностью сохранены.

## Производство через Blender

`source_assets/blender/scripts/refine_pipe_materials_v2.py` читает зафиксированные исходные модели из `dev/pipe_materials_v2/baseline`, создаёт UV-острова под реальные размеры граней и формирует материалы через Blender Python API. Плотность — 16 texel на единицу модели (256 на полный блок); она позволяет изобразить фаски и градиенты без увеличения числа полигонов.

Пять атласов имеют размер 256×256, атлас high_pressure_fluid_pipe — 512×512 из-за размещения островов. Все шесть PNG непрозрачны. У каждого острова четыре пикселя повторённого края. Одинаковые размеры граней одного материала используют один остров. Прежние текстуры сохранены для воспроизводимого сравнения; production JSON теперь ссылаются на новые атласы. Ссылка particle сохранена, чтобы не менять отдельное поведение частиц.

Использован встроенный `image_gen` для создания исходной сатиновой фактуры `source_assets/blender/pipe_materials_v2/satin_steel_source.png`. Она вносит только слабую вариацию поверхности. Градиенты, рамки, винты, маркировка, UV и геометрия создаются детерминированно Blender-скриптом. Сторонние PNG не копируются в новые материалы.

Точный запрос image_gen:

> Create a production game material source texture, square seamless tile, perfectly flat orthographic albedo surface of fine satin brushed industrial steel. Neutral middle gray, exceptionally subtle horizontal micrograin, smooth low-contrast tonal variation, clean factory machined finish, very faint fine directional hairline scratches, evenly illuminated with absolutely NO directional cast shadows or specular hotspots. No borders, no panels, no screws, no holes, no symbols, no text, no objects, no perspective, no rust, no large stains, no cloudy mottling, no coarse pixel art. Entire image is one continuous homogeneous gray metal surface. This will be used as low amplitude material microdetail under precise procedurally placed panel bevels on Minecraft industrial energy conduits and fluid pipes. It must look refined and quiet, not noisy or hand painted.

Переходы и углубления встроены в цветовую текстуру. PBR, нормал-карты, свечение, прозрачное содержимое и дополнительные рендереры не вводятся. Шейдеры для нового оформления не нужны.

При проверке в игре откорректирована запись sRGB: Blender `Image.save()` для созданного байтового изображения сохраняет переданные значения каналов; повторное преобразование в linear делало материалы слишком тёмными. Финальные PNG получают значения sRGB напрямую. Валидатор независимо декодирует PNG и проверяет средний тон стали (0.310–0.403), поэтому этот дефект будет замечен при повторном экспорте.

## Исходники и воспроизведение

- `source_assets/blender/pipe_materials_v2/*.blend`: шесть редактируемых мастеров и две сцены сравнения, текстуры упакованы.
- `source_assets/blender/pipe_materials_v2/previews/01_refined_family.png`: вся линейка.
- `source_assets/blender/pipe_materials_v2/previews/02_before_after.png`: сравнение одинаковых каркасов до и после.
- `source_assets/blockbench/pipe_materials_v2/*.bbmodel`: шесть Java Block проектов с встроенными атласами и неизменными item display.
- `dev/pipe_materials_v2/material_manifest.json`: размеры атласов, координаты островов и бюджет граней.

```powershell
& dev/fluid_pipes/runtime_blender/blender.exe --background --factory-startup --python-exit-code 1 --python source_assets/blender/scripts/refine_pipe_materials_v2.py
& dev/fluid_pipes/runtime_blender/blender.exe --background --factory-startup --python-exit-code 1 --python dev/pipe_materials_v2/validate_materials.py
powershell.exe -NoProfile -ExecutionPolicy Bypass -File dev/pipe_materials_v2/run_material_review.ps1
```

Тестовый launcher использует отдельный мир `run/pipe-materials-v2`; временно перенесённые production-моды возвращаются в `finally`. Перед повторным доказательным прогоном следует сохранить предыдущий каталог `dev/pipe_materials_v2/runtime` под другим именем: текстовые протоколы добавляют строки при последующих запусках.

Старый `create_fluid_pipe_family.py` и исходники первой версии оставлены как исторический этап. Актуальный финальный проход материалов — `refine_pipe_materials_v2.py`; после повторного построения старой геометрии его нужно выполнить последним.
