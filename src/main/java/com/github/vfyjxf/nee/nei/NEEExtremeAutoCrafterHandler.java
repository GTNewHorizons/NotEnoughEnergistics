package com.github.vfyjxf.nee.nei;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketExtremeRecipe;
import com.github.vfyjxf.nee.utils.ItemUtils;

import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * @author vfyjxf
 */
public class NEEExtremeAutoCrafterHandler implements IOverlayHandler {

    public static final NEEExtremeAutoCrafterHandler instance = new NEEExtremeAutoCrafterHandler();

    private Class<?> guiExtremeAutoCrafterClz;

    private NEEExtremeAutoCrafterHandler() {

        try {
            guiExtremeAutoCrafterClz = Class
                    .forName("wanion.avaritiaddons.block.extremeautocrafter.GuiExtremeAutoCrafter");// "Dire
                                                                                                    // Autocrafting
                                                                                                    // Table"
        } catch (ClassNotFoundException ignored) {}

    }

    @Override
    public void overlayRecipe(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex, boolean shift) {
        System.out.println("shift: " + shift);

        if (this.guiExtremeAutoCrafterClz != null && this.guiExtremeAutoCrafterClz.isInstance(firstGui)) {
            NEENetworkHandler.getInstance().sendToServer(packetExtremeRecipe(recipe, recipeIndex));
        }
    }

    private PacketExtremeRecipe packetExtremeRecipe(IRecipeHandler recipe, int recipeIndex) {
        NBTTagCompound recipeInputs = new NBTTagCompound();
        List<PositionedStack> ingredients = recipe.getIngredientStacks(recipeIndex);

        for (PositionedStack positionedStack : ingredients) {
            int col = (positionedStack.relx - 3) / 18;
            int row = (positionedStack.rely - 3) / 18;

            if (positionedStack.rely == 129) {
                col = (positionedStack.relx - 2) / 18;
            }

            int slotIndex = col + row * 9;
            ItemStack currentStack = positionedStack.item;
            ItemStack preferModItem = ItemUtils.getPreferModItem(positionedStack.items);

            if (preferModItem != null) {
                currentStack = preferModItem;
            }

            for (ItemStack stack : positionedStack.items) {
                if (ItemUtils.isPreferItems(stack)) {
                    currentStack = stack.copy();
                }
            }

            recipeInputs.setTag("#" + slotIndex, ItemUtils.writeItemStackToNBT(currentStack, new NBTTagCompound()));
        }

        return new PacketExtremeRecipe(recipeInputs);
    }

    @SubscribeEvent
    public void onActionPerformedEventPre(GuiOverlayButton.UpdateOverlayButtonsEvent.Post event) {

        if (event.gui instanceof GuiRecipe) {

        }

        if (event.gui instanceof GuiRecipe && isGuiExtremeTerm((GuiRecipe<?>) event.gui)) {
            for (int i = 0; i < event.buttonList.size(); i++) {
                event.buttonList.set(i, new NEEGuiOverlayButton(event.buttonList.get(i)));
            }
        }
    }

    private boolean isGuiExtremeTerm(GuiRecipe<?> gui) {
        return this.getClass().isInstance(gui.getHandler().getOverlayHandler(gui.firstGui, 0));
    }

}
