Dome Survival - Custom Personal Paintings V1

Что делает патч:
- добавляет новый предмет: Картина воспоминаний;
- рецепт: 1 обычная картина -> 1 Картина воспоминаний;
- при размещении на вертикальной стене предмет вешает одну из 14 пользовательских картин;
- картины используют отдельные painting_variant ресурсы и не заменяют ванильные картины;
- иконка предмета похожа на обычную картину, но слегка отличается по цвету.

Установка:
1. Распакуйте архив в C:\domesurvival с заменой файлов.
2. Запустите APPLY_CUSTOM_PAINTINGS_V1.bat
3. Полностью перезапустите игру / клиент.

Где лежат файлы:
- kubejs/assets/domesurvival/textures/painting/*.png
- kubejs/data/domesurvival/painting_variant/*.json
- kubejs/startup_scripts/domesurvival_custom_paintings_startup.js
- kubejs/server_scripts/domesurvival_custom_paintings.js

Важно:
- нужен установленный KubeJS в модпаке;
- новые startup_scripts и ресурсы требуют полного перезапуска игры, а не только /reload.
