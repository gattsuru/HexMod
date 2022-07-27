package at.petrak.hexcasting.fabric.interop.requiem;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.spell.casting.CasterModifiers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RequiemApiInterop {
    public static void init()
    {
        CasterModifiers.addCasterCostingModifiers(RequiemApiInterop::getRequiemHost);
        //CasterModifiers.addCasterEffectTargetModifiers(RequiemApiInterop::getRequiemHost);
    }

    // LadySnake's RequiemAPI has a very nice accessor that would handle a lot of this for us,
    // Except the maven is inaccessible, and configuring localMaven is a pain.
    // Replace with the below Reflection access with this system if/when it ever gets fixed.
    //public static LivingEntity getRequiemHost(LivingEntity player)
    //{
    //    LivingEntity mob = PossessionComponent.getHost(player);
    //    if(mob != null)
    //    {
    //        return mob;
    //    }
    //    else{
    //        return player;
    //    }
    //}

    private static final String requiemPossessionComponentName = "ladysnake.requiem.api.v1.possession.PossessionComponent";
    private static final String requiemPossessionGetHostMethod = "getHost";
    // This approach is hacky and relatively fragile to changes in requiem-api, though it should just stop functioning rather than break catastrophically on upstream changes.
    // But since the alternative for now involves mavenLocal(), it's the least annoying choice.
    // Thankfully, modded code access doesn't require the various hoop-jumping of reflection applied to Mojang code.
    public static LivingEntity getRequiemHost(LivingEntity player)
    {
        Class<?>[] parameterTypes = { Entity.class };
        try
        {
            Class<?> clazz = Class.forName(requiemPossessionComponentName);
            Method getHostMethod = clazz.getMethod(requiemPossessionGetHostMethod, parameterTypes);
            Object resultHost = getHostMethod.invoke(null, player);
            if(resultHost instanceof LivingEntity)
            {
                return (LivingEntity) resultHost;
            }
            return player;
        }
        catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException requiemApiNotFound)
        {
            HexAPI.LOGGER.warn(String.format("HexCasting's Requiem support tried to access %s, but was not found, with error %s",
              requiemPossessionComponentName, requiemApiNotFound));
        }
        catch (IllegalAccessException requiemApiNotAccessible)
        {
            HexAPI.LOGGER.warn(String.format("HexCasting's Requiem support tried to access %s, but was not allowed, with error %s",
              requiemPossessionComponentName, requiemApiNotAccessible));
        }
        // If we've reached this point, something has gone wrong with reflection.  Remove the listener rather than risk spamming logs or CPU.
        CasterModifiers.clearCasterCostingModifiers();
        return player;
    }
}
