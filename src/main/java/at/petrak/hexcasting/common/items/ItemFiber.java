package at.petrak.hexcasting.common.items;

import at.petrak.hexcasting.api.item.DataHolderItem;
import at.petrak.hexcasting.api.spell.SpellDatum;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class ItemFiber extends Item implements DataHolderItem {
    public static final String TAG_DATA = "data";

    public ItemFiber(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        var world = pContext.getLevel();
        if (!world.isClientSide) {
            var stack = pContext.getItemInHand();
            stack.getOrCreateTag().put(TAG_DATA, SpellDatum.make(pContext.getClickedPos()).serializeToNBT());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack pStack, Player pPlayer, LivingEntity pInteractionTarget,
        InteractionHand pUsedHand) {
        if (!(pInteractionTarget instanceof Player)) {
            if (!pPlayer.isLocalPlayer()) {
                var tag = pStack.getOrCreateTag();
                tag.put(TAG_DATA, SpellDatum.make(pInteractionTarget).serializeToNBT());
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        var stack = pPlayer.getItemInHand(pUsedHand);
        if (pPlayer.isDiscrete()) {
            stack.removeTagKey(TAG_DATA);
        }
        if (!pLevel.isClientSide) {
            stack.getOrCreateTag().put(TAG_DATA, SpellDatum.make(pPlayer).serializeToNBT());
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public @Nullable CompoundTag readDatumTag(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_DATA)) {
            return null;
        }
        return tag.getCompound(TAG_DATA);
    }

    @Override
    public boolean canWrite(ItemStack stack, @Nullable SpellDatum<?> datum) {
        return false;
    }

    @Override
    public void writeDatum(ItemStack stack, @Nullable SpellDatum<?> datum) {

    }
}
