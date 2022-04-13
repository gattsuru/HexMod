package at.petrak.hexcasting.common.mobeffect;

import at.petrak.hexcasting.api.utils.HexDamageSources;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class EffectNerveGas extends MobEffect {
    protected EffectNerveGas() {
        // category, color
        super(MobEffectCategory.HARMFUL, 0xff_c890f0);
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        int timer = 10 >> pAmplifier;
        return timer <= 0 || pDuration % timer == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity victim, int pAmplifier) {
        if (victim.walkDist >= 0.1) {
            victim.hurt(HexDamageSources.OVERCAST, pAmplifier + 1);
        }
    }
}
