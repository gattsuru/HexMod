package at.petrak.hexcasting.common.items;

import at.petrak.hexcasting.HexMod;
import at.petrak.hexcasting.api.item.DataHolder;
import at.petrak.hexcasting.api.spell.DatumType;
import at.petrak.hexcasting.api.spell.SpellDatum;
import at.petrak.hexcasting.common.entities.EntityWallScroll;
import at.petrak.hexcasting.hexmath.HexPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * TAG_OP_ID and TAG_PATTERN: "Ancient Scroll of %s" (Great Spells)
 * <br>
 * TAG_PATTERN: "Scroll" (custom)
 * <br>
 * (none): "Empty Scroll"
 * <br>
 * TAG_OP_ID: invalid
 */
public class ItemScroll extends Item implements DataHolder {
    public static final String TAG_OP_ID = "op_id";
    public static final String TAG_PATTERN = "pattern";
    public static final ResourceLocation ANCIENT_PREDICATE = new ResourceLocation(HexMod.MOD_ID, "ancient");

    public ItemScroll(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public @Nullable CompoundTag readDatumTag(ItemStack stack) {
        var stackTag = stack.getTag();
        if (stackTag == null || !stackTag.contains(TAG_PATTERN)) {
            return null;
        }

        var patTag = stackTag.getCompound(TAG_PATTERN);
        var out = new CompoundTag();
        out.put(SpellDatum.TAG_PATTERN, patTag);
        return out;
    }

    @Override
    public boolean canWrite(CompoundTag tag, SpellDatum<?> datum) {
        return datum.getType() == DatumType.PATTERN && !tag.contains(TAG_PATTERN);
    }

    @Override
    public void writeDatum(CompoundTag tag, SpellDatum<?> datum) {
        if (this.canWrite(tag, datum) && datum.getPayload() instanceof HexPattern pat) {
            tag.put(TAG_PATTERN, pat.serializeToNBT());
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        var posClicked = ctx.getClickedPos();
        var direction = ctx.getClickedFace();
        var posInFront = posClicked.relative(direction);
        Player player = ctx.getPlayer();
        ItemStack itemstack = ctx.getItemInHand();
        if (player != null && !this.mayPlace(player, direction, itemstack, posInFront)) {
            return InteractionResult.FAIL;
        }
        var level = ctx.getLevel();
        var scrollStack = itemstack.copy();
        scrollStack.setCount(1);
        var scrollEntity = new EntityWallScroll(level, posInFront, direction, scrollStack);

        // i guess
        var compoundtag = itemstack.getTag();
        if (compoundtag != null) {
            EntityType.updateCustomEntityTag(level, player, scrollEntity, compoundtag);
        }

        if (scrollEntity.survives()) {
            if (!level.isClientSide) {
                scrollEntity.playPlacementSound();
                level.gameEvent(player, GameEvent.ENTITY_PLACE, posClicked);
                level.addFreshEntity(scrollEntity);
            }

            itemstack.shrink(1);
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return InteractionResult.CONSUME;
        }
    }

    protected boolean mayPlace(Player pPlayer, Direction pDirection, ItemStack pHangingEntityStack, BlockPos pPos) {
        return !pDirection.getAxis().isVertical() && pPlayer.mayUseItemAt(pPos, pDirection, pHangingEntityStack);
    }

    @Override
    public Component getName(ItemStack pStack) {
        var tag = pStack.getOrCreateTag();
        if (tag.contains(TAG_OP_ID)) {
            return new TranslatableComponent("item.hexcasting.scroll.of",
                new TranslatableComponent("hexcasting.spell." + ResourceLocation.tryParse(tag.getString(TAG_OP_ID))));
        } else if (tag.contains(TAG_PATTERN)) {
            return new TranslatableComponent("item.hexcasting.scroll");
        } else {
            return new TranslatableComponent("item.hexcasting.scroll.empty");
        }
    }

    // purposely no hover text
}
