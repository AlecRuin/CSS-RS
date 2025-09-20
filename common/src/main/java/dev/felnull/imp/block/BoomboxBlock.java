package dev.felnull.imp.block;

import dev.felnull.imp.IamMusicPlayer;
import dev.felnull.imp.blockentity.BoomboxBlockEntity;
import dev.felnull.imp.blockentity.IMPBlockEntities;
import dev.felnull.imp.item.BoomboxItem;
import dev.felnull.otyacraftengine.shape.bundle.DirectionVoxelShapesBundle;
import dev.felnull.otyacraftengine.util.OEVoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class BoomboxBlock extends IMPBaseEntityBlock {
    private static final DirectionVoxelShapesBundle SHAPE = OEVoxelShapeUtils.makeAllDirection(OEVoxelShapeUtils.getShapeFromResource(new ResourceLocation(IamMusicPlayer.MODID, "boombox"), BoomboxBlock.class));
    public static final BooleanProperty RAISED = IMPBlockStateProperties.RAISE;
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;
    public static final DirectionProperty VERTICAL_DIRECTION = BlockStateProperties.VERTICAL_DIRECTION;

    protected BoomboxBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(RAISED, true)
                .setValue(FACE, AttachFace.FLOOR)
                .setValue(VERTICAL_DIRECTION, Direction.UP)
        );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        if (blockPlaceContext.getClickedFace() == Direction.UP){
            return super.getStateForPlacement(blockPlaceContext)
                    .setValue(FACING, blockPlaceContext.getHorizontalDirection().getOpposite())
                    .setValue(FACE, AttachFace.FLOOR)
                    .setValue(VERTICAL_DIRECTION, Direction.UP);
        } else if (blockPlaceContext.getClickedFace() == Direction.DOWN){
            return super.getStateForPlacement(blockPlaceContext)
                    .setValue(FACING, blockPlaceContext.getHorizontalDirection().getOpposite())
                    .setValue(FACE, AttachFace.CEILING)
                    .setValue(VERTICAL_DIRECTION, Direction.UP);
        } else {
            return super.getStateForPlacement(blockPlaceContext)
                    .setValue(FACING, blockPlaceContext.getHorizontalDirection().getOpposite())
                    .setValue(FACE, AttachFace.WALL)
                    .setValue(VERTICAL_DIRECTION, blockPlaceContext.getNearestLookingVerticalDirection());
        }
    }

    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {

        if (player.isCrouching()) {
            var be = level.getBlockEntity(blockPos);
            if (be instanceof BoomboxBlockEntity boombox) {
                if (blockHitResult.getDirection() == Direction.UP) {
                    if (boombox.getBoomboxData().cycleRaisedHandle()) {
                        level.playSound(null, blockPos, boombox.getBoomboxData().isLidOpen() ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
                        return InteractionResult.sidedSuccess(level.isClientSide());
                    }
                } else if (blockHitResult.getDirection() == blockState.getValue(FACING)) {
                    if (boombox.getBoomboxData().cycleLidOpen(level))
                        return InteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        } else {
            return super.use(blockState, level, blockPos, player, interactionHand, blockHitResult);
        }
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new BoomboxBlockEntity(blockPos, blockState);
    }

    @Override
    public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        //var shp = SHAPE;
        VoxelShape tempVoxel = SHAPE.getShape(blockState.getValue(FACING));

        if (blockState.getValue(FACE) == AttachFace.WALL) {
            if (blockState.getValue(FACING) == Direction.SOUTH) {
                //tempVoxel = OEVoxelShapeUtils.rotateBoxY180(tempVoxel);
            } else if (blockState.getValue(FACING) == Direction.EAST) {
                tempVoxel = OEVoxelShapeUtils.rotateBoxX90(tempVoxel);
            } else if (blockState.getValue(FACING) == Direction.WEST) {
                tempVoxel = OEVoxelShapeUtils.rotateBoxX270(tempVoxel);
            } else if (blockState.getValue(FACING) == Direction.NORTH) {
                tempVoxel = OEVoxelShapeUtils.rotateBoxX180(tempVoxel);
            }
            tempVoxel = OEVoxelShapeUtils.rotateBoxZ90(tempVoxel);
            return(tempVoxel);
        } else if (blockState.getValue(FACE) == AttachFace.CEILING) {
            return(OEVoxelShapeUtils.rotateBoxZ180(tempVoxel));
        }

        return tempVoxel;
        //return shp.getShape(blockState.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RAISED);
        builder.add(FACE);
        builder.add(VERTICAL_DIRECTION);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, IMPBlockEntities.BOOMBOX.get(), BoomboxBlockEntity::tick);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter blockGetter, BlockPos blockPos, BlockState blockState) {
        var be = blockGetter.getBlockEntity(blockPos);
        if (be instanceof BoomboxBlockEntity boomboxBlockEntity)
            return BoomboxItem.createByBE(boomboxBlockEntity, true);
        return super.getCloneItemStack(blockGetter, blockPos, blockState);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide) return; //server side
        BlockEntity be = level.getBlockEntity(pos);//grab that block lmao
        if (!(be instanceof BoomboxBlockEntity boombox)) return;
        boolean powered = level.hasNeighborSignal(pos); //figure out if its powered adjacently
        if (powered) {
            if(boombox.getBoomboxData().isPlaying()){
                boombox.getBoomboxData().setPlaying(false, level, pos, new HashSet<>());//my jerry-rigged play-pause function
            }else{
                boombox.getBoomboxData().setPlaying(true, level, pos, new HashSet<>());//my jerry-rigged play-pause function
            }
        }
    }
}
