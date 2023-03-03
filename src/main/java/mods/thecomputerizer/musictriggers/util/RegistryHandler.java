package mods.thecomputerizer.musictriggers.util;

import mods.thecomputerizer.musictriggers.Constants;
import mods.thecomputerizer.musictriggers.common.MusicTriggersBlocks;
import mods.thecomputerizer.musictriggers.common.objects.MusicRecorderEntity;
import mods.thecomputerizer.musictriggers.config.ConfigRegistry;
import mods.thecomputerizer.musictriggers.util.packets.PacketDynamicChannelInfo;
import mods.thecomputerizer.musictriggers.util.packets.PacketInitChannels;
import mods.thecomputerizer.musictriggers.util.packets.PacketJukeBoxCustom;
import mods.thecomputerizer.musictriggers.util.packets.PacketSyncServerInfo;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Objects;

import static mods.thecomputerizer.musictriggers.common.MusicTriggersItems.*;


@GameRegistry.ObjectHolder(Constants.MODID)
@Mod.EventBusSubscriber(modid = Constants.MODID)
public final class RegistryHandler {
    public static SimpleNetworkWrapper network;

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> e) {
        if(ConfigRegistry.REGISTER_DISCS) {
            e.getRegistry().register(BLANK_RECORD);
            e.getRegistry().register(MUSIC_TRIGGERS_RECORD);
            e.getRegistry().register(CUSTOM_RECORD);
            e.getRegistry().register(MUSIC_RECORDER);
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> e) {
        if(ConfigRegistry.REGISTER_DISCS) {
            e.getRegistry().register(MusicTriggersBlocks.MUSIC_RECORDER);
            GameRegistry.registerTileEntity(MusicRecorderEntity.class,new ResourceLocation(Constants.MODID,"tile.music_recorder"));
        }
    }


    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onModelRegister(ModelRegistryEvent event) {
        if(ConfigRegistry.REGISTER_DISCS) {
            ModelLoader.setCustomModelResourceLocation(MUSIC_TRIGGERS_RECORD, 0,
                    new ModelResourceLocation(Objects.requireNonNull(MUSIC_TRIGGERS_RECORD.getRegistryName()), "inventory"));
            ModelLoader.setCustomModelResourceLocation(CUSTOM_RECORD, 0,
                    new ModelResourceLocation(Objects.requireNonNull(CUSTOM_RECORD.getRegistryName()), "inventory"));
            ModelLoader.setCustomModelResourceLocation(BLANK_RECORD, 0,
                    new ModelResourceLocation(Objects.requireNonNull(BLANK_RECORD.getRegistryName()), "inventory"));
            ModelLoader.setCustomModelResourceLocation(MUSIC_RECORDER, 0,
                    new ModelResourceLocation(Objects.requireNonNull(MUSIC_RECORDER.getRegistryName()), "normal"));
        }
    }

    public static void init() {
        network = NetworkRegistry.INSTANCE.newSimpleChannel(Constants.MODID);
        registerPackets();
    }

    private static void registerPackets() {
        int id = 0;
        network.registerMessage(PacketInitChannels.class, PacketInitChannels.Message.class, id++, Side.SERVER);
        network.registerMessage(PacketDynamicChannelInfo.class, PacketDynamicChannelInfo.Message.class, id++, Side.SERVER);
        network.registerMessage(PacketSyncServerInfo.class, PacketSyncServerInfo.Message.class, id++, Side.CLIENT);
        network.registerMessage(PacketJukeBoxCustom.class, PacketJukeBoxCustom.Message.class, id, Side.CLIENT);
    }
}
