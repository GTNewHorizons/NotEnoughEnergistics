package com.github.vfyjxf.nee.asm;

import static com.github.vfyjxf.nee.nei.NEECraftingHandler.INPUT_KEY;
import static com.github.vfyjxf.nee.utils.GuiUtils.isPatternTerm;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import appeng.client.gui.implementations.GuiInterface;
import appeng.container.slot.SlotFake;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.PositionedStack;

import com.github.vfyjxf.nee.config.NEEConfig;
import com.github.vfyjxf.nee.nei.NEECraftingHandler;
import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketSlotStackChange;
import com.github.vfyjxf.nee.network.packet.PacketStackCountChange;
import com.github.vfyjxf.nee.utils.GuiUtils;
import com.github.vfyjxf.nee.utils.ItemUtils;
import cpw.mods.fml.common.Loader;

public class AppengHelper {

    private static boolean hasLwjgl3 = Loader.isModLoaded("lwjgl3ify");

    // LWJGL2 reports different scroll values for every platform, 120 for one tick on Windows.
    // LWJGL3 reports the delta in exact scroll ticks.
    private static int dwheelToScrollSteps(int dWheel) {
        return hasLwjgl3 ? dWheel : dWheel / 120;
    }

    /**
     * Prevent the scroll bar from being triggered when modifying the number of items This method is not intended to be
     * called by NEE. Do not use this method for any reason.
     */
    public static boolean handleMouseWheelInput(int dWheel) {
        Minecraft mc = Minecraft.getMinecraft();
        boolean isPatternTerm = isPatternTerm(mc.currentScreen);
        boolean isInterface = mc.currentScreen instanceof GuiInterface;
        if (isPatternTerm || isInterface) {
            if (dWheel != 0) {
                int x = Mouse.getEventX() * mc.currentScreen.width / mc.displayWidth;
                int y = mc.currentScreen.height - Mouse.getEventY() * mc.currentScreen.height / mc.displayHeight - 1;
                Slot currentSlot = ((GuiContainer) mc.currentScreen).getSlotAtPosition(x, y);
                if (currentSlot instanceof SlotFake && currentSlot.getHasStack()) {
                    // try to change current itemstack to next ingredient;
                    if (Keyboard.isKeyDown(NEIClientConfig.getKeyBinding("nee.ingredient"))
                            && GuiUtils.isCraftingSlot(currentSlot)) {
                        handleRecipeIngredientChange((GuiContainer) mc.currentScreen, currentSlot, dWheel);
                        return true;
                    } else if (Keyboard.isKeyDown(NEIClientConfig.getKeyBinding("nee.count"))) {
                        int changeCount = dwheelToScrollSteps(dWheel);
                        NEENetworkHandler.getInstance()
                                .sendToServer(new PacketStackCountChange(currentSlot.slotNumber, changeCount));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void handleRecipeIngredientChange(GuiContainer gui, Slot currentSlot, int dWheel) {
        List<Integer> craftingSlots = new ArrayList<>();
        int currentSlotIndex = currentSlot.getSlotIndex();
        PositionedStack currentIngredients = NEECraftingHandler.ingredients.get(INPUT_KEY + currentSlotIndex);
        if (currentIngredients != null && currentIngredients.items.length > 1) {
            int currentStackIndex = ItemUtils.getIngredientIndex(currentSlot.getStack(), currentIngredients);
            if (currentStackIndex >= 0) {
                int nextStackIndex = dwheelToScrollSteps(dWheel);
                for (int j = 0; j < Math.abs(nextStackIndex); j++) {
                    currentStackIndex = nextStackIndex > 0 ? currentStackIndex + 1 : currentStackIndex - 1;
                    if (currentStackIndex >= currentIngredients.items.length) {
                        currentStackIndex = 0;
                    } else if (currentStackIndex < 0) {
                        currentStackIndex = currentIngredients.items.length - 1;
                    }

                    ItemStack currentStack = currentIngredients.items[currentStackIndex].copy();
                    currentStack.stackSize = currentSlot.getStack().stackSize;

                    if (NEEConfig.allowSynchronousSwitchIngredient) {
                        for (Slot slot : getCraftingSlots(gui)) {

                            PositionedStack slotIngredients = NEECraftingHandler.ingredients
                                    .get(INPUT_KEY + slot.getSlotIndex());

                            boolean areItemStackEqual = currentSlot.getHasStack() && slot.getHasStack()
                                    && currentSlot.getStack().isItemEqual(slot.getStack())
                                    && ItemStack.areItemStackTagsEqual(currentSlot.getStack(), slot.getStack());

                            boolean areIngredientEqual = slotIngredients != null
                                    && currentIngredients.contains(slotIngredients.items[0]);

                            if (areItemStackEqual && areIngredientEqual) {
                                craftingSlots.add(slot.slotNumber);
                            }
                        }
                    } else {
                        craftingSlots.add(currentSlot.slotNumber);
                    }
                    NEENetworkHandler.getInstance()
                            .sendToServer(new PacketSlotStackChange(currentStack, craftingSlots));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Slot> getCraftingSlots(GuiContainer gui) {
        List<Slot> craftingSlots = new ArrayList<>();
        for (Slot slot : (List<Slot>) gui.inventorySlots.inventorySlots) {
            if (GuiUtils.isCraftingSlot(slot)) {
                craftingSlots.add(slot);
            }
        }
        return craftingSlots;
    }
}
