package com.wasted.domesurvival.forge.quest;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;

/**
 * Narrow reflection bridge to FTB Quests 2001.4.x.
 *
 * Dome Survival intentionally does not hard-link FTB classes in its main
 * compile classpath. This helper uses only a few stable public methods to mark
 * a mirrored reward as CLAIMED without executing Reward.claim().
 *
 * That is the key to the campaign rule:
 *   global quest completion, but exactly one global material reward.
 */
public final class FtbQuestRewardCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile State state = State.UNTESTED;
    private static volatile String lastError = "";

    private FtbQuestRewardCompat() {
    }

    public static boolean isAvailable() {
        return resolve();
    }

    public static String status() {
        resolve();
        return state.name() + (lastError.isEmpty() ? "" : ": " + lastError);
    }

    /**
     * Marks every non-technical reward on the specified quest as claimed for
     * this player's current FTB team, without granting its contents.
     *
     * Returns false if the reflection contract is unavailable. In that case
     * callers must log loudly; silent reward duplication is not acceptable.
     */
    public static boolean suppressVisibleRewards(ServerPlayer player, String questId) {
        if (!resolve()) {
            return false;
        }

        try {
            Reflection r = ReflectionHolder.INSTANCE;
            Object teamData = r.teamDataGet.invoke(null, (Player) player);
            Object serverQuestFile = r.serverQuestFileInstance.get(null);
            long questLong = Long.parseLong(questId, 16);
            Object quest = r.getQuest.invoke(serverQuestFile, questLong);

            if (teamData == null || quest == null) {
                return false;
            }

            Object rewardsObject = r.getRewards.invoke(quest);
            if (!(rewardsObject instanceof Collection<?> rewards)) {
                return false;
            }

            String internalId = QuestGlobalRegistry.internalRewardId(questId);
            long now = System.currentTimeMillis();

            for (Object reward : rewards) {
                long rewardId = ((Number) r.getRewardId.invoke(reward)).longValue();
                String code = String.format("%016X", rewardId);

                if (code.equalsIgnoreCase(internalId)) {
                    continue;
                }

                r.markRewardAsClaimed.invoke(
                        teamData,
                        player.getUUID(),
                        reward,
                        now
                );
            }

            return true;
        } catch (Throwable t) {
            fail(t);
            return false;
        }
    }

    private static boolean resolve() {
        if (state == State.AVAILABLE) {
            return true;
        }
        if (state == State.UNAVAILABLE) {
            return false;
        }

        synchronized (FtbQuestRewardCompat.class) {
            if (state != State.UNTESTED) {
                return state == State.AVAILABLE;
            }

            try {
                ReflectionHolder.INSTANCE.verify();
                state = State.AVAILABLE;
                LOGGER.info("[DomeQuest] FTB reward compatibility: AVAILABLE");
                return true;
            } catch (Throwable t) {
                fail(t);
                return false;
            }
        }
    }

    private static void fail(Throwable t) {
        state = State.UNAVAILABLE;
        lastError = t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage());
        LOGGER.error(
                "[DomeQuest] FTB reward compatibility FAILED. "
                        + "Global quest sync is unsafe for one-shot rewards until fixed.",
                t
        );
    }

    private enum State {
        UNTESTED,
        AVAILABLE,
        UNAVAILABLE
    }

    private static final class ReflectionHolder {
        private static final Reflection INSTANCE;

        static {
            try {
                INSTANCE = new Reflection();
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    private static final class Reflection {
        private final Method teamDataGet;
        private final Field serverQuestFileInstance;
        private final Method getQuest;
        private final Method getRewards;
        private final Method getRewardId;
        private final Method markRewardAsClaimed;

        private Reflection() throws ReflectiveOperationException {
            Class<?> teamDataClass =
                    Class.forName("dev.ftb.mods.ftbquests.quest.TeamData");
            Class<?> serverQuestFileClass =
                    Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile");
            Class<?> questClass =
                    Class.forName("dev.ftb.mods.ftbquests.quest.Quest");
            Class<?> rewardClass =
                    Class.forName("dev.ftb.mods.ftbquests.quest.reward.Reward");

            teamDataGet = teamDataClass.getMethod("get", Player.class);
            serverQuestFileInstance = serverQuestFileClass.getField("INSTANCE");
            getQuest = serverQuestFileClass.getMethod("getQuest", long.class);
            getRewards = questClass.getMethod("getRewards");
            getRewardId = rewardClass.getMethod("getId");
            markRewardAsClaimed = teamDataClass.getMethod(
                    "markRewardAsClaimed",
                    UUID.class,
                    rewardClass,
                    long.class
            );
        }

        private void verify() {
            // Construction is the verification. Kept as a method so the outer
            // class can trigger class initialization in one explicit place.
        }
    }
}
