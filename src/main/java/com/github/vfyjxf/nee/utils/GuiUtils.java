package com.github.vfyjxf.nee.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import com.glodblock.github.client.gui.GuiFluidPatternTerminal;
import com.glodblock.github.client.gui.GuiFluidPatternTerminalEx;
import com.glodblock.github.client.gui.container.ContainerFluidPatternTerminal;
import com.glodblock.github.client.gui.container.ContainerFluidPatternTerminalEx;

import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.client.gui.implementations.GuiPatternTermEx;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import appeng.helpers.IContainerCraftingPacket;
import cpw.mods.fml.common.Loader;

/**
 * @author vfyjxf
 */
public class GuiUtils {

    private static final boolean isWirelessCraftingTerminalModLoaded = Loader.isModLoaded(ModIDs.WCT);
    private static final boolean isFluidCraftModloaded = Loader.isModLoaded(ModIDs.FC);

    public static boolean isGuiWirelessCrafting(GuiScreen gui) {
        if (!isWirelessCraftingTerminalModLoaded) return false;
        return gui instanceof net.p455w0rd.wirelesscraftingterminal.client.gui.GuiWirelessCraftingTerminal;
    }

    public static boolean isCraftingSlot(Slot slot) {
        if (slot == null) {
            return false;
        }
        Container container = Minecraft.getMinecraft().thePlayer.openContainer;
        if (!isPatternContainer(container)) {
            return false;
        }
        IContainerCraftingPacket cct = (IContainerCraftingPacket) container;
        IInventory craftMatrix = cct.getInventoryByName("crafting");
        return craftMatrix.equals(slot.inventory);
    }

    public static boolean isPatternContainer(Container container) {
        if (isFluidCraftModloaded) {
            return container instanceof ContainerPatternTerm || container instanceof ContainerPatternTermEx
                    || container instanceof ContainerFluidPatternTerminal
                    || container instanceof ContainerFluidPatternTerminalEx;
        } else {
            return container instanceof ContainerPatternTerm || container instanceof ContainerPatternTermEx;
        }
    }

    public static boolean isFluidCraftPatternTermEx(GuiScreen guiScreen) {
        return isFluidCraftModloaded && guiScreen instanceof GuiFluidPatternTerminalEx;
    }

    public static boolean isPatternTerm(GuiScreen guiScreen) {
        if (isFluidCraftModloaded) {
            return guiScreen instanceof GuiPatternTerm || guiScreen instanceof GuiPatternTermEx
                    || guiScreen instanceof GuiFluidPatternTerminal
                    || guiScreen instanceof GuiFluidPatternTerminalEx;
        } else {
            return guiScreen instanceof GuiPatternTerm || guiScreen instanceof GuiPatternTermEx;
        }
    }

}
