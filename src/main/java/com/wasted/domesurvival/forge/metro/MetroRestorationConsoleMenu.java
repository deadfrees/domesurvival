package com.wasted.domesurvival.forge.metro;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public final class MetroRestorationConsoleMenu extends AbstractContainerMenu {
    public static final int BUTTON_RESTORE = 0;
    public static final int BUTTON_DEPOSIT = 1;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final BlockPos consolePos;

    public MetroRestorationConsoleMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory,
                new SimpleContainerData(MetroRestorationConsoleBlockEntity.DATA_COUNT),
                extraData.readBlockPos());
    }

    public MetroRestorationConsoleMenu(int id, Inventory inventory, MetroRestorationConsoleBlockEntity console) {
        this(id, inventory, console.getDataAccess(), console.getBlockPos());
    }

    private MetroRestorationConsoleMenu(int id, Inventory inventory, ContainerData data, BlockPos pos) {
        super(MetroRegistry.METRO_RESTORATION_CONSOLE_MENU.get(), id);
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
        this.data = data;
        this.consolePos = pos.immutable();
        checkContainerDataCount(data, MetroRestorationConsoleBlockEntity.DATA_COUNT);
        addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, MetroRegistry.METRO_RESTORATION_CONSOLE.get());
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        if (buttonId != BUTTON_RESTORE && buttonId != BUTTON_DEPOSIT) return false;

        BlockEntity blockEntity = serverPlayer.level().getBlockEntity(consolePos);
        if (!(blockEntity instanceof MetroRestorationConsoleBlockEntity console)) {
            serverPlayer.sendSystemMessage(text("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438\u0432\u044f\u0437\u043a\u0438 \u0441\u0442\u0430\u043d\u0446\u0438\u0438."));
            return true;
        }

        MetroRestorationService.Result result = buttonId == BUTTON_DEPOSIT
                ? MetroRestorationService.tryDeposit(serverPlayer, console)
                : MetroRestorationService.tryRestore(serverPlayer, console);
        serverPlayer.sendSystemMessage(resultMessage(result));
        broadcastChanges();
        return true;
    }

    private static Component resultMessage(MetroRestorationService.Result result) {
        return switch (result) {
            case SUCCESS -> text("\u0421\u0442\u0430\u043d\u0446\u0438\u044f \u0432\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u0430. LEVEL 2 \u0430\u043a\u0442\u0438\u0432\u0438\u0440\u043e\u0432\u0430\u043d.");
            case DEPOSITED -> text("\u041c\u0430\u0442\u0435\u0440\u0438\u0430\u043b\u044b \u043f\u0440\u0438\u043d\u044f\u0442\u044b. \u041f\u0440\u043e\u0433\u0440\u0435\u0441\u0441 \u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d.");
            case DEPOSITS_COMPLETE -> text("\u0412\u0441\u0435 \u043c\u0430\u0442\u0435\u0440\u0438\u0430\u043b\u044b \u0441\u0434\u0430\u043d\u044b. \u0421\u0442\u0430\u043d\u0446\u0438\u044e \u043c\u043e\u0436\u043d\u043e \u0432\u043e\u0441\u0441\u0442\u0430\u043d\u0430\u0432\u043b\u0438\u0432\u0430\u0442\u044c.");
            case NOTHING_TO_DEPOSIT -> text("\u0412 \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u0435 \u043d\u0435\u0442 \u043d\u0443\u0436\u043d\u044b\u0445 \u0434\u043b\u044f \u044d\u0442\u043e\u0439 \u0441\u0442\u0430\u043d\u0446\u0438\u0438 \u043c\u0430\u0442\u0435\u0440\u0438\u0430\u043b\u043e\u0432.");
            case ALREADY_RESTORED -> text("\u0421\u0442\u0430\u043d\u0446\u0438\u044f \u0443\u0436\u0435 \u0432\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u0430.");
            case BUSY -> text("\u0412\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0442\u0430\u043d\u0446\u0438\u0438 \u0443\u0436\u0435 \u0432\u044b\u043f\u043e\u043b\u043d\u044f\u0435\u0442\u0441\u044f.");
            case MISSING_RESOURCES -> text("\u0421\u043d\u0430\u0447\u0430\u043b\u0430 \u0441\u0434\u0430\u0439\u0442\u0435 \u0432\u0441\u0435 \u043d\u0435\u043e\u0431\u0445\u043e\u0434\u0438\u043c\u044b\u0435 \u043c\u0430\u0442\u0435\u0440\u0438\u0430\u043b\u044b.");
            case TOO_FAR -> text("\u0412\u044b \u0441\u043b\u0438\u0448\u043a\u043e\u043c \u0434\u0430\u043b\u0435\u043a\u043e \u043e\u0442 \u043a\u043e\u043d\u0441\u043e\u043b\u0438.");
            case INVALID_STATION -> text("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438\u0432\u044f\u0437\u043a\u0438 \u0441\u0442\u0430\u043d\u0446\u0438\u0438.");
            case ERROR -> text("\u041e\u0448\u0438\u0431\u043a\u0430 \u0442\u0440\u0430\u043d\u0437\u0430\u043a\u0446\u0438\u0438. \u0421\u043e\u0441\u0442\u043e\u044f\u043d\u0438\u0435 \u0440\u0435\u0441\u0443\u0440\u0441\u043e\u0432 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u043e.");
        };
    }

    private static Component text(String value) {
        return Component.literal(value);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public StationState state() {
        return StationState.byOrdinal(data.get(MetroRestorationConsoleBlockEntity.DATA_STATE));
    }

    public boolean discovered() {
        return data.get(MetroRestorationConsoleBlockEntity.DATA_DISCOVERED) != 0;
    }

    public boolean restored() {
        return data.get(MetroRestorationConsoleBlockEntity.DATA_RESTORED) != 0;
    }

    public int deposited(int requirementIndex) {
        if (requirementIndex < 0 || requirementIndex >= MetroRestorationRequirements.all().size()) return 0;
        return data.get(MetroRestorationConsoleBlockEntity.DATA_DEPOSIT_START + requirementIndex);
    }

    public boolean allDeposited() {
        for (int i = 0; i < MetroRestorationRequirements.all().size(); i++) {
            if (deposited(i) < MetroRestorationRequirements.all().get(i).required()) return false;
        }
        return true;
    }
}
