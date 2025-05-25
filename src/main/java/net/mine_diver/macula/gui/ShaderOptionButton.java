package net.mine_diver.macula.gui;

import net.mine_diver.macula.option.ShaderConfig;
import net.mine_diver.macula.option.ShaderOption;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;

public class ShaderOptionButton extends ButtonWidget {
    private final ShaderOption enumShaderOption;
    
    public ShaderOptionButton(final ShaderOption enumShaderOption, final int x, final int y, final int widthIn, final int heightIn) {
        super(enumShaderOption.ordinal(), x, y, widthIn, heightIn, getButtonText(enumShaderOption));
        this.enumShaderOption = enumShaderOption;
    }
    
    public ShaderOption getEnumShaderOption() {
        return this.enumShaderOption;
    }
    
    private static String getButtonText(final ShaderOption eso) {
        final String nameText = I18n.getTranslation(eso.getResourceKey()) + ": ";
        return switch (eso) {
            case SHADOW_RES_MUL -> nameText + ShadersScreen.toStringQuality(ShaderConfig.configShadowResMul);
            default -> throw new IllegalStateException("Unexpected value: " + eso);
        };
    }
    
    public void updateButtonText() {
        this.text = getButtonText(this.enumShaderOption);
    }
}
