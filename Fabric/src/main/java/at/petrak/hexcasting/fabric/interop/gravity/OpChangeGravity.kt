package at.petrak.hexcasting.fabric.interop.gravity

import at.petrak.hexcasting.api.misc.GravitySetting
import at.petrak.hexcasting.api.misc.ManaConstants
import at.petrak.hexcasting.api.spell.*
import at.petrak.hexcasting.api.spell.casting.CastingContext
import at.petrak.hexcasting.api.spell.mishaps.MishapImmuneEntity
import at.petrak.hexcasting.api.spell.mishaps.MishapInvalidSpellDatumType
import at.petrak.hexcasting.api.spell.mishaps.MishapNotEnoughArgs
import at.petrak.hexcasting.xplat.IXplatAbstractions
import me.andrew.gravitychanger.api.GravityChangerAPI
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

object OpChangeGravity : SpellOperator {
    override val argc = 2
    private const val maxArgc = 3
    private const val maxPlayerGravityTravelXZDistance = 16 * 25
    private const val maxEntityGravityCeilingDistance = 2000

    class EntitiesWithGravitas {
        companion object
        {
            val ActiveGravityTimers = mutableListOf<Entity>()
            val TimersToRemove = mutableListOf<Entity>()
        }
    }

    override fun consumeFromStack(stack: MutableList<SpellDatum<*>>, ctx: CastingContext): List<SpellDatum<*>>
    {
        if(stack.size >= maxArgc)
        {
            val threeArgs = stack.takeLast(maxArgc)
            for (_i in 0 until maxArgc) stack.removeLast()
            return threeArgs
        }
        else if (stack.size == this.argc) {
            val twoArgs = stack.takeLast(argc)
            if (twoArgs[0].payload == ctx.caster || twoArgs[0].payload !is ServerPlayer) {
                for (_i in 0 until argc) stack.removeLast()
                return twoArgs
            }
            else
            {
                if(twoArgs[0].payload is Entity) {
                    throw MishapImmuneEntity(twoArgs[0].payload as Entity)
                }
                else
                {
                    throw MishapInvalidSpellDatumType(twoArgs[0].payload)
                }
            }
        }
        else
        {
            throw MishapNotEnoughArgs(this.argc, stack.size)
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

        if(args.count() >= 3) {
            val timeRaw = args.getChecked<Double>(2)
            //We'll 'charge' for duration even if Direction.DOWN, but treat it as an infinite duration anyway; as if it 'finished' to normal gravity.
            infiniteDuration = snapped == Direction.DOWN
            manaCost = ManaConstants.DUST_UNIT * (0.20 * (max(timeRaw, 1.0))).roundToInt()
            time = timeRaw * 20.0
        }
        else if((target is ServerPlayer && target != ctx.caster))
        {
            // ServerPlayer extends target, so no need for a separate 'is' check here.
            // Possibly make a configurable setting to allow infinite duration gravity changes to other players, disallow against monsters?
            throw MishapImmuneEntity(target)
        }
        else
        {
            infiniteDuration = true
            time = -1.0
            manaCost = ManaConstants.CRYSTAL_UNIT * 10
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
                    time,
                    Vec3(target.x, target.y, target.z)
            ))
            if(!permanent || target is ServerPlayer)
            {
                EntitiesWithGravitas.ActiveGravityTimers.add(target)
            }
        }
    }

    private fun tickDownGravity(entity: Entity) {
        val gravity = IXplatAbstractions.INSTANCE.getGravitySetting(entity)
        if (gravity != null && gravity.gravityDirection != null) {
            if (gravity.gravityDirection == Direction.DOWN || !entity.isAlive)
            {
                GravityChangerAPI.setDefaultGravityDirection(entity, Direction.DOWN)
                EntitiesWithGravitas.TimersToRemove.add(entity)
                return
            }
            else if (!gravity.permanent) {
                val gravityTime = gravity.timeLeft - 1
                if (gravityTime < 0) {
                    GravityChangerAPI.setDefaultGravityDirection(entity, Direction.DOWN)
                    IXplatAbstractions.INSTANCE.setGravitySetting(entity, GravitySetting.deny())
                } else {
                    IXplatAbstractions.INSTANCE.setGravitySetting(
                        entity,
                        GravitySetting(
                            gravity.gravityDirection,
                            gravity.permanent,
                            gravityTime,
                            gravity.origin
                        )
                    )
                }
            }
            if(entity is ServerPlayer)
            {
                if(entity.y > maxEntityGravityCeilingDistance || abs(entity.x - gravity.origin.x) > maxPlayerGravityTravelXZDistance || abs(entity.z - gravity.origin.z) > maxPlayerGravityTravelXZDistance)
                {
                    GravityChangerAPI.setDefaultGravityDirection(entity, Direction.DOWN)
                    IXplatAbstractions.INSTANCE.setGravitySetting(entity, GravitySetting.deny())
                }
            }
            else if(entity.y > maxEntityGravityCeilingDistance)
            {
                entity.remove(Entity.RemovalReason.DISCARDED)
            }
        }
    }

    fun fabricTickDownAllGravityChanges(world: ServerLevel)
    {
        for (entity in EntitiesWithGravitas.ActiveGravityTimers) {
            tickDownGravity(entity)
        }
        // Have to do this as a separate step, as it's unsafe to modify a list within its iterator.
        EntitiesWithGravitas.ActiveGravityTimers.removeAll(EntitiesWithGravitas.TimersToRemove)
        EntitiesWithGravitas.TimersToRemove.clear()
    }

    fun fabricRespawnNormalGrav(world: ClientLevel)
    {
        // Only player respawn event is in Architect, which we don't (yet) depend on.
        // Thus, this mess.  Thankfully, cpu impact should be minimal, even running 20hz.
        for (player in world.players()) {
            // This method can only ever fire on the client side; the code below would never run but probably wouldn't be optimized out.
            // Included for clarity.
            //if (player !is LocalPlayer)
            //    continue
            // And, yes, this is the best test available short of adding network communications;
            // neither GravityAPI nor CardinalComponents syncs their data.
            if(player.isDeadOrDying){
                GravityChangerAPI.setDefaultGravityDirection(player, Direction.DOWN)
            }
        }
    }
}