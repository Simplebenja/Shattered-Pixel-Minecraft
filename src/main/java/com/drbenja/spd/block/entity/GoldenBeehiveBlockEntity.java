package com.drbenja.spd.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Nameable;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.drbenja.spd.block.GoldenBeehiveBlock;
import com.drbenja.spd.config.goldenbee.GoldenBeeConfig;
import com.drbenja.spd.entity.mob.GoldenBeeEntity;
import com.drbenja.spd.init.ModBlockEntities;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Golden Nest BlockEntity — ocupantes (máx 6) + miel vía blockstate.
 *
 * Implementa WorldlyContainer vacío solo para que MCreator / NeoForge
 * pueda registrar Capabilities.Item.BLOCK (WorldlyContainerWrapper).
 * El inventario NO se usa: tamaño 0, nada entra ni sale.
 */
public class GoldenBeehiveBlockEntity extends BlockEntity implements WorldlyContainer, Nameable {

    public static final int MAX_OCCUPANTS = 6;
    public static final int MIN_TICKS_IN_NEST = 600;

    private final List<Occupant> occupants = new ArrayList<>();
    /** Inventario dummy vacío (MCreator capability). */
    private final NonNullList<ItemStack> dummyItems = NonNullList.withSize(0, ItemStack.EMPTY);

    public GoldenBeehiveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GOLDEN_BEEHIVE.get(), pos, state);
    }

    // ---- API pública del nido ----

    public boolean isFull() {
        return this.occupants.size() >= MAX_OCCUPANTS;
    }

    public int getOccupantCount() {
        return this.occupants.size();
    }

    @Override
    public Component getName() {
        return Component.translatable("block.spd.golden_beehive");
    }

    @Override
    public Component getDisplayName() {
        int honey = 0;
        BlockState st = this.getBlockState();
        if (st.hasProperty(GoldenBeehiveBlock.HONEY_LEVEL)) {
            honey = st.getValue(GoldenBeehiveBlock.HONEY_LEVEL);
        }
        return Component.translatable("block.spd.golden_beehive")
                .append(Component.literal(" (" + honey + "/" + GoldenBeehiveBlock.MAX_HONEY
                        + ", " + this.occupants.size() + "/" + MAX_OCCUPANTS + ")")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
    }


    public boolean isNestEmpty() {
        return this.occupants.isEmpty();
    }

    public static boolean canAcceptEntity(Entity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return false;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id != null && GoldenBeeConfig.isEntityBlacklisted(id)) {
            return false;
        }
        if (entity instanceof GoldenBeeEntity) {
            return true;
        }
        if (entity instanceof Bee) {
            return true;
        }
        return id != null && GoldenBeeConfig.isEntityWhitelisted(id);
    }

    public boolean tryEnter(Entity entity) {
        if (this.level == null || this.level.isClientSide()) {
            return false;
        }
        if (this.isFull() || !canAcceptEntity(entity)) {
            return false;
        }

        CompoundTag data = new CompoundTag();
        if (!saveEntityToTag(entity, data)) {
            return false;
        }

        boolean hadNectar = false;
        if (entity instanceof Bee bee) {
            hadNectar = bee.hasNectar();
        } else if (entity instanceof GoldenBeeEntity gb) {
            hadNectar = gb.isCarryingNectarPublic();
        } else if (data.getBooleanOr("HasNectar", false) || data.getBooleanOr("CarryingNectar", false)) {
            hadNectar = true;
        }

        if (entity instanceof GoldenBeeEntity gb) {
            data.putInt("SpdCommand", gb.getCommand().ordinal());
            if (gb.getWorkOriginPublic() != null) {
                data.putInt("SpdWorkX", gb.getWorkOriginPublic().getX());
                data.putInt("SpdWorkY", gb.getWorkOriginPublic().getY());
                data.putInt("SpdWorkZ", gb.getWorkOriginPublic().getZ());
            }
        }

        this.occupants.add(new Occupant(data, 0, MIN_TICKS_IN_NEST, hadNectar));
        entity.discard();
        this.setChanged();
        this.level.playSound(null, this.worldPosition, SoundEvents.BEEHIVE_ENTER,
                SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    public void releaseReadyOccupants(ServerLevel level, boolean forceAll) {
        if (this.occupants.isEmpty()) {
            return;
        }

        boolean night = level.getSkyDarken() >= 4;
        boolean rain = level.isRainingAt(this.worldPosition);
        if (!forceAll && (night || rain)) {
            return;
        }

        Iterator<Occupant> it = this.occupants.iterator();
        while (it.hasNext()) {
            Occupant occ = it.next();
            if (!forceAll && occ.ticksInNest < occ.minTicksInNest) {
                continue;
            }

            Entity entity = spawnFromOccupant(level, occ);
            if (entity == null) {
                it.remove();
                this.setChanged();
                continue;
            }

            if (occ.hadNectar) {
                BlockState state = this.getBlockState();
                int honey = state.getValue(GoldenBeehiveBlock.HONEY_LEVEL);
                if (honey < GoldenBeehiveBlock.MAX_HONEY) {
                    level.setBlock(this.worldPosition,
                            state.setValue(GoldenBeehiveBlock.HONEY_LEVEL, honey + 1), 3);
                }
            }

            it.remove();
            this.setChanged();
            level.playSound(null, this.worldPosition, SoundEvents.BEEHIVE_EXIT,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    public void emptyAll(boolean spawn) {
        if (!(this.level instanceof ServerLevel server)) {
            this.occupants.clear();
            return;
        }
        if (spawn) {
            for (Occupant occ : new ArrayList<>(this.occupants)) {
                spawnFromOccupant(server, occ);
            }
        }
        this.occupants.clear();
        this.setChanged();
    }

    /**
     * Guarda ocupantes en el ItemStack del bloque (para drop con NBT).
     * MC 26: BLOCK_ENTITY_DATA es TypedEntityData<BlockEntityType<?>>, no CustomData.
     */
    public void saveToItem(ItemStack stack) {
        try {
            CompoundTag nestTag = new CompoundTag();
            ListTag list = new ListTag();
            for (Occupant occ : this.occupants) {
                CompoundTag entry = new CompoundTag();
                entry.put("EntityData", occ.entityData);
                entry.putInt("TicksInNest", occ.ticksInNest);
                entry.putInt("MinTicksInNest", occ.minTicksInNest);
                entry.putBoolean("HadNectar", occ.hadNectar);
                list.add(entry);
            }
            nestTag.put("Occupants", list);
            CompoundTag beTag = new CompoundTag();
            beTag.put("NestData", nestTag);
            // TypedEntityData.of(tipo del BE, datos)
            stack.set(DataComponents.BLOCK_ENTITY_DATA,
                    TypedEntityData.of(this.getType(), beTag));
        } catch (Throwable t) {
            // Si falla el componente, el drop sigue sin ocupantes
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GoldenBeehiveBlockEntity nest) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (nest.occupants.isEmpty()) {
            return;
        }
        for (Occupant occ : nest.occupants) {
            occ.ticksInNest++;
        }
        if (level.getGameTime() % 20 == 0) {
            nest.releaseReadyOccupants(server, false);
        }
    }

    // ---- Spawn / serialización de entidades ----

    @Nullable
    private Entity spawnFromOccupant(ServerLevel level, Occupant occ) {
        CompoundTag data = occ.entityData.copy();
        data.remove("UUID");

        Entity entity = null;
        try {
            ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data);
            Optional<EntityType<?>> typeOpt = EntityType.by(input);
            if (typeOpt.isPresent()) {
                entity = typeOpt.get().create(level, EntitySpawnReason.LOAD);
                if (entity != null) {
                    entity.load(input);
                }
            }
        } catch (Throwable t) {
            String id = data.getStringOr("id", "");
            if (!id.isEmpty()) {
                try {
                    EntityType<?> et = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id));
                    if (et != null) {
                        entity = et.create(level, EntitySpawnReason.LOAD);
                        if (entity != null) {
                            try {
                                ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data);
                                entity.load(input);
                            } catch (Throwable ignored) {
                                if (entity instanceof GoldenBeeEntity gb) {
                                    gb.loadFromNestTag(data);
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        if (entity == null) {
            return null;
        }

        placeAndAdd(level, entity);

        if (entity instanceof GoldenBeeEntity gb) {
            int cmd = data.getIntOr("SpdCommand", -1);
            BlockPos work = null;
            if (data.contains("SpdWorkX")) {
                work = new BlockPos(
                        data.getIntOr("SpdWorkX", 0),
                        data.getIntOr("SpdWorkY", 0),
                        data.getIntOr("SpdWorkZ", 0));
            }
            gb.applyHiveExitState(cmd, work, this.worldPosition);
            gb.setHiveReenterCooldown(40);
        }

        return entity;
    }

    private void placeAndAdd(ServerLevel level, Entity entity) {
        BlockPos pos = this.worldPosition;
        Direction facing = Direction.NORTH;
        BlockState st = this.getBlockState();
        if (st.hasProperty(HorizontalDirectionalBlock.FACING)) {
            facing = st.getValue(HorizontalDirectionalBlock.FACING);
        }
        BlockPos spawnPos = pos.relative(facing);
        if (!level.getBlockState(spawnPos).getCollisionShape(level, spawnPos).isEmpty()) {
            for (Direction d : Direction.values()) {
                BlockPos p = pos.relative(d);
                if (level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                    spawnPos = p;
                    break;
                }
            }
        }

        RandomSource rng = level.getRandom();
        entity.snapTo(
                spawnPos.getX() + 0.5D,
                spawnPos.getY() + 0.25D,
                spawnPos.getZ() + 0.5D,
                rng.nextFloat() * 360F,
                0
        );

        if (entity instanceof Bee bee) {
            bee.setHealth(bee.getMaxHealth());
        }

        level.addFreshEntity(entity);
    }

    private static boolean saveEntityToTag(Entity entity, CompoundTag data) {
        try {
            TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            entity.saveWithoutId(output);
            CompoundTag built = output.buildResult();
            for (String key : built.keySet()) {
                data.put(key, built.get(key));
            }
            Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (key != null) {
                data.putString("id", key.toString());
            }
            return true;
        } catch (Throwable t) {
            try {
                Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                if (key != null) {
                    data.putString("id", key.toString());
                }
                if (entity instanceof GoldenBeeEntity gb) {
                    data.putBoolean("CarryingNectar", gb.isCarryingNectarPublic());
                    data.putInt("SpdCommand", gb.getCommand().ordinal());
                }
                return key != null;
            } catch (Throwable t2) {
                return false;
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        try {
            ListTag list = new ListTag();
            for (Occupant occ : this.occupants) {
                CompoundTag entry = new CompoundTag();
                entry.put("EntityData", occ.entityData);
                entry.putInt("TicksInNest", occ.ticksInNest);
                entry.putInt("MinTicksInNest", occ.minTicksInNest);
                entry.putBoolean("HadNectar", occ.hadNectar);
                list.add(entry);
            }
            CompoundTag wrapper = new CompoundTag();
            wrapper.put("Occupants", list);
            output.store("NestData", CompoundTag.CODEC, wrapper);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.occupants.clear();
        try {
            Optional<CompoundTag> wrapperOpt = input.read("NestData", CompoundTag.CODEC);
            if (wrapperOpt.isEmpty()) {
                return;
            }
            CompoundTag wrapper = wrapperOpt.get();
            ListTag list = wrapper.getList("Occupants").orElse(new ListTag());
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i).orElse(null);
                if (entry == null) continue;
                CompoundTag entityData = entry.getCompound("EntityData").orElse(new CompoundTag());
                int ticks = entry.getIntOr("TicksInNest", 0);
                int min = entry.getIntOr("MinTicksInNest", MIN_TICKS_IN_NEST);
                boolean nectar = entry.getBooleanOr("HadNectar", false);
                this.occupants.add(new Occupant(entityData, ticks, min, nectar));
            }
        } catch (Exception ignored) {
        }
    }

    // ---- WorldlyContainer vacío (solo para capability de MCreator) ----

    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // no-op
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {
        // no-op
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return false;
    }

    // ---- Occupant ----

    private static final class Occupant {
        final CompoundTag entityData;
        int ticksInNest;
        final int minTicksInNest;
        final boolean hadNectar;

        Occupant(CompoundTag entityData, int ticksInNest, int minTicksInNest, boolean hadNectar) {
            this.entityData = entityData;
            this.ticksInNest = ticksInNest;
            this.minTicksInNest = minTicksInNest;
            this.hadNectar = hadNectar;
        }
    }
}
