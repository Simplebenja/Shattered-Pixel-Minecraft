package com.drbenja.spd.block;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.drbenja.spd.block.entity.GoldenBeehiveBlockEntity;
import com.drbenja.spd.init.ModBlockEntities;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Golden Nest — panal custom (NeoForge 26 / MC 26).
 */
public class GoldenBeehiveBlock extends BaseEntityBlock {
public static final IntegerProperty HONEY_LEVEL = IntegerProperty.create("honey_level", 0, 10);
    public static final int MAX_HONEY = 10;
    public static final int HARVEST_AMOUNT = 5;

    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);

    public GoldenBeehiveBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(HONEY_LEVEL, 0));
    }
@Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, HONEY_LEVEL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(HorizontalDirectionalBlock.FACING,
                rot.rotate(state.getValue(HorizontalDirectionalBlock.FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(HorizontalDirectionalBlock.FACING)));
    }

    public static int visualTier(int honey) {
        if (honey >= MAX_HONEY) return 2;
        if (honey >= 5) return 1;
        return 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                       Player player, InteractionHand hand, BlockHitResult hit) {
        int honey = state.getValue(HONEY_LEVEL);
        boolean bottle = stack.is(Items.GLASS_BOTTLE);
        boolean shears = stack.is(Items.SHEARS);

        if (honey >= HARVEST_AMOUNT && (bottle || shears)) {
            if (!level.isClientSide()) {
                if (bottle) {
                    stack.shrink(1);
                    ItemStack honeyBottle = new ItemStack(Items.HONEY_BOTTLE);
                    if (!player.hasInfiniteMaterials()) {
                        if (!player.getInventory().add(honeyBottle)) {
                            net.minecraft.world.level.block.Block.popResource(level, pos, honeyBottle);
                        }
                    }
                    level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.awardStat(Stats.ITEM_USED.get(Items.GLASS_BOTTLE));
                } else {
                    ItemStack comb = new ItemStack(Items.HONEYCOMB, 3);
                    if (!player.getInventory().add(comb)) {
                        net.minecraft.world.level.block.Block.popResource(level, pos, comb);
                    }
                    level.playSound(null, pos, SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
                    stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));
                    player.awardStat(Stats.ITEM_USED.get(Items.SHEARS));
                }

                int newHoney = Math.max(0, honey - HARVEST_AMOUNT);
                level.setBlock(pos, state.setValue(HONEY_LEVEL, newHoney), 3);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GoldenBeehiveBlockEntity nest) {
            if (!player.isCreative()) {
                ItemStack drop = new ItemStack(this);
                nest.saveToItem(drop);
                drop.set(DataComponents.BLOCK_STATE,
                        BlockItemStateProperties.EMPTY.with(HONEY_LEVEL, state.getValue(HONEY_LEVEL)));

                ItemEntity entity = new ItemEntity(level,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, drop);
                entity.setDefaultPickUpDelay();
                level.addFreshEntity(entity);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // El drop con NBT lo gestiona playerWillDestroy para evitar doble panal.
        // Solo devolver vacío aquí (el loot table también daría el bloque).
        return java.util.Collections.emptyList();
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof GoldenBeehiveBlockEntity nest) {
            nest.emptyAll(true);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GoldenBeehiveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.GOLDEN_BEEHIVE.get(), GoldenBeehiveBlockEntity::serverTick);
    }

    // Tooltip Honey/Bees: en MC 26 Block ya no tiene appendHoverText.
    // Usar ItemTooltipEvent si se necesita (o BlockItem custom).

    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(HONEY_LEVEL) >= MAX_HONEY && random.nextInt(3) == 0) {
            Direction dir = state.getValue(HorizontalDirectionalBlock.FACING);
            double x = pos.getX() + 0.5D + dir.getStepX() * 0.55D;
            double y = pos.getY() + 0.4D + random.nextDouble() * 0.3D;
            double z = pos.getZ() + 0.5D + dir.getStepZ() * 0.55D;
            level.addParticle(ParticleTypes.DRIPPING_HONEY, x, y, z, 0, 0, 0);
        }
    }
}
