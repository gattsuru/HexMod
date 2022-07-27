package at.petrak.hexcasting.api.spell.casting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * A system for cases where HexCasting should treat the 'true' caster of a spell as someone or something other than
 * the actual player.  Initial use case to support Requiem, where the ServerPlayer is riding a Mob that should get both effects
 * and some costs, but it is plausible may want to have things like redirecting effects or costs to a Poppet or surrounding mobs.
 */
public class CasterModifiers {
    private static final List<UnaryOperator<LivingEntity>> HAS_CASTER_HP_COST_MODIFIERS = new ArrayList<>();
    private static final List<UnaryOperator<LivingEntity>> HAS_CASTER_EFFECT_TARGET_MODIFIERS = new ArrayList<>();

    public static void addCasterCostingModifiers(UnaryOperator<LivingEntity> modifier)
    {
        HAS_CASTER_HP_COST_MODIFIERS.add(modifier);
    }

    public static void clearCasterCostingModifiers()
    {
        HAS_CASTER_HP_COST_MODIFIERS.clear();
    }

    public static void addCasterEffectTargetModifiers(UnaryOperator<LivingEntity> modifier)
    {
        HAS_CASTER_EFFECT_TARGET_MODIFIERS.add(modifier);
    }

    /**
     * Applies any Caster HP cost modifiers, and returns the final LivingEntity to apply changes to.
     * There's no guarantee for ordering (yet); the UnaryOperators here should assume that another mod may have already
     * successfully changed the final target.
     * @param player  The player to check for modifications.
     * @return        The LivingEntity to act as a caster for HP costs, or the original player.
     */
    public static LivingEntity applyCasterHPCostModifiers(final ServerPlayer player) {
        LivingEntity resultCaster = player;
        for(final var modifier : HAS_CASTER_HP_COST_MODIFIERS)
        {
            final var outputCaster = modifier.apply(resultCaster);
            if(outputCaster != null)
            {
                resultCaster = outputCaster;
            }
        }
        return resultCaster;
    }

    /**
     * Applies any Caster Effect Target modifiers, and returns the final LivingEntity to apply changes to.
     * There's no guarantee for ordering (yet); the UnaryOperators here should assume that another mod may have already
     * successfully changed the final target.
     * @param player  The player to check for modifications.
     * @return        The LivingEntity to act as a caster for given purposes.
     */
    public static LivingEntity applyCasterEffectTargetModifiers(final ServerPlayer player) {
        LivingEntity resultCaster = player;
        for(final var modifier : HAS_CASTER_EFFECT_TARGET_MODIFIERS)
        {
            final var outputCaster = modifier.apply(resultCaster);
            if(outputCaster != null)
            {
                resultCaster = outputCaster;
            }
        }
        return resultCaster;
    }
}
