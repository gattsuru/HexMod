package at.petrak.hexcasting.common.casting.operators.spells

import at.petrak.hexcasting.api.item.ColorizedItem
import at.petrak.hexcasting.api.item.ColorizerItem
import at.petrak.hexcasting.api.misc.ManaConstants
import at.petrak.hexcasting.api.spell.*
import at.petrak.hexcasting.api.spell.casting.CastingContext
import at.petrak.hexcasting.api.spell.mishaps.MishapBadItem
import at.petrak.hexcasting.api.spell.mishaps.MishapBadOffhandItem
import at.petrak.hexcasting.common.lib.HexItems
import at.petrak.hexcasting.xplat.IXplatAbstractions
import net.minecraft.world.entity.item.ItemEntity
import kotlin.math.abs

object OpColorizeItem : SpellOperator {
    override val argc = 1

    override fun execute(
        args: List<SpellDatum<*>>,
        ctx: CastingContext
    ): Triple<RenderedSpell, Int, List<ParticleSpray>> {
        val entity = args.getChecked<ItemEntity>(0, argc)
        ctx.assertEntityInRange(entity)
        if(entity.item.item !is ColorizedItem)
        {
            throw MishapBadItem.of(
                    entity,
                    "colorizable"
            )
        }

        val (handStack, hand) = ctx.getHeldItemToOperateOn(IXplatAbstractions.INSTANCE::isColorizer)
        if (!IXplatAbstractions.INSTANCE.isColorizer(handStack)) {
            throw MishapBadOffhandItem.of(
                handStack,
                hand,
                "colorizer"
            )
        }
        if (handStack.item.equals(HexItems.UUID_COLORIZER))
        {
            throw MishapBadOffhandItem.of(
                    handStack,
                    hand,
                    "colorizer.soul"
            )
        }
        return Triple(
            Spell(entity),
            ManaConstants.DUST_UNIT,
            listOf(ParticleSpray.burst(entity.position(), 0.5))
        )
    }

    private data class Spell(val input: ItemEntity) : RenderedSpell {
        override fun cast(ctx: CastingContext) {
            val handStack = ctx.getHeldItemToOperateOn(IXplatAbstractions.INSTANCE::isColorizer).first.copy()
            if (IXplatAbstractions.INSTANCE.isColorizer(handStack)) {
                if (ctx.withdrawItem(handStack.item, 1, true)) {
                    // Have to make a fresh ItemStack to update the ItemEntity's model.
                    val targetItemStack = input.item.copy()
                    if(targetItemStack.item is ColorizedItem) {
                        ((targetItemStack.item)as ColorizedItem).writeColor(targetItemStack, (handStack.item as ColorizerItem))
                        input.item = targetItemStack;
                    }
                }
            }
        }
    }
}
