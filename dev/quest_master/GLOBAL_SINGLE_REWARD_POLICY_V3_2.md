# Global single-reward policy v3.2

## Каноническое правило

**Прогресс общий на весь сервер. Награда у квеста одна на весь сервер.**

Игрок, чьё реальное действие ПЕРВЫМ завершило квест:
- завершает квест для всех;
- получает item/XP reward;
- получает completion feedback.

Все остальные:
- получают completed quest;
- не получают копию награды;
- не могут забрать эту награду позже;
- при offline catch-up получают только прогресс.

## Если несколько игроков участвуют

Для квеста с несколькими задачами награду получает игрок, действие которого
закрыло квест последним и тем самым впервые создало global completion.

Пример:
- Игрок A принёс часть ресурсов.
- Игрок B выполнил последнюю незакрытую задачу.
- Квест становится completed для сервера.
- Reward получает B.

## Как это реализовано

1. Invisible internal reward теперь всегда первый в reward list.
2. Он атомарно резервирует global completion/winner.
3. Если игрок первый:
   - FTB после internal reward автоматически выдаёт visible reward ему.
4. Если это mirror/catch-up:
   - Dome Survival через узкий reflection bridge вызывает
     `TeamData.markRewardAsClaimed(...)`;
   - `Reward.claim()` НЕ вызывается;
   - FTB считает visible reward уже забранной;
   - предмет/XP не выдаётся.
5. Visible rewards имеют `team_reward: true` и `auto: "enabled"`.

## Почему reflection

Dome Survival не держит FTB Quests в compile classpath. Чтобы не вносить
жёсткую зависимость, используется только стабильный публичный контракт
FTB Quests 2001.4.x:
- `TeamData.get(Player)`
- `ServerQuestFile.INSTANCE`
- `getQuest(long)`
- `Quest.getRewards()`
- `Reward.getId()`
- `TeamData.markRewardAsClaimed(UUID, Reward, long)`

При несовместимости catch-up останавливается с ERROR вместо тихого дюпа.

## Диагностика

`/domequest sync reward_compat`

Ожидается:
`FTB reward compat: AVAILABLE`

`/domequest sync winner <QUEST_ID>`

Показывает игрока, который получил единственную global reward.
