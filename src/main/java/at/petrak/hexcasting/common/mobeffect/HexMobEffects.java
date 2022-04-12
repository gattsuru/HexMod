package at.petrak.hexcasting.common.mobeffect;

import at.petrak.hexcasting.HexMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class HexMobEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,
        HexMod.MOD_ID);

    public static final RegistryObject<EffectNerveGas> NERVE_GAS = EFFECTS.register("nerve_gas", EffectNerveGas::new);
}
