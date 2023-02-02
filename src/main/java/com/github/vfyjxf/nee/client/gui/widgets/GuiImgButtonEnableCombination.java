package com.github.vfyjxf.nee.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import appeng.client.gui.widgets.ITooltip;

import com.github.vfyjxf.nee.config.ItemCombination;

public class GuiImgButtonEnableCombination extends GuiButton implements ITooltip {

    private ItemCombination currentValue;

    public GuiImgButtonEnableCombination(int x, int y, ItemCombination value) {
        super(0, x, y, "");
        this.xPosition = x;
        this.yPosition = y;
        this.width = 8;
        this.height = 8;
        this.currentValue = value;
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
        if (this.visible) {
            GL11.glPushMatrix();
            GL11.glTranslatef(this.xPosition, this.yPosition, 0.0F);
            GL11.glScalef(0.5f, 0.5f, 0.5f);
            if (this.enabled) {
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                GL11.glColor4f(0.5f, 0.5f, 0.5f, 1.0f);
            }
            minecraft.renderEngine.bindTexture(new ResourceLocation("neenergistics", "textures/gui/states.png"));
            this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
                    && mouseX < this.xPosition + this.width
                    && mouseY < this.yPosition + this.height;
            this.drawTexturedModalRect(0, 0, 0, 0, 16, 16);
            if (this.currentValue == ItemCombination.ENABLED) {
                this.drawTexturedModalRect(0, 0, 16, 16, 16, 16);
            }
            if (this.currentValue == ItemCombination.DISABLED) {
                this.drawTexturedModalRect(0, 0, 16, 0, 16, 16);
            }
            if (this.currentValue == ItemCombination.WHITELIST) {
                this.drawTexturedModalRect(0, 0, 0, 16, 16, 16);
            }
            this.mouseDragged(minecraft, mouseX, mouseY);
            GL11.glPopMatrix();
        }
    }

    public void setValue(ItemCombination currentValue) {
        this.currentValue = currentValue;
    }

    public ItemCombination getCurrentValue() {
        return currentValue;
    }

    @Override
    public String getMessage() {
        return I18n.format("gui.neenergistics.button.title.combination") + "\n"
                + I18n.format("gui.neenergistics.button.tooltip.combination", currentValue.getLocalName());
    }

    @Override
    public int xPos() {
        return this.xPosition;
    }

    @Override
    public int yPos() {
        return this.yPosition;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }
}
