package com.sandevistanmod.logic;

import com.sandevistanmod.data.SandevistanData;
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

/** Chip do Link Neural: botao direito para inserir (sem o doutor). Tem durabilidade, consertavel na bigorna com redstone ou ferro. */
public class ChipItem extends Item {
    private final int type;
    private final String effectKey;

    public ChipItem(Properties properties, int type, String effectKey) {
        super(properties);
        this.type = type;
        this.effectKey = effectKey;
    }

    public int type() {
        return type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (!SandevistanLogic.insertChip(sp, stack, type)) return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(net.minecraft.world.item.Items.REDSTONE) || repair.is(net.minecraft.world.item.Items.IRON_INGOT);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Tx.t(ChatFormatting.AQUA, effectKey));
        tooltip.add(Tx.t(ChatFormatting.GRAY, "tooltip.chip.1"));
        tooltip.add(Tx.t(ChatFormatting.GRAY, "tooltip.chip.2"));
        tooltip.add(Tx.t(ChatFormatting.GRAY, "tooltip.chip.3", (int) SandevistanData.LOAD_CHIP));
        int max = SandevistanData.chipMax(type);
        tooltip.add(Tx.t(ChatFormatting.DARK_GRAY, "chip.dur", Math.max(0, max - stack.getDamageValue()), max));
    }
}
