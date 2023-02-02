package com.github.vfyjxf.nee.network.packet;

import static com.github.vfyjxf.nee.nei.NEECraftingHandler.INPUT_KEY;
import static com.github.vfyjxf.nee.nei.NEECraftingHandler.OUTPUT_KEY;
import static com.github.vfyjxf.nee.nei.NEECraftingHelper.RECIPE_LENGTH;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerNull;

import com.github.vfyjxf.nee.block.tile.TilePatternInterface;
import com.google.common.base.Optional;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketSetRecipe implements IMessage {

    private NBTTagCompound recipe;

    public PacketSetRecipe() {}

    public PacketSetRecipe(NBTTagCompound recipe) {
        this.recipe = recipe;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.recipe = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, this.recipe);
    }

    public static final class Handler implements IMessageHandler<PacketSetRecipe, IMessage> {

        @Override
        public IMessage onMessage(PacketSetRecipe message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            Container container = player.openContainer;

            if (container instanceof AEBaseContainer) {
                AEBaseContainer aeContainer = (AEBaseContainer) container;
                IGrid grid = message.getNetwork(aeContainer);
                if (grid != null) {
                    for (IGridNode gridNode : grid.getMachines(TilePatternInterface.class)) {

                        if (gridNode.getMachine() instanceof TilePatternInterface) {

                            TilePatternInterface tpi = (TilePatternInterface) gridNode.getMachine();
                            NBTTagCompound currentTag = message.recipe.getCompoundTag(OUTPUT_KEY);
                            ItemStack result = currentTag == null ? null : ItemStack.loadItemStackFromNBT(currentTag);

                            if (tpi.getProxy().isActive() && tpi.canPutPattern(result)) {

                                ItemStack patternStack = message.getPatternStack(player, message.recipe);

                                if (patternStack != null) {

                                    // if (tpi.putPattern(patternStack)) {
                                    // return null;
                                    // }
                                }
                            }
                        }
                    }
                }
            }

            return null;
        }
    }

    private IGrid getNetwork(AEBaseContainer container) {
        if (container.getTarget() instanceof IActionHost) {
            IActionHost ah = (IActionHost) container.getTarget();
            IGridNode gn = ah.getActionableNode();
            return gn.getGrid();
        }
        return null;
    }

    private ItemStack getPatternStack(EntityPlayer player, NBTTagCompound recipe) {
        ItemStack[] recipeInputs = new ItemStack[RECIPE_LENGTH];
        NBTTagCompound currentStack;
        for (int i = 0; i < recipeInputs.length; i++) {
            currentStack = recipe.getCompoundTag(INPUT_KEY + i);
            recipeInputs[i] = currentStack == null ? null : ItemStack.loadItemStackFromNBT(currentStack);
        }
        InventoryCrafting ic = new InventoryCrafting(new ContainerNull(), 3, 3);
        for (int i = 0; i < RECIPE_LENGTH; i++) {
            ic.setInventorySlotContents(i, recipeInputs[i]);
        }

        ItemStack result = CraftingManager.getInstance().findMatchingRecipe(ic, player.worldObj);
        if (result != null) {
            ItemStack patternStack = null;
            Optional<ItemStack> maybePattern = AEApi.instance().definitions().items().encodedPattern().maybeStack(1);
            if (maybePattern.isPresent()) {
                patternStack = maybePattern.get();
            }
            if (patternStack != null) {
                final NBTTagCompound patternValue = new NBTTagCompound();
                final NBTTagList tagIn = new NBTTagList();
                for (ItemStack stack : recipeInputs) {
                    tagIn.appendTag(crateItemTag(stack));
                }
                patternValue.setTag("in", tagIn);
                patternValue.setTag("out", result.writeToNBT(new NBTTagCompound()));
                patternValue.setBoolean("crafting", true);
                patternValue.setBoolean("substitute", false);

                patternStack.setTagCompound(patternValue);
                return patternStack;
            }
        }

        return null;
    }

    private NBTTagCompound crateItemTag(ItemStack itemStack) {
        NBTTagCompound tag = new NBTTagCompound();
        if (itemStack != null) {
            itemStack.writeToNBT(tag);
        }
        return tag;
    }
}
