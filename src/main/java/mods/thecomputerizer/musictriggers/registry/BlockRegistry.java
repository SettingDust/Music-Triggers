package mods.thecomputerizer.musictriggers.registry;

import mods.thecomputerizer.musictriggers.Constants;
import mods.thecomputerizer.musictriggers.registry.blocks.MusicRecorder;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class BlockRegistry {

    public static final Block MUSIC_RECORDER = makeBlock("music_recorder", MusicRecorder::new,
            block -> block.setCreativeTab(CreativeTabs.MISC));

    @SuppressWarnings("SameParameterValue")
    private static Block makeBlock(final String name, final Supplier<Block> constructor, final Consumer<Block> config) {
        final Block block = constructor.get();
        config.accept(block);
        block.setRegistryName(Constants.MODID, name);
        block.setTranslationKey(Constants.MODID+"."+name);
        return block;
    }
}
