package com.github.vfyjxf.nee.nei;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.oredict.OreDictionary;

import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketCraftingRequest;
import com.github.vfyjxf.nee.utils.GuiUtils;
import com.github.vfyjxf.nee.utils.Ingredient;
import com.github.vfyjxf.nee.utils.IngredientTracker;
import com.github.vfyjxf.nee.utils.ItemUtils;
import com.github.vfyjxf.nee.utils.ModIDs;
import com.glodblock.github.client.gui.GuiFluidCraftAmount;
import com.glodblock.github.client.gui.base.FCGuiAmount;

import appeng.api.storage.data.IAEItemStack;
import appeng.client.gui.implementations.GuiAmount;
import appeng.client.gui.implementations.GuiCraftAmount;
import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerNull;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.helpers.InventoryAction;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import thaumicenergistics.common.network.packet.server.Packet_S_ArcaneCraftingTerminal;

public class NEECraftingPreviewHandler {

    public static final NEECraftingPreviewHandler instance = new NEECraftingPreviewHandler();

    private boolean isPatternInterfaceExists = false;

    private NBTTagCompound patternCompound = null;
    private IngredientTracker tracker = null;
    private boolean isAutoStart = false;
    private int resultStackSize = 0;
    private String modID = "";

    private NEECraftingPreviewHandler() {}

    public boolean handle(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex) {
        final PositionedStack pStack = recipe.getResultStack(recipeIndex);
        this.isAutoStart = NEIClientConfig.isKeyHashDown("nee.nopreview");
        this.modID = getModID(firstGui.inventorySlots);
        this.resultStackSize = 0;
        this.patternCompound = null;
        this.tracker = null;

        if ((this.isAutoStart || NEIClientConfig.isKeyHashDown("nee.preview")) && !this.modID.isEmpty()) {
            firstGui.mc.displayGuiScreen(firstGui);

            if (pStack != null) {
                this.resultStackSize = pStack.item.stackSize;
            }

            if (pStack != null && existsRecipeResult(firstGui, pStack)) {

                if (this.isAutoStart) {
                    final PacketCraftingRequest craftingRequest = new PacketCraftingRequest(
                            this.modID,
                            PacketCraftingRequest.COMMAND_OPEN_CRAFT_CONFIRM,
                            pStack.item.writeToNBT(new NBTTagCompound()),
                            pStack.item.stackSize,
                            true);
                    NEENetworkHandler.getInstance().sendToServer(craftingRequest);
                } else {
                    openCraftAmount(firstGui, pStack.item);
                }

                return true;
            } else if (pStack != null && this.isPatternInterfaceExists && isCraftingTableRecipe(recipe)) {
                this.patternCompound = getPatternStack(recipe, recipeIndex, Minecraft.getMinecraft().theWorld);

                if (this.isAutoStart) {
                    final PacketCraftingRequest craftingRequest = new PacketCraftingRequest(
                            this.modID,
                            PacketCraftingRequest.COMMAND_CREATE_PATTERN,
                            this.patternCompound,
                            pStack.item.stackSize,
                            true);
                    NEENetworkHandler.getInstance().sendToServer(craftingRequest);
                } else {
                    openCraftAmount(firstGui, pStack.item);
                }

                return true;
            } else {

                this.tracker = new IngredientTracker(firstGui, recipe, recipeIndex);

                if (this.tracker.hasNext()) {

                    if (this.isAutoStart) {
                        requestNextIngredient();
                    } else if (pStack != null) {
                        openCraftAmount(firstGui, pStack.item);
                    } else {
                        PositionedStack otherStack = null;

                        for (PositionedStack positionedStack : recipe.getOtherStacks(recipeIndex)) {
                            otherStack = positionedStack;
                            break;
                        }

                        if (otherStack != null) {
                            this.resultStackSize = otherStack.item.stackSize;
                            openCraftAmount(firstGui, otherStack.item);
                        } else {
                            return false;
                        }
                    }

                    return true;
                } else {
                    this.tracker = null;
                    return false;
                }

            }

        }

        return false;
    }

    private void openCraftAmount(GuiContainer firstGui, ItemStack itemstack) {
        final IAEItemStack aeItemStack = AEItemStack.create(itemstack);

        if (this.modID.equals(ModIDs.ThE)) {
            Packet_S_ArcaneCraftingTerminal.sendAutoCraft(Minecraft.getMinecraft().thePlayer, aeItemStack);
        } else if (this.modID.equals(ModIDs.FC)) {
            ((AEBaseContainer) firstGui.inventorySlots).setTargetStack(aeItemStack);
            com.glodblock.github.network.CPacketInventoryAction packet = new com.glodblock.github.network.CPacketInventoryAction(
                    InventoryAction.AUTO_CRAFT,
                    0,
                    0);
            com.glodblock.github.FluidCraft.proxy.netHandler.sendToServer(packet);
        } else if (this.modID.equals(ModIDs.WCT)) {
            ((AEBaseContainer) firstGui.inventorySlots).setTargetStack(aeItemStack);
            net.p455w0rd.wirelesscraftingterminal.core.sync.packets.PacketInventoryAction packet = new net.p455w0rd.wirelesscraftingterminal.core.sync.packets.PacketInventoryAction(
                    InventoryAction.AUTO_CRAFT,
                    0,
                    0);
            net.p455w0rd.wirelesscraftingterminal.core.sync.network.NetworkHandler.instance.sendToServer(packet);
        } else if (this.modID.equals("AE")) {
            ((AEBaseContainer) firstGui.inventorySlots).setTargetStack(aeItemStack);
            NetworkHandler.instance.sendToServer(new PacketInventoryAction(InventoryAction.AUTO_CRAFT, 0, 0));
        }

    }

    @SubscribeEvent
    public void onGuiCraftConfirmOpen(GuiOpenEvent event) {
        final GuiScreen old = Minecraft.getMinecraft().currentScreen;

        if (this.patternCompound != null && old instanceof GuiCraftConfirm) {
            this.patternCompound = null;
        }

        if (this.tracker != null && old instanceof GuiCraftConfirm) {
            if (old != null && this.tracker.hasNext()) {
                requestNextIngredient();
            } else {
                this.tracker = null;
            }
        }
    }

    @SubscribeEvent
    public void onCraftConfirmActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {

        if ((this.tracker != null || this.patternCompound != null) && event.gui instanceof GuiContainer gui) {
            final int craftAmount = getCraftAmount(gui, event.button);

            if (craftAmount == -1) {
                return;
            }

            if (this.tracker != null) {
                final int craftMultiplier = (int) Math.ceil((1f * craftAmount) / this.resultStackSize);
                this.isAutoStart = this.isAutoStart || GuiScreen.isShiftKeyDown();

                for (Ingredient ingr : this.tracker.getIngredients()) {
                    ingr.setRequireCount(ingr.getRequireCount() * craftMultiplier);
                }

                this.tracker.calculateIngredients();

                requestNextIngredient();
                event.setCanceled(true);
            } else if (this.patternCompound != null) {
                PacketCraftingRequest craftingRequest = new PacketCraftingRequest(
                        this.modID,
                        PacketCraftingRequest.COMMAND_CREATE_PATTERN,
                        this.patternCompound,
                        craftAmount,
                        GuiScreen.isShiftKeyDown());
                NEENetworkHandler.getInstance().sendToServer(craftingRequest);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onCraftConfirmActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {

        if ((this.tracker != null || this.patternCompound != null) && event.gui instanceof GuiCraftConfirm guiConfirm
                && getCancelButton(guiConfirm) == event.button) {

            if (this.patternCompound != null) {
                PacketCraftingRequest craftingRequest = new PacketCraftingRequest(
                        this.modID,
                        PacketCraftingRequest.COMMAND_REMOVE_PATTERN,
                        this.patternCompound,
                        0,
                        false);
                NEENetworkHandler.getInstance().sendToServer(craftingRequest);
            }

            this.patternCompound = null;
            this.tracker = null;
        }
    }

    private static GuiButton getCancelButton(GuiCraftConfirm gui) {
        return ReflectionHelper.getPrivateValue(GuiCraftConfirm.class, gui, "cancel");
    }

    private static int getCraftAmount(GuiContainer screen, GuiButton button) {

        try {
            if (Loader.isModLoaded(ModIDs.FC) && screen instanceof GuiFluidCraftAmount gui) {

                if (ReflectionHelper.getPrivateValue(FCGuiAmount.class, gui, "submit") == button) {
                    return (int) ReflectionHelper.findMethod(FCGuiAmount.class, gui, new String[] { "getAmount" })
                            .invoke(gui);
                }

            } else if (screen instanceof GuiCraftAmount gui) {

                if (ReflectionHelper.getPrivateValue(GuiAmount.class, gui, "nextBtn") == button) {
                    return (int) ReflectionHelper.findMethod(GuiAmount.class, gui, new String[] { "getAmount" })
                            .invoke(gui);
                }

            }
        } catch (Exception ex) {
            return 1;
        }

        return -1;
    }

    private static String getModID(Container container) {

        if (Loader.isModLoaded(ModIDs.ThE)
                && container.getClass().getName().startsWith("thaumicenergistics.common.container")) {
            return ModIDs.ThE;
        } else if (Loader.isModLoaded(ModIDs.FC)
                && container.getClass().getName().startsWith("com.glodblock.github.client.gui.container")) {
                    return ModIDs.FC;
                } else
            if (Loader.isModLoaded(ModIDs.WCT) && container.getClass().getName()
                    .startsWith("net.p455w0rd.wirelesscraftingterminal.common.container")) {
                        return ModIDs.WCT;
                    } else
                if (container instanceof AEBaseContainer) {
                    return "AE";
                }

        return "";
    }

    private void requestNextIngredient() {
        final ItemStack stack = this.tracker.getNextIngredient();

        if (stack != null) {
            PacketCraftingRequest craftingRequest = new PacketCraftingRequest(
                    this.modID,
                    PacketCraftingRequest.COMMAND_OPEN_CRAFT_CONFIRM,
                    stack.writeToNBT(new NBTTagCompound()),
                    stack.stackSize,
                    this.isAutoStart);
            NEENetworkHandler.getInstance().sendToServer(craftingRequest);
        }
    }

    private boolean isCraftingTableRecipe(IRecipeHandler recipe) {
        if (recipe instanceof TemplateRecipeHandler templateRecipeHandler) {
            String overlayIdentifier = templateRecipeHandler.getOverlayIdentifier();
            return "crafting".equals(overlayIdentifier) || "crafting2x2".equals(overlayIdentifier);
        } else {
            return false;
        }
    }

    private NBTTagCompound getPatternStack(IRecipeHandler recipeHandler, int recipeIndex, World world) {
        final InventoryCrafting ic = new InventoryCrafting(new ContainerNull(), 3, 3);
        final List<PositionedStack> ingredients = recipeHandler.getIngredientStacks(recipeIndex);

        for (final PositionedStack positionedStack : ingredients) {
            final int col = (positionedStack.relx - 25) / 18;
            final int row = (positionedStack.rely - 6) / 18;
            final int slotIndex = col + row * 3;

            if (positionedStack.items != null && positionedStack.items.length > 0) {
                final ItemStack[] currentStackList = positionedStack.items;
                ItemStack stack = positionedStack.items[0];

                final ItemStack preferModItem = ItemUtils.getPreferModItem(positionedStack.items);

                if (preferModItem != null) {
                    stack = preferModItem;
                }

                for (ItemStack currentStack : currentStackList) {
                    if (Platform.isRecipePrioritized(currentStack) || ItemUtils.isPreferItems(currentStack)) {
                        stack = currentStack.copy();
                    }
                }

                if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                    stack.setItemDamage(0);
                }

                ic.setInventorySlotContents(slotIndex, stack);
            } else {
                ic.setInventorySlotContents(slotIndex, null);
            }
        }

        final ItemStack result = CraftingManager.getInstance().findMatchingRecipe(ic, world);

        if (result != null) {
            final NBTTagCompound patternValue = new NBTTagCompound();
            final NBTTagList tagIn = new NBTTagList();

            for (int slotIndex = 0; slotIndex < ic.getSizeInventory(); slotIndex++) {
                final ItemStack itemStack = ic.getStackInSlot(slotIndex);

                if (itemStack != null) {
                    tagIn.appendTag(itemStack.writeToNBT(new NBTTagCompound()));
                } else {
                    tagIn.appendTag(new NBTTagCompound());
                }
            }

            patternValue.setTag("in", tagIn);
            patternValue.setTag("out", result.writeToNBT(new NBTTagCompound()));
            patternValue.setBoolean("crafting", true);
            patternValue.setBoolean("substitute", false);

            return patternValue;
        }

        return null;
    }

    public void setIsPatternInterfaceExists(boolean isPatternInterfaceExists) {
        this.isPatternInterfaceExists = isPatternInterfaceExists;
    }

    public boolean canCraftRecipeResult(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex) {
        final PositionedStack pStack = recipe.getResultStack(recipeIndex);

        if (pStack == null) {
            return false;
        }

        if (this.isPatternInterfaceExists && isCraftingTableRecipe(recipe)) {
            return true;
        }

        return existsRecipeResult(firstGui, pStack);
    }

    private boolean existsRecipeResult(GuiContainer firstGui, PositionedStack pStack) {
        return pStack != null && !GuiUtils
                .getStorageStacks(firstGui, aestack -> aestack.isCraftable() && aestack.isSameType(pStack.item))
                .isEmpty();
    }

}
