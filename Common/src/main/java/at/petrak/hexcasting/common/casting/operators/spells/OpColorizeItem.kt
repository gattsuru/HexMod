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
            listOf()
        )
    }

    private data class Spell(val input: ItemEntity) : RenderedSpell {
        override fun cast(ctx: CastingContext) {
            val handStack = ctx.getHeldItemToOperateOn(IXplatAbstractions.INSTANCE::isColorizer).first.copy()
            if (IXplatAbstractions.INSTANCE.isColorizer(handStack)) {
                if (ctx.withdrawItem(handStack.item, 1, true)) {
                    if(input.item.item is ColorizedItem) {
                        (input.item.item as ColorizedItem).writeColor(input.item, (handStack.item as ColorizerItem))

                        //  Have to make a fresh ItemEntity to update the model.
                        //  Give it a little 'oomph' so it feels reactive.
                        val lookAngle = ctx.caster.lookAngle.scale(0.5)
                        ctx.world.addWithUUID(ItemEntity(ctx.world,
                            input.x,
                            input.y,
                            input.z,
                            input.item,
                            lookAngle.x + (Math.random() - 0.5) * 0.04,
                            abs(lookAngle.y + (Math.random() - 0.5) * 0.04), // always want to shove it upwards.
                            lookAngle.x + (Math.random() - 0.5) * 0.04))

                        input.kill()
                    }
                }
            }
        }
    }
}
