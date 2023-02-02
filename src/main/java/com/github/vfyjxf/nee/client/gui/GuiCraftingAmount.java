package com.github.vfyjxf.nee.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.p455w0rd.wirelesscraftingterminal.items.ItemEnum;
import net.p455w0rd.wirelesscraftingterminal.reference.Reference;

import appeng.api.AEApi;
import appeng.api.definitions.IDefinitions;
import appeng.api.definitions.IParts;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiNumberBox;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.container.AEBaseContainer;
import appeng.core.AEConfig;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.helpers.Reflected;
import appeng.parts.reporting.PartCraftingTerminal;

import com.github.vfyjxf.nee.container.ContainerCraftingAmount;
import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketCraftingRequest;
import com.github.vfyjxf.nee.utils.GuiUtils;
import com.github.vfyjxf.nee.utils.ModIDs;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;

public class GuiCraftingAmount extends AEBaseGui {

    private GuiNumberBox amountToCraft;
    private GuiTabButton originalGuiBtn;

    private GuiButton next;

    private GuiButton plus1;
    private GuiButton plus10;
    private GuiButton plus100;
    private GuiButton plus1000;
    private GuiButton minus1;
    private GuiButton minus10;
    private GuiButton minus100;
    private GuiButton minus1000;

    private GuiBridge originalGui;

    private boolean isWirelessCrafting;

    @Reflected
    public GuiCraftingAmount(final InventoryPlayer inventoryPlayer, final ITerminalHost te) {
        super(new ContainerCraftingAmount(inventoryPlayer, te));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initGui() {
        super.initGui();

        final int a = AEConfig.instance.craftItemsByStackAmounts(0);
        final int b = AEConfig.instance.craftItemsByStackAmounts(1);
        final int c = AEConfig.instance.craftItemsByStackAmounts(2);
        final int d = AEConfig.instance.craftItemsByStackAmounts(3);

        this.buttonList.add(this.plus1 = new GuiButton(0, this.guiLeft + 20, this.guiTop + 26, 22, 20, "+" + a));
        this.buttonList.add(this.plus10 = new GuiButton(0, this.guiLeft + 48, this.guiTop + 26, 28, 20, "+" + b));
        this.buttonList.add(this.plus100 = new GuiButton(0, this.guiLeft + 82, this.guiTop + 26, 32, 20, "+" + c));
        this.buttonList.add(this.plus1000 = new GuiButton(0, this.guiLeft + 120, this.guiTop + 26, 38, 20, "+" + d));

        this.buttonList.add(this.minus1 = new GuiButton(0, this.guiLeft + 20, this.guiTop + 75, 22, 20, "-" + a));
        this.buttonList.add(this.minus10 = new GuiButton(0, this.guiLeft + 48, this.guiTop + 75, 28, 20, "-" + b));
        this.buttonList.add(this.minus100 = new GuiButton(0, this.guiLeft + 82, this.guiTop + 75, 32, 20, "-" + c));
        this.buttonList.add(this.minus1000 = new GuiButton(0, this.guiLeft + 120, this.guiTop + 75, 38, 20, "-" + d));

        this.buttonList.add(
                this.next = new GuiButton(0, this.guiLeft + 128, this.guiTop + 51, 38, 20, GuiText.Next.getLocal()));

        ItemStack myIcon = null;
        final Object target = ((AEBaseContainer) this.inventorySlots).getTarget();
        final IDefinitions definitions = AEApi.instance().definitions();
        final IParts parts = definitions.parts();

        if (target instanceof PartCraftingTerminal) {
            for (final ItemStack stack : parts.craftingTerminal().maybeStack(1).asSet()) {
                myIcon = stack;
            }
            this.originalGui = GuiBridge.GUI_CRAFTING_TERMINAL;
        }
        if (Loader.isModLoaded(ModIDs.WCT) && GuiUtils.isWirelessTerminalGuiObject(target)) {
            myIcon = new ItemStack(ItemEnum.WIRELESS_CRAFTING_TERMINAL.getItem());
            this.isWirelessCrafting = true;
        }

        if (this.originalGui != null && myIcon != null) {
            this.buttonList.add(
                    this.originalGuiBtn = new GuiTabButton(
                            this.guiLeft + 154,
                            this.guiTop,
                            myIcon,
                            myIcon.getDisplayName(),
                            itemRender));
        }
        if (this.isWirelessCrafting && myIcon != null) {
            this.buttonList.add(
                    this.originalGuiBtn = new GuiTabButton(
                            this.guiLeft + 154,
                            this.guiTop,
                            myIcon,
                            myIcon.getDisplayName(),
                            itemRender));
        }

        this.amountToCraft = new GuiNumberBox(
                this.fontRendererObj,
                this.guiLeft + 62,
                this.guiTop + 57,
                59,
                this.fontRendererObj.FONT_HEIGHT,
                Integer.class);
        this.amountToCraft.setEnableBackgroundDrawing(false);
        this.amountToCraft.setMaxStringLength(16);
        this.amountToCraft.setTextColor(0xFFFFFF);
        this.amountToCraft.setVisible(true);
        this.amountToCraft.setFocused(true);
        this.amountToCraft.setText("1");
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRendererObj.drawString(GuiText.SelectAmount.getLocal(), 8, 6, 4210752);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.next.displayString = isShiftKeyDown() ? GuiText.Start.getLocal() : GuiText.Next.getLocal();

        this.bindTexture("guis/craftAmt.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);

        try {
            Long.parseLong(this.amountToCraft.getText());
            this.next.enabled = this.amountToCraft.getText().length() > 0;
        } catch (final NumberFormatException e) {
            this.next.enabled = false;
        }

        this.amountToCraft.drawTextBox();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float btn) {
        super.drawScreen(mouseX, mouseY, btn);
    }

    @Override
    protected void keyTyped(final char character, final int key) {
        if (!this.checkHotbarKeys(key)) {
            if (key == 28) {
                this.actionPerformed(this.next);
            }
            if ((key == 211 || key == 205
                    || key == 203
                    || key == 14
                    || character == '-'
                    || Character.isDigit(character)) && this.amountToCraft.textboxKeyTyped(character, key)) {
                try {
                    String out = this.amountToCraft.getText();

                    boolean fixed = false;
                    while (out.startsWith("0") && out.length() > 1) {
                        out = out.substring(1);
                        fixed = true;
                    }

                    if (fixed) {
                        this.amountToCraft.setText(out);
                    }

                    if (out.isEmpty()) {
                        out = "0";
                    }

                    final long result = Long.parseLong(out);
                    if (result < 0) {
                        this.amountToCraft.setText("1");
                    }
                } catch (final NumberFormatException e) {
                    // :P
                }
            } else {
                super.keyTyped(character, key);
            }
        }
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);

        try {

            if (btn == this.originalGuiBtn) {
                if (!this.isWirelessCrafting) {
                    NetworkHandler.instance.sendToServer(new PacketSwitchGuis(this.originalGui));
                } else if (Loader.isModLoaded(ModIDs.WCT)) {
                    openWirelessCraftingGui();
                }
            }

            if (btn == this.next) {
                NEENetworkHandler.getInstance().sendToServer(
                        new PacketCraftingRequest(Integer.parseInt(this.amountToCraft.getText()), isShiftKeyDown()));
            }
        } catch (final NumberFormatException e) {
            // nope..
            this.amountToCraft.setText("1");
        }

        final boolean isPlus = btn == this.plus1 || btn == this.plus10 || btn == this.plus100 || btn == this.plus1000;
        final boolean isMinus = btn == this.minus1 || btn == this.minus10
                || btn == this.minus100
                || btn == this.minus1000;

        if (isPlus || isMinus) {
            this.addQty(this.getQty(btn));
        }
    }

    private void addQty(final int i) {
        try {
            String out = this.amountToCraft.getText();

            boolean fixed = false;
            while (out.startsWith("0") && out.length() > 1) {
                out = out.substring(1);
                fixed = true;
            }

            if (fixed) {
                this.amountToCraft.setText(out);
            }

            if (out.isEmpty()) {
                out = "0";
            }

            long result = Integer.parseInt(out);

            if (result == 1 && i > 1) {
                result = 0;
            }

            result += i;
            if (result < 1) {
                result = 1;
            }

            out = Long.toString(result);
            Integer.parseInt(out);
            this.amountToCraft.setText(out);
        } catch (final NumberFormatException e) {
            // :P
        }
    }

    protected String getBackground() {
        return "guis/craftAmt.png";
    }

    @Optional.Method(modid = ModIDs.WCT)
    private void openWirelessCraftingGui() {
        net.p455w0rd.wirelesscraftingterminal.core.sync.network.NetworkHandler.instance.sendToServer(
                new net.p455w0rd.wirelesscraftingterminal.core.sync.packets.PacketSwitchGuis(Reference.GUI_WCT));
    }
}
