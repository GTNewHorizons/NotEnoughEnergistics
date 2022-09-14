package com.github.vfyjxf.nee.utils;

import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.client.gui.implementations.GuiPatternTermEx;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import appeng.helpers.IContainerCraftingPacket;
import com.github.vfyjxf.nee.container.WCTContainerCraftingConfirm;
import cpw.mods.fml.common.Loader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

/**
 * @author vfyjxf
 */
public class GuiUtils {

    private static final boolean isWirelessCraftingTerminalModLoaded = Loader.isModLoaded(ModIDs.WCT);

    public static boolean isGuiWirelessCrafting(GuiScreen gui) {
        if (!isWirelessCraftingTerminalModLoaded) return false;
        return gui instanceof net.p455w0rd.wirelesscraftingterminal.client.gui.GuiWirelessCraftingTerminal;
    }

    public static boolean isWirelessCraftingTermContainer(Container container) {
        if (!isWirelessCraftingTerminalModLoaded) return false;
        return container
                instanceof net.p455w0rd.wirelesscraftingterminal.common.container.ContainerWirelessCraftingTerminal;
    }

    public static boolean isContainerWirelessCraftingConfirm(Container container) {
        if (!isWirelessCraftingTerminalModLoaded) return false;
        return container instanceof net.p455w0rd.wirelesscraftingterminal.common.container.ContainerCraftConfirm;
    }

    public static boolean isWirelessGuiCraftConfirm(GuiScreen gui) {
        if (!isWirelessCraftingTerminalModLoaded) return false;
        return gui instanceof net.p455w0rd.wirelesscraftingterminal.client.gui.GuiCraftConfirm;
    }

    public static boolean isWCTContainerCraftingConfirm(Container container) {
        return container instanceof WCTContainerCraftingConfirm;
    }

    public static boolean isWirelessTerminalGuiObject(Object guiObj) {
        if (!isWirelessCraftingTerminalModLoaded) return false;
        return guiObj instanceof net.p455w0rd.wirelesscraftingterminal.helpers.WirelessTerminalGuiObject;
    }

    public static boolean isCraftingSlot(Slot slot) {
        if (slot == null) {
            return false;
        }
        Container container = Minecraft.getMinecraft().thePlayer.openContainer;
        if (!(container instanceof ContainerPatternTerm) && !(container instanceof ContainerPatternTermEx)) {
            return false;
        }
        IContainerCraftingPacket cct = (IContainerCraftingPacket) container;
        IInventory craftMatrix = cct.getInventoryByName("crafting");
        return craftMatrix.equals(slot.inventory);
    }

    public static boolean isGuiCraftingTerm(GuiScreen guiScreen) {
        return guiScreen instanceof GuiCraftingTerm || isGuiWirelessCrafting(guiScreen);
    }

    public static boolean isPatternTerm(GuiScreen guiScreen) {
        return guiScreen instanceof GuiPatternTerm || guiScreen instanceof GuiPatternTermEx;
    }

    public static boolean isGuiCraftConfirm(GuiScreen gui) {
        return gui instanceof GuiCraftConfirm || isWirelessGuiCraftConfirm(gui);
    }
}
