package at.petrak.hexcasting.fabric.interop.gravity

import at.petrak.hexcasting.api.misc.GravitySetting
import at.petrak.hexcasting.api.misc.ManaConstants
import at.petrak.hexcasting.api.spell.*
import at.petrak.hexcasting.api.spell.casting.CastingContext
import at.petrak.hexcasting.api.spell.mishaps.MishapNotEnoughArgs
import at.petrak.hexcasting.xplat.IXplatAbstractions
import me.andrew.gravitychanger.api.GravityChangerAPI
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import kotlin.math.roundToInt

object OpChangeGravity : SpellOperator {
    override val argc = 2
    val maxArgc = 3

    override fun consumeFromStack(stack: MutableList<SpellDatum<*>>, ctx: CastingContext): List<SpellDatum<*>>
    {
        if (this.argc > stack.size)
            throw MishapNotEnoughArgs(this.argc, stack.size)
        val twoArgs = stack.takeLast(argc)
        if(twoArgs[0].payload == ctx.caster)
        {
            for (_i in 0 until argc) stack.removeLast()
            return twoArgs
        }
        else
        {
            val threeArgs = stack.takeLast(maxArgc)
            for (_i in 0 until maxArgc) stack.removeLast()
            return threeArgs
        }
    }

    override fun execute(args: List<SpellDatum<*>>, ctx: CastingContext):
            Triple<RenderedSpell, Int, List<ParticleSpray>> {
        val target = args.getChecked<Entity>(0)
        val vec = args.getChecked<Vec3>(1)
        val snapped = Direction.getNearest(vec.x, vec.y, vec.z)
        val time : Double
        val infiniteDuration: Boolean
        val manaCost: Int

        // If casting on self, use the permanent two-arg version
        // We've already checked once during the arg consume phase, but this is cleaner than trusting counts.
        if (target == ctx.caster)
        {
            infiniteDuration = true
            time = -1.0
            manaCost = ManaConstants.CRYSTAL_UNIT * 10
        }
        else // if casting on anything else, use the three-arg version.
        // Shouldn't be possible to get less than two-args here, but I'm paranoid.
        {
            val timeRaw = if(args.count() >= 3) {
                args.getChecked<Double>(2)
            } else {
                throw MishapNotEnoughArgs(this.argc + 1, 2)
            }
            //We'll 'charge' for duration even if Direction.DOWN, but treat it as an infinite duration anyway; as if it 'finished' to normal gravity.
            infiniteDuration = snapped == Direction.DOWN
            manaCost = ManaConstants.DUST_UNIT * (0.20 * (timeRaw + 1.0)).roundToInt()
            time = timeRaw * 20.0;
        }

        return Triple(
            Spell(target, snapped, infiniteDuration, time.roundToInt()),
            manaCost,
            listOf(ParticleSpray(target.position(), Vec3.atLowerCornerOf(snapped.normal), 0.1, 0.1))
        )
    }

    private data class Spell(val target: Entity, val dir: Direction, val permanent: Boolean, val time: Int) : RenderedSpell {
        override fun cast(ctx: CastingContext) {
            GravityChangerAPI.setDefaultGravityDirection(target, dir)
            IXplatAbstractions.INSTANCE.setGravitySetting(
                    target,
            GravitySetting(
                    dir,
                    permanent,
                    time
            ))
        }
    }

    private fun tickDownGravity(entity: Entity)
    {
        val gravity = IXplatAbstractions.INSTANCE.getGravitySetting(entity);
        if (gravity.gravityDirection != null && gravity.gravityDirection != Direction.DOWN)
        {
            if(!gravity.permanent) {
                val gravityTime = gravity.timeLeft - 1
                if (gravityTime < 0) {
                    IXplatAbstractions.INSTANCE.setGravitySetting(entity, GravitySetting.deny())
                } else {
                    IXplatAbstractions.INSTANCE.setGravitySetting(
                            entity,
                            GravitySetting(
                                    gravity.gravityDirection,
                                    gravity.permanent,
                                    gravityTime
                            )
                    )
                }
            }
            if(entity.y > 10000)
            {
                if(entity is ServerPlayer) {
                    entity.hurt(DamageSource.OUT_OF_WORLD, 5f)
                }
                else
                {
                    entity.remove(Entity.RemovalReason.DISCARDED);
                }
            }
        }
    }

    fun fabricTickDownAllGravityChanges(world: ServerLevel)
    {
        for (entity in world.allEntities) {
            tickDownGravity(entity);
        }
    }
}