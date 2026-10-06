package com.wasted.domesurvival.organicprobe;
@mezz.jei.api.JeiPlugin
public final class JeiCapture implements mezz.jei.api.IModPlugin {
    static volatile mezz.jei.api.runtime.IJeiRuntime runtime;
    public net.minecraft.resources.ResourceLocation getPluginUid(){return new net.minecraft.resources.ResourceLocation("domesurvival","organic_review_probe");}
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime value){runtime=value;}
}
