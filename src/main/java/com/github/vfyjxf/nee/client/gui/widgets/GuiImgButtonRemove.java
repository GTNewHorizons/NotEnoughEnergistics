package com.github.vfyjxf.nee.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import appeng.client.gui.widgets.ITooltip;

public class GuiImgButtonRemove extends GuiButton implements ITooltip {

    public GuiImgButtonRemove(int x, int y) {
        super(0, x, y, "");
        this.width = 8;
        this.height = 8;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {

            GL11.glPushMatrix();
            GL11.glTranslatef(this.xPosition, this.yPosition, 0.0F);
            GL11.glScalef(0.5f, 0.5f, 0.5f);
            if (this.enabled) {
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                GL11.glColor4f(0.5f, 0.5f, 0.5f, 1.0f);
            }
            mc.renderEngine.bindTexture(new ResourceLocation("neenergistics", "textures/gui/states.png"));
            this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
                    && mouseX < this.xPosition + this.width
                    && mouseY < this.yPosition + this.height;
            this.drawTexturedModalRect(0, 0, 0, 0, 16, 16);
            this.drawTexturedModalRect(0, 0, 0, 32, 16, 16);
            this.mouseDragged(mc, mouseX, mouseY);
            GL11.glPopMatrix();
        }
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public String getMessage() {
        return I18n.format("gui.neenergistics.button.title.remove") + "\n"
                + I18n.format("gui.neenergistics.button.tooltip.remove");
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
