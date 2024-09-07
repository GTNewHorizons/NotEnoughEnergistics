package com.github.vfyjxf.nee.nei;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketArcaneRecipe;
import com.github.vfyjxf.nee.utils.ItemUtils;

import appeng.util.Platform;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * @author vfyjxf
 */
public class NEEKnowledgeInscriberHandler implements IOverlayHandler {

    public static final NEEKnowledgeInscriberHandler instance = new NEEKnowledgeInscriberHandler();

    private Class<?> knowledgeInscriberClz;
    private Class<?> itemAspectClz;

    private NEEKnowledgeInscriberHandler() {

        try {
            knowledgeInscriberClz = Class.forName("thaumicenergistics.client.gui.GuiKnowledgeInscriber");// "Knowledge
                                                                                                         // Inscriber"
        } catch (ClassNotFoundException ignored) {}

        try {
            itemAspectClz = Class.forName("com.djgiannuzz.thaumcraftneiplugin.items.ItemAspect");
        } catch (ClassNotFoundException ignored) {}

    }

    @Override
    public void overlayRecipe(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex, boolean shift) {
        if (knowledgeInscriberClz != null && knowledgeInscriberClz.isInstance(firstGui)) {
            NEENetworkHandler.getInstance().sendToServer(packetArcaneRecipe(recipe, recipeIndex));
        }
    }

    private PacketArcaneRecipe packetArcaneRecipe(IRecipeHandler recipe, int recipeIndex) {
        final NBTTagCompound recipeInputs = new NBTTagCompound();
        List<PositionedStack> ingredients = recipe.getIngredientStacks(recipeIndex);

        if (itemAspectClz != null) {
            ingredients.removeIf(positionedStack -> itemAspectClz.isInstance(positionedStack.item.getItem()));
        }

        for (PositionedStack positionedStack : ingredients) {
            if (positionedStack.items != null && positionedStack.items.length > 0) {
                int slotIndex = getSlotIndex(positionedStack.relx * 100 + positionedStack.rely);
                final ItemStack[] currentStackList = positionedStack.items;
                ItemStack stack = positionedStack.item;

                for (ItemStack currentStack : currentStackList) {
                    if (Platform.isRecipePrioritized(currentStack)) {
                        stack = currentStack.copy();
                    }
                }

                recipeInputs.setTag("#" + slotIndex, ItemUtils.writeItemStackToNBT(stack, new NBTTagCompound()));
            }
        }

        return new PacketArcaneRecipe(recipeInputs);
    }

    private int getSlotIndex(int xy) {
        switch (xy) {
            case 7533:
                return 1;
            case 10333:
                return 2;
            case 4960:
                return 3;
            case 7660:
                return 4;
            case 10360:
                return 5;
            case 4987:
                return 6;
            case 7687:
                return 7;
            case 10387:
                return 8;
            case 4832:
            default:
                return 0;
        }
    }

    @SubscribeEvent
    public void onActionPerformedEventPre(GuiOverlayButton.UpdateOverlayButtonsEvent.Post event) {
        if (event.gui instanceof GuiRecipe && isGuiPatternTerm((GuiRecipe<?>) event.gui)) {
            for (int i = 0; i < event.buttonList.size(); i++) {
                event.buttonList.set(i, new NEEGuiOverlayButton(event.buttonList.get(i)));
            }
        }
    }

    private boolean isGuiPatternTerm(GuiRecipe<?> gui) {
        return this.getClass().isInstance(gui.getHandler().getOverlayHandler(gui.firstGui, 0));
    }

}
