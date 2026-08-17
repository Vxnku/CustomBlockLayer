package ru.vxnku.customblocklayer.mixin;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemGroups.class)
public class ItemGroupsMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("CBL-ItemGroupsMixin");

    @Inject(method = "getGroupsToDisplay", at = @At("HEAD"), cancellable = true)
    private static void customBlockLayer$onGetGroupsToDisplay(CallbackInfoReturnable<List<ItemGroup>> cir) {
        try {
            List<ItemGroup> list = new ArrayList<>(Registries.ITEM_GROUP.stream().filter(ItemGroup::shouldDisplay).toList());
            LOGGER.info("[CBL-Mixin] Creative Tabs display list built with {} active tabs.", list.size());
            cir.setReturnValue(list);
        } catch (Exception e) {
            LOGGER.error("[CBL-Mixin] Error providing getGroupsToDisplay: ", e);
        }
    }

    @Inject(method = "getGroups", at = @At("HEAD"), cancellable = true)
    private static void customBlockLayer$onGetGroups(CallbackInfoReturnable<List<ItemGroup>> cir) {
        try {
            List<ItemGroup> list = new ArrayList<>(Registries.ITEM_GROUP.stream().toList());
            cir.setReturnValue(list);
        } catch (Exception e) {
            LOGGER.error("[CBL-Mixin] Error providing getGroups: ", e);
        }
    }
}
