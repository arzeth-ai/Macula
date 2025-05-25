package net.mine_diver.macula.mixin;

import net.minecraft.client.gui.widget.EntryListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntryListWidget.class)
public interface ScrollableBaseAccessor {
    @Accessor("width")
    int macula_getWidth();
}
