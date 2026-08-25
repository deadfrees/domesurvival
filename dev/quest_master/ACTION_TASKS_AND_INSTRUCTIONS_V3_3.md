# Phase 3.3 — Action Tasks & Clear Instructions

## Что изменено

Во всех текущих Главе 0 и Главе 1:

1. У каждого квеста теперь есть понятный `subtitle`.
2. В начале описания явно написано:
   - `ЧТО СДЕЛАТЬ`
   - `КАК ЗАСЧИТЫВАЕТСЯ`
3. Все 16 старых ручных `checkmark` task удалены.
4. Вместо них используются `advancement` task на скрытые технические advancements Dome Survival.
5. Технические advancements имеют только `minecraft:impossible` criterion и НЕ выдаются обычной игрой.
6. Java-события Dome Survival выдают нужный advancement только после реального действия игрока.
7. Item tasks остаются стандартными FTB item tasks.

## Почему теперь нельзя «просто поставить галочку»

FTB CheckmarkTask допускает ручную отправку.
AdvancementTask проверяет реальное состояние advancement у игрока. Пока advancement
не выдан сервером, task не может быть выполнен.

Java выдаёт advancement только по фактическому событию:
- выход за Купол;
- возвращение;
- открытое небо;
- укрытие;
- взаимодействие с airlock;
- взаимодействие с Joseph/maneogflow;
- readiness у шлюза;
- выполненные зависимости главы.

## Внешний вид

Убирается `[No Subtitle]`.
Вместо непонятной серебристой галочки у action quests показывается задача
«Автоматически: ...», связанная с реальным действием.

## Dev reset

Если во время разработки нужно повторно протестировать конкретный action quest,
нужно сбросить и FTB quest, и его technical advancement. Например:

`/advancement revoke <player> only domesurvival:quest_actions/ch0_return_rule`

В обычном survival-прохождении это не требуется.
