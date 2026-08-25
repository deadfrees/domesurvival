# Global campaign synchronization — v3.1

## Что гарантирует слой Dome Survival

FTB Quests сам хранит прогресс на уровне FTB Team. Это хорошо для обычных сборок,
но недостаточно для нашей кампании, потому что у двух игроков могут быть разные
FTB Teams.

Dome Survival добавляет поверх него server-global completion ledger.

### Теперь правило main story такое

1. Один игрок выполняет main quest.
2. Invisible internal reward вызывает `/domequest complete ...`.
3. Quest ID записывается в глобальный SavedData.
4. Тот же quest force-complete отправляется всем игрокам, которые сейчас онлайн,
   независимо от их FTB Team.
5. Offline player при следующем входе через 40 ticks получает только те global
   quests, revision которых выше его последней синхронизации.
6. Catch-up feedback подавляется, чтобы игрок при входе не получил десятки
   звуков/частиц.
7. Видимые item/XP rewards остаются персональными. Offline player после catch-up
   видит quest completed и может получить свою reward; награда не теряется.

### Производительность

- Никакого перебора offline profiles каждый tick.
- Global catch-up выполняется один раз после login и только по пропущенным revision.
- Player action checks идут 2 раза/сек только для ранних auto-action quests.
- Нет chunk loading и глобальных world scans.

### Resync dev command

`/domequest sync inspect`
`/domequest sync catchup`
`/domequest sync resync`

`resync` сбрасывает только personal sync revision и повторно накладывает global
quest completion. Мир и сюжетные flags не сбрасываются.
