package com.sandevistanmod.logic;

import com.sandevistanmod.data.SandevistanVariant;
import com.sandevistanmod.data.Tx;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Os implantes Sandevistan (Apogee, Falcon, Dynalar, Zetatech, QianT): so se instalam com o Ripperdoc. */
public class SandevistanItem extends Item {
    private final int variant;

    public SandevistanItem(Properties properties, int variant) {
        super(properties);
        this.variant = variant;
    }

    public int variant() {
        return variant;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Tx.t(ChatFormatting.AQUA, "clinic.hint"));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SandevistanVariant v = SandevistanVariant.of(variant);
        tooltip.add(Tx.t(ChatFormatting.GREEN, "tooltip.1"));
        if (variant == 0) {
            tooltip.add(Tx.t(ChatFormatting.GRAY, "implant.tip.1"));
            tooltip.add(Tx.t(ChatFormatting.GRAY, "implant.tip.2"));
            tooltip.add(Tx.t(ChatFormatting.GRAY, "implant.tip.3"));
            tooltip.add(Tx.t(ChatFormatting.RED, "implant.tip.4"));
        } else {
            tooltip.add(Tx.t(ChatFormatting.GRAY, "variant.tip.slow", v.slow));
            tooltip.add(Tx.t(ChatFormatting.GRAY, "variant.tip.time", (int) v.seconds));
            tooltip.add(Tx.t(ChatFormatting.GRAY, "variant.tip.dodge", Math.round(v.dodge * 100f)));
            if (variant == 3) tooltip.add(Tx.t(ChatFormatting.YELLOW, "variant.tip.zetatech"));
            if (variant == 4) tooltip.add(Tx.t(ChatFormatting.GOLD, "variant.tip.qiant"));
            tooltip.add(Tx.t(ChatFormatting.RED, "implant.tip.4"));
        }
        tooltip.add(Tx.t(ChatFormatting.GOLD, "tooltip.load", (int) v.load));
    }
}
