package com.github.vfyjxf.nee.utils;

import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.helpers.IContainerCraftingPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

/**
 * @author vfyjxf
 */
public class GuiUtils {

    public static boolean isPatternTermExGui(GuiScreen container) {
        try {
            Class<?> patternTermExClz = Class.forName("appeng.client.gui.implementations.GuiPatternTermEx");
            return patternTermExClz.isInstance(container);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isPatternTermExContainer(Container container) {
        try {
            Class<?> patternTermExClz = Class.forName("appeng.container.implementations.ContainerPatternTermEx");
            return patternTermExClz.isInstance(container);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isCraftingSlot(Slot slot) {
        Container container = Minecraft.getMinecraft().thePlayer.openContainer;
        if (slot != null) {
            if (container instanceof ContainerPatternTerm || GuiUtils.isPatternTermExContainer(container)) {
                IContainerCraftingPacket cct = (IContainerCraftingPacket) container;
                IInventory craftMatrix = cct.getInventoryByName("crafting");
                return craftMatrix.equals(slot.inventory);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean isGuiWirelessCrafting(GuiScreen gui) {

        try {
            Class<?> guiWirelessCraftingClass =
                    Class.forName("net.p455w0rd.wirelesscraftingterminal.client.gui.GuiWirelessCraftingTerminal");
            return guiWirelessCraftingClass.isInstance(gui);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isWirelessCraftingTermContainer(Container container) {
        try {
            Class<?> wirelessCraftingTermContainerClass = Class.forName(
                    "net.p455w0rd.wirelesscraftingterminal.common.container.ContainerWirelessCraftingTerminal");
            return wirelessCraftingTermContainerClass.isInstance(container);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isWirelessGuiCraftConfirm(GuiScreen gui) {
        try {
            Class<?> wirelessGuiCraftConfirmClass =
                    Class.forName("net.p455w0rd.wirelesscraftingterminal.client.gui.GuiCraftConfirm");
            return wirelessGuiCraftConfirmClass.isInstance(gui);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isContainerWirelessCraftingConfirm(Container container) {
        try {
            Class<?> wirelessCraftingConfirmClass =
                    Class.forName("net.p455w0rd.wirelesscraftingterminal.common.container.ContainerCraftConfirm");
            return wirelessCraftingConfirmClass.isInstance(container);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isWCTContainerCraftingConfirm(Container container) {
        try {
            Class<?> wctCraftingConfirmClass =
                    Class.forName("com.github.vfyjxf.nee.container.WCTContainerCraftingConfirm");
            return wctCraftingConfirmClass.isInstance(container);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isWirelessTerminalGuiObject(Object guiObj) {
        try {
            Class<?> wirelessTerminalGuiObjClass =
                    Class.forName("net.p455w0rd.wirelesscraftingterminal.helpers.WirelessTerminalGuiObject");
            return wirelessTerminalGuiObjClass.isInstance(guiObj);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isGuiCraftingTerm(GuiScreen guiScreen) {
        return guiScreen instanceof GuiCraftingTerm || isGuiWirelessCrafting(guiScreen);
    }

    public static boolean isPatternTerm(GuiScreen guiScreen) {
        return guiScreen instanceof GuiPatternTerm || isPatternTermExGui(guiScreen);
    }

    public static boolean isGuiCraftConfirm(GuiScreen gui) {
        return gui instanceof GuiCraftConfirm || isWirelessGuiCraftConfirm(gui);
    }
}
