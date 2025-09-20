package dev.felnull.imp.item;

import dev.felnull.imp.block.BoomboxBlock;
import dev.felnull.imp.blockentity.BoomboxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class SynchronizerItem extends Item {
    public SynchronizerItem(Properties properties) {
        super(properties);
    }

    // Right-click
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        Level world = context.getLevel();
        BlockEntity be = world.getBlockEntity(pos);

        if (be instanceof BoomboxBlockEntity special) {
            List<BlockPos> blocks = getSyncedBlocks(stack,world);
            special.getBoomboxData().addSynchronizer(stack);
            if (!blocks.contains(pos)) {
                // Add block to NBT
                CompoundTag blockTag = new CompoundTag();
                blockTag.putInt("x", pos.getX());
                blockTag.putInt("y", pos.getY());
                blockTag.putInt("z", pos.getZ());
                ListTag list = stack.getOrCreateTag().getList("syncedBlocks", Tag.TAG_COMPOUND);
                list.add(blockTag);
                stack.getOrCreateTag().put("syncedBlocks", list);
            }
            if (!world.isClientSide)
                player.displayClientMessage(Component.literal("Block added to synchronizer!"), true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public static List<BlockPos> getSyncedBlocks(ItemStack stack, Level world) {
        List<BlockPos> blocks = new ArrayList<>();
        if (!stack.hasTag() || !stack.getTag().contains("syncedBlocks")) return blocks;
        ListTag list = stack.getTag().getList("syncedBlocks", Tag.TAG_COMPOUND);
        ListTag cleanedList = new ListTag(); // optional: rebuild NBT without invalid positions
        for (Tag t : list) {
            CompoundTag btag = (CompoundTag) t;
            BlockPos pos = new BlockPos(btag.getInt("x"), btag.getInt("y"), btag.getInt("z"));
            if (world.getBlockEntity(pos) != null) {
                blocks.add(pos);
                cleanedList.add(btag);
            }
        }
        stack.getOrCreateTag().put("syncedBlocks", cleanedList);
        return blocks;
    }

}
