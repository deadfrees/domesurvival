# DOMESURVIVAL — аудит визуальных ассетов

Снимок до переработки: 2026-09-12T09:39:06.142Z. Сканирование исходников и ресурсов, без изменения Java, PNG, JSON-ассетов и игровых миров.

## Границы и проверяемость

Активные корни Gradle: `src/main/resources` и `src/generated/resources` (`build.gradle`: стандартный source set + `sourceSets.main.resources.srcDir`). Каталог `main/resources` вне `src` не подключён к сборке: его файлы не следует редактировать вместо активных. `build` — результаты сборки, `run` — runtime-копии, `resourcepacks` — подключаемые отдельно пакеты; наличие файла не доказывает, что пакет выбран игроком. Архивы и JAR зарегистрированы как контейнеры, сторонние игровые ассеты внутри них не распаковывались. Миры/логи и кеши .git/.gradle/node_modules исключены.

Полный машиночитаемый каталог каждого найденного файла, SHA-256 исходников, регистрации с номерами строк и связи находятся в [asset_inventory.json](dev/visual_overhaul/asset_inventory.json). Повторный запуск `node dev/visual_overhaul/audit_assets.mjs` перезапишет снимок; для сохранения baseline запускайте в отдельной копии.

Registry ID получены из активного Java: DeferredRegister, проверенные вспомогательные регистрации материалов и RegistryObject/RegisterEvent. Блок и его BlockItem объединены в одну строку; entity/painting/fluid/particle остаются отдельными строками. Наличие lang или JSON само по себе не считается регистрацией. Связи GUI и renderer получены из client registration и проверенных общих BE; «не обнаружен» означает предел статического анализа, а не доказательство отсутствия. Приоритет — техническая очередность, не результат визуального одобрения в игре.

## Объём

| Метрика | Количество |
| --- | --- |
| files | 7195 |
| active | 1394 |
| registered_objects | 205 |
| active_models | 598 |
| models_with_errors | 6 |
| models_with_warnings | 223 |

| Статус файла | Количество |
| --- | --- |
| auxiliary_source | 280 |
| generated_build_copy | 1397 |
| inactive_duplicate_source | 128 |
| inactive_patch_or_backup | 2472 |
| modpack_backup | 1 |
| modpack_distribution | 69 |
| packaged_source | 1394 |
| runtime_copy_or_pack | 1454 |

| Активный формат | Количество |
| --- | --- |
| blockstate_json | 75 |
| model_json | 598 |
| obj_material | 8 |
| obj_mesh | 8 |
| particle_json | 4 |
| texture_metadata | 20 |
| texture_png | 681 |

## Все зарегистрированные визуальные объекты

Короткие пути моделей/текстур в таблице — resource locations; полные пути и их размеры перечислены в JSON. Для наследуемых vanilla-текстур размеры прочитаны из локального client.jar. `builtin/generated` создаёт объём спрайта в item baker и не является отсутствующей моделью.

| Название | Registry ID | Тип | Текущая модель | Текущая текстура | Размер PNG | Renderer | Анимация | GUI | Переработка | Приоритет |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 01_trio_friends | domesurvival:01_trio_friends | painting | нет JSON / код | domesurvival:painting/01_trio_friends | 128×128 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| 02_recording_in_yard | domesurvival:02_recording_in_yard | painting | нет JSON / код | domesurvival:painting/02_recording_in_yard | 256×320 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| 03_airsoft_team | domesurvival:03_airsoft_team | painting | нет JSON / код | domesurvival:painting/03_airsoft_team | 128×128 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| 04_fishing_closeup | domesurvival:04_fishing_closeup | painting | нет JSON / код | domesurvival:painting/04_fishing_closeup | 256×320 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| 05_calm_lake_fishing | domesurvival:05_calm_lake_fishing | painting | нет JSON / код | domesurvival:painting/05_calm_lake_fishing | 192×256 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| 06_relaxing_on_grass | domesurvival:06_relaxing_on_grass | painting | нет JSON / код | domesurvival:painting/06_relaxing_on_grass | 192×256 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| 07_pink_hat_portrait | domesurvival:07_pink_hat_portrait | painting | нет JSON / код | domesurvival:painting/07_pink_hat_portrait | 64×64 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| 08_watermelon_park | domesurvival:08_watermelon_park | painting | нет JSON / код | domesurvival:painting/08_watermelon_park | 256×320 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| 09_white_hat_portrait | domesurvival:09_white_hat_portrait | painting | нет JSON / код | domesurvival:painting/09_white_hat_portrait | 64×64 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| 10_flexing_portrait | domesurvival:10_flexing_portrait | painting | нет JSON / код | domesurvival:painting/10_flexing_portrait | 64×64 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| 11_prize_shop_winners | domesurvival:11_prize_shop_winners | painting | нет JSON / код | domesurvival:painting/11_prize_shop_winners | 256×320 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| 12_kitchen_character | domesurvival:12_kitchen_character | painting | нет JSON / код | domesurvival:painting/12_kitchen_character | 64×64 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| 13_music_studio_friends | domesurvival:13_music_studio_friends | painting | нет JSON / код | domesurvival:painting/13_music_studio_friends | 128×128 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| 14_mirror_group_selfie | domesurvival:14_mirror_group_selfie | painting | нет JSON / код | domesurvival:painting/14_mirror_group_selfie | 256×320 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| 15_voxel_company_bright_light | domesurvival:15_voxel_company_bright_light | painting | нет JSON / код | domesurvival:painting/15_voxel_company_bright_light | 128×128 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| 16_tricolor_portrait | domesurvival:16_tricolor_portrait | painting | нет JSON / код | domesurvival:painting/16_tricolor_portrait | 64×64 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| 17_bee_hero_amber_hive | domesurvival:17_bee_hero_amber_hive | painting | нет JSON / код | domesurvival:painting/17_bee_hero_amber_hive | 64×64 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| 18_wedding_kiss_tree | domesurvival:18_wedding_kiss_tree | painting | нет JSON / код | domesurvival:painting/18_wedding_kiss_tree | 128×128 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| 19_night_selfie_friendship | domesurvival:19_night_selfie_friendship | painting | нет JSON / код | domesurvival:painting/19_night_selfie_friendship | 64×64 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| 20_brown_suit_limo | domesurvival:20_brown_suit_limo | painting | нет JSON / код | domesurvival:painting/20_brown_suit_limo | 256×320 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| 21_bw_party_point | domesurvival:21_bw_party_point | painting | нет JSON / код | domesurvival:painting/21_bw_party_point | 256×320 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| 22_party_toast_indoor | domesurvival:22_party_toast_indoor | painting | нет JSON / код | domesurvival:painting/22_party_toast_indoor | 128×128 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| acid_rain_streak | domesurvival:acid_rain_streak | particle | нет JSON / код | domesurvival:particle/acid_rain_streak | 16×16 | AcidRainParticle.Provider | tick/move спрайта в AcidRainParticle | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| airlock_binding_key | domesurvival:airlock_binding_key | item | domesurvival:item/airlock_binding_key | domesurvival:item/airlock_binding_key | 32×32 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| airlock_control_panel | domesurvival:airlock_control_panel | block+item | domesurvival:block/airlock_control_panel<br>domesurvival:block/airlock_control_panel_active<br>domesurvival:item/airlock_control_panel | domesurvival:block/airlock_control_panel | 64×256 | vanilla baked block model | PNG mcmeta; blockstate indicator/open state | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| airlock_door | domesurvival:airlock_door | block | domesurvival:block/airlock_door | minecraft:block/iron_block | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| airlock_gate | domesurvival:airlock_gate | block+item | domesurvival:block/airlock_gate_dynamic_empty<br>domesurvival:block/airlock_gate_p00<br>domesurvival:block/airlock_gate_p01<br>domesurvival:block/airlock_gate_p02<br>domesurvival:block/airlock_gate_p03<br>domesurvival:block/airlock_gate_p04<br>domesurvival:block/airlock_gate_p10<br>domesurvival:block/airlock_gate_p11<br>domesurvival:block/airlock_gate_p12<br>domesurvival:block/airlock_gate_p13<br>domesurvival:block/airlock_gate_p14<br>domesurvival:block/airlock_gate_p20<br>domesurvival:block/airlock_gate_p21<br>domesurvival:block/airlock_gate_p22<br>domesurvival:block/airlock_gate_p23<br>domesurvival:block/airlock_gate_p24<br>domesurvival:block/airlock_gate_p30<br>domesurvival:block/airlock_gate_p31<br>domesurvival:block/airlock_gate_p32<br>domesurvival:block/airlock_gate_p33<br>domesurvival:block/airlock_gate_p34<br>domesurvival:block/airlock_gate_p40<br>domesurvival:block/airlock_gate_p41<br>domesurvival:block/airlock_gate_p42<br>domesurvival:block/airlock_gate_p43<br>domesurvival:block/airlock_gate_p44<br>domesurvival:block/airlock_gate_unformed<br>domesurvival:item/airlock_gate | domesurvival:block/airlock_gate/p00<br>domesurvival:block/airlock_gate/p00_back<br>domesurvival:block/airlock_gate/p01<br>domesurvival:block/airlock_gate/p01_back<br>domesurvival:block/airlock_gate/p02<br>domesurvival:block/airlock_gate/p02_back<br>domesurvival:block/airlock_gate/p03<br>domesurvival:block/airlock_gate/p03_back<br>domesurvival:block/airlock_gate/p04<br>domesurvival:block/airlock_gate/p04_back<br>domesurvival:block/airlock_gate/p10<br>domesurvival:block/airlock_gate/p10_back<br>domesurvival:block/airlock_gate/p11<br>domesurvival:block/airlock_gate/p11_back<br>domesurvival:block/airlock_gate/p12<br>domesurvival:block/airlock_gate/p12_back<br>domesurvival:block/airlock_gate/p13<br>domesurvival:block/airlock_gate/p13_back<br>domesurvival:block/airlock_gate/p14<br>domesurvival:block/airlock_gate/p14_back<br>domesurvival:block/airlock_gate/p20<br>domesurvival:block/airlock_gate/p20_back<br>domesurvival:block/airlock_gate/p21<br>domesurvival:block/airlock_gate/p21_back<br>domesurvival:block/airlock_gate/p22<br>domesurvival:block/airlock_gate/p22_back<br>domesurvival:block/airlock_gate/p23<br>domesurvival:block/airlock_gate/p23_back<br>domesurvival:block/airlock_gate/p24<br>domesurvival:block/airlock_gate/p24_back<br>domesurvival:block/airlock_gate/p30<br>domesurvival:block/airlock_gate/p30_back<br>domesurvival:block/airlock_gate/p31<br>domesurvival:block/airlock_gate/p31_back<br>domesurvival:block/airlock_gate/p32<br>domesurvival:block/airlock_gate/p32_back<br>domesurvival:block/airlock_gate/p33<br>domesurvival:block/airlock_gate/p33_back<br>domesurvival:block/airlock_gate/p34<br>domesurvival:block/airlock_gate/p34_back<br>domesurvival:block/airlock_gate/p40<br>domesurvival:block/airlock_gate/p40_back<br>domesurvival:block/airlock_gate/p41<br>domesurvival:block/airlock_gate/p41_back<br>domesurvival:block/airlock_gate/p42<br>domesurvival:block/airlock_gate/p42_back<br>domesurvival:block/airlock_gate/p43<br>domesurvival:block/airlock_gate/p43_back<br>domesurvival:block/airlock_gate/p44<br>domesurvival:block/airlock_gate/p44_back<br>domesurvival:block/airlock_gate/side<br>domesurvival:block/airlock_gate_unformed_dark | 16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16 | AirlockGateBlockEntityRenderer | procedural in AirlockGateBlockEntityRenderer | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| airlock_panel | domesurvival:airlock_panel | block | domesurvival:block/airlock_panel_closed<br>domesurvival:block/airlock_panel_open | minecraft:block/lime_concrete<br>minecraft:block/red_concrete | 16×16<br>16×16 | vanilla baked block model | blockstate indicator/open state | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| basic_energy_pipe | domesurvival:basic_energy_pipe | block+item | domesurvival:block/basic_energy_pipe_arm<br>domesurvival:block/basic_energy_pipe_core<br>domesurvival:item/basic_energy_pipe | domesurvival:block/basic_energy_pipe<br>domesurvival:block/basic_energy_pipe_core<br>domesurvival:block/basic_energy_pipe_detail | 64×64<br>64×64<br>16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| basic_fluid_pipe | domesurvival:basic_fluid_pipe | block+item | domesurvival:block/basic_fluid_pipe_arm<br>domesurvival:block/basic_fluid_pipe_core<br>domesurvival:item/basic_fluid_pipe | domesurvival:block/basic_fluid_pipe | 16×16 | vanilla baked multipart/cutout; FluidPipeBlockEntityRenderer пуст и не подключён | нет динамического внутреннего renderer (FluidPipeBlockEntityRenderer.java) | нет отдельного GUI; взаимодействия см. FluidPipeBlock | Да: единый визуальный язык; сохранить контракты | HIGH |
| bio_module | domesurvival:bio_module | item | domesurvival:item/bio_module | domesurvival:item/genetics/damaged_pig_cryocapsule | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| bio_repair_kit | domesurvival:bio_repair_kit | item | domesurvival:item/bio_repair_kit | domesurvival:item/genetics/bio_repair_kit | 32×32 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| biogel | domesurvival:biogel | item | domesurvival:item/biogel | domesurvival:item/genetics/biogel | 32×32 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| bioincubator | domesurvival:bioincubator | block+item | domesurvival:block/bioincubator<br>domesurvival:block/bioincubator_input_port_down<br>domesurvival:block/bioincubator_input_port_east<br>domesurvival:block/bioincubator_input_port_north<br>domesurvival:block/bioincubator_input_port_south<br>domesurvival:block/bioincubator_input_port_up<br>domesurvival:block/bioincubator_input_port_west<br>domesurvival:block/bioincubator_lit<br>domesurvival:block/bioincubator_output_port_down<br>domesurvival:block/bioincubator_output_port_east<br>domesurvival:block/bioincubator_output_port_north<br>domesurvival:block/bioincubator_output_port_south<br>domesurvival:block/bioincubator_output_port_up<br>domesurvival:block/bioincubator_output_port_west<br>domesurvival:item/bioincubator | domesurvival:block/bio/bioincubator_first_base<br>domesurvival:block/bio/bioincubator_first_casing<br>domesurvival:block/bio/bioincubator_first_connector_pad<br>domesurvival:block/bio/bioincubator_first_cyan<br>domesurvival:block/bio/bioincubator_first_dna<br>domesurvival:block/bio/bioincubator_first_dna_off<br>domesurvival:block/bio/bioincubator_first_frame<br>domesurvival:block/bio/bioincubator_first_inset<br>domesurvival:block/bio/bioincubator_first_top<br>domesurvival:block/coal_generator_port_fuel<br>domesurvival:block/coal_generator_port_item | 32×32<br>32×32<br>32×32<br>32×32<br>32×128<br>32×32<br>32×32<br>32×32<br>32×32<br>16×16<br>16×16 | vanilla baked block model | PNG mcmeta | BioincubatorScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| chicken_cryocapsule | domesurvival:chicken_cryocapsule | item | domesurvival:item/chicken_cryocapsule | domesurvival:item/genetics/damaged_pig_cryocapsule | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| coal_coke | domesurvival:coal_coke | item | domesurvival:item/coal_coke | domesurvival:item/metallurgy/coal_coke | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| coal_generator | domesurvival:coal_generator | block+item | domesurvival:block/coal_generator<br>domesurvival:block/coal_generator_lit<br>domesurvival:block/machine_input_port_down<br>domesurvival:block/machine_input_port_east<br>domesurvival:block/machine_input_port_north<br>domesurvival:block/machine_input_port_south<br>domesurvival:block/machine_input_port_up<br>domesurvival:block/machine_input_port_west<br>domesurvival:block/machine_output_port_down<br>domesurvival:block/machine_output_port_east<br>domesurvival:block/machine_output_port_north<br>domesurvival:block/machine_output_port_south<br>domesurvival:block/machine_output_port_up<br>domesurvival:block/machine_output_port_west<br>domesurvival:item/coal_generator | domesurvival:block/coal_generator_front<br>domesurvival:block/coal_generator_front_on<br>domesurvival:block/coal_generator_port_fuel<br>domesurvival:block/coal_generator_port_item<br>domesurvival:block/coal_generator_side<br>domesurvival:block/coal_generator_top | 16×16<br>16×64<br>16×16<br>16×16<br>16×16<br>16×16 | vanilla baked block model | PNG mcmeta | CoalGeneratorScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| coke_oven | domesurvival:coke_oven | block+item | domesurvival:block/coke_oven_bottom_output<br>domesurvival:block/coke_oven_ready<br>domesurvival:block/coke_oven_ready_on<br>domesurvival:item/coke_oven | domesurvival:block/bfbricks<br>domesurvival:block/bfbricksdark<br>domesurvival:block/bfbrickslit<br>domesurvival:block/bftoolshot<br>domesurvival:block/bftoolst<br>domesurvival:block/campfire_log_lit<br>domesurvival:block/firetools<br>minecraft:block/campfire_log<br>minecraft:block/fire_0<br>minecraft:block/smooth_stone | 64×64<br>64×64<br>64×64<br>32×32<br>32×32<br>16×16<br>32×32<br>16×16<br>16×512<br>16×16 | vanilla baked block model | PNG mcmeta | CokeOvenScreen | Да: единый визуальный язык; сохранить контракты | CRITICAL |
| coke_oven_part | domesurvival:coke_oven_part | block | domesurvival:block/large_coke_oven_input_port<br>domesurvival:block/large_coke_oven_output_port | domesurvival:block/metallurgy/detailed/input_connector<br>domesurvival:block/metallurgy/detailed/output_connector | 16×16<br>16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | CokeOvenScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| compact_01_trio_friends | domesurvival:compact_01_trio_friends | painting | нет JSON / код | domesurvival:painting/compact_01_trio_friends | 192×128 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| compact_02_recording_in_yard | domesurvival:compact_02_recording_in_yard | painting | нет JSON / код | domesurvival:painting/compact_02_recording_in_yard | 128×192 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| compact_03_airsoft_team | domesurvival:compact_03_airsoft_team | painting | нет JSON / код | domesurvival:painting/compact_03_airsoft_team | 192×128 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| compact_04_fishing_closeup | domesurvival:compact_04_fishing_closeup | painting | нет JSON / код | domesurvival:painting/compact_04_fishing_closeup | 128×192 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| compact_05_calm_lake_fishing | domesurvival:compact_05_calm_lake_fishing | painting | нет JSON / код | domesurvival:painting/compact_05_calm_lake_fishing | 128×192 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| compact_06_relaxing_on_grass | domesurvival:compact_06_relaxing_on_grass | painting | нет JSON / код | domesurvival:painting/compact_06_relaxing_on_grass | 128×192 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| compact_07_pink_hat_portrait | domesurvival:compact_07_pink_hat_portrait | painting | нет JSON / код | domesurvival:painting/compact_07_pink_hat_portrait | 128×192 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| compact_08_watermelon_park | domesurvival:compact_08_watermelon_park | painting | нет JSON / код | domesurvival:painting/compact_08_watermelon_park | 128×192 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| compact_09_white_hat_portrait | domesurvival:compact_09_white_hat_portrait | painting | нет JSON / код | domesurvival:painting/compact_09_white_hat_portrait | 128×192 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| compact_10_flexing_portrait | domesurvival:compact_10_flexing_portrait | painting | нет JSON / код | domesurvival:painting/compact_10_flexing_portrait | 128×192 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| compact_11_prize_shop_winners | domesurvival:compact_11_prize_shop_winners | painting | нет JSON / код | domesurvival:painting/compact_11_prize_shop_winners | 128×192 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| compact_12_kitchen_character | domesurvival:compact_12_kitchen_character | painting | нет JSON / код | domesurvival:painting/compact_12_kitchen_character | 128×192 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| compact_13_music_studio_friends | domesurvival:compact_13_music_studio_friends | painting | нет JSON / код | domesurvival:painting/compact_13_music_studio_friends | 192×128 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| compact_14_mirror_group_selfie | domesurvival:compact_14_mirror_group_selfie | painting | нет JSON / код | domesurvival:painting/compact_14_mirror_group_selfie | 128×192 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| compact_15_voxel_company_bright_light | domesurvival:compact_15_voxel_company_bright_light | painting | нет JSON / код | domesurvival:painting/compact_15_voxel_company_bright_light | 192×128 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| compact_16_tricolor_portrait | domesurvival:compact_16_tricolor_portrait | painting | нет JSON / код | domesurvival:painting/compact_16_tricolor_portrait | 128×128 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| compact_17_bee_hero_amber_hive | domesurvival:compact_17_bee_hero_amber_hive | painting | нет JSON / код | domesurvival:painting/compact_17_bee_hero_amber_hive | 128×128 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| compact_18_wedding_kiss_tree | domesurvival:compact_18_wedding_kiss_tree | painting | нет JSON / код | domesurvival:painting/compact_18_wedding_kiss_tree | 192×128 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| compact_19_night_selfie_friendship | domesurvival:compact_19_night_selfie_friendship | painting | нет JSON / код | domesurvival:painting/compact_19_night_selfie_friendship | 128×128 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| compact_20_brown_suit_limo | domesurvival:compact_20_brown_suit_limo | painting | нет JSON / код | domesurvival:painting/compact_20_brown_suit_limo | 128×192 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| compact_21_bw_party_point | domesurvival:compact_21_bw_party_point | painting | нет JSON / код | domesurvival:painting/compact_21_bw_party_point | 128×192 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| compact_22_party_toast_indoor | domesurvival:compact_22_party_toast_indoor | painting | нет JSON / код | domesurvival:painting/compact_22_party_toast_indoor | 192×128 | vanilla PaintingRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| copper_furnace | domesurvival:copper_furnace | block+item | domesurvival:block/copper_furnace<br>domesurvival:block/copper_furnace_on<br>domesurvival:item/copper_furnace | domesurvival:block/copper_furnace/copper_furnace_back<br>domesurvival:block/copper_furnace/copper_furnace_body<br>domesurvival:block/copper_furnace/copper_furnace_bottom<br>domesurvival:block/copper_furnace/copper_furnace_door<br>domesurvival:block/copper_furnace/copper_furnace_fire<br>domesurvival:block/copper_furnace/copper_furnace_fire_off<br>domesurvival:block/copper_furnace/copper_furnace_gauge<br>domesurvival:block/copper_furnace/copper_furnace_iron<br>domesurvival:block/copper_furnace/copper_furnace_rim<br>domesurvival:block/copper_furnace/copper_furnace_soot<br>domesurvival:block/copper_furnace/copper_furnace_top<br>domesurvival:block/copper_furnace/copper_furnace_vent | 16×16<br>16×16<br>16×16<br>16×16<br>16×64<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16 | vanilla baked block model | PNG mcmeta; blockstate indicator/open state | vanilla FurnaceScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| copper_hopper | domesurvival:copper_hopper | block+item | domesurvival:block/copper_hopper<br>domesurvival:block/copper_hopper_down<br>domesurvival:item/copper_hopper | domesurvival:block/hopper/copper_accent<br>domesurvival:block/hopper/copper_body<br>domesurvival:block/hopper/copper_connector<br>domesurvival:block/hopper/copper_dark<br>domesurvival:block/hopper/copper_inside<br>domesurvival:block/hopper/copper_panel<br>domesurvival:block/hopper/copper_rim | 16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | TieredHopperScreen | Ревизия после эталона | MEDIUM |
| copper_item_pipe | domesurvival:copper_item_pipe | block+item | domesurvival:block/copper_item_pipe_arm<br>domesurvival:block/copper_item_pipe_core<br>domesurvival:item/copper_item_pipe | domesurvival:block/item_pipe/copper_item_pipe<br>domesurvival:block/item_pipe/copper_item_pipe_core<br>domesurvival:block/item_pipe/copper_item_pipe_detail | 64×64<br>64×64<br>16×16 | ItemPipeBlockEntityRenderer | procedural in ItemPipeBlockEntityRenderer | ItemConnectorScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| copper_plate | domesurvival:copper_plate | item | domesurvival:item/copper_plate | domesurvival:item/copper_plate | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| copper_rod | domesurvival:copper_rod | item | domesurvival:item/copper_rod | domesurvival:item/materials/copper | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| copper_sieve_mesh | domesurvival:copper_sieve_mesh | item | domesurvival:item/copper_sieve_mesh | domesurvival:item/sieve_mesh | 32×32 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| copper_tube | domesurvival:copper_tube | item | domesurvival:item/copper_tube | domesurvival:item/copper_tube | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| copper_wire | domesurvival:copper_wire | item | domesurvival:item/copper_wire | domesurvival:item/common/spool_flange<br>domesurvival:item/materials/copper | 16×16<br>16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| cow_cryocapsule | domesurvival:cow_cryocapsule | item | domesurvival:item/cow_cryocapsule | domesurvival:item/genetics/damaged_pig_cryocapsule | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| damaged_pig_cryocapsule | domesurvival:damaged_pig_cryocapsule | item | domesurvival:item/damaged_pig_cryocapsule | domesurvival:item/genetics/damaged_pig_cryocapsule | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| deepslate_goteium_ore | domesurvival:deepslate_goteium_ore | block+item | domesurvival:block/deepslate_goteium_ore<br>domesurvival:item/deepslate_goteium_ore | domesurvival:block/deepslate_goteium_ore | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| deepslate_lead_ore | domesurvival:deepslate_lead_ore | block+item | domesurvival:block/deepslate_lead_ore<br>domesurvival:item/deepslate_lead_ore | domesurvival:block/deepslate_lead_ore | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| deepslate_nickel_ore | domesurvival:deepslate_nickel_ore | block+item | domesurvival:block/deepslate_nickel_ore<br>domesurvival:item/deepslate_nickel_ore | domesurvival:block/deepslate_nickel_ore | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| deepslate_silver_ore | domesurvival:deepslate_silver_ore | block+item | domesurvival:block/deepslate_silver_ore<br>domesurvival:item/deepslate_silver_ore | domesurvival:block/deepslate_silver_ore | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| deepslate_solarite_ore | domesurvival:deepslate_solarite_ore | block+item | domesurvival:block/deepslate_solarite_ore<br>domesurvival:item/deepslate_solarite_ore | domesurvival:block/deepslate_solarite_ore | 16×16 | vanilla baked block model | blockstate indicator/open state | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| deepslate_tin_ore | domesurvival:deepslate_tin_ore | block+item | domesurvival:block/deepslate_tin_ore<br>domesurvival:item/deepslate_tin_ore | domesurvival:block/deepslate_tin_ore | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| deepslate_voltarium_ore | domesurvival:deepslate_voltarium_ore | block+item | domesurvival:block/deepslate_voltarium_ore<br>domesurvival:item/deepslate_voltarium_ore | domesurvival:block/deepslate_voltarium_ore | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| desh_hopper | domesurvival:desh_hopper | block+item | domesurvival:block/desh_hopper<br>domesurvival:block/desh_hopper_down<br>domesurvival:item/desh_hopper | domesurvival:block/hopper/desh_accent<br>domesurvival:block/hopper/desh_body<br>domesurvival:block/hopper/desh_connector<br>domesurvival:block/hopper/desh_dark<br>domesurvival:block/hopper/desh_inside<br>domesurvival:block/hopper/desh_panel<br>domesurvival:block/hopper/desh_rim | 16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | TieredHopperScreen | Ревизия после эталона | MEDIUM |
| desh_item_pipe | domesurvival:desh_item_pipe | block+item | domesurvival:block/desh_item_pipe_arm<br>domesurvival:block/desh_item_pipe_core<br>domesurvival:item/desh_item_pipe | domesurvival:block/item_pipe/desh_item_pipe<br>domesurvival:block/item_pipe/desh_item_pipe_core<br>domesurvival:block/item_pipe/desh_item_pipe_detail | 64×64<br>64×64<br>16×16 | ItemPipeBlockEntityRenderer | procedural in ItemPipeBlockEntityRenderer | ItemConnectorScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| dome_foundation | domesurvival:dome_foundation | block+item | domesurvival:block/dome_foundation<br>domesurvival:item/dome_foundation | minecraft:block/smooth_stone | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| dome_frame | domesurvival:dome_frame | block+item | domesurvival:block/dome_frame<br>domesurvival:item/dome_frame | minecraft:block/iron_block | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| energy_buffer | domesurvival:energy_buffer | block+item | domesurvival:block/energy_buffer_v39_0<br>domesurvival:block/energy_buffer_v39_2<br>domesurvival:block/energy_buffer_v39_4<br>domesurvival:block/energy_buffer_v39_6<br>domesurvival:block/energy_buffer_v39_8<br>domesurvival:block/machine_input_port_down<br>domesurvival:block/machine_input_port_east<br>domesurvival:block/machine_input_port_north<br>domesurvival:block/machine_input_port_south<br>domesurvival:block/machine_input_port_up<br>domesurvival:block/machine_input_port_west<br>domesurvival:block/machine_output_port_down<br>domesurvival:block/machine_output_port_east<br>domesurvival:block/machine_output_port_north<br>domesurvival:block/machine_output_port_south<br>domesurvival:block/machine_output_port_up<br>domesurvival:block/machine_output_port_west<br>domesurvival:item/energy_buffer | domesurvival:block/coal_generator_port_fuel<br>domesurvival:block/coal_generator_port_item<br>domesurvival:block/coal_generator_side<br>domesurvival:block/energy_buffer/front_0<br>domesurvival:block/energy_buffer/front_2<br>domesurvival:block/energy_buffer/front_4<br>domesurvival:block/energy_buffer/front_5<br>domesurvival:block/energy_buffer/front_6<br>domesurvival:block/energy_buffer/front_8 | 16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | EnergyBufferScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| energy_buffer_adamantium | domesurvival:energy_buffer_adamantium | block+item | domesurvival:block/energy_buffer_adamantium_0<br>domesurvival:block/energy_buffer_adamantium_1<br>domesurvival:block/energy_buffer_adamantium_2<br>domesurvival:block/energy_buffer_adamantium_3<br>domesurvival:block/energy_buffer_adamantium_4<br>domesurvival:block/machine_input_port_down<br>domesurvival:block/machine_input_port_east<br>domesurvival:block/machine_input_port_north<br>domesurvival:block/machine_input_port_south<br>domesurvival:block/machine_input_port_up<br>domesurvival:block/machine_input_port_west<br>domesurvival:block/machine_output_port_down<br>domesurvival:block/machine_output_port_east<br>domesurvival:block/machine_output_port_north<br>domesurvival:block/machine_output_port_south<br>domesurvival:block/machine_output_port_up<br>domesurvival:block/machine_output_port_west<br>domesurvival:item/energy_buffer_adamantium | domesurvival:block/coal_generator_port_fuel<br>domesurvival:block/coal_generator_port_item<br>domesurvival:block/coal_generator_side<br>domesurvival:block/energy_buffer_adamantium/front_0<br>domesurvival:block/energy_buffer_adamantium/front_1<br>domesurvival:block/energy_buffer_adamantium/front_2<br>domesurvival:block/energy_buffer_adamantium/front_3<br>domesurvival:block/energy_buffer_adamantium/front_4 | 16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | AdamantiumEnergyBufferScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| energy_buffer_creative | domesurvival:energy_buffer_creative | block+item | domesurvival:block/energy_buffer_creative<br>domesurvival:block/machine_input_port_down<br>domesurvival:block/machine_input_port_east<br>domesurvival:block/machine_input_port_north<br>domesurvival:block/machine_input_port_south<br>domesurvival:block/machine_input_port_up<br>domesurvival:block/machine_input_port_west<br>domesurvival:block/machine_output_port_down<br>domesurvival:block/machine_output_port_east<br>domesurvival:block/machine_output_port_north<br>domesurvival:block/machine_output_port_south<br>domesurvival:block/machine_output_port_up<br>domesurvival:block/machine_output_port_west<br>domesurvival:item/energy_buffer_creative | domesurvival:block/coal_generator_port_fuel<br>domesurvival:block/coal_generator_port_item<br>domesurvival:block/coal_generator_side<br>domesurvival:block/energy_buffer_creative/front | 16×16<br>16×16<br>16×16<br>16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | CreativeEnergyBufferScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| energy_buffer_titan | domesurvival:energy_buffer_titan | block+item | domesurvival:block/energy_buffer_titan_0<br>domesurvival:block/energy_buffer_titan_1<br>domesurvival:block/energy_buffer_titan_2<br>domesurvival:block/energy_buffer_titan_3<br>domesurvival:block/energy_buffer_titan_4<br>domesurvival:block/machine_input_port_down<br>domesurvival:block/machine_input_port_east<br>domesurvival:block/machine_input_port_north<br>domesurvival:block/machine_input_port_south<br>domesurvival:block/machine_input_port_up<br>domesurvival:block/machine_input_port_west<br>domesurvival:block/machine_output_port_down<br>domesurvival:block/machine_output_port_east<br>domesurvival:block/machine_output_port_north<br>domesurvival:block/machine_output_port_south<br>domesurvival:block/machine_output_port_up<br>domesurvival:block/machine_output_port_west<br>domesurvival:item/energy_buffer_titan | domesurvival:block/coal_generator_port_fuel<br>domesurvival:block/coal_generator_port_item<br>domesurvival:block/coal_generator_side<br>domesurvival:block/energy_buffer_titan/front_0<br>domesurvival:block/energy_buffer_titan/front_1<br>domesurvival:block/energy_buffer_titan/front_2<br>domesurvival:block/energy_buffer_titan/front_3<br>domesurvival:block/energy_buffer_titan/front_4 | 16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | TitanEnergyBufferScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| fiber_sieve_mesh | domesurvival:fiber_sieve_mesh | item | domesurvival:item/fiber_sieve_mesh | domesurvival:item/sieve_mesh | 32×32 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| filter_regeneration_media | domesurvival:filter_regeneration_media | item | domesurvival:item/filter_regeneration_media | domesurvival:item/metallurgy/coal_coke | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| filter_regeneration_station | domesurvival:filter_regeneration_station | block+item | domesurvival:block/filter_regeneration_station<br>domesurvival:block/filter_regeneration_station_active<br>domesurvival:item/filter_regeneration_station | domesurvival:block/coal_generator_side<br>domesurvival:block/coal_generator_top<br>domesurvival:block/filter_regeneration_station_front<br>domesurvival:block/filter_regeneration_station_front_active | 16×16<br>16×16<br>32×32<br>32×256 | vanilla baked block model | PNG mcmeta; blockstate indicator/open state | FilterRegenerationScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| filtering_item_pipe | domesurvival:filtering_item_pipe | block+item | domesurvival:block/filtering_item_pipe_arm<br>domesurvival:block/filtering_item_pipe_core<br>domesurvival:item/filtering_item_pipe | domesurvival:block/item_pipe/filtering_item_pipe<br>domesurvival:block/item_pipe/filtering_item_pipe_core<br>domesurvival:block/item_pipe/filtering_item_pipe_detail<br>domesurvival:block/item_pipe/route_down<br>domesurvival:block/item_pipe/route_east<br>domesurvival:block/item_pipe/route_north<br>domesurvival:block/item_pipe/route_south<br>domesurvival:block/item_pipe/route_up<br>domesurvival:block/item_pipe/route_west | 64×64<br>64×64<br>16×16<br>64×64<br>64×64<br>64×64<br>64×64<br>64×64<br>64×64 | ItemPipeBlockEntityRenderer | procedural in ItemPipeBlockEntityRenderer | FilteringItemPipeScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| flowing_purified_water | domesurvival:flowing_purified_water | fluid | нет JSON / код | minecraft:block/water_still<br>minecraft:block/water_flow<br>minecraft:block/water_overlay | 16×512<br>32×1024<br>16×16 | IClientFluidTypeExtensions (см. ModFluids.java) | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| forming_press | domesurvival:forming_press | block+item | domesurvival:block/forming_press<br>domesurvival:block/forming_press_active<br>domesurvival:block/forming_press_input_port_down<br>domesurvival:block/forming_press_input_port_east<br>domesurvival:block/forming_press_input_port_north<br>domesurvival:block/forming_press_input_port_south<br>domesurvival:block/forming_press_input_port_up<br>domesurvival:block/forming_press_input_port_west<br>domesurvival:block/forming_press_output_port_down<br>domesurvival:block/forming_press_output_port_east<br>domesurvival:block/forming_press_output_port_north<br>domesurvival:block/forming_press_output_port_south<br>domesurvival:block/forming_press_output_port_up<br>domesurvival:block/forming_press_output_port_west<br>domesurvival:item/forming_press | domesurvival:block/coal_generator_port_energy<br>domesurvival:block/coal_generator_port_fuel<br>domesurvival:block/forming_press_front<br>domesurvival:block/forming_press_front_active<br>domesurvival:block/forming_press_side<br>domesurvival:block/forming_press_top | 16×16<br>16×16<br>16×16<br>16×64<br>16×16<br>16×16 | vanilla baked block model | PNG mcmeta | FormingPressScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| goteium_block | domesurvival:goteium_block | block+item | domesurvival:block/goteium_block<br>domesurvival:item/goteium_block | domesurvival:block/goteium_block | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| goteium_gear | domesurvival:goteium_gear | item | domesurvival:item/goteium_gear | domesurvival:item/goteium_gear | 32×32 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| goteium_ingot | domesurvival:goteium_ingot | item | domesurvival:item/goteium_ingot | domesurvival:item/goteium_ingot | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| goteium_nugget | domesurvival:goteium_nugget | item | domesurvival:item/goteium_nugget | domesurvival:item/goteium_nugget | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| goteium_ore | domesurvival:goteium_ore | block+item | domesurvival:block/goteium_ore<br>domesurvival:item/goteium_ore | domesurvival:block/goteium_ore | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| goteium_plate | domesurvival:goteium_plate | item | domesurvival:item/goteium_plate | domesurvival:item/goteium_plate | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| high_flow_oxygen_pipe | domesurvival:high_flow_oxygen_pipe | block+item | domesurvival:block/high_flow_oxygen_pipe_connection<br>domesurvival:block/high_flow_oxygen_pipe_core<br>domesurvival:item/high_flow_oxygen_pipe | domesurvival:block/oxygen_pipe/high_flow_oxygen_pipe_bolts<br>domesurvival:block/oxygen_pipe/high_flow_oxygen_pipe_metal<br>domesurvival:block/oxygen_pipe/high_flow_oxygen_pipe_oxygen<br>domesurvival:block/oxygen_pipe/high_flow_oxygen_pipe_panel<br>domesurvival:block/oxygen_pipe/high_flow_oxygen_pipe_seal | 16×16<br>16×16<br>16×16<br>16×16<br>16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| high_pressure_fluid_pipe | domesurvival:high_pressure_fluid_pipe | block+item | domesurvival:block/high_pressure_fluid_pipe_arm<br>domesurvival:block/high_pressure_fluid_pipe_core<br>domesurvival:item/high_pressure_fluid_pipe | domesurvival:block/high_pressure_fluid_pipe | 16×16 | vanilla baked multipart/cutout; FluidPipeBlockEntityRenderer пуст и не подключён | нет динамического внутреннего renderer (FluidPipeBlockEntityRenderer.java) | нет отдельного GUI; взаимодействия см. FluidPipeBlock | Да: единый визуальный язык; сохранить контракты | HIGH |
| high_voltage_energy_pipe | domesurvival:high_voltage_energy_pipe | block+item | domesurvival:block/high_voltage_energy_pipe_arm<br>domesurvival:block/high_voltage_energy_pipe_core<br>domesurvival:item/high_voltage_energy_pipe | domesurvival:block/high_voltage_energy_pipe<br>domesurvival:block/high_voltage_energy_pipe_core<br>domesurvival:block/high_voltage_energy_pipe_detail | 64×64<br>64×64<br>16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| hopper_upgrade_copper_to_steel | domesurvival:hopper_upgrade_copper_to_steel | item | domesurvival:item/hopper_upgrade_copper_to_steel | domesurvival:item/hopper_upgrade_copper_to_steel | 64×64 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| hopper_upgrade_steel_to_desh | domesurvival:hopper_upgrade_steel_to_desh | item | domesurvival:item/hopper_upgrade_steel_to_desh | domesurvival:item/hopper_upgrade_steel_to_desh | 64×64 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| hopper_upgrade_vanilla_to_copper | domesurvival:hopper_upgrade_vanilla_to_copper | item | domesurvival:item/hopper_upgrade_vanilla_to_copper | domesurvival:item/hopper_upgrade_vanilla_to_copper | 64×64 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| improved_water_filter | domesurvival:improved_water_filter | item | domesurvival:item/improved_water_filter | domesurvival:item/improved_water_filter | 64×64 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| industrial_water_filter | domesurvival:industrial_water_filter | item | domesurvival:item/industrial_water_filter | domesurvival:item/industrial_water_filter | 64×64 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| item_pipe_packet | domesurvival:item_pipe_packet | particle | нет JSON / код | domesurvival:particle/item_pipe_packet | 16×16 | ItemPipePacketParticle.Provider | tick/move спрайта в ItemPipePacketParticle | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| lanos_abandoned | domesurvival:lanos_abandoned | block+item | domesurvival:block/lanos_abandoned<br>domesurvival:item/lanos_abandoned | domesurvival:block/lanos_abandoned | 256×256 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | LanosTrunkScreen | Ревизия после эталона | MEDIUM |
| lanos_decorative | domesurvival:lanos_decorative | block+item | domesurvival:block/lanos_decorative<br>domesurvival:item/lanos_decorative | domesurvival:block/lanos_decorative | 256×256 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | LanosTrunkScreen | Ревизия после эталона | MEDIUM |
| lanos_hitbox_part | domesurvival:lanos_hitbox_part | block | domesurvival:block/lanos_hitbox_part | minecraft:block/iron_block | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| large_oxygen_tank | domesurvival:large_oxygen_tank | item | domesurvival:item/large_oxygen_tank | domesurvival:item/large_oxygen_tank<br>domesurvival:models/armor/oxygen_tank | 16×16<br>64×32 | OxygenTankCurioRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| lead_block | domesurvival:lead_block | block+item | domesurvival:block/lead_block<br>domesurvival:item/lead_block | domesurvival:block/lead_block | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| lead_gear | domesurvival:lead_gear | item | domesurvival:item/lead_gear | domesurvival:item/engineering_gears/lead_gear | 32×32 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| lead_ingot | domesurvival:lead_ingot | item | domesurvival:item/lead_ingot | domesurvival:item/lead_ingot | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| lead_nugget | domesurvival:lead_nugget | item | domesurvival:item/lead_nugget | domesurvival:item/lead_nugget | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| lead_ore | domesurvival:lead_ore | block+item | domesurvival:block/lead_ore<br>domesurvival:item/lead_ore | domesurvival:block/lead_ore | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| machine_stabilizer | domesurvival:machine_stabilizer | block+item | domesurvival:block/machine_stabilizer<br>domesurvival:item/machine_stabilizer | domesurvival:block/machine_stabilizer/mechanism_base_bolts<br>domesurvival:block/machine_stabilizer/mechanism_base_copper<br>domesurvival:block/machine_stabilizer/mechanism_base_core<br>domesurvival:block/machine_stabilizer/mechanism_base_frame<br>domesurvival:block/machine_stabilizer/mechanism_base_gear<br>domesurvival:block/machine_stabilizer/mechanism_base_node<br>domesurvival:block/machine_stabilizer/mechanism_base_panel | 16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| machine_wrench | domesurvival:machine_wrench | item | domesurvival:item/machine_wrench | domesurvival:item/machine_wrench | 32×32 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| medium_oxygen_tank | domesurvival:medium_oxygen_tank | item | domesurvival:item/medium_oxygen_tank | domesurvival:item/medium_oxygen_tank<br>domesurvival:models/armor/oxygen_tank | 16×16<br>64×32 | OxygenTankCurioRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| memory_painting | domesurvival:memory_painting | item | domesurvival:item/memory_painting | domesurvival:item/memory_painting | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| memory_painting_entity | domesurvival:memory_painting_entity | entity | нет JSON / код | см. renderer/общий sprite | не применимо | MemoryPaintingRenderer | procedural in MemoryPaintingRenderer | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| nickel_block | domesurvival:nickel_block | block+item | domesurvival:block/nickel_block<br>domesurvival:item/nickel_block | domesurvival:block/nickel_block | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| nickel_gear | domesurvival:nickel_gear | item | domesurvival:item/nickel_gear | domesurvival:item/engineering_gears/nickel_gear | 32×32 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| nickel_ingot | domesurvival:nickel_ingot | item | domesurvival:item/nickel_ingot | domesurvival:item/nickel_ingot | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| nickel_nugget | domesurvival:nickel_nugget | item | domesurvival:item/nickel_nugget | domesurvival:item/nickel_nugget | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| nickel_ore | domesurvival:nickel_ore | block+item | domesurvival:block/nickel_ore<br>domesurvival:item/nickel_ore | domesurvival:block/nickel_ore | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| nickel_plate | domesurvival:nickel_plate | item | domesurvival:item/nickel_plate | domesurvival:item/nickel_plate | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| nickel_tube | domesurvival:nickel_tube | item | domesurvival:item/nickel_tube | domesurvival:item/nickel_tube | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| nutrient_mix | domesurvival:nutrient_mix | item | domesurvival:item/nutrient_mix | domesurvival:item/genetics/nutrient_mix | 32×32 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| oxygen_complex_air_intake | domesurvival:oxygen_complex_air_intake | block+item | domesurvival:block/oxygen_complex_air_intake_formed_off<br>domesurvival:block/oxygen_complex_air_intake_unformed_off<br>domesurvival:item/oxygen_complex_air_intake | domesurvival:block/coal_generator_side<br>domesurvival:block/coal_generator_top<br>domesurvival:block/energy_buffer/back<br>domesurvival:block/energy_buffer/bottom<br>domesurvival:block/energy_buffer/side_input<br>domesurvival:block/energy_buffer/side_output<br>domesurvival:block/oxygen_complex/air_intake_side<br>domesurvival:block/oxygen_complex/amber_off<br>domesurvival:block/oxygen_complex/amber_on<br>domesurvival:block/oxygen_complex/cyan_off<br>domesurvival:block/oxygen_complex/cyan_on<br>domesurvival:block/oxygen_complex/gauge<br>domesurvival:block/oxygen_complex/green_off<br>domesurvival:block/oxygen_complex/green_on<br>domesurvival:block/oxygen_complex/grille<br>domesurvival:block/oxygen_complex/hazard<br>domesurvival:block/oxygen_complex/metal_dark<br>domesurvival:block/oxygen_complex/metal_light<br>domesurvival:block/oxygen_complex/metal_mid<br>domesurvival:block/oxygen_complex/oxygen_glass_off<br>domesurvival:block/oxygen_complex/oxygen_glass_on<br>domesurvival:block/oxygen_complex/panel_base<br>domesurvival:block/oxygen_complex/pipe<br>domesurvival:block/oxygen_complex/port_input<br>domesurvival:block/oxygen_complex/port_off<br>domesurvival:block/oxygen_complex/port_output<br>domesurvival:block/oxygen_complex/screen_o2 | 16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16 | OxygenComplexPortRenderer | blockstate indicator/open state; procedural in OxygenComplexPortRenderer | OxygenComplexScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| oxygen_complex_compression | domesurvival:oxygen_complex_compression | block+item | domesurvival:block/oxygen_complex_compression_formed_off<br>domesurvival:block/oxygen_complex_compression_formed_on<br>domesurvival:block/oxygen_complex_compression_unformed_off<br>domesurvival:block/oxygen_complex_compression_unformed_on<br>domesurvival:item/oxygen_complex_compression | domesurvival:block/coal_generator_side<br>domesurvival:block/coal_generator_top<br>domesurvival:block/energy_buffer/back<br>domesurvival:block/energy_buffer/bottom<br>domesurvival:block/energy_buffer/side_input<br>domesurvival:block/energy_buffer/side_output<br>domesurvival:block/oxygen_complex/amber_off<br>domesurvival:block/oxygen_complex/amber_on<br>domesurvival:block/oxygen_complex/compression_side<br>domesurvival:block/oxygen_complex/cyan_off<br>domesurvival:block/oxygen_complex/cyan_on<br>domesurvival:block/oxygen_complex/gauge<br>domesurvival:block/oxygen_complex/green_off<br>domesurvival:block/oxygen_complex/green_on<br>domesurvival:block/oxygen_complex/grille<br>domesurvival:block/oxygen_complex/hazard<br>domesurvival:block/oxygen_complex/metal_dark<br>domesurvival:block/oxygen_complex/metal_light<br>domesurvival:block/oxygen_complex/metal_mid<br>domesurvival:block/oxygen_complex/oxygen_glass_off<br>domesurvival:block/oxygen_complex/oxygen_glass_on<br>domesurvival:block/oxygen_complex/panel_base<br>domesurvival:block/oxygen_complex/pipe<br>domesurvival:block/oxygen_complex/port_input<br>domesurvival:block/oxygen_complex/port_off<br>domesurvival:block/oxygen_complex/port_output<br>domesurvival:block/oxygen_complex/screen_o2 | 16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16 | OxygenComplexPortRenderer | blockstate indicator/open state; procedural in OxygenComplexPortRenderer | OxygenComplexScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| oxygen_complex_filtration | domesurvival:oxygen_complex_filtration | block+item | domesurvival:block/oxygen_complex_filtration_formed_off<br>domesurvival:block/oxygen_complex_filtration_formed_on<br>domesurvival:block/oxygen_complex_filtration_unformed_off<br>domesurvival:block/oxygen_complex_filtration_unformed_on<br>domesurvival:item/oxygen_complex_filtration | domesurvival:block/coal_generator_side<br>domesurvival:block/coal_generator_top<br>domesurvival:block/energy_buffer/back<br>domesurvival:block/energy_buffer/bottom<br>domesurvival:block/energy_buffer/side_input<br>domesurvival:block/energy_buffer/side_output<br>domesurvival:block/oxygen_complex/amber_off<br>domesurvival:block/oxygen_complex/amber_on<br>domesurvival:block/oxygen_complex/cyan_off<br>domesurvival:block/oxygen_complex/cyan_on<br>domesurvival:block/oxygen_complex/filtration_side<br>domesurvival:block/oxygen_complex/gauge<br>domesurvival:block/oxygen_complex/green_off<br>domesurvival:block/oxygen_complex/green_on<br>domesurvival:block/oxygen_complex/grille<br>domesurvival:block/oxygen_complex/hazard<br>domesurvival:block/oxygen_complex/metal_dark<br>domesurvival:block/oxygen_complex/metal_light<br>domesurvival:block/oxygen_complex/metal_mid<br>domesurvival:block/oxygen_complex/oxygen_glass_off<br>domesurvival:block/oxygen_complex/oxygen_glass_on<br>domesurvival:block/oxygen_complex/panel_base<br>domesurvival:block/oxygen_complex/pipe<br>domesurvival:block/oxygen_complex/port_input<br>domesurvival:block/oxygen_complex/port_off<br>domesurvival:block/oxygen_complex/port_output<br>domesurvival:block/oxygen_complex/screen_o2 | 16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16 | OxygenComplexPortRenderer | blockstate indicator/open state; procedural in OxygenComplexPortRenderer | OxygenComplexScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| oxygen_complex_output | domesurvival:oxygen_complex_output | block+item | domesurvival:block/oxygen_complex_output_formed_off<br>domesurvival:block/oxygen_complex_output_unformed_off<br>domesurvival:item/oxygen_complex_output | domesurvival:block/coal_generator_side<br>domesurvival:block/coal_generator_top<br>domesurvival:block/energy_buffer/back<br>domesurvival:block/energy_buffer/bottom<br>domesurvival:block/energy_buffer/side_input<br>domesurvival:block/energy_buffer/side_output<br>domesurvival:block/oxygen_complex/amber_off<br>domesurvival:block/oxygen_complex/amber_on<br>domesurvival:block/oxygen_complex/cyan_off<br>domesurvival:block/oxygen_complex/cyan_on<br>domesurvival:block/oxygen_complex/gauge<br>domesurvival:block/oxygen_complex/green_off<br>domesurvival:block/oxygen_complex/green_on<br>domesurvival:block/oxygen_complex/grille<br>domesurvival:block/oxygen_complex/hazard<br>domesurvival:block/oxygen_complex/metal_dark<br>domesurvival:block/oxygen_complex/metal_light<br>domesurvival:block/oxygen_complex/metal_mid<br>domesurvival:block/oxygen_complex/output_side<br>domesurvival:block/oxygen_complex/oxygen_glass_off<br>domesurvival:block/oxygen_complex/oxygen_glass_on<br>domesurvival:block/oxygen_complex/panel_base<br>domesurvival:block/oxygen_complex/pipe<br>domesurvival:block/oxygen_complex/port_input<br>domesurvival:block/oxygen_complex/port_off<br>domesurvival:block/oxygen_complex/port_output<br>domesurvival:block/oxygen_complex/screen_o2 | 16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16 | OxygenComplexPortRenderer | blockstate indicator/open state; procedural in OxygenComplexPortRenderer | OxygenComplexScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| oxygen_electrolyzer | domesurvival:oxygen_electrolyzer | block+item | domesurvival:block/machine_input_port_down<br>domesurvival:block/machine_input_port_east<br>domesurvival:block/machine_input_port_north<br>domesurvival:block/machine_input_port_south<br>domesurvival:block/machine_input_port_up<br>domesurvival:block/machine_input_port_west<br>domesurvival:block/machine_output_port_down<br>domesurvival:block/machine_output_port_east<br>domesurvival:block/machine_output_port_north<br>domesurvival:block/machine_output_port_south<br>domesurvival:block/machine_output_port_up<br>domesurvival:block/machine_output_port_west<br>domesurvival:block/oxygen_electrolyzer<br>domesurvival:block/oxygen_electrolyzer_lit<br>domesurvival:item/oxygen_electrolyzer | domesurvival:block/coal_generator_port_fuel<br>domesurvival:block/coal_generator_port_item<br>domesurvival:block/coal_generator_side<br>domesurvival:block/coal_generator_top<br>domesurvival:block/oxygen_electrolyzer_front<br>domesurvival:block/oxygen_electrolyzer_front_on | 16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×128 | vanilla baked block model | PNG mcmeta | OxygenElectrolyzerScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| oxygen_filler | domesurvival:oxygen_filler | block+item | domesurvival:block/machine_input_port_down<br>domesurvival:block/machine_input_port_east<br>domesurvival:block/machine_input_port_north<br>domesurvival:block/machine_input_port_south<br>domesurvival:block/machine_input_port_up<br>domesurvival:block/machine_input_port_west<br>domesurvival:block/machine_output_port_down<br>domesurvival:block/machine_output_port_east<br>domesurvival:block/machine_output_port_north<br>domesurvival:block/machine_output_port_south<br>domesurvival:block/machine_output_port_up<br>domesurvival:block/machine_output_port_west<br>domesurvival:block/oxygen_filler<br>domesurvival:block/oxygen_filler_lit<br>domesurvival:item/oxygen_filler | domesurvival:block/coal_generator_port_fuel<br>domesurvival:block/coal_generator_port_item<br>domesurvival:block/coal_generator_side<br>domesurvival:block/coal_generator_top<br>domesurvival:block/oxygen_filler_front<br>domesurvival:block/oxygen_filler_front_on | 16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×128 | vanilla baked block model | PNG mcmeta | OxygenFillerScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| oxygen_mask | domesurvival:oxygen_mask | item | domesurvival:item/oxygen_mask | domesurvival:item/oxygen_mask<br>domesurvival:models/armor/m40_gasmask_domesurvival | 16×16<br>256×256 | OxygenMaskCurioRenderer | procedural in OxygenMaskCurioRenderer | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| oxygen_pipe | domesurvival:oxygen_pipe | block+item | domesurvival:block/oxygen_pipe_connection<br>domesurvival:block/oxygen_pipe_core<br>domesurvival:item/oxygen_pipe | domesurvival:block/oxygen_pipe/oxygen_pipe_bolts<br>domesurvival:block/oxygen_pipe/oxygen_pipe_metal<br>domesurvival:block/oxygen_pipe/oxygen_pipe_oxygen<br>domesurvival:block/oxygen_pipe/oxygen_pipe_panel<br>domesurvival:block/oxygen_pipe/oxygen_pipe_seal | 16×16<br>16×16<br>16×16<br>16×16<br>16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| pulse_matrix | domesurvival:pulse_matrix | item | domesurvival:item/pulse_matrix | domesurvival:item/pulse_matrix/tone_00<br>domesurvival:item/pulse_matrix/tone_00_side<br>domesurvival:item/pulse_matrix/tone_01<br>domesurvival:item/pulse_matrix/tone_01_side<br>domesurvival:item/pulse_matrix/tone_02<br>domesurvival:item/pulse_matrix/tone_02_side<br>domesurvival:item/pulse_matrix/tone_03<br>domesurvival:item/pulse_matrix/tone_03_side<br>domesurvival:item/pulse_matrix/tone_04<br>domesurvival:item/pulse_matrix/tone_04_side<br>domesurvival:item/pulse_matrix/tone_05<br>domesurvival:item/pulse_matrix/tone_05_side<br>domesurvival:item/pulse_matrix/tone_06<br>domesurvival:item/pulse_matrix/tone_06_side<br>domesurvival:item/pulse_matrix/tone_07<br>domesurvival:item/pulse_matrix/tone_07_side<br>domesurvival:item/pulse_matrix/tone_08<br>domesurvival:item/pulse_matrix/tone_08_side<br>domesurvival:item/pulse_matrix/tone_09<br>domesurvival:item/pulse_matrix/tone_09_side<br>domesurvival:item/pulse_matrix/tone_10<br>domesurvival:item/pulse_matrix/tone_10_side<br>domesurvival:item/pulse_matrix/tone_11<br>domesurvival:item/pulse_matrix/tone_11_side<br>domesurvival:item/pulse_matrix/tone_12<br>domesurvival:item/pulse_matrix/tone_12_side<br>domesurvival:item/pulse_matrix/tone_13<br>domesurvival:item/pulse_matrix/tone_13_side | 32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32<br>32×32 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| purified_water | domesurvival:purified_water | fluid | нет JSON / код | minecraft:block/water_still<br>minecraft:block/water_flow<br>minecraft:block/water_overlay | 16×512<br>32×1024<br>16×16 | IClientFluidTypeExtensions (см. ModFluids.java) | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| purified_water | domesurvival:purified_water | fluid_type | нет JSON / код | minecraft:block/water_still<br>minecraft:block/water_flow<br>minecraft:block/water_overlay | 16×512<br>32×1024<br>16×16 | IClientFluidTypeExtensions (см. ModFluids.java) | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| raw_goteium | domesurvival:raw_goteium | item | domesurvival:item/raw_goteium | domesurvival:item/raw_goteium | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| raw_lead | domesurvival:raw_lead | item | domesurvival:item/raw_lead | domesurvival:item/raw_lead | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| raw_nickel | domesurvival:raw_nickel | item | domesurvival:item/raw_nickel | domesurvival:item/raw_nickel | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| raw_silver | domesurvival:raw_silver | item | domesurvival:item/raw_silver | domesurvival:item/raw_silver | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| raw_tin | domesurvival:raw_tin | item | domesurvival:item/raw_tin | domesurvival:item/raw_tin | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| raw_voltarium | domesurvival:raw_voltarium | item | domesurvival:item/raw_voltarium | domesurvival:item/raw_voltarium | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| reinforced_energy_pipe | domesurvival:reinforced_energy_pipe | block+item | domesurvival:block/reinforced_energy_pipe_arm<br>domesurvival:block/reinforced_energy_pipe_core<br>domesurvival:item/reinforced_energy_pipe | domesurvival:block/reinforced_energy_pipe<br>domesurvival:block/reinforced_energy_pipe_core<br>domesurvival:block/reinforced_energy_pipe_detail | 64×64<br>64×64<br>16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| reinforced_fluid_pipe | domesurvival:reinforced_fluid_pipe | block+item | domesurvival:block/reinforced_fluid_pipe_arm<br>domesurvival:block/reinforced_fluid_pipe_core<br>domesurvival:item/reinforced_fluid_pipe | domesurvival:block/reinforced_fluid_pipe | 16×16 | vanilla baked multipart/cutout; FluidPipeBlockEntityRenderer пуст и не подключён | нет динамического внутреннего renderer (FluidPipeBlockEntityRenderer.java) | нет отдельного GUI; взаимодействия см. FluidPipeBlock | Да: единый визуальный язык; сохранить контракты | HIGH |
| reinforced_glass | domesurvival:reinforced_glass | block+item | domesurvival:block/reinforced_glass<br>domesurvival:item/reinforced_glass | minecraft:block/glass | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| reinforced_oxygen_pipe | domesurvival:reinforced_oxygen_pipe | block+item | domesurvival:block/reinforced_oxygen_pipe_connection<br>domesurvival:block/reinforced_oxygen_pipe_core<br>domesurvival:item/reinforced_oxygen_pipe | domesurvival:block/oxygen_pipe/reinforced_oxygen_pipe_bolts<br>domesurvival:block/oxygen_pipe/reinforced_oxygen_pipe_metal<br>domesurvival:block/oxygen_pipe/reinforced_oxygen_pipe_oxygen<br>domesurvival:block/oxygen_pipe/reinforced_oxygen_pipe_panel<br>domesurvival:block/oxygen_pipe/reinforced_oxygen_pipe_seal | 16×16<br>16×16<br>16×16<br>16×16<br>16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| sand_sieve | domesurvival:sand_sieve | block+item | domesurvival:block/sand_sieve<br>domesurvival:item/sand_sieve | minecraft:block/dark_oak_planks<br>minecraft:block/deepslate_tiles<br>minecraft:block/polished_blackstone_bricks | 16×16<br>16×16<br>16×16 | SandSieveBlockEntityRenderer | blockstate indicator/open state; procedural in SandSieveBlockEntityRenderer | SandSieveScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| sandstorm_mote | domesurvival:sandstorm_mote | particle | нет JSON / код | domesurvival:particle/sandstorm_mote | 16×16 | SandstormParticle.Provider | tick/move спрайта в SandstormParticle | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| service_pass_through | domesurvival:service_pass_through | block+item | domesurvival:block/service_pass_through_empty_z<br>domesurvival:block/service_pass_through_z<br>domesurvival:item/service_pass_through | domesurvival:block/service_pass_through/collar<br>domesurvival:block/service_pass_through/metal<br>domesurvival:block/service_pass_through/tunnel | 16×16<br>16×16<br>16×16 | ServicePassThroughRenderer | procedural in ServicePassThroughRenderer | нет отдельного GUI; взаимодействия см. ServicePassThroughBlock | Да: единый визуальный язык; сохранить контракты | HIGH |
| shaft_furnace | domesurvival:shaft_furnace | block+item | domesurvival:block/shaft_furnace_bottom_output<br>domesurvival:block/shaft_furnace_ready<br>domesurvival:block/shaft_furnace_ready_on<br>domesurvival:item/shaft_furnace | domesurvival:block/shaft_furnace_dark/bfbricks<br>domesurvival:block/shaft_furnace_dark/bfbricksdark<br>domesurvival:block/shaft_furnace_dark/bfbrickslit<br>domesurvival:block/shaft_furnace_dark/bftoolshot_blue<br>domesurvival:block/shaft_furnace_dark/bftoolst<br>domesurvival:block/shaft_furnace_dark/blue_fire<br>domesurvival:block/shaft_furnace_dark/campfire_log_lit_blue<br>domesurvival:block/shaft_furnace_dark/firetools<br>domesurvival:block/shaft_furnace_dark/firetools_blue<br>domesurvival:block/shaft_furnace_dark/wither_skeleton_head<br>minecraft:block/campfire_log<br>minecraft:block/polished_deepslate | 64×64<br>64×64<br>64×64<br>32×32<br>32×32<br>16×512<br>16×16<br>32×32<br>32×32<br>64×32<br>16×16<br>16×16 | vanilla baked block model | PNG mcmeta | ShaftFurnaceScreen | Да: единый визуальный язык; сохранить контракты | CRITICAL |
| shaft_furnace_part | domesurvival:shaft_furnace_part | block | domesurvival:block/shaft_furnace_part_empty | minecraft:block/blackstone | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | ShaftFurnaceScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| sheep_cryocapsule | domesurvival:sheep_cryocapsule | item | domesurvival:item/sheep_cryocapsule | domesurvival:item/genetics/damaged_pig_cryocapsule | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| silver_block | domesurvival:silver_block | block+item | domesurvival:block/silver_block<br>domesurvival:item/silver_block | domesurvival:block/silver_block | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| silver_ingot | domesurvival:silver_ingot | item | domesurvival:item/silver_ingot | domesurvival:item/silver_ingot | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| silver_nugget | domesurvival:silver_nugget | item | domesurvival:item/silver_nugget | domesurvival:item/silver_nugget | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| silver_ore | domesurvival:silver_ore | block+item | domesurvival:block/silver_ore<br>domesurvival:item/silver_ore | domesurvival:block/silver_ore | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| silver_rod | domesurvival:silver_rod | item | domesurvival:item/silver_rod | domesurvival:item/materials/silver | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| silver_wire | domesurvival:silver_wire | item | domesurvival:item/silver_wire | domesurvival:item/common/spool_flange<br>domesurvival:item/materials/silver | 16×16<br>16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| slag | domesurvival:slag | item | domesurvival:item/slag | minecraft:item/flint | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| small_oxygen_tank | domesurvival:small_oxygen_tank | item | domesurvival:item/small_oxygen_tank | domesurvival:item/small_oxygen_tank<br>domesurvival:models/armor/oxygen_tank | 16×16<br>64×32 | OxygenTankCurioRenderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| solar_panel_mk1 | domesurvival:solar_panel_mk1 | block+item | domesurvival:block/solar_panel_mk1<br>domesurvival:item/solar_panel_mk1 | domesurvival:block/solar_connector_body<br>domesurvival:block/solar_connector_contact<br>domesurvival:block/solar_connector_mount<br>domesurvival:block/solar_panel_mk1<br>domesurvival:item/solar_generator_mk1 | 16×16<br>16×16<br>16×16<br>256×256<br>32×32 | vanilla baked block model + loader forge:obj | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | SolarPanelScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| solar_panel_mk2 | domesurvival:solar_panel_mk2 | block+item | domesurvival:block/solar_panel_mk2<br>domesurvival:item/solar_panel_mk2 | domesurvival:block/solar_connector_body<br>domesurvival:block/solar_connector_contact<br>domesurvival:block/solar_connector_mount<br>domesurvival:block/solar_panel_mk2<br>domesurvival:item/solar_generator_mk2 | 16×16<br>16×16<br>16×16<br>256×256<br>32×32 | vanilla baked block model + loader forge:obj | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | SolarPanelScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| solar_panel_mk3 | domesurvival:solar_panel_mk3 | block+item | domesurvival:block/solar_panel_mk3<br>domesurvival:item/solar_panel_mk3 | domesurvival:block/solar_connector_body<br>domesurvival:block/solar_connector_contact<br>domesurvival:block/solar_connector_mount<br>domesurvival:block/solar_panel_mk3<br>domesurvival:item/solar_generator_mk3 | 16×16<br>16×16<br>16×16<br>256×256<br>32×32 | vanilla baked block model + loader forge:obj | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | SolarPanelScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| solarite_block | domesurvival:solarite_block | block+item | domesurvival:block/solarite_block<br>domesurvival:item/solarite_block | domesurvival:block/solarite_block | 32×32 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| solarite_crystal | domesurvival:solarite_crystal | item | domesurvival:item/solarite_crystal | domesurvival:item/solarite_crystal | 32×32 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| solarite_ore | domesurvival:solarite_ore | block+item | domesurvival:block/solarite_ore<br>domesurvival:item/solarite_ore | domesurvival:block/solarite_ore | 16×16 | vanilla baked block model | blockstate indicator/open state | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| solarite_shard | domesurvival:solarite_shard | item | domesurvival:item/solarite_shard | domesurvival:item/solarite_shard | 32×32 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| steel_block | domesurvival:steel_block | block+item | domesurvival:block/steel_block<br>domesurvival:item/steel_block | domesurvival:block/steel_block | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| steel_gear | domesurvival:steel_gear | item | domesurvival:item/steel_gear | domesurvival:item/engineering_gears/steel_gear | 32×32 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| steel_hopper | domesurvival:steel_hopper | block+item | domesurvival:block/steel_hopper<br>domesurvival:block/steel_hopper_down<br>domesurvival:item/steel_hopper | domesurvival:block/hopper/steel_accent<br>domesurvival:block/hopper/steel_body<br>domesurvival:block/hopper/steel_connector<br>domesurvival:block/hopper/steel_dark<br>domesurvival:block/hopper/steel_inside<br>domesurvival:block/hopper/steel_panel<br>domesurvival:block/hopper/steel_rim | 16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | TieredHopperScreen | Ревизия после эталона | MEDIUM |
| steel_ingot | domesurvival:steel_ingot | item | domesurvival:item/steel_ingot | domesurvival:item/metallurgy/steel_ingot | 256×256 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| steel_item_pipe | domesurvival:steel_item_pipe | block+item | domesurvival:block/steel_item_pipe_arm<br>domesurvival:block/steel_item_pipe_core<br>domesurvival:item/steel_item_pipe | domesurvival:block/item_pipe/steel_item_pipe<br>domesurvival:block/item_pipe/steel_item_pipe_core<br>domesurvival:block/item_pipe/steel_item_pipe_detail | 64×64<br>64×64<br>16×16 | ItemPipeBlockEntityRenderer | procedural in ItemPipeBlockEntityRenderer | ItemConnectorScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| steel_nugget | domesurvival:steel_nugget | item | domesurvival:item/steel_nugget | domesurvival:item/steel_nugget | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| steel_plate | domesurvival:steel_plate | item | domesurvival:item/steel_plate | domesurvival:item/steel_plate | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| steel_rod | domesurvival:steel_rod | item | domesurvival:item/steel_rod | domesurvival:item/materials/steel | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| steel_sieve_mesh | domesurvival:steel_sieve_mesh | item | domesurvival:item/steel_sieve_mesh | domesurvival:item/sieve_mesh | 32×32 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| steel_tube | domesurvival:steel_tube | item | domesurvival:item/steel_tube | domesurvival:item/steel_tube | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| steel_wire | domesurvival:steel_wire | item | domesurvival:item/steel_wire | domesurvival:item/common/spool_flange<br>domesurvival:item/materials/steel | 16×16<br>16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| surface_suit_boots | domesurvival:surface_suit_boots | item | domesurvival:item/surface_suit_boots | domesurvival:item/surface_suit_boots<br>domesurvival:models/armor/surface_suit_layer_1<br>domesurvival:models/armor/surface_suit_layer_2 | 16×16<br>64×64<br>64×64 | vanilla HumanoidArmorLayer | поза и движение vanilla HumanoidArmorLayer | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| surface_suit_chestplate | domesurvival:surface_suit_chestplate | item | domesurvival:item/surface_suit_chestplate | domesurvival:item/surface_suit_chestplate<br>domesurvival:models/armor/surface_suit_layer_1<br>domesurvival:models/armor/surface_suit_layer_2 | 16×16<br>64×64<br>64×64 | vanilla HumanoidArmorLayer | поза и движение vanilla HumanoidArmorLayer | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| surface_suit_helmet | domesurvival:surface_suit_helmet | item | domesurvival:item/surface_suit_helmet | domesurvival:item/surface_suit_helmet<br>domesurvival:models/armor/surface_suit_layer_1<br>domesurvival:models/armor/surface_suit_layer_2 | 16×16<br>64×64<br>64×64 | vanilla HumanoidArmorLayer | поза и движение vanilla HumanoidArmorLayer | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| surface_suit_leggings | domesurvival:surface_suit_leggings | item | domesurvival:item/surface_suit_leggings | domesurvival:item/surface_suit_leggings<br>domesurvival:models/armor/surface_suit_layer_1<br>domesurvival:models/armor/surface_suit_layer_2 | 16×16<br>64×64<br>64×64 | vanilla HumanoidArmorLayer | поза и движение vanilla HumanoidArmorLayer | не обнаружен по связи registry/menu | Да: единый визуальный язык; сохранить контракты | HIGH |
| tin_block | domesurvival:tin_block | block+item | domesurvival:block/tin_block<br>domesurvival:item/tin_block | domesurvival:block/tin_block | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| tin_gear | domesurvival:tin_gear | item | domesurvival:item/tin_gear | domesurvival:item/engineering_gears/tin_gear | 32×32 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| tin_ingot | domesurvival:tin_ingot | item | domesurvival:item/tin_ingot | domesurvival:item/tin_ingot | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| tin_nugget | domesurvival:tin_nugget | item | domesurvival:item/tin_nugget | domesurvival:item/tin_nugget | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| tin_ore | domesurvival:tin_ore | block+item | domesurvival:block/tin_ore<br>domesurvival:item/tin_ore | domesurvival:block/tin_ore | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| tin_plate | domesurvival:tin_plate | item | domesurvival:item/tin_plate | domesurvival:item/tin_plate | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| tin_tube | domesurvival:tin_tube | item | domesurvival:item/tin_tube | domesurvival:item/tin_tube | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| universal_tank | domesurvival:universal_tank | block+item | domesurvival:block/universal_tank<br>domesurvival:item/universal_tank | domesurvival:block/airlock_gate_unformed_dark<br>minecraft:block/glass | 16×16<br>16×16 | UniversalTankBlockEntityRenderer | procedural in UniversalTankBlockEntityRenderer | UniversalTankScreen | Да: единый визуальный язык; сохранить контракты | HIGH |
| ventilation_bubble | domesurvival:ventilation_bubble | particle | нет JSON / код | domesurvival:particle/ventilation_bubble | 32×32 | VentilationBubbleParticle.Provider | tick/move спрайта в VentilationBubbleParticle | не обнаружен по связи registry/menu | Ревизия после эталона | LOW |
| voltarium_block | domesurvival:voltarium_block | block+item | domesurvival:block/voltarium_block<br>domesurvival:item/voltarium_block | domesurvival:block/voltarium_block | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| voltarium_gear | domesurvival:voltarium_gear | item | domesurvival:item/voltarium_gear | domesurvival:item/voltarium_gear | 32×32 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| voltarium_ingot | domesurvival:voltarium_ingot | item | domesurvival:item/voltarium_ingot | domesurvival:item/voltarium_ingot | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| voltarium_nugget | domesurvival:voltarium_nugget | item | domesurvival:item/voltarium_nugget | domesurvival:item/voltarium_nugget | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| voltarium_ore | domesurvival:voltarium_ore | block+item | domesurvival:block/voltarium_ore<br>domesurvival:item/voltarium_ore | domesurvival:block/voltarium_ore | 16×16 | vanilla baked block model | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| voltarium_plate | domesurvival:voltarium_plate | item | domesurvival:item/voltarium_plate | domesurvival:item/voltarium_plate | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| voltarium_rod | domesurvival:voltarium_rod | item | domesurvival:item/voltarium_rod | domesurvival:item/materials/voltarium | 16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| voltarium_wire | domesurvival:voltarium_wire | item | domesurvival:item/voltarium_wire | domesurvival:item/common/spool_flange<br>domesurvival:item/materials/voltarium | 16×16<br>16×16 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| water_filter_cartridge | domesurvival:water_filter_cartridge | item | domesurvival:item/water_filter_cartridge | domesurvival:item/water_filter_cartridge | 64×64 | vanilla item renderer | не обнаружена в asset graph; поведение кода см. дополнительные аудиты | не обнаружен по связи registry/menu | Ревизия после эталона | MEDIUM |
| water_purifier | domesurvival:water_purifier | block+item | domesurvival:block/machine_input_port_down<br>domesurvival:block/machine_input_port_east<br>domesurvival:block/machine_input_port_north<br>domesurvival:block/machine_input_port_south<br>domesurvival:block/machine_input_port_up<br>domesurvival:block/machine_input_port_west<br>domesurvival:block/machine_output_port_down<br>domesurvival:block/machine_output_port_east<br>domesurvival:block/machine_output_port_north<br>domesurvival:block/machine_output_port_south<br>domesurvival:block/machine_output_port_up<br>domesurvival:block/machine_output_port_west<br>domesurvival:block/water_purifier<br>domesurvival:block/water_purifier_lit<br>domesurvival:item/water_purifier | domesurvival:block/coal_generator_port_fuel<br>domesurvival:block/coal_generator_port_item<br>domesurvival:block/coal_generator_side<br>domesurvival:block/coal_generator_top<br>domesurvival:block/water_purifier_front<br>domesurvival:block/water_purifier_front_on | 16×16<br>16×16<br>16×16<br>16×16<br>16×16<br>16×96 | vanilla baked block model | PNG mcmeta | WaterPurifierScreen | Да: единый визуальный язык; сохранить контракты | HIGH |

## Весь активный renderer/model/GUI-код

Это расширенный поиск визуальной ответственности (включая вспомогательные классы), а не утверждение, что каждый класс рисует отдельный объект.

| Класс | Роль | Путь | Технологии |
| --- | --- | --- | --- |
| ClientModEvents | visual_support | src/main/java/com/wasted/domesurvival/forge/client/ClientModEvents.java | Curios |
| DomeSurvivalScreenTuner | visual_support | src/main/java/com/wasted/domesurvival/forge/client/DomeSurvivalScreenTuner.java | vanilla/Forge Java |
| EnergyStorageTransferRateOverlay | overlay | src/main/java/com/wasted/domesurvival/forge/client/EnergyStorageTransferRateOverlay.java | vanilla/Forge Java |
| FilteringItemPipeScreen | GUI | src/main/java/com/wasted/domesurvival/forge/client/itempipe/FilteringItemPipeScreen.java | vanilla/Forge Java |
| ItemConnectorScreen | GUI | src/main/java/com/wasted/domesurvival/forge/client/itempipe/ItemConnectorScreen.java | vanilla/Forge Java |
| ItemPipeBlockEntityRenderer | renderer | src/main/java/com/wasted/domesurvival/forge/client/itempipe/ItemPipeBlockEntityRenderer.java | VertexConsumer |
| ItemPipePacketParticle | visual_support | src/main/java/com/wasted/domesurvival/forge/client/itempipe/ItemPipePacketParticle.java | vanilla/Forge Java |
| JosephGuiButtonOverlay | overlay | src/main/java/com/wasted/domesurvival/forge/client/JosephGuiButtonOverlay.java | vanilla/Forge Java |
| OxygenEquipmentModelCache | model_or_loader | src/main/java/com/wasted/domesurvival/forge/client/model/OxygenEquipmentModelCache.java | vanilla/Forge Java |
| OxygenMaskModel | model_or_loader | src/main/java/com/wasted/domesurvival/forge/client/model/OxygenMaskModel.java | vanilla/Forge Java |
| OxygenModelMesh | model_or_loader | src/main/java/com/wasted/domesurvival/forge/client/model/OxygenModelMesh.java | vanilla/Forge Java |
| OxygenTankModel | model_or_loader | src/main/java/com/wasted/domesurvival/forge/client/model/OxygenTankModel.java | vanilla/Forge Java |
| OxygenHudOverlay | overlay | src/main/java/com/wasted/domesurvival/forge/client/OxygenHudOverlay.java | vanilla/Forge Java |
| MemoryPaintingRenderer | renderer | src/main/java/com/wasted/domesurvival/forge/client/painting/MemoryPaintingRenderer.java | vanilla/Forge Java |
| AcidRainParticle | visual_support | src/main/java/com/wasted/domesurvival/forge/client/particle/AcidRainParticle.java | vanilla/Forge Java |
| SandstormParticle | visual_support | src/main/java/com/wasted/domesurvival/forge/client/particle/SandstormParticle.java | vanilla/Forge Java |
| VentilationBubbleParticle | visual_support | src/main/java/com/wasted/domesurvival/forge/client/particle/VentilationBubbleParticle.java | vanilla/Forge Java |
| AirlockGateBlockEntityRenderer | renderer | src/main/java/com/wasted/domesurvival/forge/client/render/AirlockGateBlockEntityRenderer.java | VertexConsumer |
| M40MaskMesh | visual_support | src/main/java/com/wasted/domesurvival/forge/client/render/M40MaskMesh.java | VertexConsumer |
| M40MaskRenderLayer | visual_support | src/main/java/com/wasted/domesurvival/forge/client/render/M40MaskRenderLayer.java | VertexConsumer |
| OxygenComplexPortRenderer | renderer | src/main/java/com/wasted/domesurvival/forge/client/render/OxygenComplexPortRenderer.java | VertexConsumer |
| OxygenMaskCurioRenderer | renderer | src/main/java/com/wasted/domesurvival/forge/client/render/OxygenMaskCurioRenderer.java | Curios,VertexConsumer |
| OxygenTankCurioRenderer | renderer | src/main/java/com/wasted/domesurvival/forge/client/render/OxygenTankCurioRenderer.java | Curios |
| SandSieveBlockEntityRenderer | renderer | src/main/java/com/wasted/domesurvival/forge/client/render/SandSieveBlockEntityRenderer.java | vanilla/Forge Java |
| ServicePassThroughRenderer | renderer | src/main/java/com/wasted/domesurvival/forge/client/render/ServicePassThroughRenderer.java | vanilla/Forge Java |
| UniversalTankBlockEntityRenderer | renderer | src/main/java/com/wasted/domesurvival/forge/client/render/UniversalTankBlockEntityRenderer.java | VertexConsumer |
| AdamantiumEnergyBufferScreen | GUI | src/main/java/com/wasted/domesurvival/forge/client/screen/AdamantiumEnergyBufferScreen.java | vanilla/Forge Java |
| CoalGeneratorScreen | GUI | src/main/java/com/wasted/domesurvival/forge/client/screen/CoalGeneratorScreen.java | vanilla/Forge Java |
| CokeOvenScreen | GUI | src/main/java/com/wasted/domesurvival/forge/client/screen/CokeOvenScreen.java | vanilla/Forge Java |
| CreativeEnergyBufferScreen | GUI | src/main/java/com/wasted/domesurvival/forge/client/screen/CreativeEnergyBufferScreen.java | vanilla/Forge Java |
| EnergyBufferScreen | GUI | src/main/java/com/wasted/domesurvival/forge/client/screen/EnergyBufferScreen.java | vanilla/Forge Java |
| OxygenComplexScreen | GUI | src/main/java/com/wasted/domesurvival/forge/client/screen/OxygenComplexScreen.java | vanilla/Forge Java |
| OxygenElectrolyzerScreen | GUI | src/main/java/com/wasted/domesurvival/forge/client/screen/OxygenElectrolyzerScreen.java | vanilla/Forge Java |
| OxygenFillerScreen | GUI | src/main/java/com/wasted/domesurvival/forge/client/screen/OxygenFillerScreen.java | vanilla/Forge Java |
| ShaftFurnaceScreen | GUI | src/main/java/com/wasted/domesurvival/forge/client/screen/ShaftFurnaceScreen.java | vanilla/Forge Java |
| TitanEnergyBufferScreen | GUI | src/main/java/com/wasted/domesurvival/forge/client/screen/TitanEnergyBufferScreen.java | vanilla/Forge Java |
| UniversalTankScreen | GUI | src/main/java/com/wasted/domesurvival/forge/client/screen/UniversalTankScreen.java | vanilla/Forge Java |
| WaterPurifierScreen | GUI | src/main/java/com/wasted/domesurvival/forge/client/screen/WaterPurifierScreen.java | vanilla/Forge Java |
| TieredHopperScreen | GUI | src/main/java/com/wasted/domesurvival/forge/client/TieredHopperScreen.java | vanilla/Forge Java |
| UniversalTankClientEvents | visual_support | src/main/java/com/wasted/domesurvival/forge/client/UniversalTankClientEvents.java | vanilla/Forge Java |
| LanosTrunkScreen | GUI | src/main/java/com/wasted/domesurvival/forge/lanos/LanosTrunkScreen.java | vanilla/Forge Java |
| BioincubatorScreen | GUI | src/main/java/com/wasted/domesurvival/forge/machine/bio/BioincubatorScreen.java | vanilla/Forge Java |
| FilterRegenerationScreen | GUI | src/main/java/com/wasted/domesurvival/forge/machine/filter/FilterRegenerationScreen.java | vanilla/Forge Java |
| FormingPressScreen | GUI | src/main/java/com/wasted/domesurvival/forge/machine/forming/FormingPressScreen.java | vanilla/Forge Java |
| PortVisual | visual_support | src/main/java/com/wasted/domesurvival/forge/machine/side/PortVisual.java | vanilla/Forge Java |
| SandSieveScreen | GUI | src/main/java/com/wasted/domesurvival/forge/machine/sieve/SandSieveScreen.java | vanilla/Forge Java |
| SolarPanelClientEvents | visual_support | src/main/java/com/wasted/domesurvival/forge/machine/solar/client/SolarPanelClientEvents.java | vanilla/Forge Java |
| SolarPanelScreen | GUI | src/main/java/com/wasted/domesurvival/forge/machine/solar/client/SolarPanelScreen.java | vanilla/Forge Java |
| HorrorCreeperModelMixin | model_or_loader | src/main/java/com/wasted/domesurvival/forge/mixin/HorrorCreeperModelMixin.java | vanilla/Forge Java |
| HorrorDrownedModelMixin | model_or_loader | src/main/java/com/wasted/domesurvival/forge/mixin/HorrorDrownedModelMixin.java | vanilla/Forge Java |
| HorrorEndermanModelMixin | model_or_loader | src/main/java/com/wasted/domesurvival/forge/mixin/HorrorEndermanModelMixin.java | vanilla/Forge Java |
| HorrorSkeletonModelMixin | model_or_loader | src/main/java/com/wasted/domesurvival/forge/mixin/HorrorSkeletonModelMixin.java | vanilla/Forge Java |
| HorrorSpiderModelMixin | model_or_loader | src/main/java/com/wasted/domesurvival/forge/mixin/HorrorSpiderModelMixin.java | vanilla/Forge Java |
| HorrorZombieModelMixin | model_or_loader | src/main/java/com/wasted/domesurvival/forge/mixin/HorrorZombieModelMixin.java | vanilla/Forge Java |
| LevelLoadingScreenAccessor | visual_support | src/main/java/com/wasted/domesurvival/forge/mixin/LevelLoadingScreenAccessor.java | vanilla/Forge Java |
| ModParticles | visual_support | src/main/java/com/wasted/domesurvival/forge/particle/ModParticles.java | vanilla/Forge Java |
| FluidPipeBlockEntityRenderer | renderer | src/main/java/com/wasted/domesurvival/forge/transport/fluid/FluidPipeBlockEntityRenderer.java | vanilla/Forge Java |
| FluidPipeClientEvents | visual_support | src/main/java/com/wasted/domesurvival/forge/transport/fluid/FluidPipeClientEvents.java | vanilla/Forge Java |

## Полный каталог активных файлов

Каждая строка — самостоятельный ресурс; модели состояния одной машины не являются дополнительными registry-объектами.

| Путь | Формат | Размер PNG / байты |
| --- | --- | --- |
| src/main/resources/assets/domesurvival/blockstates/airlock_control_panel.json | blockstate_json | 971 |
| src/main/resources/assets/domesurvival/blockstates/airlock_door.json | blockstate_json | 88 |
| src/main/resources/assets/domesurvival/blockstates/airlock_gate.json | blockstate_json | 26853 |
| src/main/resources/assets/domesurvival/blockstates/airlock_panel.json | blockstate_json | 167 |
| src/main/resources/assets/domesurvival/blockstates/basic_energy_pipe.json | blockstate_json | 1299 |
| src/main/resources/assets/domesurvival/blockstates/basic_fluid_pipe.json | blockstate_json | 1292 |
| src/main/resources/assets/domesurvival/blockstates/bioincubator.json | blockstate_json | 3370 |
| src/main/resources/assets/domesurvival/blockstates/coal_generator.json | blockstate_json | 3504 |
| src/main/resources/assets/domesurvival/blockstates/coke_oven.json | blockstate_json | 1087 |
| src/main/resources/assets/domesurvival/blockstates/coke_oven_part.json | blockstate_json | 1351 |
| src/main/resources/assets/domesurvival/blockstates/copper_furnace.json | blockstate_json | 875 |
| src/main/resources/assets/domesurvival/blockstates/copper_hopper.json | blockstate_json | 831 |
| src/main/resources/assets/domesurvival/blockstates/copper_item_pipe.json | blockstate_json | 1292 |
| src/main/resources/assets/domesurvival/blockstates/deepslate_goteium_ore.json | blockstate_json | 98 |
| src/main/resources/assets/domesurvival/blockstates/deepslate_lead_ore.json | blockstate_json | 95 |
| src/main/resources/assets/domesurvival/blockstates/deepslate_nickel_ore.json | blockstate_json | 97 |
| src/main/resources/assets/domesurvival/blockstates/deepslate_silver_ore.json | blockstate_json | 97 |
| src/main/resources/assets/domesurvival/blockstates/deepslate_solarite_ore.json | blockstate_json | 172 |
| src/main/resources/assets/domesurvival/blockstates/deepslate_tin_ore.json | blockstate_json | 94 |
| src/main/resources/assets/domesurvival/blockstates/deepslate_voltarium_ore.json | blockstate_json | 100 |
| src/main/resources/assets/domesurvival/blockstates/desh_hopper.json | blockstate_json | 821 |
| src/main/resources/assets/domesurvival/blockstates/desh_item_pipe.json | blockstate_json | 1278 |
| src/main/resources/assets/domesurvival/blockstates/dome_foundation.json | blockstate_json | 82 |
| src/main/resources/assets/domesurvival/blockstates/dome_frame.json | blockstate_json | 77 |
| src/main/resources/assets/domesurvival/blockstates/energy_buffer.json | blockstate_json | 5952 |
| src/main/resources/assets/domesurvival/blockstates/energy_buffer_adamantium.json | blockstate_json | 6094 |
| src/main/resources/assets/domesurvival/blockstates/energy_buffer_creative.json | blockstate_json | 2668 |
| src/main/resources/assets/domesurvival/blockstates/energy_buffer_titan.json | blockstate_json | 5994 |
| src/main/resources/assets/domesurvival/blockstates/filtering_item_pipe.json | blockstate_json | 1313 |
| src/main/resources/assets/domesurvival/blockstates/filter_regeneration_station.json | blockstate_json | 887 |
| src/main/resources/assets/domesurvival/blockstates/forming_press.json | blockstate_json | 2210 |
| src/main/resources/assets/domesurvival/blockstates/goteium_block.json | blockstate_json | 89 |
| src/main/resources/assets/domesurvival/blockstates/goteium_ore.json | blockstate_json | 88 |
| src/main/resources/assets/domesurvival/blockstates/high_flow_oxygen_pipe.json | blockstate_json | 1369 |
| src/main/resources/assets/domesurvival/blockstates/high_pressure_fluid_pipe.json | blockstate_json | 1348 |
| src/main/resources/assets/domesurvival/blockstates/high_voltage_energy_pipe.json | blockstate_json | 1348 |
| src/main/resources/assets/domesurvival/blockstates/lanos_abandoned.json | blockstate_json | 391 |
| src/main/resources/assets/domesurvival/blockstates/lanos_decorative.json | blockstate_json | 395 |
| src/main/resources/assets/domesurvival/blockstates/lanos_hitbox_part.json | blockstate_json | 94 |
| src/main/resources/assets/domesurvival/blockstates/lead_block.json | blockstate_json | 86 |
| src/main/resources/assets/domesurvival/blockstates/lead_ore.json | blockstate_json | 85 |
| src/main/resources/assets/domesurvival/blockstates/machine_stabilizer.json | blockstate_json | 102 |
| src/main/resources/assets/domesurvival/blockstates/nickel_block.json | blockstate_json | 88 |
| src/main/resources/assets/domesurvival/blockstates/nickel_ore.json | blockstate_json | 87 |
| src/main/resources/assets/domesurvival/blockstates/oxygen_complex_air_intake.json | blockstate_json | 2315 |
| src/main/resources/assets/domesurvival/blockstates/oxygen_complex_compression.json | blockstate_json | 2321 |
| src/main/resources/assets/domesurvival/blockstates/oxygen_complex_filtration.json | blockstate_json | 2305 |
| src/main/resources/assets/domesurvival/blockstates/oxygen_complex_output.json | blockstate_json | 2251 |
| src/main/resources/assets/domesurvival/blockstates/oxygen_electrolyzer.json | blockstate_json | 3544 |
| src/main/resources/assets/domesurvival/blockstates/oxygen_filler.json | blockstate_json | 3496 |
| src/main/resources/assets/domesurvival/blockstates/oxygen_pipe.json | blockstate_json | 1149 |
| src/main/resources/assets/domesurvival/blockstates/reinforced_energy_pipe.json | blockstate_json | 1334 |
| src/main/resources/assets/domesurvival/blockstates/reinforced_fluid_pipe.json | blockstate_json | 1327 |
| src/main/resources/assets/domesurvival/blockstates/reinforced_glass.json | blockstate_json | 83 |
| src/main/resources/assets/domesurvival/blockstates/reinforced_oxygen_pipe.json | blockstate_json | 1376 |
| src/main/resources/assets/domesurvival/blockstates/sand_sieve.json | blockstate_json | 713 |
| src/main/resources/assets/domesurvival/blockstates/service_pass_through.json | blockstate_json | 798 |
| src/main/resources/assets/domesurvival/blockstates/shaft_furnace.json | blockstate_json | 1123 |
| src/main/resources/assets/domesurvival/blockstates/shaft_furnace_part.json | blockstate_json | 125 |
| src/main/resources/assets/domesurvival/blockstates/silver_block.json | blockstate_json | 88 |
| src/main/resources/assets/domesurvival/blockstates/silver_ore.json | blockstate_json | 87 |
| src/main/resources/assets/domesurvival/blockstates/solarite_block.json | blockstate_json | 90 |
| src/main/resources/assets/domesurvival/blockstates/solarite_ore.json | blockstate_json | 152 |
| src/main/resources/assets/domesurvival/blockstates/solar_panel_mk1.json | blockstate_json | 392 |
| src/main/resources/assets/domesurvival/blockstates/solar_panel_mk2.json | blockstate_json | 392 |
| src/main/resources/assets/domesurvival/blockstates/solar_panel_mk3.json | blockstate_json | 392 |
| src/main/resources/assets/domesurvival/blockstates/steel_block.json | blockstate_json | 87 |
| src/main/resources/assets/domesurvival/blockstates/steel_hopper.json | blockstate_json | 826 |
| src/main/resources/assets/domesurvival/blockstates/steel_item_pipe.json | blockstate_json | 1285 |
| src/main/resources/assets/domesurvival/blockstates/tin_block.json | blockstate_json | 85 |
| src/main/resources/assets/domesurvival/blockstates/tin_ore.json | blockstate_json | 84 |
| src/main/resources/assets/domesurvival/blockstates/universal_tank.json | blockstate_json | 122 |
| src/main/resources/assets/domesurvival/blockstates/voltarium_block.json | blockstate_json | 91 |
| src/main/resources/assets/domesurvival/blockstates/voltarium_ore.json | blockstate_json | 90 |
| src/main/resources/assets/domesurvival/blockstates/water_purifier.json | blockstate_json | 3504 |
| src/main/resources/assets/domesurvival/models/block/airlock_control_panel.json | model_json | 63500 |
| src/main/resources/assets/domesurvival/models/block/airlock_control_panel_active.json | model_json | 63516 |
| src/main/resources/assets/domesurvival/models/block/airlock_door.json | model_json | 103 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_dynamic_empty.json | model_json | 133 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_open_empty.json | model_json | 133 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p00.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p00_open.json | model_json | 1422 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p00_stage1.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p00_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p00_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p00_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p00_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p01.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p01_open.json | model_json | 1422 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p01_stage1.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p01_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p01_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p01_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p01_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p02.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p02_open.json | model_json | 1422 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p02_stage1.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p02_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p02_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p02_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p02_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p03.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p03_open.json | model_json | 1422 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p03_stage1.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p03_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p03_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p03_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p03_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p04.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p04_open.json | model_json | 1422 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p04_stage1.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p04_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p04_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p04_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p04_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p10.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p10_open.json | model_json | 132 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p10_stage1.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p10_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p10_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p10_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p10_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p11.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p11_open.json | model_json | 132 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p11_stage1.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p11_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p11_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p11_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p11_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p12.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p12_open.json | model_json | 132 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p12_stage1.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p12_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p12_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p12_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p12_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p13.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p13_open.json | model_json | 132 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p13_stage1.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p13_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p13_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p13_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p13_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p14.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p14_open.json | model_json | 132 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p14_stage1.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p14_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p14_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p14_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p14_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p20.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p20_open.json | model_json | 132 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p20_stage1.json | model_json | 2545 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p20_stage2.json | model_json | 2545 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p20_stage3.json | model_json | 2547 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p20_stage4.json | model_json | 2547 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p20_stage5.json | model_json | 2547 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p21.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p21_open.json | model_json | 132 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p21_stage1.json | model_json | 2545 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p21_stage2.json | model_json | 2545 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p21_stage3.json | model_json | 2547 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p21_stage4.json | model_json | 2547 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p21_stage5.json | model_json | 2547 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p22.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p22_open.json | model_json | 132 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p22_stage1.json | model_json | 2545 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p22_stage2.json | model_json | 2545 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p22_stage3.json | model_json | 2547 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p22_stage4.json | model_json | 2547 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p22_stage5.json | model_json | 2547 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p23.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p23_open.json | model_json | 132 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p23_stage1.json | model_json | 2545 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p23_stage2.json | model_json | 2545 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p23_stage3.json | model_json | 2547 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p23_stage4.json | model_json | 2547 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p23_stage5.json | model_json | 2547 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p24.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p24_open.json | model_json | 132 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p24_stage1.json | model_json | 2545 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p24_stage2.json | model_json | 2545 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p24_stage3.json | model_json | 2547 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p24_stage4.json | model_json | 2547 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p24_stage5.json | model_json | 2547 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p30.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p30_open.json | model_json | 132 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p30_stage1.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p30_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p30_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p30_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p30_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p31.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p31_open.json | model_json | 132 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p31_stage1.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p31_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p31_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p31_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p31_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p32.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p32_open.json | model_json | 132 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p32_stage1.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p32_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p32_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p32_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p32_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p33.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p33_open.json | model_json | 132 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p33_stage1.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p33_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p33_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p33_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p33_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p34.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p34_open.json | model_json | 132 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p34_stage1.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p34_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p34_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p34_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p34_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p40.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p40_open.json | model_json | 1432 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p40_stage1.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p40_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p40_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p40_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p40_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p41.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p41_open.json | model_json | 1432 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p41_stage1.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p41_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p41_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p41_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p41_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p42.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p42_open.json | model_json | 1432 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p42_stage1.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p42_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p42_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p42_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p42_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p43.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p43_open.json | model_json | 1432 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p43_stage1.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p43_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p43_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p43_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p43_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p44.json | model_json | 1417 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p44_open.json | model_json | 1432 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p44_stage1.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p44_stage2.json | model_json | 1423 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p44_stage3.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p44_stage4.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_p44_stage5.json | model_json | 1424 |
| src/main/resources/assets/domesurvival/models/block/airlock_gate_unformed.json | model_json | 129 |
| src/main/resources/assets/domesurvival/models/block/airlock_panel.json | model_json | 106 |
| src/main/resources/assets/domesurvival/models/block/airlock_panel_closed.json | model_json | 106 |
| src/main/resources/assets/domesurvival/models/block/airlock_panel_open.json | model_json | 107 |
| src/main/resources/assets/domesurvival/models/block/basic_energy_pipe_arm.json | model_json | 5384 |
| src/main/resources/assets/domesurvival/models/block/basic_energy_pipe_core.json | model_json | 7541 |
| src/main/resources/assets/domesurvival/models/block/basic_energy_pipe_inventory.json | model_json | 5435 |
| src/main/resources/assets/domesurvival/models/block/basic_fluid_pipe_arm.json | model_json | 1319 |
| src/main/resources/assets/domesurvival/models/block/basic_fluid_pipe_core.json | model_json | 1320 |
| src/main/resources/assets/domesurvival/models/block/basic_fluid_pipe_inventory.json | model_json | 5433 |
| src/main/resources/assets/domesurvival/models/block/bioincubator.json | model_json | 38571 |
| src/main/resources/assets/domesurvival/models/block/bioincubator_input_port_down.json | model_json | 1175 |
| src/main/resources/assets/domesurvival/models/block/bioincubator_input_port_east.json | model_json | 2308 |
| src/main/resources/assets/domesurvival/models/block/bioincubator_input_port_north.json | model_json | 539 |
| src/main/resources/assets/domesurvival/models/block/bioincubator_input_port_south.json | model_json | 2309 |
| src/main/resources/assets/domesurvival/models/block/bioincubator_input_port_up.json | model_json | 3833 |
| src/main/resources/assets/domesurvival/models/block/bioincubator_input_port_west.json | model_json | 2300 |
| src/main/resources/assets/domesurvival/models/block/bioincubator_lit.json | model_json | 38567 |
| src/main/resources/assets/domesurvival/models/block/bioincubator_on.json | model_json | 457 |
| src/main/resources/assets/domesurvival/models/block/bioincubator_output_port_down.json | model_json | 1175 |
| src/main/resources/assets/domesurvival/models/block/bioincubator_output_port_east.json | model_json | 2308 |
| src/main/resources/assets/domesurvival/models/block/bioincubator_output_port_north.json | model_json | 539 |
| src/main/resources/assets/domesurvival/models/block/bioincubator_output_port_south.json | model_json | 2309 |
| src/main/resources/assets/domesurvival/models/block/bioincubator_output_port_up.json | model_json | 3833 |
| src/main/resources/assets/domesurvival/models/block/bioincubator_output_port_west.json | model_json | 2300 |
| src/main/resources/assets/domesurvival/models/block/coal_generator.json | model_json | 235 |
| src/main/resources/assets/domesurvival/models/block/coal_generator_energy_port_down.json | model_json | 505 |
| src/main/resources/assets/domesurvival/models/block/coal_generator_energy_port_east.json | model_json | 506 |
| src/main/resources/assets/domesurvival/models/block/coal_generator_energy_port_north.json | model_json | 506 |
| src/main/resources/assets/domesurvival/models/block/coal_generator_energy_port_south.json | model_json | 507 |
| src/main/resources/assets/domesurvival/models/block/coal_generator_energy_port_up.json | model_json | 504 |
| src/main/resources/assets/domesurvival/models/block/coal_generator_energy_port_west.json | model_json | 505 |
| src/main/resources/assets/domesurvival/models/block/coal_generator_fuel_port_up.json | model_json | 502 |
| src/main/resources/assets/domesurvival/models/block/coal_generator_item_port_down.json | model_json | 503 |
| src/main/resources/assets/domesurvival/models/block/coal_generator_lit.json | model_json | 238 |
| src/main/resources/assets/domesurvival/models/block/coke_oven_bottom_output.json | model_json | 862 |
| src/main/resources/assets/domesurvival/models/block/coke_oven_item.json | model_json | 14414 |
| src/main/resources/assets/domesurvival/models/block/coke_oven_lower.json | model_json | 7825 |
| src/main/resources/assets/domesurvival/models/block/coke_oven_lower_on.json | model_json | 7801 |
| src/main/resources/assets/domesurvival/models/block/coke_oven_ready.json | model_json | 130327 |
| src/main/resources/assets/domesurvival/models/block/coke_oven_ready_on.json | model_json | 132840 |
| src/main/resources/assets/domesurvival/models/block/coke_oven_rear_input.json | model_json | 856 |
| src/main/resources/assets/domesurvival/models/block/coke_oven_upper.json | model_json | 8731 |
| src/main/resources/assets/domesurvival/models/block/coke_oven_upper_on.json | model_json | 8707 |
| src/main/resources/assets/domesurvival/models/block/copper_furnace.json | model_json | 29676 |
| src/main/resources/assets/domesurvival/models/block/copper_furnace_on.json | model_json | 29652 |
| src/main/resources/assets/domesurvival/models/block/copper_hopper.json | model_json | 30301 |
| src/main/resources/assets/domesurvival/models/block/copper_hopper_down.json | model_json | 30304 |
| src/main/resources/assets/domesurvival/models/block/copper_item_pipe_arm.json | model_json | 5411 |
| src/main/resources/assets/domesurvival/models/block/copper_item_pipe_core.json | model_json | 7577 |
| src/main/resources/assets/domesurvival/models/block/copper_item_pipe_inventory.json | model_json | 19641 |
| src/main/resources/assets/domesurvival/models/block/copper_item_pipe_item_corner_1to1.json | model_json | 447 |
| src/main/resources/assets/domesurvival/models/block/copper_item_pipe_item_world_1to1.json | model_json | 449 |
| src/main/resources/assets/domesurvival/models/block/deepslate_goteium_ore.json | model_json | 118 |
| src/main/resources/assets/domesurvival/models/block/deepslate_lead_ore.json | model_json | 115 |
| src/main/resources/assets/domesurvival/models/block/deepslate_nickel_ore.json | model_json | 117 |
| src/main/resources/assets/domesurvival/models/block/deepslate_silver_ore.json | model_json | 117 |
| src/main/resources/assets/domesurvival/models/block/deepslate_solarite_ore.json | model_json | 119 |
| src/main/resources/assets/domesurvival/models/block/deepslate_tin_ore.json | model_json | 114 |
| src/main/resources/assets/domesurvival/models/block/deepslate_voltarium_ore.json | model_json | 120 |
| src/main/resources/assets/domesurvival/models/block/desh_hopper.json | model_json | 42293 |
| src/main/resources/assets/domesurvival/models/block/desh_hopper_down.json | model_json | 42372 |
| src/main/resources/assets/domesurvival/models/block/desh_item_pipe_arm.json | model_json | 5405 |
| src/main/resources/assets/domesurvival/models/block/desh_item_pipe_core.json | model_json | 7569 |
| src/main/resources/assets/domesurvival/models/block/desh_item_pipe_inventory.json | model_json | 19633 |
| src/main/resources/assets/domesurvival/models/block/desh_item_pipe_item_corner_1to1.json | model_json | 441 |
| src/main/resources/assets/domesurvival/models/block/desh_item_pipe_item_world_1to1.json | model_json | 443 |
| src/main/resources/assets/domesurvival/models/block/dome_foundation.json | model_json | 106 |
| src/main/resources/assets/domesurvival/models/block/dome_frame.json | model_json | 104 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer.json | model_json | 460 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_0.json | model_json | 451 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_1.json | model_json | 451 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_2.json | model_json | 451 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_3.json | model_json | 451 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_4.json | model_json | 451 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_5.json | model_json | 451 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_6.json | model_json | 451 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_7.json | model_json | 451 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_8.json | model_json | 451 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_adamantium_0.json | model_json | 466 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_adamantium_1.json | model_json | 466 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_adamantium_2.json | model_json | 466 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_adamantium_3.json | model_json | 466 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_adamantium_4.json | model_json | 466 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_base_0.json | model_json | 750 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_base_1.json | model_json | 750 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_base_2.json | model_json | 750 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_base_3.json | model_json | 750 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_base_4.json | model_json | 750 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_base_5.json | model_json | 750 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_base_6.json | model_json | 750 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_base_7.json | model_json | 750 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_base_8.json | model_json | 750 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_creative.json | model_json | 460 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_overlay_down_input.json | model_json | 24 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_overlay_down_output.json | model_json | 24 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_overlay_east_input.json | model_json | 24 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_overlay_east_output.json | model_json | 24 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_overlay_north_input.json | model_json | 24 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_overlay_north_output.json | model_json | 24 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_overlay_south_input.json | model_json | 24 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_overlay_south_output.json | model_json | 24 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_overlay_up_input.json | model_json | 24 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_overlay_up_output.json | model_json | 24 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_overlay_west_input.json | model_json | 24 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_overlay_west_output.json | model_json | 24 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_swapped.json | model_json | 460 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_titan_0.json | model_json | 461 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_titan_1.json | model_json | 461 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_titan_2.json | model_json | 461 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_titan_3.json | model_json | 461 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_titan_4.json | model_json | 461 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_v39_0.json | model_json | 453 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_v39_1.json | model_json | 453 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_v39_2.json | model_json | 453 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_v39_3.json | model_json | 453 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_v39_4.json | model_json | 453 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_v39_5.json | model_json | 453 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_v39_6.json | model_json | 453 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_v39_7.json | model_json | 453 |
| src/main/resources/assets/domesurvival/models/block/energy_buffer_v39_8.json | model_json | 453 |
| src/main/resources/assets/domesurvival/models/block/filtering_item_pipe_arm.json | model_json | 5420 |
| src/main/resources/assets/domesurvival/models/block/filtering_item_pipe_core.json | model_json | 7985 |
| src/main/resources/assets/domesurvival/models/block/filtering_item_pipe_inventory.json | model_json | 20061 |
| src/main/resources/assets/domesurvival/models/block/filtering_item_pipe_item_corner_1to1.json | model_json | 456 |
| src/main/resources/assets/domesurvival/models/block/filtering_item_pipe_item_world_1to1.json | model_json | 458 |
| src/main/resources/assets/domesurvival/models/block/filter_regeneration_station.json | model_json | 246 |
| src/main/resources/assets/domesurvival/models/block/filter_regeneration_station_active.json | model_json | 253 |
| src/main/resources/assets/domesurvival/models/block/forming_press.json | model_json | 232 |
| src/main/resources/assets/domesurvival/models/block/forming_press_active.json | model_json | 239 |
| src/main/resources/assets/domesurvival/models/block/forming_press_energy_port_down.json | model_json | 223 |
| src/main/resources/assets/domesurvival/models/block/forming_press_energy_port_east.json | model_json | 224 |
| src/main/resources/assets/domesurvival/models/block/forming_press_energy_port_north.json | model_json | 224 |
| src/main/resources/assets/domesurvival/models/block/forming_press_energy_port_south.json | model_json | 225 |
| src/main/resources/assets/domesurvival/models/block/forming_press_energy_port_up.json | model_json | 222 |
| src/main/resources/assets/domesurvival/models/block/forming_press_energy_port_west.json | model_json | 223 |
| src/main/resources/assets/domesurvival/models/block/forming_press_input_port_down.json | model_json | 215 |
| src/main/resources/assets/domesurvival/models/block/forming_press_input_port_east.json | model_json | 216 |
| src/main/resources/assets/domesurvival/models/block/forming_press_input_port_north.json | model_json | 216 |
| src/main/resources/assets/domesurvival/models/block/forming_press_input_port_south.json | model_json | 217 |
| src/main/resources/assets/domesurvival/models/block/forming_press_input_port_up.json | model_json | 214 |
| src/main/resources/assets/domesurvival/models/block/forming_press_input_port_west.json | model_json | 215 |
| src/main/resources/assets/domesurvival/models/block/forming_press_item_port_down.json | model_json | 221 |
| src/main/resources/assets/domesurvival/models/block/forming_press_item_port_east.json | model_json | 222 |
| src/main/resources/assets/domesurvival/models/block/forming_press_item_port_north.json | model_json | 222 |
| src/main/resources/assets/domesurvival/models/block/forming_press_item_port_south.json | model_json | 223 |
| src/main/resources/assets/domesurvival/models/block/forming_press_item_port_up.json | model_json | 220 |
| src/main/resources/assets/domesurvival/models/block/forming_press_item_port_west.json | model_json | 221 |
| src/main/resources/assets/domesurvival/models/block/forming_press_output_port_down.json | model_json | 217 |
| src/main/resources/assets/domesurvival/models/block/forming_press_output_port_east.json | model_json | 218 |
| src/main/resources/assets/domesurvival/models/block/forming_press_output_port_north.json | model_json | 218 |
| src/main/resources/assets/domesurvival/models/block/forming_press_output_port_south.json | model_json | 219 |
| src/main/resources/assets/domesurvival/models/block/forming_press_output_port_up.json | model_json | 216 |
| src/main/resources/assets/domesurvival/models/block/forming_press_output_port_west.json | model_json | 217 |
| src/main/resources/assets/domesurvival/models/block/goteium_block.json | model_json | 109 |
| src/main/resources/assets/domesurvival/models/block/goteium_ore.json | model_json | 108 |
| src/main/resources/assets/domesurvival/models/block/high_flow_oxygen_pipe_connection.json | model_json | 1265 |
| src/main/resources/assets/domesurvival/models/block/high_flow_oxygen_pipe_core.json | model_json | 1393 |
| src/main/resources/assets/domesurvival/models/block/high_flow_oxygen_pipe_inventory.json | model_json | 6617 |
| src/main/resources/assets/domesurvival/models/block/high_pressure_fluid_pipe_arm.json | model_json | 1335 |
| src/main/resources/assets/domesurvival/models/block/high_pressure_fluid_pipe_core.json | model_json | 1336 |
| src/main/resources/assets/domesurvival/models/block/high_pressure_fluid_pipe_inventory.json | model_json | 5449 |
| src/main/resources/assets/domesurvival/models/block/high_voltage_energy_pipe_arm.json | model_json | 5405 |
| src/main/resources/assets/domesurvival/models/block/high_voltage_energy_pipe_core.json | model_json | 7569 |
| src/main/resources/assets/domesurvival/models/block/high_voltage_energy_pipe_inventory.json | model_json | 5449 |
| src/main/resources/assets/domesurvival/models/block/lanos_abandoned.json | model_json | 171993 |
| src/main/resources/assets/domesurvival/models/block/lanos_abandoned.mtl | obj_material | 194 |
| src/main/resources/assets/domesurvival/models/block/lanos_abandoned.obj | obj_mesh | 220704 |
| src/main/resources/assets/domesurvival/models/block/lanos_decorative.json | model_json | 156316 |
| src/main/resources/assets/domesurvival/models/block/lanos_decorative.mtl | obj_material | 194 |
| src/main/resources/assets/domesurvival/models/block/lanos_decorative.obj | obj_mesh | 201372 |
| src/main/resources/assets/domesurvival/models/block/lanos_hitbox_part.json | model_json | 87 |
| src/main/resources/assets/domesurvival/models/block/large_coke_oven.json | model_json | 278 |
| src/main/resources/assets/domesurvival/models/block/large_coke_oven.mtl | obj_material | 316 |
| src/main/resources/assets/domesurvival/models/block/large_coke_oven.obj | obj_mesh | 338265 |
| src/main/resources/assets/domesurvival/models/block/large_coke_oven_fire.json | model_json | 404 |
| src/main/resources/assets/domesurvival/models/block/large_coke_oven_fire_off.json | model_json | 390 |
| src/main/resources/assets/domesurvival/models/block/large_coke_oven_input_port.json | model_json | 336 |
| src/main/resources/assets/domesurvival/models/block/large_coke_oven_output_port.json | model_json | 340 |
| src/main/resources/assets/domesurvival/models/block/lead_block.json | model_json | 106 |
| src/main/resources/assets/domesurvival/models/block/lead_ore.json | model_json | 105 |
| src/main/resources/assets/domesurvival/models/block/machine_input_port_down.json | model_json | 503 |
| src/main/resources/assets/domesurvival/models/block/machine_input_port_east.json | model_json | 504 |
| src/main/resources/assets/domesurvival/models/block/machine_input_port_north.json | model_json | 504 |
| src/main/resources/assets/domesurvival/models/block/machine_input_port_south.json | model_json | 505 |
| src/main/resources/assets/domesurvival/models/block/machine_input_port_up.json | model_json | 502 |
| src/main/resources/assets/domesurvival/models/block/machine_input_port_west.json | model_json | 503 |
| src/main/resources/assets/domesurvival/models/block/machine_output_port_down.json | model_json | 503 |
| src/main/resources/assets/domesurvival/models/block/machine_output_port_east.json | model_json | 504 |
| src/main/resources/assets/domesurvival/models/block/machine_output_port_north.json | model_json | 504 |
| src/main/resources/assets/domesurvival/models/block/machine_output_port_south.json | model_json | 505 |
| src/main/resources/assets/domesurvival/models/block/machine_output_port_up.json | model_json | 502 |
| src/main/resources/assets/domesurvival/models/block/machine_output_port_west.json | model_json | 503 |
| src/main/resources/assets/domesurvival/models/block/machine_stabilizer.json | model_json | 73593 |
| src/main/resources/assets/domesurvival/models/block/nickel_block.json | model_json | 108 |
| src/main/resources/assets/domesurvival/models/block/nickel_ore.json | model_json | 107 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_air_intake_formed_off.json | model_json | 31686 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_air_intake_formed_on.json | model_json | 31674 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_air_intake_off.json | model_json | 272 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_air_intake_on.json | model_json | 271 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_air_intake_unformed_off.json | model_json | 31686 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_air_intake_unformed_on.json | model_json | 31674 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_compression_formed_off.json | model_json | 31636 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_compression_formed_on.json | model_json | 31618 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_compression_off.json | model_json | 275 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_compression_on.json | model_json | 274 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_compression_unformed_off.json | model_json | 31636 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_compression_unformed_on.json | model_json | 31618 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_filtration_formed_off.json | model_json | 40166 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_filtration_formed_on.json | model_json | 40094 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_filtration_off.json | model_json | 272 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_filtration_on.json | model_json | 271 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_filtration_unformed_off.json | model_json | 40166 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_filtration_unformed_on.json | model_json | 40094 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_output_formed_off.json | model_json | 25849 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_output_formed_on.json | model_json | 25825 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_output_off.json | model_json | 260 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_output_on.json | model_json | 259 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_output_unformed_off.json | model_json | 25849 |
| src/main/resources/assets/domesurvival/models/block/oxygen_complex_output_unformed_on.json | model_json | 25825 |
| src/main/resources/assets/domesurvival/models/block/oxygen_electrolyzer.json | model_json | 240 |
| src/main/resources/assets/domesurvival/models/block/oxygen_electrolyzer_lit.json | model_json | 243 |
| src/main/resources/assets/domesurvival/models/block/oxygen_filler.json | model_json | 234 |
| src/main/resources/assets/domesurvival/models/block/oxygen_filler_lit.json | model_json | 237 |
| src/main/resources/assets/domesurvival/models/block/oxygen_pipe_connection.json | model_json | 1204 |
| src/main/resources/assets/domesurvival/models/block/oxygen_pipe_core.json | model_json | 1331 |
| src/main/resources/assets/domesurvival/models/block/oxygen_pipe_inventory.json | model_json | 4293 |
| src/main/resources/assets/domesurvival/models/block/reinforced_energy_pipe_arm.json | model_json | 5399 |
| src/main/resources/assets/domesurvival/models/block/reinforced_energy_pipe_core.json | model_json | 7561 |
| src/main/resources/assets/domesurvival/models/block/reinforced_energy_pipe_inventory.json | model_json | 5445 |
| src/main/resources/assets/domesurvival/models/block/reinforced_fluid_pipe_arm.json | model_json | 1329 |
| src/main/resources/assets/domesurvival/models/block/reinforced_fluid_pipe_core.json | model_json | 1330 |
| src/main/resources/assets/domesurvival/models/block/reinforced_fluid_pipe_inventory.json | model_json | 5443 |
| src/main/resources/assets/domesurvival/models/block/reinforced_glass.json | model_json | 136 |
| src/main/resources/assets/domesurvival/models/block/reinforced_oxygen_pipe_connection.json | model_json | 1274 |
| src/main/resources/assets/domesurvival/models/block/reinforced_oxygen_pipe_core.json | model_json | 1402 |
| src/main/resources/assets/domesurvival/models/block/reinforced_oxygen_pipe_inventory.json | model_json | 6645 |
| src/main/resources/assets/domesurvival/models/block/sand_sieve.json | model_json | 3487 |
| src/main/resources/assets/domesurvival/models/block/service_pass_through_empty_z.json | model_json | 5782 |
| src/main/resources/assets/domesurvival/models/block/service_pass_through_z.json | model_json | 5182 |
| src/main/resources/assets/domesurvival/models/block/shaft_furnace_bottom_output.json | model_json | 885 |
| src/main/resources/assets/domesurvival/models/block/shaft_furnace_input_port.json | model_json | 338 |
| src/main/resources/assets/domesurvival/models/block/shaft_furnace_item.json | model_json | 14844 |
| src/main/resources/assets/domesurvival/models/block/shaft_furnace_large.json | model_json | 350 |
| src/main/resources/assets/domesurvival/models/block/shaft_furnace_large.mtl | obj_material | 136 |
| src/main/resources/assets/domesurvival/models/block/shaft_furnace_large.obj | obj_mesh | 269329 |
| src/main/resources/assets/domesurvival/models/block/shaft_furnace_large_on.json | model_json | 353 |
| src/main/resources/assets/domesurvival/models/block/shaft_furnace_lower.json | model_json | 7877 |
| src/main/resources/assets/domesurvival/models/block/shaft_furnace_lower_on.json | model_json | 7853 |
| src/main/resources/assets/domesurvival/models/block/shaft_furnace_output_port.json | model_json | 338 |
| src/main/resources/assets/domesurvival/models/block/shaft_furnace_part_empty.json | model_json | 87 |
| src/main/resources/assets/domesurvival/models/block/shaft_furnace_ready.json | model_json | 358654 |
| src/main/resources/assets/domesurvival/models/block/shaft_furnace_ready_on.json | model_json | 367928 |
| src/main/resources/assets/domesurvival/models/block/shaft_furnace_rear_input.json | model_json | 881 |
| src/main/resources/assets/domesurvival/models/block/shaft_furnace_upper.json | model_json | 9159 |
| src/main/resources/assets/domesurvival/models/block/shaft_furnace_upper_on.json | model_json | 9135 |
| src/main/resources/assets/domesurvival/models/block/silver_block.json | model_json | 108 |
| src/main/resources/assets/domesurvival/models/block/silver_ore.json | model_json | 107 |
| src/main/resources/assets/domesurvival/models/block/solarite_block.json | model_json | 110 |
| src/main/resources/assets/domesurvival/models/block/solarite_ore.json | model_json | 109 |
| src/main/resources/assets/domesurvival/models/block/solar_panel_mk1.json | model_json | 471 |
| src/main/resources/assets/domesurvival/models/block/solar_panel_mk1.mtl | obj_material | 246 |
| src/main/resources/assets/domesurvival/models/block/solar_panel_mk1.obj | obj_mesh | 554988 |
| src/main/resources/assets/domesurvival/models/block/solar_panel_mk2.json | model_json | 471 |
| src/main/resources/assets/domesurvival/models/block/solar_panel_mk2.mtl | obj_material | 246 |
| src/main/resources/assets/domesurvival/models/block/solar_panel_mk2.obj | obj_mesh | 554989 |
| src/main/resources/assets/domesurvival/models/block/solar_panel_mk3.json | model_json | 471 |
| src/main/resources/assets/domesurvival/models/block/solar_panel_mk3.mtl | obj_material | 246 |
| src/main/resources/assets/domesurvival/models/block/solar_panel_mk3.obj | obj_mesh | 554989 |
| src/main/resources/assets/domesurvival/models/block/steel_block.json | model_json | 107 |
| src/main/resources/assets/domesurvival/models/block/steel_hopper.json | model_json | 35075 |
| src/main/resources/assets/domesurvival/models/block/steel_hopper_down.json | model_json | 35116 |
| src/main/resources/assets/domesurvival/models/block/steel_item_pipe_arm.json | model_json | 5408 |
| src/main/resources/assets/domesurvival/models/block/steel_item_pipe_core.json | model_json | 7573 |
| src/main/resources/assets/domesurvival/models/block/steel_item_pipe_inventory.json | model_json | 19637 |
| src/main/resources/assets/domesurvival/models/block/steel_item_pipe_item_corner_1to1.json | model_json | 444 |
| src/main/resources/assets/domesurvival/models/block/steel_item_pipe_item_world_1to1.json | model_json | 446 |
| src/main/resources/assets/domesurvival/models/block/tin_block.json | model_json | 105 |
| src/main/resources/assets/domesurvival/models/block/tin_ore.json | model_json | 104 |
| src/main/resources/assets/domesurvival/models/block/universal_tank.json | model_json | 7289 |
| src/main/resources/assets/domesurvival/models/block/voltarium_block.json | model_json | 111 |
| src/main/resources/assets/domesurvival/models/block/voltarium_ore.json | model_json | 110 |
| src/main/resources/assets/domesurvival/models/block/water_purifier.json | model_json | 235 |
| src/main/resources/assets/domesurvival/models/block/water_purifier_lit.json | model_json | 238 |
| src/main/resources/assets/domesurvival/models/item/airlock_binding_key.json | model_json | 123 |
| src/main/resources/assets/domesurvival/models/item/airlock_control_panel.json | model_json | 1809 |
| src/main/resources/assets/domesurvival/models/item/airlock_gate.json | model_json | 62 |
| src/main/resources/assets/domesurvival/models/item/basic_energy_pipe.json | model_json | 1782 |
| src/main/resources/assets/domesurvival/models/item/basic_fluid_pipe.json | model_json | 1781 |
| src/main/resources/assets/domesurvival/models/item/biogel.json | model_json | 114 |
| src/main/resources/assets/domesurvival/models/item/bioincubator.json | model_json | 1027 |
| src/main/resources/assets/domesurvival/models/item/bio_module.json | model_json | 131 |
| src/main/resources/assets/domesurvival/models/item/bio_repair_kit.json | model_json | 122 |
| src/main/resources/assets/domesurvival/models/item/chicken_cryocapsule.json | model_json | 47 |
| src/main/resources/assets/domesurvival/models/item/coal_coke.json | model_json | 113 |
| src/main/resources/assets/domesurvival/models/item/coal_generator.json | model_json | 55 |
| src/main/resources/assets/domesurvival/models/item/coke_oven.json | model_json | 474784 |
| src/main/resources/assets/domesurvival/models/item/copper_furnace.json | model_json | 1633 |
| src/main/resources/assets/domesurvival/models/item/copper_hopper.json | model_json | 1630 |
| src/main/resources/assets/domesurvival/models/item/copper_item_pipe.json | model_json | 1781 |
| src/main/resources/assets/domesurvival/models/item/copper_plate.json | model_json | 105 |
| src/main/resources/assets/domesurvival/models/item/copper_rod.json | model_json | 4973 |
| src/main/resources/assets/domesurvival/models/item/copper_sieve_mesh.json | model_json | 103 |
| src/main/resources/assets/domesurvival/models/item/copper_tube.json | model_json | 110 |
| src/main/resources/assets/domesurvival/models/item/copper_wire.json | model_json | 11386 |
| src/main/resources/assets/domesurvival/models/item/cow_cryocapsule.json | model_json | 47 |
| src/main/resources/assets/domesurvival/models/item/damaged_pig_cryocapsule.json | model_json | 47 |
| src/main/resources/assets/domesurvival/models/item/deepslate_goteium_ore.json | model_json | 59 |
| src/main/resources/assets/domesurvival/models/item/deepslate_lead_ore.json | model_json | 56 |
| src/main/resources/assets/domesurvival/models/item/deepslate_nickel_ore.json | model_json | 58 |
| src/main/resources/assets/domesurvival/models/item/deepslate_silver_ore.json | model_json | 58 |
| src/main/resources/assets/domesurvival/models/item/deepslate_solarite_ore.json | model_json | 60 |
| src/main/resources/assets/domesurvival/models/item/deepslate_tin_ore.json | model_json | 55 |
| src/main/resources/assets/domesurvival/models/item/deepslate_voltarium_ore.json | model_json | 61 |
| src/main/resources/assets/domesurvival/models/item/desh_hopper.json | model_json | 1628 |
| src/main/resources/assets/domesurvival/models/item/desh_item_pipe.json | model_json | 1779 |
| src/main/resources/assets/domesurvival/models/item/dome_foundation.json | model_json | 53 |
| src/main/resources/assets/domesurvival/models/item/dome_frame.json | model_json | 48 |
| src/main/resources/assets/domesurvival/models/item/energy_buffer.json | model_json | 58 |
| src/main/resources/assets/domesurvival/models/item/energy_buffer_adamantium.json | model_json | 67 |
| src/main/resources/assets/domesurvival/models/item/energy_buffer_creative.json | model_json | 61 |
| src/main/resources/assets/domesurvival/models/item/energy_buffer_titan.json | model_json | 62 |
| src/main/resources/assets/domesurvival/models/item/fiber_sieve_mesh.json | model_json | 103 |
| src/main/resources/assets/domesurvival/models/item/filtering_item_pipe.json | model_json | 1784 |
| src/main/resources/assets/domesurvival/models/item/filter_regeneration_media.json | model_json | 118 |
| src/main/resources/assets/domesurvival/models/item/filter_regeneration_station.json | model_json | 66 |
| src/main/resources/assets/domesurvival/models/item/forming_press.json | model_json | 47 |
| src/main/resources/assets/domesurvival/models/item/goteium_block.json | model_json | 50 |
| src/main/resources/assets/domesurvival/models/item/goteium_gear.json | model_json | 111 |
| src/main/resources/assets/domesurvival/models/item/goteium_ingot.json | model_json | 112 |
| src/main/resources/assets/domesurvival/models/item/goteium_nugget.json | model_json | 113 |
| src/main/resources/assets/domesurvival/models/item/goteium_ore.json | model_json | 49 |
| src/main/resources/assets/domesurvival/models/item/goteium_plate.json | model_json | 112 |
| src/main/resources/assets/domesurvival/models/item/high_flow_oxygen_pipe.json | model_json | 1645 |
| src/main/resources/assets/domesurvival/models/item/high_pressure_fluid_pipe.json | model_json | 1789 |
| src/main/resources/assets/domesurvival/models/item/high_voltage_energy_pipe.json | model_json | 1789 |
| src/main/resources/assets/domesurvival/models/item/hopper_upgrade_copper_to_steel.json | model_json | 135 |
| src/main/resources/assets/domesurvival/models/item/hopper_upgrade_steel_to_desh.json | model_json | 133 |
| src/main/resources/assets/domesurvival/models/item/hopper_upgrade_vanilla_to_copper.json | model_json | 137 |
| src/main/resources/assets/domesurvival/models/item/improved_water_filter.json | model_json | 41549 |
| src/main/resources/assets/domesurvival/models/item/industrial_water_filter.json | model_json | 41551 |
| src/main/resources/assets/domesurvival/models/item/lanos_abandoned.json | model_json | 306 |
| src/main/resources/assets/domesurvival/models/item/lanos_decorative.json | model_json | 307 |
| src/main/resources/assets/domesurvival/models/item/large_oxygen_tank.json | model_json | 110 |
| src/main/resources/assets/domesurvival/models/item/lead_block.json | model_json | 47 |
| src/main/resources/assets/domesurvival/models/item/lead_gear.json | model_json | 126 |
| src/main/resources/assets/domesurvival/models/item/lead_ingot.json | model_json | 109 |
| src/main/resources/assets/domesurvival/models/item/lead_nugget.json | model_json | 110 |
| src/main/resources/assets/domesurvival/models/item/lead_ore.json | model_json | 46 |
| src/main/resources/assets/domesurvival/models/item/machine_stabilizer.json | model_json | 1639 |
| src/main/resources/assets/domesurvival/models/item/machine_wrench.json | model_json | 110070 |
| src/main/resources/assets/domesurvival/models/item/machine_wrench_create.mtl | obj_material | 249 |
| src/main/resources/assets/domesurvival/models/item/machine_wrench_create.obj | obj_mesh | 42986 |
| src/main/resources/assets/domesurvival/models/item/medium_oxygen_tank.json | model_json | 111 |
| src/main/resources/assets/domesurvival/models/item/memory_painting.json | model_json | 114 |
| src/main/resources/assets/domesurvival/models/item/nickel_block.json | model_json | 49 |
| src/main/resources/assets/domesurvival/models/item/nickel_gear.json | model_json | 128 |
| src/main/resources/assets/domesurvival/models/item/nickel_ingot.json | model_json | 111 |
| src/main/resources/assets/domesurvival/models/item/nickel_nugget.json | model_json | 112 |
| src/main/resources/assets/domesurvival/models/item/nickel_ore.json | model_json | 48 |
| src/main/resources/assets/domesurvival/models/item/nickel_plate.json | model_json | 111 |
| src/main/resources/assets/domesurvival/models/item/nickel_tube.json | model_json | 110 |
| src/main/resources/assets/domesurvival/models/item/nutrient_mix.json | model_json | 120 |
| src/main/resources/assets/domesurvival/models/item/oxygen_complex_air_intake.json | model_json | 1788 |
| src/main/resources/assets/domesurvival/models/item/oxygen_complex_compression.json | model_json | 1789 |
| src/main/resources/assets/domesurvival/models/item/oxygen_complex_filtration.json | model_json | 1788 |
| src/main/resources/assets/domesurvival/models/item/oxygen_complex_output.json | model_json | 1784 |
| src/main/resources/assets/domesurvival/models/item/oxygen_electrolyzer.json | model_json | 60 |
| src/main/resources/assets/domesurvival/models/item/oxygen_filler.json | model_json | 54 |
| src/main/resources/assets/domesurvival/models/item/oxygen_mask.json | model_json | 106 |
| src/main/resources/assets/domesurvival/models/item/oxygen_pipe.json | model_json | 1635 |
| src/main/resources/assets/domesurvival/models/item/pulse_matrix.json | model_json | 135090 |
| src/main/resources/assets/domesurvival/models/item/raw_goteium.json | model_json | 110 |
| src/main/resources/assets/domesurvival/models/item/raw_lead.json | model_json | 107 |
| src/main/resources/assets/domesurvival/models/item/raw_nickel.json | model_json | 109 |
| src/main/resources/assets/domesurvival/models/item/raw_silver.json | model_json | 109 |
| src/main/resources/assets/domesurvival/models/item/raw_tin.json | model_json | 106 |
| src/main/resources/assets/domesurvival/models/item/raw_voltarium.json | model_json | 112 |
| src/main/resources/assets/domesurvival/models/item/reinforced_energy_pipe.json | model_json | 1787 |
| src/main/resources/assets/domesurvival/models/item/reinforced_fluid_pipe.json | model_json | 1786 |
| src/main/resources/assets/domesurvival/models/item/reinforced_glass.json | model_json | 54 |
| src/main/resources/assets/domesurvival/models/item/reinforced_oxygen_pipe.json | model_json | 1646 |
| src/main/resources/assets/domesurvival/models/item/sand_sieve.json | model_json | 48 |
| src/main/resources/assets/domesurvival/models/item/service_pass_through.json | model_json | 1775 |
| src/main/resources/assets/domesurvival/models/item/shaft_furnace.json | model_json | 366165 |
| src/main/resources/assets/domesurvival/models/item/sheep_cryocapsule.json | model_json | 47 |
| src/main/resources/assets/domesurvival/models/item/silver_block.json | model_json | 49 |
| src/main/resources/assets/domesurvival/models/item/silver_ingot.json | model_json | 111 |
| src/main/resources/assets/domesurvival/models/item/silver_nugget.json | model_json | 112 |
| src/main/resources/assets/domesurvival/models/item/silver_ore.json | model_json | 48 |
| src/main/resources/assets/domesurvival/models/item/silver_rod.json | model_json | 4973 |
| src/main/resources/assets/domesurvival/models/item/silver_wire.json | model_json | 11386 |
| src/main/resources/assets/domesurvival/models/item/slag.json | model_json | 95 |
| src/main/resources/assets/domesurvival/models/item/small_oxygen_tank.json | model_json | 112 |
| src/main/resources/assets/domesurvival/models/item/solarite_block.json | model_json | 51 |
| src/main/resources/assets/domesurvival/models/item/solarite_crystal.json | model_json | 114 |
| src/main/resources/assets/domesurvival/models/item/solarite_ore.json | model_json | 50 |
| src/main/resources/assets/domesurvival/models/item/solarite_shard.json | model_json | 112 |
| src/main/resources/assets/domesurvival/models/item/solar_panel_mk1.json | model_json | 118 |
| src/main/resources/assets/domesurvival/models/item/solar_panel_mk2.json | model_json | 118 |
| src/main/resources/assets/domesurvival/models/item/solar_panel_mk3.json | model_json | 118 |
| src/main/resources/assets/domesurvival/models/item/steel_block.json | model_json | 48 |
| src/main/resources/assets/domesurvival/models/item/steel_gear.json | model_json | 127 |
| src/main/resources/assets/domesurvival/models/item/steel_hopper.json | model_json | 1629 |
| src/main/resources/assets/domesurvival/models/item/steel_ingot.json | model_json | 115 |
| src/main/resources/assets/domesurvival/models/item/steel_item_pipe.json | model_json | 1780 |
| src/main/resources/assets/domesurvival/models/item/steel_nugget.json | model_json | 110 |
| src/main/resources/assets/domesurvival/models/item/steel_plate.json | model_json | 110 |
| src/main/resources/assets/domesurvival/models/item/steel_rod.json | model_json | 4971 |
| src/main/resources/assets/domesurvival/models/item/steel_sieve_mesh.json | model_json | 103 |
| src/main/resources/assets/domesurvival/models/item/steel_tube.json | model_json | 109 |
| src/main/resources/assets/domesurvival/models/item/steel_wire.json | model_json | 11384 |
| src/main/resources/assets/domesurvival/models/item/surface_suit_boots.json | model_json | 123 |
| src/main/resources/assets/domesurvival/models/item/surface_suit_chestplate.json | model_json | 128 |
| src/main/resources/assets/domesurvival/models/item/surface_suit_helmet.json | model_json | 124 |
| src/main/resources/assets/domesurvival/models/item/surface_suit_leggings.json | model_json | 126 |
| src/main/resources/assets/domesurvival/models/item/tin_block.json | model_json | 46 |
| src/main/resources/assets/domesurvival/models/item/tin_gear.json | model_json | 125 |
| src/main/resources/assets/domesurvival/models/item/tin_ingot.json | model_json | 108 |
| src/main/resources/assets/domesurvival/models/item/tin_nugget.json | model_json | 109 |
| src/main/resources/assets/domesurvival/models/item/tin_ore.json | model_json | 45 |
| src/main/resources/assets/domesurvival/models/item/tin_plate.json | model_json | 102 |
| src/main/resources/assets/domesurvival/models/item/tin_tube.json | model_json | 107 |
| src/main/resources/assets/domesurvival/models/item/universal_tank.json | model_json | 1754 |
| src/main/resources/assets/domesurvival/models/item/voltarium_block.json | model_json | 52 |
| src/main/resources/assets/domesurvival/models/item/voltarium_gear.json | model_json | 113 |
| src/main/resources/assets/domesurvival/models/item/voltarium_ingot.json | model_json | 114 |
| src/main/resources/assets/domesurvival/models/item/voltarium_nugget.json | model_json | 115 |
| src/main/resources/assets/domesurvival/models/item/voltarium_ore.json | model_json | 51 |
| src/main/resources/assets/domesurvival/models/item/voltarium_plate.json | model_json | 114 |
| src/main/resources/assets/domesurvival/models/item/voltarium_rod.json | model_json | 4979 |
| src/main/resources/assets/domesurvival/models/item/voltarium_wire.json | model_json | 11392 |
| src/main/resources/assets/domesurvival/models/item/water_filter_cartridge.json | model_json | 41563 |
| src/main/resources/assets/domesurvival/models/item/water_purifier.json | model_json | 55 |
| src/main/resources/assets/domesurvival/particles/acid_rain_streak.json | particle_json | 55 |
| src/main/resources/assets/domesurvival/particles/item_pipe_packet.json | particle_json | 65 |
| src/main/resources/assets/domesurvival/particles/sandstorm_mote.json | particle_json | 53 |
| src/main/resources/assets/domesurvival/particles/ventilation_bubble.json | particle_json | 67 |
| src/main/resources/assets/domesurvival/textures/block/airlock_control_panel.png | texture_png | 64×256 |
| src/main/resources/assets/domesurvival/textures/block/airlock_control_panel.png.mcmeta | texture_metadata | 160 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/master.png | texture_png | 80×80 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/master_back.png | texture_png | 80×80 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p00.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p00_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p01.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p01_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p02.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p02_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p03.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p03_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p04.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p04_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p10.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p10_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p11.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p11_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p12.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p12_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p13.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p13_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p14.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p14_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p20.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p20_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p21.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p21_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p22.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p22_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p23.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p23_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p24.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p24_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p30.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p30_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p31.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p31_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p32.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p32_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p33.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p33_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p34.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p34_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p40.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p40_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p41.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p41_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p42.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p42_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p43.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p43_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p44.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/p44_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s2/p00.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s2/p00_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s2/p01.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s2/p01_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s2/p10.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s2/p10_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s2/p11.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s2/p11_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s3/p00.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s3/p00_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s3/p01.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s3/p01_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s3/p02.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s3/p02_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s3/p10.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s3/p10_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s3/p11.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s3/p11_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s3/p12.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s3/p12_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s3/p20.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s3/p20_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s3/p21.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s3/p21_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s3/p22.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s3/p22_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p00.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p00_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p01.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p01_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p02.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p02_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p03.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p03_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p10.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p10_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p11.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p11_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p12.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p12_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p13.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p13_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p20.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p20_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p21.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p21_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p22.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p22_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p23.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p23_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p30.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p30_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p31.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p31_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p32.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p32_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p33.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/s4/p33_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/segment.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate/side.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/airlock_gate_unformed_dark.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/basic_energy_pipe.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/basic_energy_pipe_arm.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/basic_energy_pipe_core.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/basic_energy_pipe_detail.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/basic_fluid_pipe.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/bfbricks.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/bfbricksdark.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/bfbrickslit.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/bftoolshot.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/bftoolst.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_casing.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_cyan.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_dna.png | texture_png | 32×128 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_dna.png.mcmeta | texture_metadata | 145 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_dna_off.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_first_base.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_first_casing.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_first_connector_pad.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_first_cyan.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_first_dna.png | texture_png | 32×128 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_first_dna.png.mcmeta | texture_metadata | 146 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_first_dna_off.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_first_frame.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_first_inset.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_first_top.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_frame.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_front.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_front_on.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_panel.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_red.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_side.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_top.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_top_detailed.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/bio/bioincubator_vent.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/campfire_log_lit.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/coal_generator_front.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/coal_generator_front_on.png | texture_png | 16×64 |
| src/main/resources/assets/domesurvival/textures/block/coal_generator_front_on.png.mcmeta | texture_metadata | 133 |
| src/main/resources/assets/domesurvival/textures/block/coal_generator_port_energy.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/coal_generator_port_fuel.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/coal_generator_port_item.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/coal_generator_side.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/coal_generator_top.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/copper_furnace/copper_furnace_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/copper_furnace/copper_furnace_body.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/copper_furnace/copper_furnace_bottom.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/copper_furnace/copper_furnace_door.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/copper_furnace/copper_furnace_fire.png | texture_png | 16×64 |
| src/main/resources/assets/domesurvival/textures/block/copper_furnace/copper_furnace_fire.png.mcmeta | texture_metadata | 160 |
| src/main/resources/assets/domesurvival/textures/block/copper_furnace/copper_furnace_fire_off.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/copper_furnace/copper_furnace_gauge.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/copper_furnace/copper_furnace_iron.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/copper_furnace/copper_furnace_rim.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/copper_furnace/copper_furnace_soot.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/copper_furnace/copper_furnace_top.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/copper_furnace/copper_furnace_vent.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/deepslate_goteium_ore.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/deepslate_lead_ore.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/deepslate_nickel_ore.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/deepslate_silver_ore.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/deepslate_solarite_ore.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/deepslate_tin_ore.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/deepslate_voltarium_ore.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/bottom.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/connector_input.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/connector_off.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/connector_output.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/front.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/front_0.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/front_1.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/front_2.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/front_3.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/front_4.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/front_5.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/front_6.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/front_7.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/front_8.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/overlay_input.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/overlay_output.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/side.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/side_base.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/side_disabled.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/side_input.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/side_output.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/side_panel.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer/top.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer_adamantium/front_0.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer_adamantium/front_1.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer_adamantium/front_2.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer_adamantium/front_3.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer_adamantium/front_4.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer_creative/front.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer_titan/front_0.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer_titan/front_1.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer_titan/front_2.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer_titan/front_3.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/energy_buffer_titan/front_4.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/filter_regeneration_station_back.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/filter_regeneration_station_bottom.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/filter_regeneration_station_front.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/filter_regeneration_station_front_active.png | texture_png | 32×256 |
| src/main/resources/assets/domesurvival/textures/block/filter_regeneration_station_front_active.png.mcmeta | texture_metadata | 115 |
| src/main/resources/assets/domesurvival/textures/block/filter_regeneration_station_side.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/filter_regeneration_station_top.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/filter_regenerator_black.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/filter_regenerator_front.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/filter_regenerator_hazard.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/filter_regenerator_lamp_green.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/filter_regenerator_lamp_green_active.png | texture_png | 16×96 |
| src/main/resources/assets/domesurvival/textures/block/filter_regenerator_lamp_green_active.png.mcmeta | texture_metadata | 181 |
| src/main/resources/assets/domesurvival/textures/block/filter_regenerator_lamp_orange.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/filter_regenerator_mesh.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/filter_regenerator_mesh_active.png | texture_png | 16×128 |
| src/main/resources/assets/domesurvival/textures/block/filter_regenerator_mesh_active.png.mcmeta | texture_metadata | 163 |
| src/main/resources/assets/domesurvival/textures/block/filter_regenerator_metal.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/filter_regenerator_metal_dark.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/filter_regenerator_side.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/filter_regenerator_top.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/filter_regenerator_vent.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/firetools.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/forming_press_front.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/forming_press_front_active.png | texture_png | 16×64 |
| src/main/resources/assets/domesurvival/textures/block/forming_press_front_active.png.mcmeta | texture_metadata | 84 |
| src/main/resources/assets/domesurvival/textures/block/forming_press_port_energy.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/forming_press_port_item.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/forming_press_side.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/forming_press_top.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/goteium_block.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/goteium_ore.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/high_pressure_fluid_pipe.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/high_voltage_energy_pipe.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/high_voltage_energy_pipe_arm.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/high_voltage_energy_pipe_core.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/high_voltage_energy_pipe_detail.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/copper_accent.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/copper_body.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/copper_connector.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/copper_dark.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/copper_inside.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/copper_panel.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/copper_rim.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/desh_accent.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/desh_body.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/desh_connector.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/desh_dark.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/desh_inside.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/desh_panel.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/desh_rim.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/steel_accent.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/steel_body.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/steel_connector.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/steel_dark.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/steel_inside.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/steel_panel.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/hopper/steel_rim.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/connector_disabled.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/connector_frame.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/connector_input.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/connector_output.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/copper_item_pipe.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/copper_item_pipe_core.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/copper_item_pipe_detail.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/desh_item_pipe.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/desh_item_pipe_core.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/desh_item_pipe_detail.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/filtering_item_pipe.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/filtering_item_pipe_core.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/filtering_item_pipe_detail.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/route_down.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/route_east.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/route_north.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/route_south.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/route_up.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/route_west.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/steel_item_pipe.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/steel_item_pipe_core.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/item_pipe/steel_item_pipe_detail.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/lanos_abandoned.png | texture_png | 256×256 |
| src/main/resources/assets/domesurvival/textures/block/lanos_decorative.png | texture_png | 256×256 |
| src/main/resources/assets/domesurvival/textures/block/lead_block.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/lead_ore.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/machine_stabilizer/mechanism_base_bolts.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/machine_stabilizer/mechanism_base_copper.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/machine_stabilizer/mechanism_base_core.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/machine_stabilizer/mechanism_base_frame.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/machine_stabilizer/mechanism_base_gear.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/machine_stabilizer/mechanism_base_node.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/machine_stabilizer/mechanism_base_panel.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/coke_oven_brick.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/coke_oven_front.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/coke_oven_front_on.png | texture_png | 16×64 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/coke_oven_front_on.png.mcmeta | texture_metadata | 104 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/coke_oven_upper_front.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/dark_metal.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/coke_oven_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/coke_oven_body.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/coke_oven_bottom.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/coke_oven_door.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/coke_oven_fire.png | texture_png | 16×64 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/coke_oven_fire.png.mcmeta | texture_metadata | 160 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/coke_oven_fire_off.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/coke_oven_gauge.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/coke_oven_iron.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/coke_oven_rim.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/coke_oven_soot.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/coke_oven_top.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/coke_oven_vent.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/input_connector.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/output_connector.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/shaft_furnace_back.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/shaft_furnace_body.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/shaft_furnace_bottom.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/shaft_furnace_door.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/shaft_furnace_fire.png | texture_png | 16×64 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/shaft_furnace_fire.png.mcmeta | texture_metadata | 160 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/shaft_furnace_fire_off.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/shaft_furnace_gauge.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/shaft_furnace_iron.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/shaft_furnace_rim.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/shaft_furnace_soot.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/shaft_furnace_top.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/detailed/shaft_furnace_vent.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/input_connector.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/large_coke_oven_body.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/large_coke_oven_cutout.png | texture_png | 128×1024 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/large_coke_oven_fire.png | texture_png | 16×64 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/large_coke_oven_fire.png.mcmeta | texture_metadata | 160 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/large_coke_oven_fire_off.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/output_connector.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/shaft_furnace_front.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/shaft_furnace_front_on.png | texture_png | 16×64 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/shaft_furnace_front_on.png.mcmeta | texture_metadata | 104 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/shaft_furnace_large.png | texture_png | 128×128 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/shaft_furnace_large_on.png | texture_png | 128×512 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/shaft_furnace_large_on.png.mcmeta | texture_metadata | 104 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/shaft_furnace_side.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/metallurgy/shaft_furnace_upper_front.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/nickel_block.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/nickel_ore.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/air_intake_front_off.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/air_intake_front_on.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/air_intake_side.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/air_intake_top.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/amber_off.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/amber_on.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/compression_front_off.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/compression_front_on.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/compression_side.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/compression_top.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/cyan_off.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/cyan_on.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/filtration_front_off.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/filtration_front_on.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/filtration_side.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/filtration_top.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/formed_back_air_intake.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/formed_back_compression.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/formed_back_filtration.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/formed_back_output.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/formed_bottom_compression.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/formed_bottom_output.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/formed_side_bottom.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/formed_side_bottom_output.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/formed_side_top.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/formed_top_air_intake.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/formed_top_filtration.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/gauge.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/green_off.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/green_on.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/grille.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/hazard.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/metal_dark.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/metal_light.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/metal_mid.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/output_front_off.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/output_front_on.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/output_side.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/output_top.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/oxygen_glass_off.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/oxygen_glass_on.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/panel_base.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/pipe.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/port_input.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/port_off.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/port_output.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex/screen_o2.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex_air_intake_front_off.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex_air_intake_front_on.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex_air_intake_side.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex_air_intake_top.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex_compression_front_off.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex_compression_front_on.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex_compression_side.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex_compression_top.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex_filtration_front_off.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex_filtration_front_on.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex_filtration_side.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex_filtration_top.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex_output_front_off.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex_output_front_on.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex_output_side.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_complex_output_top.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_electrolyzer_front.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_electrolyzer_front_on.png | texture_png | 16×128 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_electrolyzer_front_on.png.mcmeta | texture_metadata | 527 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_filler_front.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_filler_front_on.png | texture_png | 16×128 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_filler_front_on.png.mcmeta | texture_metadata | 111 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_pipe/high_flow_oxygen_pipe_bolts.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_pipe/high_flow_oxygen_pipe_metal.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_pipe/high_flow_oxygen_pipe_oxygen.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_pipe/high_flow_oxygen_pipe_panel.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_pipe/high_flow_oxygen_pipe_seal.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_pipe/oxygen_pipe_bolts.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_pipe/oxygen_pipe_metal.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_pipe/oxygen_pipe_oxygen.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_pipe/oxygen_pipe_panel.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_pipe/oxygen_pipe_seal.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_pipe/reinforced_oxygen_pipe_bolts.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_pipe/reinforced_oxygen_pipe_metal.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_pipe/reinforced_oxygen_pipe_oxygen.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_pipe/reinforced_oxygen_pipe_panel.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/oxygen_pipe/reinforced_oxygen_pipe_seal.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/reinforced_energy_pipe.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/reinforced_energy_pipe_arm.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/reinforced_energy_pipe_core.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/reinforced_energy_pipe_detail.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/reinforced_fluid_pipe.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/service_pass_through/collar.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/service_pass_through/metal.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/service_pass_through/tunnel.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/shaft_furnace_dark/bfbricks.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/shaft_furnace_dark/bfbricksdark.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/shaft_furnace_dark/bfbrickslit.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/block/shaft_furnace_dark/bftoolshot.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/shaft_furnace_dark/bftoolshot_blue.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/shaft_furnace_dark/bftoolst.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/shaft_furnace_dark/blue_fire.png | texture_png | 16×512 |
| src/main/resources/assets/domesurvival/textures/block/shaft_furnace_dark/blue_fire.png.mcmeta | texture_metadata | 70 |
| src/main/resources/assets/domesurvival/textures/block/shaft_furnace_dark/campfire_log_lit.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/shaft_furnace_dark/campfire_log_lit_blue.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/shaft_furnace_dark/firetools.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/shaft_furnace_dark/firetools_blue.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/shaft_furnace_dark/wither_skeleton_head.png | texture_png | 64×32 |
| src/main/resources/assets/domesurvival/textures/block/shaft_furnace_dark/wither_skull_face.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/shaft_furnace_dark/wither_skull_side.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/silver_block.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/silver_ore.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/solarite_block.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/block/solarite_ore.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/solar_connector_body.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/solar_connector_contact.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/solar_connector_mount.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/solar_panel_mk1.png | texture_png | 256×256 |
| src/main/resources/assets/domesurvival/textures/block/solar_panel_mk2.png | texture_png | 256×256 |
| src/main/resources/assets/domesurvival/textures/block/solar_panel_mk3.png | texture_png | 256×256 |
| src/main/resources/assets/domesurvival/textures/block/steel_block.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/tin_block.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/tin_ore.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/universal_tank/valve_input.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/universal_tank/valve_output.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/voltarium_block.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/voltarium_ore.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/water_purifier_front.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/block/water_purifier_front_on.png | texture_png | 16×96 |
| src/main/resources/assets/domesurvival/textures/block/water_purifier_front_on.png.mcmeta | texture_metadata | 76 |
| src/main/resources/assets/domesurvival/textures/gui/bio/bio_faces_atlas.png | texture_png | 160×128 |
| src/main/resources/assets/domesurvival/textures/gui/bio/chicken.png | texture_png | 36×36 |
| src/main/resources/assets/domesurvival/textures/gui/bio/cow.png | texture_png | 36×36 |
| src/main/resources/assets/domesurvival/textures/gui/bio/mule.png | texture_png | 36×36 |
| src/main/resources/assets/domesurvival/textures/gui/bio/parrot.png | texture_png | 36×36 |
| src/main/resources/assets/domesurvival/textures/gui/bio/pig.png | texture_png | 36×36 |
| src/main/resources/assets/domesurvival/textures/gui/bio/polar_bear.png | texture_png | 36×36 |
| src/main/resources/assets/domesurvival/textures/gui/bio/sheep.png | texture_png | 36×36 |
| src/main/resources/assets/domesurvival/textures/gui/coal_generator_ports.png | texture_png | 24×6 |
| src/main/resources/assets/domesurvival/textures/gui/early_world_loading.png | texture_png | 1920×1080 |
| src/main/resources/assets/domesurvival/textures/gui/loading/loading_bar_fill_v27.png | texture_png | 1223×30 |
| src/main/resources/assets/domesurvival/textures/gui/loading/loading_dome_city.png | texture_png | 1672×941 |
| src/main/resources/assets/domesurvival/textures/gui/oxygen_empty.png | texture_png | 9×9 |
| src/main/resources/assets/domesurvival/textures/gui/oxygen_full.png | texture_png | 9×9 |
| src/main/resources/assets/domesurvival/textures/gui/oxygen_half.png | texture_png | 9×9 |
| src/main/resources/assets/domesurvival/textures/gui/oxygen_tank_source.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/gui/quests/chapter_00_under_dome.png | texture_png | 1672×941 |
| src/main/resources/assets/domesurvival/textures/gui/quests/chapter_01_first_days.png | texture_png | 1672×941 |
| src/main/resources/assets/domesurvival/textures/gui/quests/chapter_02_beyond_gate.png | texture_png | 1672×941 |
| src/main/resources/assets/domesurvival/textures/gui/quests/chapter_03_settlement.png | texture_png | 1672×941 |
| src/main/resources/assets/domesurvival/textures/gui/quests/chapter_04_food_system.png | texture_png | 1672×941 |
| src/main/resources/assets/domesurvival/textures/gui/quests/chapter_06_power.png | texture_png | 1672×941 |
| src/main/resources/assets/domesurvival/textures/gui/quests/chapter_07_industrial_district.png | texture_png | 1672×941 |
| src/main/resources/assets/domesurvival/textures/gui/solar_heat_vignette.png | texture_png | 256×256 |
| src/main/resources/assets/domesurvival/textures/gui/ui/v32/corridor.png | texture_png | 1920×1080 |
| src/main/resources/assets/domesurvival/textures/gui/ui/v32/network.png | texture_png | 1920×1080 |
| src/main/resources/assets/domesurvival/textures/gui/ui/v32/system.png | texture_png | 1920×1080 |
| src/main/resources/assets/domesurvival/textures/gui/ui/v32/world.png | texture_png | 1920×1080 |
| src/main/resources/assets/domesurvival/textures/item/airlock_binding_key.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/common/spool_flange.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/copper_plate.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/copper_rod.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/copper_tube.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/copper_wire.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/engineering_gears/lead_gear.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/engineering_gears/nickel_gear.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/engineering_gears/steel_gear.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/engineering_gears/tin_gear.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/filter_regeneration_media.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/genetics/biogel.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/genetics/bio_repair_kit.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/genetics/chicken_cryocapsule.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/genetics/cow_cryocapsule.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/genetics/damaged_pig_cryocapsule.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/genetics/nutrient_mix.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/genetics/sheep_cryocapsule.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/goteium_gear.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/goteium_ingot.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/goteium_nugget.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/goteium_plate.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/hopper_upgrade_copper_to_steel.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/item/hopper_upgrade_steel_to_desh.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/item/hopper_upgrade_vanilla_to_copper.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/item/improved_water_filter.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/item/industrial_water_filter.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/item/large_oxygen_tank.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/lead_ingot.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/lead_nugget.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench/machine_wrench_bolt.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench/machine_wrench_copper.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench/machine_wrench_copper_dark.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench/machine_wrench_dark_steel.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench/machine_wrench_engraved.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench/machine_wrench_flat.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench/machine_wrench_hd.png | texture_png | 512×1024 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench/machine_wrench_hd.png.mcmeta | texture_metadata | 67 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench/machine_wrench_oxide.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench/machine_wrench_rubber.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench/machine_wrench_steel.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench/machine_wrench_steel_edge.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench/machine_wrench_teeth.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench/machine_wrench_worn_steel.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench_2d.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench_create.png | texture_png | 128×128 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench_custom_simple.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench_custom_wrench.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/machine_wrench_simple.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/materials/copper.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/materials/silver.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/materials/steel.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/materials/tin.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/materials/voltarium.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/medium_oxygen_tank.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/memory_painting.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/metallurgy/coal_coke.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/metallurgy/slag.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/metallurgy/steel_ingot.png | texture_png | 256×256 |
| src/main/resources/assets/domesurvival/textures/item/nickel_ingot.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/nickel_nugget.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/nickel_plate.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/nickel_tube.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/oxygen_mask.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_00.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_00_side.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_01.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_01_side.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_02.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_02_side.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_03.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_03_side.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_04.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_04_side.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_05.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_05_side.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_06.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_06_side.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_07.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_07_side.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_08.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_08_side.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_09.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_09_side.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_10.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_10_side.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_11.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_11_side.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_12.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_12_side.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_13.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix/tone_13_side.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/pulse_matrix_2d.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/item/raw_goteium.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/raw_lead.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/raw_nickel.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/raw_silver.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/raw_tin.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/raw_voltarium.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/sieve_mesh.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/silver_ingot.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/silver_nugget.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/silver_rod.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/silver_wire.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/small_oxygen_tank.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/solarite_crystal.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/solarite_shard.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/solar_generator_mk1.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/solar_generator_mk2.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/solar_generator_mk3.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/steel_nugget.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/steel_plate.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/steel_rod.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/steel_tube.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/steel_wire.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/surface_suit_boots.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/surface_suit_chestplate.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/surface_suit_helmet.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/surface_suit_leggings.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/tin_ingot.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/tin_nugget.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/tin_plate.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/tin_tube.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/voltarium_gear.png | texture_png | 32×32 |
| src/main/resources/assets/domesurvival/textures/item/voltarium_ingot.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/voltarium_nugget.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/voltarium_plate.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/voltarium_rod.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/voltarium_wire.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/item/water_filter_cartridge.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/models/armor/m40_gasmask_domesurvival.png | texture_png | 256×256 |
| src/main/resources/assets/domesurvival/textures/models/armor/oxygen_mask.png | texture_png | 64×32 |
| src/main/resources/assets/domesurvival/textures/models/armor/oxygen_tank.png | texture_png | 64×32 |
| src/main/resources/assets/domesurvival/textures/models/armor/surface_suit_blue_layer_1.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/models/armor/surface_suit_blue_layer_2.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/models/armor/surface_suit_layer_1.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/models/armor/surface_suit_layer_2.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/npc/dome_security_officer.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/npc/expedition_soldier.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/npc/joseph_cooper.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/painting/01_trio_friends.png | texture_png | 128×128 |
| src/main/resources/assets/domesurvival/textures/painting/02_recording_in_yard.png | texture_png | 256×320 |
| src/main/resources/assets/domesurvival/textures/painting/03_airsoft_team.png | texture_png | 128×128 |
| src/main/resources/assets/domesurvival/textures/painting/04_fishing_closeup.png | texture_png | 256×320 |
| src/main/resources/assets/domesurvival/textures/painting/05_calm_lake_fishing.png | texture_png | 192×256 |
| src/main/resources/assets/domesurvival/textures/painting/06_relaxing_on_grass.png | texture_png | 192×256 |
| src/main/resources/assets/domesurvival/textures/painting/07_pink_hat_portrait.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/painting/08_watermelon_park.png | texture_png | 256×320 |
| src/main/resources/assets/domesurvival/textures/painting/09_white_hat_portrait.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/painting/10_flexing_portrait.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/painting/11_prize_shop_winners.png | texture_png | 256×320 |
| src/main/resources/assets/domesurvival/textures/painting/12_kitchen_character.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/painting/13_music_studio_friends.png | texture_png | 128×128 |
| src/main/resources/assets/domesurvival/textures/painting/14_mirror_group_selfie.png | texture_png | 256×320 |
| src/main/resources/assets/domesurvival/textures/painting/15_voxel_company_bright_light.png | texture_png | 128×128 |
| src/main/resources/assets/domesurvival/textures/painting/16_tricolor_portrait.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/painting/17_bee_hero_amber_hive.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/painting/18_wedding_kiss_tree.png | texture_png | 128×128 |
| src/main/resources/assets/domesurvival/textures/painting/19_night_selfie_friendship.png | texture_png | 64×64 |
| src/main/resources/assets/domesurvival/textures/painting/20_brown_suit_limo.png | texture_png | 256×320 |
| src/main/resources/assets/domesurvival/textures/painting/21_bw_party_point.png | texture_png | 256×320 |
| src/main/resources/assets/domesurvival/textures/painting/22_party_toast_indoor.png | texture_png | 128×128 |
| src/main/resources/assets/domesurvival/textures/painting/compact_01_trio_friends.png | texture_png | 192×128 |
| src/main/resources/assets/domesurvival/textures/painting/compact_02_recording_in_yard.png | texture_png | 128×192 |
| src/main/resources/assets/domesurvival/textures/painting/compact_03_airsoft_team.png | texture_png | 192×128 |
| src/main/resources/assets/domesurvival/textures/painting/compact_04_fishing_closeup.png | texture_png | 128×192 |
| src/main/resources/assets/domesurvival/textures/painting/compact_05_calm_lake_fishing.png | texture_png | 128×192 |
| src/main/resources/assets/domesurvival/textures/painting/compact_06_relaxing_on_grass.png | texture_png | 128×192 |
| src/main/resources/assets/domesurvival/textures/painting/compact_07_pink_hat_portrait.png | texture_png | 128×192 |
| src/main/resources/assets/domesurvival/textures/painting/compact_08_watermelon_park.png | texture_png | 128×192 |
| src/main/resources/assets/domesurvival/textures/painting/compact_09_white_hat_portrait.png | texture_png | 128×192 |
| src/main/resources/assets/domesurvival/textures/painting/compact_10_flexing_portrait.png | texture_png | 128×192 |
| src/main/resources/assets/domesurvival/textures/painting/compact_11_prize_shop_winners.png | texture_png | 128×192 |
| src/main/resources/assets/domesurvival/textures/painting/compact_12_kitchen_character.png | texture_png | 128×192 |
| src/main/resources/assets/domesurvival/textures/painting/compact_13_music_studio_friends.png | texture_png | 192×128 |
| src/main/resources/assets/domesurvival/textures/painting/compact_14_mirror_group_selfie.png | texture_png | 128×192 |
| src/main/resources/assets/domesurvival/textures/painting/compact_15_voxel_company_bright_light.png | texture_png | 192×128 |
| src/main/resources/assets/domesurvival/textures/painting/compact_16_tricolor_portrait.png | texture_png | 128×128 |
| src/main/resources/assets/domesurvival/textures/painting/compact_17_bee_hero_amber_hive.png | texture_png | 128×128 |
| src/main/resources/assets/domesurvival/textures/painting/compact_18_wedding_kiss_tree.png | texture_png | 192×128 |
| src/main/resources/assets/domesurvival/textures/painting/compact_19_night_selfie_friendship.png | texture_png | 128×128 |
| src/main/resources/assets/domesurvival/textures/painting/compact_20_brown_suit_limo.png | texture_png | 128×192 |
| src/main/resources/assets/domesurvival/textures/painting/compact_21_bw_party_point.png | texture_png | 128×192 |
| src/main/resources/assets/domesurvival/textures/painting/compact_22_party_toast_indoor.png | texture_png | 192×128 |
| src/main/resources/assets/domesurvival/textures/particle/acid_rain_streak.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/particle/item_pipe_packet.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/particle/sandstorm_mote.png | texture_png | 16×16 |
| src/main/resources/assets/domesurvival/textures/particle/ventilation_bubble.png | texture_png | 32×32 |
| src/main/resources/assets/minecraft/textures/environment/rain.png | texture_png | 32×512 |
| src/main/resources/assets/minecraft/textures/environment/sun.png | texture_png | 32×32 |

## Неактивные и внешние ресурсы

В JSON перечислен каждый файл; сводка здесь не смешивает резервные копии с production-ассетами. Архивы ниже перечислены без распаковки.

| Контейнер/группа | Файлы |
| --- | --- |
| _manual_backups | 117 |
| _patch_backups | 116 |
| backups | 2164 |
| build | 1394 |
| config | 39 |
| dev | 58 |
| kubejs | 23 |
| main | 128 |
| run/config | 181 |
| run/dome-export-server | 48 |
| run/journeymap | 645 |
| run/kubejs | 2 |
| run/mods | 1 |
| run/saros_road_signs_mod | 5 |
| run/screenshots | 8 |
| run/visual-prototype | 308 |

| Архив/JAR | Статус | Байт |
| --- | --- | --- |
| backups/BBM_COMPAT_V3_2_20260829_223602/run/resourcepacks/Blues_Better_Monstersv0.11.zip | inactive_patch_or_backup | 708323 |
| backups/HORROR_ANIMATION_STACK_V2_20260829_215355/run/resourcepacks/FreshAnimations_v1.10.4.zip | inactive_patch_or_backup | 850941 |
| backups/HORROR_HOSTILE_OVERHAUL_V3_BBM_20260829_222834/run/resourcepacks/Enhanced_Evil_FA_v1.1_NO_SPIDERS.zip | inactive_patch_or_backup | 320331 |
| backups/HORROR_SPIDER_HOTFIX_V2_2_20260829_220730/run/mods/entity_model_features-3.2.3-1.20.1-forge.jar | inactive_patch_or_backup | 751492 |
| backups/HORROR_SPIDER_HOTFIX_V2_2_20260829_220730/run/resourcepacks/Enhanced_Evil_FA_v1.1.zip | inactive_patch_or_backup | 324727 |
| backups/HORROR_SPIDER_HOTFIX_V2_2_20260829_220730/run/resourcepacks/Fresh_Animations_Spiders_v2.1.zip | inactive_patch_or_backup | 77409 |
| backups/HORROR_STACK_V2_3_STABLE_20260829_221614/run/resourcepacks/Fresh_Animations_Spiders_v2.2.zip | inactive_patch_or_backup | 77869 |
| backups/LCMT_COMPAT_V1_20260830_201720/mod0074.jar | inactive_patch_or_backup | 344069 |
| backups/LCMT_COMPAT_V2_20260830_202232/mod0074.jar | inactive_patch_or_backup | 344069 |
| backups/WORLDGEN_COMPAT_20260830_135521/run/mods/yulari-1.20.1-lostcities.jar | inactive_patch_or_backup | 349432 |
| build/downloadMcpConfig/output.zip | generated_build_copy | 1740711 |
| build/libs/domesurvival-0.2.0.jar | generated_build_copy | 179180984 |
| build/reobfJar/output.jar | generated_build_copy | 179180984 |
| dependency_audit/audit.zip | auxiliary_source | 16538 |
| dev/generated/fullmods/mod0001.jar | auxiliary_source | 7508746 |
| dev/generated/fullmods/mod0002.jar | auxiliary_source | 29553 |
| dev/generated/fullmods/mod0003.jar | auxiliary_source | 74871659 |
| dev/generated/fullmods/mod0004.jar | auxiliary_source | 53429235 |
| dev/generated/fullmods/mod0005.jar | auxiliary_source | 588274 |
| dev/generated/fullmods/mod0006.jar | auxiliary_source | 90146 |
| dev/generated/fullmods/mod0007.jar | auxiliary_source | 89085 |
| dev/generated/fullmods/mod0008.jar | auxiliary_source | 561635 |
| dev/generated/fullmods/mod0009.jar | auxiliary_source | 157633 |
| dev/generated/fullmods/mod0010.jar | auxiliary_source | 1206193 |
| dev/generated/fullmods/mod0011.jar | auxiliary_source | 341451 |
| dev/generated/fullmods/mod0012.jar | auxiliary_source | 3185155 |
| dev/generated/fullmods/mod0013.jar | auxiliary_source | 20266 |
| dev/generated/fullmods/mod0014.jar | auxiliary_source | 21714 |
| dev/generated/fullmods/mod0015.jar | auxiliary_source | 360622 |
| dev/generated/fullmods/mod0016.jar | auxiliary_source | 64187 |
| dev/generated/fullmods/mod0017.jar | auxiliary_source | 3271683 |
| dev/generated/fullmods/mod0018.jar | auxiliary_source | 1144136 |
| dev/generated/fullmods/mod0019.jar | auxiliary_source | 31689 |
| dev/generated/fullmods/mod0020.jar | auxiliary_source | 10873660 |
| dev/generated/fullmods/mod0021.jar | auxiliary_source | 112067 |
| dev/generated/fullmods/mod0022.jar | auxiliary_source | 758123 |
| dev/generated/fullmods/mod0023.jar | auxiliary_source | 8579488 |
| dev/generated/fullmods/mod0024.jar | auxiliary_source | 8086367 |
| dev/generated/fullmods/mod0025.jar | auxiliary_source | 1629591 |
| dev/generated/fullmods/mod0026.jar | auxiliary_source | 1589618 |
| dev/generated/fullmods/mod0027.jar | auxiliary_source | 420179 |
| dev/generated/fullmods/mod0028.jar | auxiliary_source | 26173153 |
| dev/generated/fullmods/mod0029.jar | auxiliary_source | 3313188 |
| dev/generated/fullmods/mod0030.jar | auxiliary_source | 6157 |
| dev/generated/fullmods/mod0031.jar | auxiliary_source | 26578 |
| dev/generated/fullmods/mod0032.jar | auxiliary_source | 28150 |
| dev/generated/fullmods/mod0033.jar | auxiliary_source | 115893 |
| dev/generated/fullmods/mod0034.jar | auxiliary_source | 383868 |
| dev/generated/fullmods/mod0035.jar | auxiliary_source | 1159014 |
| dev/generated/fullmods/mod0036.jar | auxiliary_source | 780558 |
| dev/generated/fullmods/mod0037.jar | auxiliary_source | 1255469 |
| dev/generated/fullmods/mod0038.jar | auxiliary_source | 252579 |
| dev/generated/fullmods/mod0039.jar | auxiliary_source | 991548 |
| dev/generated/fullmods/mod0040.jar | auxiliary_source | 317181 |
| dev/generated/fullmods/mod0041.jar | auxiliary_source | 11713434 |
| dev/generated/fullmods/mod0042.jar | auxiliary_source | 75049 |
| dev/generated/fullmods/mod0043.jar | auxiliary_source | 311290 |
| dev/generated/fullmods/mod0044.jar | auxiliary_source | 553476 |
| dev/generated/fullmods/mod0045.jar | auxiliary_source | 1426211 |
| dev/generated/fullmods/mod0046.jar | auxiliary_source | 4184900 |
| dev/generated/fullmods/mod0047.jar | auxiliary_source | 625171 |
| dev/generated/fullmods/mod0048.jar | auxiliary_source | 1637547 |
| dev/generated/fullmods/mod0049.jar | auxiliary_source | 42209 |
| dev/generated/fullmods/mod0050.jar | auxiliary_source | 1402158 |
| dev/generated/fullmods/mod0051.jar | auxiliary_source | 714454 |
| dev/generated/fullmods/mod0052.jar | auxiliary_source | 12558456 |
| dev/generated/fullmods/mod0053.jar | auxiliary_source | 1136805 |
| dev/generated/fullmods/mod0054.jar | auxiliary_source | 1054586 |
| dev/generated/fullmods/mod0055.jar | auxiliary_source | 517999 |
| dev/generated/fullmods/mod0056.jar | auxiliary_source | 37168 |
| dev/generated/fullmods/mod0057.jar | auxiliary_source | 472175 |
| dev/generated/fullmods/mod0058.jar | auxiliary_source | 282461 |
| dev/generated/fullmods/mod0059.jar | auxiliary_source | 7080665 |
| dev/generated/fullmods/mod0060.jar | auxiliary_source | 136923 |
| dev/generated/fullmods/mod0061.jar | auxiliary_source | 432753 |
| dev/generated/fullmods/mod0062.jar | auxiliary_source | 1787530 |
| dev/generated/fullmods/mod0063.jar | auxiliary_source | 1130880 |
| dev/generated/fullmods/mod0064.jar | auxiliary_source | 1622684 |
| dev/generated/fullmods/mod0065.jar | auxiliary_source | 3114590 |
| dev/generated/fullmods/mod0066.jar | auxiliary_source | 206584 |
| dev/generated/fullmods/mod0067.jar | auxiliary_source | 10180151 |
| dev/generated/fullmods/mod0068.jar | auxiliary_source | 622948 |
| dev/generated/fullmods/mod0069.jar | auxiliary_source | 574141 |
| dev/generated/fullmods/mod0070.jar | auxiliary_source | 4549423 |
| dev/generated/fullmods/mod0071.jar | auxiliary_source | 225344 |
| dev/generated/fullmods/mod0072.jar | auxiliary_source | 333350 |
| dev/generated/fullmods/mod0073.jar | auxiliary_source | 533724 |
| dev/generated/fullmods/mod0074.jar | auxiliary_source | 344069 |
| dev/generated/fullmods/mod0075.jar | auxiliary_source | 370520 |
| dev/generated/fullmods/mod0076.jar | auxiliary_source | 918022 |
| dev/generated/fullmods/mod0077.jar | auxiliary_source | 792357 |
| dev/generated/fullmods/mod0078.jar | auxiliary_source | 492968 |
| dev/generated/fullmods/mod0079.jar | auxiliary_source | 473534 |
| dev/generated/fullmods/mod0080.jar | auxiliary_source | 291088 |
| dev/generated/yulari_repack_validation/yulari-1.20.1-lostcities.jar | auxiliary_source | 344069 |
| dev/LCMT_AUDIT/lcmt.zip | auxiliary_source | 344069 |
| dev/LCMT_COMPAT_V2_WORK_20260830_202232/mod0074.work.jar | auxiliary_source | 343839 |
| dev/LCMT_COMPAT_WORK_20260830_201720/mod0074.patched.jar | inactive_patch_or_backup | 343839 |
| devmods/mod0001.jar | auxiliary_source | 7508861 |
| devmods/mod0002.jar | auxiliary_source | 29553 |
| devmods/mod0003.jar | auxiliary_source | 53429226 |
| devmods/mod0004.jar | auxiliary_source | 580602 |
| devmods/mod0005.jar | auxiliary_source | 91808 |
| devmods/mod0006.jar | auxiliary_source | 89085 |
| devmods/mod0007.jar | auxiliary_source | 561670 |
| devmods/mod0008.jar | auxiliary_source | 157633 |
| devmods/mod0009.jar | auxiliary_source | 1206255 |
| devmods/mod0010.jar | auxiliary_source | 341451 |
| devmods/mod0011.jar | auxiliary_source | 3185275 |
| devmods/mod0012.jar | auxiliary_source | 20299 |
| devmods/mod0013.jar | auxiliary_source | 1509649 |
| devmods/mod0014.jar | auxiliary_source | 21714 |
| devmods/mod0015.jar | auxiliary_source | 360700 |
| devmods/mod0016.jar | auxiliary_source | 64187 |
| devmods/mod0017.jar | auxiliary_source | 3271742 |
| devmods/mod0018.jar | auxiliary_source | 1144128 |
| devmods/mod0019.jar | auxiliary_source | 31724 |
| devmods/mod0020.jar | auxiliary_source | 10873769 |
| devmods/mod0021.jar | auxiliary_source | 112171 |
| devmods/mod0022.jar | auxiliary_source | 8086393 |
| devmods/mod0023.jar | auxiliary_source | 1629693 |
| devmods/mod0024.jar | auxiliary_source | 1590221 |
| devmods/mod0025.jar | auxiliary_source | 420179 |
| devmods/mod0026.jar | auxiliary_source | 26174014 |
| devmods/mod0027.jar | auxiliary_source | 3313327 |
| devmods/mod0028.jar | auxiliary_source | 6170 |
| devmods/mod0029.jar | auxiliary_source | 26600 |
| devmods/mod0030.jar | auxiliary_source | 28188 |
| devmods/mod0031.jar | auxiliary_source | 123034 |
| devmods/mod0032.jar | auxiliary_source | 383868 |
| devmods/mod0033.jar | auxiliary_source | 1159043 |
| devmods/mod0034.jar | auxiliary_source | 780558 |
| devmods/mod0035.jar | auxiliary_source | 1255469 |
| devmods/mod0036.jar | auxiliary_source | 252579 |
| devmods/mod0037.jar | auxiliary_source | 317181 |
| devmods/mod0038.jar | auxiliary_source | 12129396 |
| devmods/mod0039.jar | auxiliary_source | 75049 |
| devmods/mod0040.jar | auxiliary_source | 311290 |
| devmods/mod0041.jar | auxiliary_source | 553487 |
| devmods/mod0042.jar | auxiliary_source | 1426211 |
| devmods/mod0043.jar | auxiliary_source | 4184996 |
| devmods/mod0044.jar | auxiliary_source | 625185 |
| devmods/mod0045.jar | auxiliary_source | 1658792 |
| devmods/mod0046.jar | auxiliary_source | 12558456 |
| devmods/mod0047.jar | auxiliary_source | 1136805 |
| devmods/mod0048.jar | auxiliary_source | 1054586 |
| devmods/mod0049.jar | auxiliary_source | 517999 |
| devmods/mod0050.jar | auxiliary_source | 37178 |
| devmods/mod0051.jar | auxiliary_source | 472175 |
| devmods/mod0052.jar | auxiliary_source | 282474 |
| devmods/mod0053.jar | auxiliary_source | 7080698 |
| devmods/mod0054.jar | auxiliary_source | 136923 |
| devmods/mod0055.jar | auxiliary_source | 432753 |
| devmods/mod0056.jar | auxiliary_source | 1798244 |
| devmods/mod0057.jar | auxiliary_source | 1130880 |
| devmods/mod0058.jar | auxiliary_source | 1622705 |
| devmods/mod0059.jar | auxiliary_source | 3114590 |
| devmods/mod0060.jar | auxiliary_source | 622948 |
| devmods/mod0061.jar | auxiliary_source | 574141 |
| devmods/mod0062.jar | auxiliary_source | 4549423 |
| devmods/mod0063.jar | auxiliary_source | 225344 |
| devmods/mod0064.jar | auxiliary_source | 333350 |
| devmods/mod0065.jar | auxiliary_source | 533738 |
| devmods/mod0066.jar | auxiliary_source | 370524 |
| devmods/mod0067.jar | auxiliary_source | 492998 |
| devmods/mod0068.jar | auxiliary_source | 291139 |
| DOMESURVIVAL_QUEST_PHASE8_2_CHAPTER5_VISUAL_RESOURCE_FIX_20260825.zip | auxiliary_source | 2739383 |
| DOMESURVIVAL_QUEST_PHASE8_3_ALL_CHAPTER_BACKGROUNDS_20260825.zip | auxiliary_source | 17062587 |
| DOMESURVIVAL_QUEST_PHASE8_4_SOFT_BALANCE_20260825.zip | auxiliary_source | 17107871 |
| DOMESURVIVAL_QUEST_PHASE9_0_INDUSTRIAL_DISTRICT_20260825.zip | auxiliary_source | 2673933 |
| DOME_V3_COLLECT.zip | inactive_patch_or_backup | 136676 |
| FINAL_PACK_AUDIT.zip | auxiliary_source | 16100 |
| gradle/wrapper/gradle-wrapper.jar | auxiliary_source | 62076 |
| libs/CustomNPCs-1.20.1-GBPort-Unofficial-1.20.1.20260711.jar | inactive_patch_or_backup | 10873769 |
| modpack/mods/ad_astra-forge-1.20.1-1.15.20.jar | modpack_distribution | 7508861 |
| modpack/mods/AI-Improvements-1.20-0.5.2.jar | modpack_distribution | 29553 |
| modpack/mods/AmbientSounds_FORGE_v6.3.8_mc1.20.1.jar | modpack_distribution | 53429226 |
| modpack/mods/architectury-9.2.14-forge (1).jar | modpack_distribution | 580602 |
| modpack/mods/athena-forge-1.20.1-3.1.2.jar | modpack_distribution | 91808 |
| modpack/mods/awcapi-neoforge-1.20.1-1.0.2.jar | modpack_distribution | 89085 |
| modpack/mods/balm-forge-1.20.1-7.3.42.jar | modpack_distribution | 561670 |
| modpack/mods/botarium-forge-1.20.1-2.3.4.jar | modpack_distribution | 157633 |
| modpack/mods/BrewinAndChewin-1.20.1-3.2.1.jar | modpack_distribution | 1206255 |
| modpack/mods/Chunky-1.3.146.jar | modpack_distribution | 341451 |
| modpack/mods/citadel-2.6.3-1.20.1.jar | modpack_distribution | 3185275 |
| modpack/mods/Clumps-forge-1.20.1-12.0.0.4.jar | modpack_distribution | 20299 |
| modpack/mods/cofh_core-1.20.1-11.0.2.56.jar | modpack_distribution | 1509649 |
| modpack/mods/common-networking-forge-1.0.6-1.20.1.jar | modpack_distribution | 21714 |
| modpack/mods/Corgilib-Forge-1.20.1-4.0.3.4.jar | modpack_distribution | 360700 |
| modpack/mods/coroutil-forge-1.20.1-1.3.7.jar | modpack_distribution | 64187 |
| modpack/mods/CraftTweaker-forge-1.20.1-14.0.60.jar | modpack_distribution | 3271742 |
| modpack/mods/CreativeCore_FORGE_v2.12.39_mc1.20.1.jar | modpack_distribution | 1144128 |
| modpack/mods/cupboard-1.20.1-3.9.jar | modpack_distribution | 31724 |
| modpack/mods/CustomNPCs-1.20.1-GBPort-Unofficial-1.20.1.20260711.jar | modpack_distribution | 10873769 |
| modpack/mods/Data_Anchor-forge-1.20.1-1.0.0.20.jar | modpack_distribution | 112171 |
| modpack/mods/EnderIO-1.20.1-6.2.18-beta-all.jar | modpack_distribution | 8086393 |
| modpack/mods/Enhanced-Celestials-forge-1.20.1-5.0.3.2.jar | modpack_distribution | 1629693 |
| modpack/mods/entityculling-forge-1.10.5-mc1.20.1.jar | modpack_distribution | 1590221 |
| modpack/mods/fallingtrees-forge-mc1.20-0.13.2-SNAPSHOT.jar | modpack_distribution | 4025908 |
| modpack/mods/fancymenu_forge_3.9.9_MC_1.20.1.jar | modpack_distribution | 26174014 |
| modpack/mods/FarmersDelight-1.20.1-1.3.2.jar | modpack_distribution | 3313327 |
| modpack/mods/FastFurnace-1.20.1-8.0.2.jar | modpack_distribution | 6170 |
| modpack/mods/FastSuite-1.20.1-5.1.2.jar | modpack_distribution | 26600 |
| modpack/mods/FastWorkbench-1.20.1-8.0.4.jar | modpack_distribution | 28188 |
| modpack/mods/ferritecore-6.0.1-forge.jar | modpack_distribution | 123034 |
| modpack/mods/framework-forge-1.20.1-0.8.0.jar | modpack_distribution | 383868 |
| modpack/mods/ftb-chunks-forge-2001.3.8.jar | modpack_distribution | 1159043 |
| modpack/mods/ftb-library-forge-2001.2.13.jar | modpack_distribution | 780558 |
| modpack/mods/ftb-quests-forge-2001.4.22.jar | modpack_distribution | 1255469 |
| modpack/mods/ftb-teams-forge-2001.3.2.jar | modpack_distribution | 252579 |
| modpack/mods/gravestone-forge-1.20.1-1.0.35.jar | modpack_distribution | 317181 |
| modpack/mods/ImmersiveEngineering-1.20.1-10.2.0-183.jar | modpack_distribution | 12129396 |
| modpack/mods/invtweaks-1.20.1-1.2.2.jar | modpack_distribution | 75049 |
| modpack/mods/ironchest-1.20.1-14.4.4.jar | modpack_distribution | 311290 |
| modpack/mods/Jade-1.20.1-Forge-11.13.3.jar | modpack_distribution | 553487 |
| modpack/mods/jei-1.20.1-forge-15.21.0.148.jar | modpack_distribution | 1426211 |
| modpack/mods/journeymap-forge-1.20.1-6.0.0.jar | modpack_distribution | 4184996 |
| modpack/mods/konkrete_forge_1.8.0_MC_1.20-1.20.1.jar | modpack_distribution | 625185 |
| modpack/mods/kubejs-forge-2001.6.5-build.26.jar | modpack_distribution | 1658792 |
| modpack/mods/Mekanism-1.20.1-10.4.16.80.jar | modpack_distribution | 12558456 |
| modpack/mods/MekanismAdditions-1.20.1-10.4.16.80.jar | modpack_distribution | 1136805 |
| modpack/mods/MekanismGenerators-1.20.1-10.4.16.80.jar | modpack_distribution | 1054586 |
| modpack/mods/MekanismTools-1.20.1-10.4.16.80.jar | modpack_distribution | 517999 |
| modpack/mods/melody_forge_1.0.3_MC_1.20.1-1.20.4.jar | modpack_distribution | 37178 |
| modpack/mods/MutantsZombies-1.4.0-Forge-mc1.20.1.jar | modpack_distribution | 472175 |
| modpack/mods/pandalib-forge-mc1.20-0.5.2-SNAPSHOT.jar | modpack_distribution | 864398 |
| modpack/mods/Placebo-1.20.1-8.6.3.jar | modpack_distribution | 282474 |
| modpack/mods/PresenceFootsteps-1.20.1-1.9.1-beta.1.jar | modpack_distribution | 7080698 |
| modpack/mods/resourcefulconfig-forge-1.20.1-2.1.3.jar | modpack_distribution | 136923 |
| modpack/mods/resourcefullib-forge-1.20.1-2.1.29.jar | modpack_distribution | 432753 |
| modpack/mods/rhino-forge-2001.2.3-build.10.jar | modpack_distribution | 1798244 |
| modpack/mods/sophisticatedbackpacks-1.20.1-3.24.62.2017.jar | modpack_distribution | 1130880 |
| modpack/mods/sophisticatedcore-1.20.1-1.3.73.2192.jar | modpack_distribution | 1622705 |
| modpack/mods/spark-1.10.53-forge.jar | modpack_distribution | 3114590 |
| modpack/mods/thermal_dynamics-1.20.1-11.0.1.23.jar | modpack_distribution | 622948 |
| modpack/mods/thermal_expansion-1.20.1-11.0.1.29.jar | modpack_distribution | 574141 |
| modpack/mods/thermal_foundation-1.20.1-11.0.6.70.jar | modpack_distribution | 4549423 |
| modpack/mods/thermal_innovation-1.20.1-11.0.1.23.jar | modpack_distribution | 225344 |
| modpack/mods/thermal_integration-1.20.1-11.0.1.27.jar | modpack_distribution | 333350 |
| modpack/mods/waystones-forge-1.20.1-14.1.20.jar | modpack_distribution | 533738 |
| modpack/mods/YungsApi-1.20-Forge-4.0.6.jar | modpack_distribution | 370524 |
| modpack/mods/YungsBetterMineshafts-1.20-Forge-4.0.4.jar | modpack_distribution | 492998 |
| modpack/mods/zombieawareness-1.20.1-1.13.1.jar | modpack_distribution | 291139 |
| modpack/tree_backup_20260829_030842/FallingTree-1.20.1-4.3.4.jar | modpack_backup | 420179 |
| NPC_STAGE4B_REVIEW.zip | auxiliary_source | 4763 |
| OXYGEN_BACK_SLOT_SOURCE.zip | auxiliary_source | 13959 |
| run/backup_fallingtree_20260829_020743/mods/FallingTree-1.20.1-4.3.4.jar | runtime_copy_or_pack | 420179 |
| run/disabled_tree_mods/FallingTree-1.20.1-4.3.4.jar | runtime_copy_or_pack | 420179 |
| run/disabled_tree_mods_20260829_025345/FallingTree-1.20.1-4.3.4.jar | runtime_copy_or_pack | 420179 |
| run/disabled_tree_mods_20260829_025436/fallingtrees-forge-mc1.20-0.13.2-SNAPSHOT.jar | runtime_copy_or_pack | 4025908 |
| run/disabled_tree_mods_20260829_025436/pandalib-forge-mc1.20-0.5.2-SNAPSHOT.jar | runtime_copy_or_pack | 864398 |
| run/local/ftbchunks/data/0d8b3591-dc53-4441-8b1d-979d1fa3b166/minecraft_overworld/0EA5F-0EA5F.zip | runtime_copy_or_pack | 35520 |
| run/local/ftbchunks/data/0d8b3591-dc53-4441-8b1d-979d1fa3b166/minecraft_overworld/0EA5F-0EA60.zip | runtime_copy_or_pack | 19673 |
| run/local/ftbchunks/data/0d8b3591-dc53-4441-8b1d-979d1fa3b166/minecraft_overworld/0EA60-0EA5F.zip | runtime_copy_or_pack | 65552 |
| run/local/ftbchunks/data/0d8b3591-dc53-4441-8b1d-979d1fa3b166/minecraft_overworld/0EA60-0EA60.zip | runtime_copy_or_pack | 39106 |
| run/local/ftbchunks/data/29eb205e-a099-4cea-b0f1-0a4da4ef5ea1/minecraft_overworld/0EA5F-0EA61.zip | runtime_copy_or_pack | 30576 |
| run/local/ftbchunks/data/29eb205e-a099-4cea-b0f1-0a4da4ef5ea1/minecraft_overworld/0EA60-0EA61.zip | runtime_copy_or_pack | 6951 |
| run/local/ftbchunks/data/3296997d-4104-4e08-af1e-e254cf1489b5/minecraft_overworld/0EA5F-0EA5F.zip | runtime_copy_or_pack | 63288 |
| run/local/ftbchunks/data/3296997d-4104-4e08-af1e-e254cf1489b5/minecraft_overworld/0EA5F-0EA60.zip | runtime_copy_or_pack | 53050 |
| run/local/ftbchunks/data/3296997d-4104-4e08-af1e-e254cf1489b5/minecraft_overworld/0EA60-0EA5F.zip | runtime_copy_or_pack | 33366 |
| run/local/ftbchunks/data/3296997d-4104-4e08-af1e-e254cf1489b5/minecraft_overworld/0EA60-0EA60.zip | runtime_copy_or_pack | 24212 |
| run/local/ftbchunks/data/349fe0a0-4056-4785-82bb-2f6e95033b53/minecraft_overworld/0EA5F-0EA5F.zip | runtime_copy_or_pack | 27968 |
| run/local/ftbchunks/data/349fe0a0-4056-4785-82bb-2f6e95033b53/minecraft_overworld/0EA5F-0EA60.zip | runtime_copy_or_pack | 38111 |
| run/local/ftbchunks/data/349fe0a0-4056-4785-82bb-2f6e95033b53/minecraft_overworld/0EA60-0EA5F.zip | runtime_copy_or_pack | 40942 |
| run/local/ftbchunks/data/349fe0a0-4056-4785-82bb-2f6e95033b53/minecraft_overworld/0EA60-0EA60.zip | runtime_copy_or_pack | 53101 |
| run/local/ftbchunks/data/48fb5a7b-5767-45ed-be35-425b322d6a07/minecraft_overworld/0EA5F-0EA5F.zip | runtime_copy_or_pack | 21402 |
| run/local/ftbchunks/data/48fb5a7b-5767-45ed-be35-425b322d6a07/minecraft_overworld/0EA5F-0EA60.zip | runtime_copy_or_pack | 25619 |
| run/local/ftbchunks/data/48fb5a7b-5767-45ed-be35-425b322d6a07/minecraft_overworld/0EA60-0EA5F.zip | runtime_copy_or_pack | 27700 |
| run/local/ftbchunks/data/48fb5a7b-5767-45ed-be35-425b322d6a07/minecraft_overworld/0EA60-0EA60.zip | runtime_copy_or_pack | 38413 |
| run/local/ftbchunks/data/5094ec50-4100-4d3c-9d94-2085366b6cd2/minecraft_overworld/0EA5F-0EA60.zip | runtime_copy_or_pack | 14607 |
| run/local/ftbchunks/data/5094ec50-4100-4d3c-9d94-2085366b6cd2/minecraft_overworld/0EA5F-0EA61.zip | runtime_copy_or_pack | 104940 |
| run/local/ftbchunks/data/5094ec50-4100-4d3c-9d94-2085366b6cd2/minecraft_overworld/0EA60-0EA60.zip | runtime_copy_or_pack | 1928 |
| run/local/ftbchunks/data/5094ec50-4100-4d3c-9d94-2085366b6cd2/minecraft_overworld/0EA60-0EA61.zip | runtime_copy_or_pack | 25437 |
| run/local/ftbchunks/data/8bfb5bf2-6608-48f5-a90e-9b34a71fe31b/minecraft_overworld/0EA5E-0EA5E.zip | runtime_copy_or_pack | 32360 |
| run/local/ftbchunks/data/8bfb5bf2-6608-48f5-a90e-9b34a71fe31b/minecraft_overworld/0EA5F-0EA5D.zip | runtime_copy_or_pack | 7517 |
| run/local/ftbchunks/data/8bfb5bf2-6608-48f5-a90e-9b34a71fe31b/minecraft_overworld/0EA5F-0EA5E.zip | runtime_copy_or_pack | 112548 |
| run/local/ftbchunks/data/a1424c96-647f-4510-ac1d-54191cc0847f/minecraft_overworld/0EA5F-0EA60.zip | runtime_copy_or_pack | 91758 |
| run/local/ftbchunks/data/a1424c96-647f-4510-ac1d-54191cc0847f/minecraft_overworld/0EA5F-0EA61.zip | runtime_copy_or_pack | 40654 |
| run/local/ftbchunks/data/a1424c96-647f-4510-ac1d-54191cc0847f/minecraft_overworld/0EA60-0EA60.zip | runtime_copy_or_pack | 32640 |
| run/local/ftbchunks/data/a1424c96-647f-4510-ac1d-54191cc0847f/minecraft_overworld/0EA60-0EA61.zip | runtime_copy_or_pack | 6286 |
| run/local/ftbchunks/data/c96ff273-ca76-42f9-8a44-30459c987a6c/minecraft_overworld/0EA5F-0EA5F.zip | runtime_copy_or_pack | 31428 |
| run/local/ftbchunks/data/c96ff273-ca76-42f9-8a44-30459c987a6c/minecraft_overworld/0EA5F-0EA60.zip | runtime_copy_or_pack | 31411 |
| run/local/ftbchunks/data/c96ff273-ca76-42f9-8a44-30459c987a6c/minecraft_overworld/0EA60-0EA5F.zip | runtime_copy_or_pack | 15427 |
| run/local/ftbchunks/data/c96ff273-ca76-42f9-8a44-30459c987a6c/minecraft_overworld/0EA60-0EA60.zip | runtime_copy_or_pack | 21878 |
| run/local/ftbchunks/data/cbf63490-b1c8-43de-960a-0df37c2ab901/minecraft_overworld/0EA5F-0EA5F.zip | runtime_copy_or_pack | 56274 |
| run/local/ftbchunks/data/cbf63490-b1c8-43de-960a-0df37c2ab901/minecraft_overworld/0EA5F-0EA60.zip | runtime_copy_or_pack | 17819 |
| run/local/ftbchunks/data/cbf63490-b1c8-43de-960a-0df37c2ab901/minecraft_overworld/0EA60-0EA5F.zip | runtime_copy_or_pack | 44205 |
| run/local/ftbchunks/data/cbf63490-b1c8-43de-960a-0df37c2ab901/minecraft_overworld/0EA60-0EA60.zip | runtime_copy_or_pack | 17199 |
| run/local/ftbchunks/data/d174a993-5d89-4000-a570-b235672787d0/minecraft_overworld/0EA5F-0EA5F.zip | runtime_copy_or_pack | 45654 |
| run/local/ftbchunks/data/d174a993-5d89-4000-a570-b235672787d0/minecraft_overworld/0EA5F-0EA60.zip | runtime_copy_or_pack | 40060 |
| run/local/ftbchunks/data/d174a993-5d89-4000-a570-b235672787d0/minecraft_overworld/0EA60-0EA5F.zip | runtime_copy_or_pack | 31813 |
| run/local/ftbchunks/data/d174a993-5d89-4000-a570-b235672787d0/minecraft_overworld/0EA60-0EA60.zip | runtime_copy_or_pack | 34339 |
| run/local/ftbchunks/data/d7a4a677-d7d7-4dcd-aa2c-56f7b90622e9/minecraft_overworld/0EA5F-0EA5F.zip | runtime_copy_or_pack | 11651 |
| run/local/ftbchunks/data/d7a4a677-d7d7-4dcd-aa2c-56f7b90622e9/minecraft_overworld/0EA5F-0EA60.zip | runtime_copy_or_pack | 10326 |
| run/local/ftbchunks/data/d7a4a677-d7d7-4dcd-aa2c-56f7b90622e9/minecraft_overworld/0EA60-0EA5F.zip | runtime_copy_or_pack | 8768 |
| run/local/ftbchunks/data/d7a4a677-d7d7-4dcd-aa2c-56f7b90622e9/minecraft_overworld/0EA60-0EA60.zip | runtime_copy_or_pack | 11177 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5C-0EA5D.zip | runtime_copy_or_pack | 15360 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5C-0EA5E.zip | runtime_copy_or_pack | 33012 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5C-0EA5F.zip | runtime_copy_or_pack | 10588 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5D-0EA5D.zip | runtime_copy_or_pack | 156711 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5D-0EA5E.zip | runtime_copy_or_pack | 192311 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5D-0EA5F.zip | runtime_copy_or_pack | 159405 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5D-0EA60.zip | runtime_copy_or_pack | 8990 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5E-0EA5B.zip | runtime_copy_or_pack | 34144 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5E-0EA5C.zip | runtime_copy_or_pack | 40064 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5E-0EA5D.zip | runtime_copy_or_pack | 165164 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5E-0EA5E.zip | runtime_copy_or_pack | 236140 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5E-0EA5F.zip | runtime_copy_or_pack | 115906 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5E-0EA60.zip | runtime_copy_or_pack | 71252 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5E-0EA61.zip | runtime_copy_or_pack | 54149 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5F-0EA5B.zip | runtime_copy_or_pack | 81746 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5F-0EA5C.zip | runtime_copy_or_pack | 99736 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5F-0EA5D.zip | runtime_copy_or_pack | 156634 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5F-0EA5E.zip | runtime_copy_or_pack | 214887 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5F-0EA5F.zip | runtime_copy_or_pack | 144057 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5F-0EA60.zip | runtime_copy_or_pack | 131374 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5F-0EA61.zip | runtime_copy_or_pack | 204402 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA5F-0EA62.zip | runtime_copy_or_pack | 33890 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA60-0EA5B.zip | runtime_copy_or_pack | 18677 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA60-0EA5C.zip | runtime_copy_or_pack | 26322 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA60-0EA5D.zip | runtime_copy_or_pack | 61761 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA60-0EA5E.zip | runtime_copy_or_pack | 66489 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA60-0EA5F.zip | runtime_copy_or_pack | 14771 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA60-0EA61.zip | runtime_copy_or_pack | 73472 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_overworld/0EA60-0EA62.zip | runtime_copy_or_pack | 29476 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_the_end/0EA5F-0EA5E.zip | runtime_copy_or_pack | 1807 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_the_end/0EA5F-0EA5F.zip | runtime_copy_or_pack | 13383 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_the_end/0EA5F-0EA60.zip | runtime_copy_or_pack | 13521 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_the_end/0EA5F-0EA61.zip | runtime_copy_or_pack | 1804 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_the_end/0EA60-0EA5E.zip | runtime_copy_or_pack | 1985 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_the_end/0EA60-0EA5F.zip | runtime_copy_or_pack | 11748 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_the_end/0EA60-0EA60.zip | runtime_copy_or_pack | 9869 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_the_end/0EA60-0EA61.zip | runtime_copy_or_pack | 1888 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_the_end/0EA61-0EA5F.zip | runtime_copy_or_pack | 3872 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_the_end/0EA61-0EA60.zip | runtime_copy_or_pack | 3848 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_the_nether/0EA5F-0EA5F.zip | runtime_copy_or_pack | 104133 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_the_nether/0EA5F-0EA60.zip | runtime_copy_or_pack | 73695 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_the_nether/0EA60-0EA5F.zip | runtime_copy_or_pack | 65579 |
| run/local/ftbchunks/data/edee6c57-018b-4c68-a72f-c6a5dc660d6a/minecraft_the_nether/0EA60-0EA60.zip | runtime_copy_or_pack | 56855 |
| run/mods.zip | runtime_copy_or_pack | 196771172 |
| run/mods_dev_hold_all/ad_astra-forge-1.20.1-1.15.20.jar | runtime_copy_or_pack | 7508861 |
| run/mods_dev_hold_all/AI-Improvements-1.20-0.5.2.jar | runtime_copy_or_pack | 29553 |
| run/mods_dev_hold_all/alexscaves-2.0.2.jar | runtime_copy_or_pack | 74871657 |
| run/mods_dev_hold_all/AmbientSounds_FORGE_v6.3.8_mc1.20.1.jar | runtime_copy_or_pack | 53429226 |
| run/mods_dev_hold_all/architectury-9.2.14-forge (1).jar | runtime_copy_or_pack | 580602 |
| run/mods_dev_hold_all/athena-forge-1.20.1-3.1.2.jar | runtime_copy_or_pack | 91808 |
| run/mods_dev_hold_all/awcapi-neoforge-1.20.1-1.0.2.jar | runtime_copy_or_pack | 89085 |
| run/mods_dev_hold_all/balm-forge-1.20.1-7.3.42.jar | runtime_copy_or_pack | 561670 |
| run/mods_dev_hold_all/betterarcheology-1.2.1-1.20.1.jar | runtime_copy_or_pack | 982838 |
| run/mods_dev_hold_all/botarium-forge-1.20.1-2.3.4.jar | runtime_copy_or_pack | 157633 |
| run/mods_dev_hold_all/BrewinAndChewin-1.20.1-3.2.1.jar | runtime_copy_or_pack | 1206255 |
| run/mods_dev_hold_all/Chunky-1.3.146.jar | runtime_copy_or_pack | 341451 |
| run/mods_dev_hold_all/citadel-2.6.3-1.20.1.jar | runtime_copy_or_pack | 3185275 |
| run/mods_dev_hold_all/Clumps-forge-1.20.1-12.0.0.4.jar | runtime_copy_or_pack | 20299 |
| run/mods_dev_hold_all/cofh_core-1.20.1-11.0.2.56.jar | runtime_copy_or_pack | 1509649 |
| run/mods_dev_hold_all/common-networking-forge-1.0.6-1.20.1.jar | runtime_copy_or_pack | 21714 |
| run/mods_dev_hold_all/Corgilib-Forge-1.20.1-4.0.3.4.jar | runtime_copy_or_pack | 360700 |
| run/mods_dev_hold_all/coroutil-forge-1.20.1-1.3.7.jar | runtime_copy_or_pack | 64187 |
| run/mods_dev_hold_all/CraftTweaker-forge-1.20.1-14.0.60.jar | runtime_copy_or_pack | 3271742 |
| run/mods_dev_hold_all/CreativeCore_FORGE_v2.12.39_mc1.20.1.jar | runtime_copy_or_pack | 1144128 |
| run/mods_dev_hold_all/cupboard-1.20.1-3.9.jar | runtime_copy_or_pack | 31724 |
| run/mods_dev_hold_all/curios-forge-5.14.1+1.20.1.jar | runtime_copy_or_pack | 398066 |
| run/mods_dev_hold_all/CustomNPCs-1.20.1-GBPort-Unofficial-1.20.1.20260711.jar | runtime_copy_or_pack | 10873769 |
| run/mods_dev_hold_all/Data_Anchor-forge-1.20.1-1.0.0.20.jar | runtime_copy_or_pack | 112171 |
| run/mods_dev_hold_all/domesurvival-0.2.0-dev.jar | runtime_copy_or_pack | 179180984 |
| run/mods_dev_hold_all/Dungeon Crawl-1.20.1-2.3.15.jar | runtime_copy_or_pack | 758123 |
| run/mods_dev_hold_all/DungeonsArise-1.20.1-2.1.57-release.jar | runtime_copy_or_pack | 8579488 |
| run/mods_dev_hold_all/EnderIO-1.20.1-6.2.18-beta-all.jar | runtime_copy_or_pack | 8086393 |
| run/mods_dev_hold_all/Enhanced-Celestials-forge-1.20.1-5.0.3.2.jar | runtime_copy_or_pack | 1629693 |
| run/mods_dev_hold_all/entityculling-forge-1.10.5-mc1.20.1.jar | runtime_copy_or_pack | 1590221 |
| run/mods_dev_hold_all/FallingTree-1.20.1-4.3.4.jar | runtime_copy_or_pack | 420179 |
| run/mods_dev_hold_all/fancymenu_forge_3.9.9_MC_1.20.1.jar | runtime_copy_or_pack | 26174014 |
| run/mods_dev_hold_all/FarmersDelight-1.20.1-1.3.2.jar | runtime_copy_or_pack | 3313327 |
| run/mods_dev_hold_all/FastFurnace-1.20.1-8.0.2.jar | runtime_copy_or_pack | 6170 |
| run/mods_dev_hold_all/FastSuite-1.20.1-5.1.2.jar | runtime_copy_or_pack | 26600 |
| run/mods_dev_hold_all/FastWorkbench-1.20.1-8.0.4.jar | runtime_copy_or_pack | 28188 |
| run/mods_dev_hold_all/ferritecore-6.0.1-forge.jar | runtime_copy_or_pack | 123034 |
| run/mods_dev_hold_all/framework-forge-1.20.1-0.8.0.jar | runtime_copy_or_pack | 383868 |
| run/mods_dev_hold_all/ftb-chunks-forge-2001.3.8.jar | runtime_copy_or_pack | 1159043 |
| run/mods_dev_hold_all/ftb-library-forge-2001.2.13.jar | runtime_copy_or_pack | 780558 |
| run/mods_dev_hold_all/ftb-quests-forge-2001.4.22.jar | runtime_copy_or_pack | 1255469 |
| run/mods_dev_hold_all/ftb-teams-forge-2001.3.2.jar | runtime_copy_or_pack | 252579 |
| run/mods_dev_hold_all/geckolib-forge-1.20.1-4.4.9.jar | runtime_copy_or_pack | 991552 |
| run/mods_dev_hold_all/gravestone-forge-1.20.1-1.0.35.jar | runtime_copy_or_pack | 317181 |
| run/mods_dev_hold_all/ImmersiveEngineering-1.20.1-10.2.0-183.jar | runtime_copy_or_pack | 12129396 |
| run/mods_dev_hold_all/invtweaks-1.20.1-1.2.2.jar | runtime_copy_or_pack | 75049 |
| run/mods_dev_hold_all/ironchest-1.20.1-14.4.4.jar | runtime_copy_or_pack | 311290 |
| run/mods_dev_hold_all/Jade-1.20.1-Forge-11.13.3.jar | runtime_copy_or_pack | 553487 |
| run/mods_dev_hold_all/jei-1.20.1-forge-15.21.0.148.jar | runtime_copy_or_pack | 1426211 |
| run/mods_dev_hold_all/journeymap-forge-1.20.1-6.0.0.jar | runtime_copy_or_pack | 4184996 |
| run/mods_dev_hold_all/konkrete_forge_1.8.0_MC_1.20-1.20.1.jar | runtime_copy_or_pack | 625185 |
| run/mods_dev_hold_all/kubejs-forge-2001.6.5-build.26.jar | runtime_copy_or_pack | 1658792 |
| run/mods_dev_hold_all/lootintegrations-1.20.1-4.7.jar | runtime_copy_or_pack | 42220 |
| run/mods_dev_hold_all/lostcities-1.20-7.5.2.jar | runtime_copy_or_pack | 1402158 |
| run/mods_dev_hold_all/mcjtylib-1.20-8.0.8.jar | runtime_copy_or_pack | 714454 |
| run/mods_dev_hold_all/Mekanism-1.20.1-10.4.16.80.jar | runtime_copy_or_pack | 12558456 |
| run/mods_dev_hold_all/MekanismAdditions-1.20.1-10.4.16.80.jar | runtime_copy_or_pack | 1136805 |
| run/mods_dev_hold_all/MekanismGenerators-1.20.1-10.4.16.80.jar | runtime_copy_or_pack | 1054586 |
| run/mods_dev_hold_all/MekanismTools-1.20.1-10.4.16.80.jar | runtime_copy_or_pack | 517999 |
| run/mods_dev_hold_all/melody_forge_1.0.3_MC_1.20.1-1.20.4.jar | runtime_copy_or_pack | 37178 |
| run/mods_dev_hold_all/MutantsZombies-1.4.0-Forge-mc1.20.1.jar | runtime_copy_or_pack | 472175 |
| run/mods_dev_hold_all/Placebo-1.20.1-8.6.3.jar | runtime_copy_or_pack | 282474 |
| run/mods_dev_hold_all/PresenceFootsteps-1.20.1-1.9.1-beta.1.jar | runtime_copy_or_pack | 7080698 |
| run/mods_dev_hold_all/resourcefulconfig-forge-1.20.1-2.1.3.jar | runtime_copy_or_pack | 136923 |
| run/mods_dev_hold_all/resourcefullib-forge-1.20.1-2.1.29.jar | runtime_copy_or_pack | 432753 |
| run/mods_dev_hold_all/rhino-forge-2001.2.3-build.10.jar | runtime_copy_or_pack | 1798244 |
| run/mods_dev_hold_all/sophisticatedbackpacks-1.20.1-3.24.62.2017.jar | runtime_copy_or_pack | 1130880 |
| run/mods_dev_hold_all/sophisticatedcore-1.20.1-1.3.73.2192.jar | runtime_copy_or_pack | 1622705 |
| run/mods_dev_hold_all/spark-1.10.53-forge.jar | runtime_copy_or_pack | 3114590 |
| run/mods_dev_hold_all/supermartijn642configlib-1.1.8-forge-mc1.20.jar | runtime_copy_or_pack | 206584 |
| run/mods_dev_hold_all/thermal_core-1.20.1-11.0.6.24.jar | runtime_copy_or_pack | 4410354 |
| run/mods_dev_hold_all/thermal_dynamics-1.20.1-11.0.1.23.jar | runtime_copy_or_pack | 622948 |
| run/mods_dev_hold_all/thermal_expansion-1.20.1-11.0.1.29.jar | runtime_copy_or_pack | 574141 |
| run/mods_dev_hold_all/thermal_foundation-1.20.1-11.0.6.70.jar | runtime_copy_or_pack | 4549423 |
| run/mods_dev_hold_all/thermal_innovation-1.20.1-11.0.1.23.jar | runtime_copy_or_pack | 225344 |
| run/mods_dev_hold_all/thermal_integration-1.20.1-11.0.1.27.jar | runtime_copy_or_pack | 333350 |
| run/mods_dev_hold_all/The_Graveyard_3.1_(FORGE)_for_1.20.1.jar | runtime_copy_or_pack | 10298153 |
| run/mods_dev_hold_all/waystones-forge-1.20.1-14.1.20.jar | runtime_copy_or_pack | 533738 |
| run/mods_dev_hold_all/yulari-1.20.1-lostcities.jar | runtime_copy_or_pack | 344069 |
| run/mods_dev_hold_all/YungsApi-1.20-Forge-4.0.6.jar | runtime_copy_or_pack | 370524 |
| run/mods_dev_hold_all/YungsBetterDesertTemples-1.20-Forge-3.0.3.jar | runtime_copy_or_pack | 918012 |
| run/mods_dev_hold_all/YungsBetterDungeons-1.20-Forge-4.0.4.jar | runtime_copy_or_pack | 792381 |
| run/mods_dev_hold_all/YungsBetterMineshafts-1.20-Forge-4.0.4.jar | runtime_copy_or_pack | 492998 |
| run/mods_dev_hold_all/YungsBetterStrongholds-1.20-Forge-4.0.3.jar | runtime_copy_or_pack | 473564 |
| run/mods_dev_hold_all/zombieawareness-1.20.1-1.13.1.jar | runtime_copy_or_pack | 291139 |
| run/mods_sync_quarantine/alexscaves-2.0.2.jar | runtime_copy_or_pack | 74871657 |
| run/mods_sync_quarantine/carryon-forge-1.20.1-2.1.2.7.jar | runtime_copy_or_pack | 439457 |
| run/mods_sync_quarantine/cloth-config-11.1.136-forge.jar | runtime_copy_or_pack | 1181413 |
| run/mods_sync_quarantine/curios-forge-5.14.1+1.20.1.jar | runtime_copy_or_pack | 398066 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.jar | runtime_copy_or_pack | 179151689 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260826_173137528.jar | runtime_copy_or_pack | 25701821 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260826_173740182.jar | runtime_copy_or_pack | 25701821 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260826_175615423.jar | runtime_copy_or_pack | 25679615 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260826_180813841.jar | runtime_copy_or_pack | 25679681 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260826_183205200.jar | runtime_copy_or_pack | 25676456 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260826_185831256.jar | runtime_copy_or_pack | 25716913 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260826_204642239.jar | runtime_copy_or_pack | 25730466 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260826_223015794.jar | runtime_copy_or_pack | 25851502 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260826_225434003.jar | runtime_copy_or_pack | 25859241 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260826_232243132.jar | runtime_copy_or_pack | 25889551 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260826_234310282.jar | runtime_copy_or_pack | 25889432 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260827_000446988.jar | runtime_copy_or_pack | 25887582 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260827_002011952.jar | runtime_copy_or_pack | 25887513 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260827_165651229.jar | runtime_copy_or_pack | 25886258 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260827_184756770.jar | runtime_copy_or_pack | 27085659 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260827_192719079.jar | runtime_copy_or_pack | 27085700 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260827_215843552.jar | runtime_copy_or_pack | 27176502 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260827_220534943.jar | runtime_copy_or_pack | 27176502 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260827_221901495.jar | runtime_copy_or_pack | 27177327 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260827_224707807.jar | runtime_copy_or_pack | 27248739 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260827_232212186.jar | runtime_copy_or_pack | 27256010 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260828_165830613.jar | runtime_copy_or_pack | 27275989 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260828_171945644.jar | runtime_copy_or_pack | 27275274 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260829_014735883.jar | runtime_copy_or_pack | 27298378 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260829_024541234.jar | runtime_copy_or_pack | 27290216 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260829_154255329.jar | runtime_copy_or_pack | 135046698 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260829_160503743.jar | runtime_copy_or_pack | 163367553 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260829_162616582.jar | runtime_copy_or_pack | 166438113 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260829_164241681.jar | runtime_copy_or_pack | 166440421 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260829_204704018.jar | runtime_copy_or_pack | 166770213 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260903_121053625.jar | runtime_copy_or_pack | 211211392 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260903_123959930.jar | runtime_copy_or_pack | 219312576 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260904_103443221.jar | runtime_copy_or_pack | 237027692 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260907_212225000.jar | runtime_copy_or_pack | 178706829 |
| run/mods_sync_quarantine/domesurvival-0.1.1-dev.replaced.20260907_235205718.jar | runtime_copy_or_pack | 178766694 |
| run/mods_sync_quarantine/domesurvival-0.2.0-dev.jar | runtime_copy_or_pack | 179166623 |
| run/mods_sync_quarantine/drippyloadingscreen_forge_3.1.5_MC_1.20.1.jar | runtime_copy_or_pack | 281881 |
| run/mods_sync_quarantine/efallingtrees-0.7.0-1.20+forge.jar | runtime_copy_or_pack | 1390721 |
| run/mods_sync_quarantine/embeddium-0.3.31+mc1.20.1.jar | runtime_copy_or_pack | 1320675 |
| run/mods_sync_quarantine/entity_model_features-3.2.3-1.20.1-forge.20260829_222905439.jar | runtime_copy_or_pack | 751492 |
| run/mods_sync_quarantine/entity_model_features-3.2.3-1.20.1-forge.jar | runtime_copy_or_pack | 751492 |
| run/mods_sync_quarantine/entity_model_features-3.2.4-1.20.1-forge.jar | runtime_copy_or_pack | 752458 |
| run/mods_sync_quarantine/entity_model_features_1.20.1-forge-3.2.1.jar | runtime_copy_or_pack | 750279 |
| run/mods_sync_quarantine/entity_model_features_2.2.5-forge-1.20.1.jar | runtime_copy_or_pack | 430927 |
| run/mods_sync_quarantine/entity_texture_features_1.20.1-forge-7.1.20260829_215610850.jar | runtime_copy_or_pack | 944851 |
| run/mods_sync_quarantine/entity_texture_features_1.20.1-forge-7.1.20260829_220855614.jar | runtime_copy_or_pack | 944851 |
| run/mods_sync_quarantine/entity_texture_features_1.20.1-forge-7.1.20260829_222905448.jar | runtime_copy_or_pack | 944851 |
| run/mods_sync_quarantine/entity_texture_features_1.20.1-forge-7.1.jar | runtime_copy_or_pack | 944851 |
| run/mods_sync_quarantine/entity_texture_features_6.2.4-forge-1.20.1.jar | runtime_copy_or_pack | 655405 |
| run/mods_sync_quarantine/fallingtrees-forge-0.12.2-1.20.jar | runtime_copy_or_pack | 4005421 |
| run/mods_sync_quarantine/fallingtrees-forge-mc1.20-0.13.2-SNAPSHOT.20260829_025757146.jar | runtime_copy_or_pack | 1566480 |
| run/mods_sync_quarantine/fallingtrees-forge-mc1.20-0.13.2-SNAPSHOT.jar | runtime_copy_or_pack | 4025908 |
| run/mods_sync_quarantine/ImmediatelyFast-1.2.4+1.20.1.jar | runtime_copy_or_pack | 625873 |
| run/mods_sync_quarantine/lootintegrations-1.20.1-4.7.jar | runtime_copy_or_pack | 42220 |
| run/mods_sync_quarantine/mobends-1.20.1-5.4.2-forge.jar | runtime_copy_or_pack | 1414670 |
| run/mods_sync_quarantine/modernfix-forge-5.27.66+mc1.20.1.jar | runtime_copy_or_pack | 1002234 |
| run/mods_sync_quarantine/oculus-mc1.20.1-1.8.0.jar | runtime_copy_or_pack | 2851119 |
| run/mods_sync_quarantine/pandalib-forge-0.3-1.20.jar | runtime_copy_or_pack | 90902 |
| run/mods_sync_quarantine/pandalib-forge-mc1.20-0.5.2-SNAPSHOT.20260829_025757157.jar | runtime_copy_or_pack | 864398 |
| run/mods_sync_quarantine/pandalib-forge-mc1.20-0.5.2-SNAPSHOT.jar | runtime_copy_or_pack | 864398 |
| run/mods_sync_quarantine/sodiumdynamiclights-forge-1.0.10-1.20.1.jar | runtime_copy_or_pack | 511601 |
| run/mods_sync_quarantine/sound-physics-remastered-forge-1.20.1-1.4.10.jar | runtime_copy_or_pack | 203819 |
| run/mods_sync_quarantine/spawnanimations-v1.10.1-mc1.17-1.21.5-mod.20260829_220855622.jar | runtime_copy_or_pack | 182692 |
| run/mods_sync_quarantine/spawnanimations-v1.10.1-mc1.17-1.21.5-mod.20260829_222905451.jar | runtime_copy_or_pack | 182692 |
| run/mods_sync_quarantine/spawnanimations-v1.10.1-mc1.17-1.21.5-mod.jar | runtime_copy_or_pack | 182692 |
| run/mods_sync_quarantine/thermal_core-1.20.1-11.0.6.24.jar | runtime_copy_or_pack | 4410354 |
| run/resourcepacks/Blues_Better_Monstersv0.11.zip | runtime_copy_or_pack | 708323 |
| run/resourcepacks/dome_survival_ui_fix6.zip | runtime_copy_or_pack | 1367 |
| run/resourcepacks/dome_survival_ui_fix7.zip | runtime_copy_or_pack | 1367 |
| run/resourcepacks/dome_survival_ui_fix8.zip | runtime_copy_or_pack | 1367 |
| run/resourcepacks/dome_survival_ui_fix9.zip | runtime_copy_or_pack | 4595 |
| run/resourcepacks/FreshAnimations_v1.10.4.zip | runtime_copy_or_pack | 850941 |
| SANDSTORM_COLLECT.zip | inactive_patch_or_backup | 26948 |
| SURVIVAL_MECHANICS_COLLECT.zip | inactive_patch_or_backup | 110773 |
| V55_1_V56_1_FIX_COLLECT.zip | inactive_patch_or_backup | 17685945 |
| V56_AIRLOCK_COLLECT.zip | inactive_patch_or_backup | 17544051 |
| V56_LOCALIZATION_RECOVERY_COLLECT.zip | inactive_patch_or_backup | 215745 |
| V58_1_GATE_RENDER_FIX_COLLECT.zip | inactive_patch_or_backup | 17665 |
| V58_STARTER_DOME_GATE_MIGRATION_COLLECT.zip | inactive_patch_or_backup | 17671638 |
| V59_VENTILATION_MODE_COLLECT.zip | inactive_patch_or_backup | 16614712 |
| V60_1_VARIABLE_GATE_SIZES_COLLECT.zip | inactive_patch_or_backup | 160901 |
| V60_2_GATE_BACKFACE_FIX_COLLECT.zip | inactive_patch_or_backup | 101198 |
| V60_SEALED_ROOM_MANAGER_COLLECT.zip | inactive_patch_or_backup | 17732314 |
| WASTED_TEST_BEFORE_FULLDEV_V6_6.zip | auxiliary_source | 359715092 |
| WORKSHOP_RESET_COLLECT.zip | inactive_patch_or_backup | 38329 |
| _manual_backups/phase10_0_technology_20260826_001910/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260826_002103/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 25669903 |
| _manual_backups/phase10_0_technology_20260826_171024/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 25670065 |
| _manual_backups/phase10_0_technology_20260826_172839/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 25670048 |
| _manual_backups/phase10_0_technology_20260826_173013/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 25701763 |
| _manual_backups/phase10_0_technology_20260826_173703/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260826_175510/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260826_180704/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260826_183130/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260826_185558/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260826_193851/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260826_222630/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260826_225313/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260826_231732/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260826_232130/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 25906943 |
| _manual_backups/phase10_0_technology_20260826_234152/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260827_000404/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260827_001944/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260827_165541/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260827_180424/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260827_181249/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 25952672 |
| _manual_backups/phase10_0_technology_20260827_184417/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 25953056 |
| _manual_backups/phase10_0_technology_20260827_184641/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 27086231 |
| _manual_backups/phase10_0_technology_20260827_190405/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260827_215752/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260827_220510/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260827_221807/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260827_224545/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260827_232111/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260828_163501/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260828_165718/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 27257798 |
| _manual_backups/phase10_0_technology_20260828_171619/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260828_173626/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260829_014430/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 27279088 |
| _manual_backups/phase10_0_technology_20260829_024037/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260829_154058/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260829_155808/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260829_162503/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260829_164104/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260829_204505/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 16718714 |
| _manual_backups/phase10_0_technology_20260908_223335/domesurvival-0.1.1-dev.jar | inactive_patch_or_backup | 179151689 |
| _manual_backups/phase10_0_technology_20260908_223656/domesurvival-0.2.0-dev.jar | inactive_patch_or_backup | 179164348 |
| _manual_backups/phase10_0_technology_20260909_204916/domesurvival-0.2.0-dev.jar | inactive_patch_or_backup | 179164231 |
| _manual_backups/phase10_0_technology_20260909_210100/domesurvival-0.2.0-dev.jar | inactive_patch_or_backup | 179166623 |
| _manual_backups/phase10_0_technology_20260910_205006/domesurvival-0.2.0-dev.jar | inactive_patch_or_backup | 179202830 |
| _manual_backups/phase10_0_technology_20260910_212158/domesurvival-0.2.0-dev.jar | inactive_patch_or_backup | 179203436 |
| _manual_backups/phase10_0_technology_20260910_220612/domesurvival-0.2.0-dev.jar | inactive_patch_or_backup | 179207741 |
| _manual_backups/phase10_0_technology_20260912_004210/domesurvival-0.2.0-dev.jar | inactive_patch_or_backup | 179206728 |
| _manual_backups/phase10_0_technology_20260912_005605/domesurvival-0.2.0-dev.jar | inactive_patch_or_backup | 179180984 |

## Обнаруженные технические проблемы

| Модель | Проблема |
| --- | --- |
| domesurvival:block/airlock_gate_p00_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p00_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p00_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p00_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p00_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p01_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p01_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p01_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p01_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p01_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p02_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p02_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p02_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p02_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p02_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p03_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p03_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p03_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p03_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p03_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p04_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p04_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p04_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p04_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p04_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p10_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p10_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p10_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p10_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p10_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p11_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p11_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p11_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p11_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p11_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p12_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p12_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p12_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p12_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p12_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p13_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p13_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p13_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p13_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p13_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p14_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p14_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p14_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p14_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p14_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p20_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p20_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p20_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p20_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p20_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p21_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p21_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p21_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p21_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p21_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p22_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p22_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p22_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p22_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p22_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p23_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p23_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p23_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p23_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p23_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p24_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p24_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p24_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p24_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p24_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p30_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p30_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p30_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p30_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p30_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p31_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p31_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p31_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p31_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p31_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p32_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p32_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p32_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p32_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p32_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p33_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p33_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p33_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p33_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p33_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p34_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p34_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p34_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p34_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p34_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p40_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p40_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p40_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p40_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p40_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p41_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p41_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p41_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p41_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p41_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p42_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p42_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p42_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p42_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p42_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p43_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p43_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p43_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p43_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p43_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p44_stage1 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p44_stage2 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p44_stage3 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p44_stage4 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/airlock_gate_p44_stage5 | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/coal_generator_energy_port_down | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/coal_generator_energy_port_east | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/coal_generator_energy_port_north | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/coal_generator_energy_port_south | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/coal_generator_energy_port_up | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/coal_generator_energy_port_west | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/coal_generator_fuel_port_up | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/coal_generator_item_port_down | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/coke_oven_bottom_output | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/coke_oven_item | CHECK: UV outside 0..16 at 12/down; CHECK: UV outside 0..16 at 12/east; CHECK: UV outside 0..16 at 12/up; CHECK: UV outside 0..16 at 12/west; CHECK: UV outside 0..16 at 30/down; CHECK: UV outside 0..16 at 30/east; CHECK: UV outside 0..16 at 30/up; CHECK: UV outside 0..16 at 30/west |
| domesurvival:block/coke_oven_lower | CHECK: UV outside 0..16 at 12/down; CHECK: UV outside 0..16 at 12/east; CHECK: UV outside 0..16 at 12/up; CHECK: UV outside 0..16 at 12/west |
| domesurvival:block/coke_oven_lower_on | CHECK: UV outside 0..16 at 12/down; CHECK: UV outside 0..16 at 12/east; CHECK: UV outside 0..16 at 12/up; CHECK: UV outside 0..16 at 12/west |
| domesurvival:block/coke_oven_ready | ERROR: element 51: inverted bounds; CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item); CHECK: more than 100 cuboids |
| domesurvival:block/coke_oven_ready_on | ERROR: element 51: inverted bounds; CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item); CHECK: flat element 126; CHECK: flat element 127; CHECK: more than 100 cuboids |
| domesurvival:block/coke_oven_rear_input | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/coke_oven_upper | CHECK: UV outside 0..16 at 12/down; CHECK: UV outside 0..16 at 12/east; CHECK: UV outside 0..16 at 12/up; CHECK: UV outside 0..16 at 12/west |
| domesurvival:block/coke_oven_upper_on | CHECK: UV outside 0..16 at 12/down; CHECK: UV outside 0..16 at 12/east; CHECK: UV outside 0..16 at 12/up; CHECK: UV outside 0..16 at 12/west |
| domesurvival:block/copper_furnace | CHECK: UV outside 0..16 at 12/down; CHECK: UV outside 0..16 at 12/east; CHECK: UV outside 0..16 at 12/up; CHECK: UV outside 0..16 at 12/west; CHECK: UV outside 0..16 at 15/down; CHECK: UV outside 0..16 at 15/east; CHECK: UV outside 0..16 at 15/up; CHECK: UV outside 0..16 at 15/west; CHECK: UV outside 0..16 at 16/down; CHECK: UV outside 0..16 at 16/east; CHECK: UV outside 0..16 at 16/up; CHECK: UV outside 0..16 at 16/west; CHECK: UV outside 0..16 at 17/down; CHECK: UV outside 0..16 at 17/east; CHECK: UV outside 0..16 at 17/up; CHECK: UV outside 0..16 at 17/west; CHECK: UV outside 0..16 at 18/down; CHECK: UV outside 0..16 at 18/east; CHECK: UV outside 0..16 at 18/up; CHECK: UV outside 0..16 at 18/west; CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/copper_furnace_on | CHECK: UV outside 0..16 at 12/down; CHECK: UV outside 0..16 at 12/east; CHECK: UV outside 0..16 at 12/up; CHECK: UV outside 0..16 at 12/west; CHECK: UV outside 0..16 at 15/down; CHECK: UV outside 0..16 at 15/east; CHECK: UV outside 0..16 at 15/up; CHECK: UV outside 0..16 at 15/west; CHECK: UV outside 0..16 at 16/down; CHECK: UV outside 0..16 at 16/east; CHECK: UV outside 0..16 at 16/up; CHECK: UV outside 0..16 at 16/west; CHECK: UV outside 0..16 at 17/down; CHECK: UV outside 0..16 at 17/east; CHECK: UV outside 0..16 at 17/up; CHECK: UV outside 0..16 at 17/west; CHECK: UV outside 0..16 at 18/down; CHECK: UV outside 0..16 at 18/east; CHECK: UV outside 0..16 at 18/up; CHECK: UV outside 0..16 at 18/west; CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/copper_hopper | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/copper_hopper_down | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/copper_item_pipe_item_corner_1to1 | CHECK: custom loader: JSON element bounds are not complete geometry |
| domesurvival:block/copper_item_pipe_item_world_1to1 | CHECK: custom loader: JSON element bounds are not complete geometry |
| domesurvival:block/desh_hopper | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/desh_hopper_down | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/desh_item_pipe_item_corner_1to1 | CHECK: custom loader: JSON element bounds are not complete geometry |
| domesurvival:block/desh_item_pipe_item_world_1to1 | CHECK: custom loader: JSON element bounds are not complete geometry |
| domesurvival:block/filtering_item_pipe_item_corner_1to1 | CHECK: custom loader: JSON element bounds are not complete geometry |
| domesurvival:block/filtering_item_pipe_item_world_1to1 | CHECK: custom loader: JSON element bounds are not complete geometry |
| domesurvival:block/forming_press_energy_port_down | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_energy_port_east | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_energy_port_north | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_energy_port_south | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_energy_port_up | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_energy_port_west | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_input_port_down | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_input_port_east | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_input_port_north | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_input_port_south | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_input_port_up | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_input_port_west | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_item_port_down | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_item_port_east | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_item_port_north | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_item_port_south | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_item_port_up | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_item_port_west | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_output_port_down | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_output_port_east | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_output_port_north | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_output_port_south | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_output_port_up | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/forming_press_output_port_west | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/lanos_abandoned | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item); CHECK: more than 100 cuboids |
| domesurvival:block/lanos_decorative | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item); CHECK: more than 100 cuboids |
| domesurvival:block/large_coke_oven | CHECK: custom loader: JSON element bounds are not complete geometry |
| domesurvival:block/large_coke_oven_fire | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/large_coke_oven_fire_off | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/machine_input_port_down | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/machine_input_port_east | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/machine_input_port_north | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/machine_input_port_south | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/machine_input_port_up | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/machine_input_port_west | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/machine_output_port_down | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/machine_output_port_east | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/machine_output_port_north | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/machine_output_port_south | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/machine_output_port_up | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/machine_output_port_west | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/shaft_furnace_bottom_output | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/shaft_furnace_item | CHECK: UV outside 0..16 at 12/down; CHECK: UV outside 0..16 at 12/east; CHECK: UV outside 0..16 at 12/up; CHECK: UV outside 0..16 at 12/west; CHECK: UV outside 0..16 at 30/down; CHECK: UV outside 0..16 at 30/east; CHECK: UV outside 0..16 at 30/up; CHECK: UV outside 0..16 at 30/west |
| domesurvival:block/shaft_furnace_large | CHECK: custom loader: JSON element bounds are not complete geometry |
| domesurvival:block/shaft_furnace_large_on | CHECK: custom loader: JSON element bounds are not complete geometry |
| domesurvival:block/shaft_furnace_lower | CHECK: UV outside 0..16 at 12/down; CHECK: UV outside 0..16 at 12/east; CHECK: UV outside 0..16 at 12/up; CHECK: UV outside 0..16 at 12/west |
| domesurvival:block/shaft_furnace_lower_on | CHECK: UV outside 0..16 at 12/down; CHECK: UV outside 0..16 at 12/east; CHECK: UV outside 0..16 at 12/up; CHECK: UV outside 0..16 at 12/west |
| domesurvival:block/shaft_furnace_ready | ERROR: element 51: inverted bounds; CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item); CHECK: more than 100 cuboids |
| domesurvival:block/shaft_furnace_ready_on | ERROR: element 51: inverted bounds; CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item); CHECK: flat element 102; CHECK: flat element 103; CHECK: more than 100 cuboids |
| domesurvival:block/shaft_furnace_rear_input | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/shaft_furnace_upper | CHECK: UV outside 0..16 at 12/down; CHECK: UV outside 0..16 at 12/east; CHECK: UV outside 0..16 at 12/up; CHECK: UV outside 0..16 at 12/west |
| domesurvival:block/shaft_furnace_upper_on | CHECK: UV outside 0..16 at 12/down; CHECK: UV outside 0..16 at 12/east; CHECK: UV outside 0..16 at 12/up; CHECK: UV outside 0..16 at 12/west |
| domesurvival:block/solar_panel_mk1 | CHECK: custom loader: JSON element bounds are not complete geometry |
| domesurvival:block/solar_panel_mk2 | CHECK: custom loader: JSON element bounds are not complete geometry |
| domesurvival:block/solar_panel_mk3 | CHECK: custom loader: JSON element bounds are not complete geometry |
| domesurvival:block/steel_hopper | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/steel_hopper_down | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:block/steel_item_pipe_item_corner_1to1 | CHECK: custom loader: JSON element bounds are not complete geometry |
| domesurvival:block/steel_item_pipe_item_world_1to1 | CHECK: custom loader: JSON element bounds are not complete geometry |
| domesurvival:item/coke_oven | ERROR: element 51: inverted bounds; CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item); CHECK: more than 100 cuboids |
| domesurvival:item/copper_furnace | CHECK: UV outside 0..16 at 12/down; CHECK: UV outside 0..16 at 12/east; CHECK: UV outside 0..16 at 12/up; CHECK: UV outside 0..16 at 12/west; CHECK: UV outside 0..16 at 15/down; CHECK: UV outside 0..16 at 15/east; CHECK: UV outside 0..16 at 15/up; CHECK: UV outside 0..16 at 15/west; CHECK: UV outside 0..16 at 16/down; CHECK: UV outside 0..16 at 16/east; CHECK: UV outside 0..16 at 16/up; CHECK: UV outside 0..16 at 16/west; CHECK: UV outside 0..16 at 17/down; CHECK: UV outside 0..16 at 17/east; CHECK: UV outside 0..16 at 17/up; CHECK: UV outside 0..16 at 17/west; CHECK: UV outside 0..16 at 18/down; CHECK: UV outside 0..16 at 18/east; CHECK: UV outside 0..16 at 18/up; CHECK: UV outside 0..16 at 18/west; CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:item/copper_hopper | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:item/desh_hopper | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |
| domesurvival:item/lanos_abandoned | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item); CHECK: more than 100 cuboids |
| domesurvival:item/lanos_decorative | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item); CHECK: more than 100 cuboids |
| domesurvival:item/machine_wrench | CHECK: more than 100 cuboids |
| domesurvival:item/pulse_matrix | CHECK: more than 100 cuboids |
| domesurvival:item/shaft_furnace | ERROR: element 51: inverted bounds; CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item); CHECK: more than 100 cuboids |
| domesurvival:item/steel_hopper | CHECK: bounds cross nominal block 0..16 (may be intentional multiblock/item) |

## Дополнительные аудиты и ограничения

Связи машин, портов, анимаций и неизменяемых контрактов: [machine_code_audit.md](dev/visual_overhaul/machine_code_audit.md). GUI, Curios, экипировка и эффекты: [ui_wearable_audit.md](dev/visual_overhaul/ui_wearable_audit.md). Масштаб всех активных JSON/OBJ-моделей: [SCALE_REPORT.md](SCALE_REPORT.md).

Статический аудит не заменяет Minecraft: здесь не подтверждены освещение, z-fighting в движении, UV bleeding с mipmap, Embeddium, посадка на игроке и реальные FPS. Разрешённые vanilla parent-цепочки взяты из установленной версии 1.20.1; ресурс-пак может переопределить их в игре. Внешние modpack JAR содержат сторонний контент и не являются авторскими ассетами DOMESURVIVAL.

## Контроль полноты baseline

Все 75 активных blockstate ID сопоставлены с Java-регистрацией блока. У каждого зарегистрированного самостоятельного item найден JSON-модельный ресурс. В разрешённых model parent/texture-цепочках не обнаружены отсутствующие ресурсы; parse errors = 0. Это проверка данных, а не подтверждение успешного рендера в Minecraft.

В открытых файлах проекта не обнаружены `.bbmodel`, `.geo.json` и `.animation.json`; сторонние архивы не распаковывались. Найдены 20 `.jem` файлов в копиях/патчах OptiFine entity models; это не GeckoLib-анимации активного мода. Активные JSON-loader: `forge:obj` и `forge:composite`. Пакет содержит 8 OBJ; наличие упакованного OBJ не доказывает использование: например, текущий Lanos строится JSON-cuboids с root scale 3.5, хотя старые Lanos OBJ тоже лежат в ресурсах.

Шесть flagged моделей относятся к двум семействам печей: у cuboid element 51 инвертирован один из интервалов from/to; item-модели наследуют дефект. Это baseline-проблема для отдельной локальной проверки, не основание менять gameplay или все печи вместе с эталоном. Наиболее тяжёлые JSON: `machine_wrench` 174 cuboids / 456 faces, `lanos_abandoned` 134 / 804, `coke_oven_ready_on` 128 / 637, `pulse_matrix` 112 / 672. Сокращать геометрию следует после визуального сравнения, не по одному числу.
