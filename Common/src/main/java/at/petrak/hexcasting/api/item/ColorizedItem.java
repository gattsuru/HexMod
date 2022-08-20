package at.petrak.hexcasting.api.item;

import at.petrak.hexcasting.api.spell.SpellDatum;
import at.petrak.hexcasting.api.utils.NBTHelper;
import at.petrak.hexcasting.common.items.colorizer.ItemPrideColorizer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

public interface ColorizedItem
{
    ResourceLocation COLOR_PRED = modLoc("color");

    String TAG_COLOR_FLOAT = "color";

    default float readColorFloat(ItemStack stack) {
        return NBTHelper.getFloat(stack, TAG_COLOR_FLOAT);
    }

    default void writeColor(ItemStack stack, ColorizerItem input)
    {
        NBTHelper.putFloat(stack, TAG_COLOR_FLOAT, getColorList().indexOf(input.colorName()));
    }

    // Returns an ArrayList containing the supported colorizer variants, in the specific order used for the Item predicates.
    // Stringly typed.
    static List<String> getColorList()
    {
        // Making a merged list of all color variants.
        // The numbering system output here is deterministic and used for storage
        // So if adding to this system, it's weakly important to maintain existing sequence order.
        var colorVariants = new ArrayList<String>();
        colorVariants.add("default"); // default case, aka no pigment applied.
        for(DyeColor dye : DyeColor.values())
        {
            colorVariants.add(dye.getName());
        }
        for(ItemPrideColorizer.Type prideDye: ItemPrideColorizer.Type.values())
        {
            colorVariants.add(prideDye.getName());
        }
        // Currently, rejecting Soul Glimmer pigments for Externalize Pigment.
        // If a way to implement lighting effects or arbitrary color offsets to a texture exist,
        // may make sense to allow it.
        // colorVariants.add("uuid"); // ItemUUIDColorizer, aka Soul Glimmer.
        return colorVariants;
    }
}
