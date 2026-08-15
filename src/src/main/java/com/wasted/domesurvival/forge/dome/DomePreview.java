package com.wasted.domesurvival.forge.dome;

import com.wasted.domesurvival.core.dome.BlockPoint;
import com.wasted.domesurvival.core.dome.DomeShellPlanner;
import com.wasted.domesurvival.core.dome.DomeSpec;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

public final class DomePreview {
    private static final int MAX_PARTICLES = 2200;

    private DomePreview() {
    }

    public static int show(ServerLevel level) {
        int shellSize = DomeShellPlanner.planOneBlockShell(DomeSpec.wastedV1()).size();
        int stride = Math.max(1, shellSize / MAX_PARTICLES);
        int index = 0;
        int sent = 0;

        for (BlockPoint p : DomeShellPlanner.planOneBlockShell(DomeSpec.wastedV1())) {
            if ((index++ % stride) != 0 || sent >= MAX_PARTICLES) continue;
            level.sendParticles(ParticleTypes.END_ROD,
                    p.x() + 0.5, p.y() + 0.5, p.z() + 0.5,
                    1, 0.0, 0.0, 0.0, 0.0);
            sent++;
        }
        return sent;
    }
}
