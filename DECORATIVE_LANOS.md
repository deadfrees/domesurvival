# DomeSurvival — Decorative Lanos

Добавлены:
- `domesurvival:lanos_decorative`
- `domesurvival:lanos_abandoned`

Обе модели статичные, поворачиваются по горизонтальному `facing`, не имеют физической коллизии и не используют BlockEntity/tick/network.

Модели занимают примерно 1.54 × 2.86 × 1.0 блока и визуально выходят за anchor-блок.

Тест:
```mcfunction
/give @s domesurvival:lanos_decorative
/give @s domesurvival:lanos_abandoned
```

Исходные модели и текстуры импортированы из предоставленных архивов GOTEICRAFT (`lanos.zip`, `lanosold.zip`), namespace перенесён в `domesurvival`.
