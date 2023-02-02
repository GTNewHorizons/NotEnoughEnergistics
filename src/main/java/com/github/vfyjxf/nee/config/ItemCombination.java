package com.github.vfyjxf.nee.config;

import java.util.Locale;

import net.minecraft.client.resources.I18n;

public enum ItemCombination {

    DISABLED,
    ENABLED,
    WHITELIST;

    public String getLocalName() {
        return I18n.format("gui.neenergistics.button.name" + "." + this.name().toLowerCase(Locale.ROOT));
    }
}
