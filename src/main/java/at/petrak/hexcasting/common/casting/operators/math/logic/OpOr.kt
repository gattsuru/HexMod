package at.petrak.hexcasting.common.casting.operators.math.logic

import at.petrak.hexcasting.api.ConstManaOperator
import at.petrak.hexcasting.api.SpellDatum
import at.petrak.hexcasting.common.casting.CastingContext
import at.petrak.hexcasting.common.casting.Widget

object OpOr : ConstManaOperator {
    override val argc = 2

    override fun execute(args: List<SpellDatum<*>>, ctx: CastingContext): List<SpellDatum<*>> {
        return listOf(
            if (args[0].payload == Widget.NULL)
                args[1]
            else
                args[0]
        )
    }
}