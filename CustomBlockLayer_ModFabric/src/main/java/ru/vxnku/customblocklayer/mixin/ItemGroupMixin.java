package ru.vxnku.customblocklayer.mixin;

import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.vxnku.customblocklayer.config.CustomBlockRegistry;

@Mixin(ItemGroup.class)
public abstract class ItemGroupMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("CBL-ItemGroupMixin");

    @Shadow
    public abstract net.minecraft.text.Text getDisplayName();

    @Inject(method = "shouldDisplay", at = @At("HEAD"), cancellable = true)
    private void customBlockLayer$onShouldDisplay(CallbackInfoReturnable<Boolean> cir) {
        try {
            ItemGroup self = (ItemGroup) (Object) this;
            Identifier id = Registries.ITEM_GROUP.getId(self);
            if (id != null && "customblocklayer".equals(id.getNamespace())) {
                String title = self.getDisplayName().getString();
                // Check if we have definitions for this column
                boolean hasBlocks = "CustomBlockLayer".equalsIgnoreCase(title)
                    ? !CustomBlockRegistry.getAllDefinitions().isEmpty()
                    : CustomBlockRegistry.getAllDefinitions().stream().anyMatch(def -> title.equalsIgnoreCase(def.getColumn()));
                
                if (hasBlocks) {
                    cir.setReturnValue(true);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[CBL-Mixin] Error checking shouldDisplay for ItemGroup: ", e);
        }
    }
}
