package com.drbenja.spd.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.NodeEvaluator;

@Mixin(NodeEvaluator.class)
public interface NodeEvaluatorAccessor {

    @Accessor("mob")
    Mob spd$getMob();
}
