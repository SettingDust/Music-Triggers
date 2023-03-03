package mods.thecomputerizer.musictriggers.client;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import mods.thecomputerizer.musictriggers.Constants;
import mods.thecomputerizer.musictriggers.MusicTriggers;
import mods.thecomputerizer.musictriggers.client.audio.Channel;
import mods.thecomputerizer.musictriggers.client.audio.ChannelManager;
import mods.thecomputerizer.musictriggers.client.data.Trigger;
import mods.thecomputerizer.musictriggers.client.gui.GuiSuperType;
import mods.thecomputerizer.musictriggers.client.gui.instance.Instance;
import mods.thecomputerizer.musictriggers.common.TriggerCommand;
import mods.thecomputerizer.musictriggers.config.ConfigDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Level;

import javax.annotation.Nullable;
import java.util.HashMap;

@SuppressWarnings("deprecation")
@Mod.EventBusSubscriber(modid = Constants.MODID, value = Side.CLIENT)
public class EventsClient {
    public static ResourceLocation IMAGE_CARD = null;
    public static boolean isWorldRendered;
    public static float fadeCount = 1000;
    public static Boolean activated = false;
    public static long timer=0;
    public static int reloadCounter = 0;
    public static boolean ismoving;
    public static String lastAdvancement;
    public static boolean advancement;
    public static boolean renderDebug = true;
    public static final HashMap<String, Boolean> commandMap = new HashMap<>();

    @SubscribeEvent
    public static void playSound(PlaySoundEvent e) {
        PositionedSoundRecord silenced = new PositionedSoundRecord(e.getSound().getSoundLocation(), SoundCategory.MUSIC,
                Float.MIN_VALUE*1000, 1F, false, 1, ISound.AttenuationType.LINEAR, 0F, 0F, 0F);
        for(String s : ConfigDebug.BLOCKED_MOD_MUSIC) {
            if(e.getSound().getSoundLocation().toString().contains(s) && e.getSound().getCategory()==SoundCategory.MUSIC) {
                if(!(!ChannelManager.canAnyChannelOverrideMusic() && ConfigDebug.PLAY_NORMAL_MUSIC)) e.setResultSound(silenced);
            }
        }
        silenced = new PositionedSoundRecord(e.getSound().getSoundLocation(), SoundCategory.RECORDS,
                Float.MIN_VALUE*1000, 1F, false, 1, ISound.AttenuationType.LINEAR, 0F, 0F, 0F);
        for(String s : ConfigDebug.BLOCKED_MOD_RECORDS) {
            if(e.getSound().getSoundLocation().toString().contains(s) && e.getSound().getCategory()==SoundCategory.RECORDS) {
                if(!(!ChannelManager.canAnyChannelOverrideMusic() && ConfigDebug.PLAY_NORMAL_MUSIC)) e.setResultSound(silenced);
            }
        }
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent e) {
        lastAdvancement = e.getAdvancement().getId().toString();
        advancement = true;
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent e) {
        if(e.getCommand() instanceof TriggerCommand) {
            TriggerCommand command = (TriggerCommand) e.getCommand();
            if(!command.getIdentifier().matches("any")) commandMap.put(command.getIdentifier(),true);
        }
    }

    public static boolean commandHelper(Trigger trigger) {
        String id = trigger.getParameter("identifier");
        return commandMap.containsKey(id) && commandMap.get(id);
    }

    public static void commandFinish(Trigger trigger) {
        String id = trigger.getParameter("identifier");
        commandMap.put(id,false);
    }

    @SubscribeEvent
    public static void clientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        isWorldRendered=false;
    }

    @SubscribeEvent
    public static void cancelRenders(RenderGameOverlayEvent.Pre e) {
        if(!isWorldRendered) {
            ChannelManager.initializeServerInfo();
            isWorldRendered = true;
        }
        if(e.getType()==RenderGameOverlayEvent.ElementType.ALL && !renderDebug) e.setCanceled(true);
    }

    public static void initReload() {
        Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
                I18n.translateToLocal("misc.musictriggers.reload_start"))
                .setStyle(new Style().setItalic(true).setColor(TextFormatting.RED)));
        reloadCounter = 5;
        ChannelManager.reloading = true;
        MusicTriggers.savedMessages.clear();
        MusicTriggers.logExternally(Level.INFO,"Reloading Music...");
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent e) {
        if(Channel.GUI.isKeyDown()) Minecraft.getMinecraft().displayGuiScreen(Instance.createGui());
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if(event.phase== TickEvent.Phase.END) {
            if (!Minecraft.getMinecraft().isGamePaused() && !(Minecraft.getMinecraft().currentScreen instanceof GuiSuperType) && !renderDebug)
                renderDebug = true;
            if (reloadCounter > 0) {
                reloadCounter -= 1;
                if (reloadCounter == 1) {
                    ChannelManager.reloadAllChannels();
                    Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
                            I18n.translateToLocal("misc.musictriggers.reload_finished"))
                            .setStyle(new Style().setItalic(true).setColor(TextFormatting.GREEN)));
                    IMAGE_CARD = null;
                    fadeCount = 1000;
                    timer = 0;
                    activated = false;
                    ismoving = false;
                    ChannelManager.reloading = false;
                }
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    @SubscribeEvent
    public static void debugInfo(RenderGameOverlayEvent.Text e) {
        if(ConfigDebug.SHOW_DEBUG && isWorldRendered && renderDebug) {
            e.getLeft().add("Music Triggers Debug Information");
            for(Channel channel : ChannelManager.getAllChannels()) {
                if (channel.curPlayingName() != null)
                    e.getLeft().add("Channel[" + channel.getChannelName() + "] Current Song: " + channel.curPlayingName());
                if (!ConfigDebug.CURRENT_SONG_ONLY) {
                    int displayCount = 0;
                    if (!channel.formatSongTime().matches("No song playing"))
                        e.getLeft().add("Channel[" + channel.getChannelName() + "] Current Song Time: " + channel.formatSongTime());
                    if (channel.formattedFadeOutTime() != null)
                        e.getLeft().add("Channel[" + channel.getChannelName() + "] Fading Out: " + channel.formattedFadeOutTime());
                    if (channel.formattedFadeInTime() != null)
                        e.getLeft().add("Channel[" + channel.getChannelName() + "] Fading In: " + channel.formattedFadeInTime());
                    synchronized(channel.getPlayableTriggers()) {
                        if (!channel.getPlayableTriggers().isEmpty()) {
                            StringBuilder s = new StringBuilder();
                            for (Trigger trigger : channel.getPlayableTriggers()) {
                                String name = trigger.getNameWithID();
                                if (Minecraft.getMinecraft().fontRenderer.getStringWidth(s + " " + name) > 0.75f * (new ScaledResolution(Minecraft.getMinecraft())).getScaledWidth()) {
                                    if (displayCount == 0) {
                                        e.getLeft().add("Channel[" + channel.getChannelName() + "] Playable Events: " + s);
                                        displayCount++;
                                    } else e.getLeft().add(s.toString());
                                    s = new StringBuilder();
                                }
                                s.append(" ").append(name);
                            }
                            if (displayCount == 0)
                                e.getLeft().add("Channel[" + channel.getChannelName() + "] Playable Events: " + s);
                            else e.getLeft().add(s.toString());
                        }
                    }
                }
            }
            if (!ConfigDebug.CURRENT_SONG_ONLY) {
                int displayCount = 0;
                StringBuilder sm = new StringBuilder();
                sm.append("minecraft");
                for (String ev : ConfigDebug.BLOCKED_MOD_MUSIC) {
                    if(Minecraft.getMinecraft().fontRenderer.getStringWidth(sm+" "+ev)>0.75f*(new ScaledResolution(Minecraft.getMinecraft())).getScaledWidth()) {
                        if(displayCount==0) {
                            e.getLeft().add("Blocked Mods: " + sm);
                            displayCount++;
                        } else e.getLeft().add(sm.toString());
                        sm = new StringBuilder();
                    }
                    sm.append(" ").append(ev);
                }
                if(displayCount==0) e.getLeft().add("Blocked Mods: " + sm);
                else e.getLeft().add(sm.toString());
                displayCount=0;
                Minecraft mc = Minecraft.getMinecraft();
                EntityPlayer player = mc.player;
                World world = player.getEntityWorld();
                if(player!=null && world!=null) {
                    e.getLeft().add("Current Biome: " + world.getBiome(player.getPosition()).getRegistryName());
                    e.getLeft().add("Current Dimension: " + player.dimension);
                    e.getLeft().add("Current Total Light: " + world.getLight(roundedPos(player), true));
                    e.getLeft().add("Current Block Light: " + world.getLightFor(EnumSkyBlock.BLOCK, roundedPos(player)));
                    if (MusicPicker.effectList != null && !MusicPicker.effectList.isEmpty()) {
                        StringBuilder se = new StringBuilder();
                        for (String ev : MusicPicker.effectList) {
                            if(Minecraft.getMinecraft().fontRenderer.getStringWidth(se+" "+ev)>0.75f*(new ScaledResolution(Minecraft.getMinecraft())).getScaledWidth()) {
                                if(displayCount==0) {
                                    e.getLeft().add("Effect List: " + se);
                                    displayCount++;
                                } else e.getLeft().add(se.toString());
                                se = new StringBuilder();
                            }
                            se.append(" ").append(ev);
                        }
                        if(displayCount==0) e.getLeft().add("Effect List: " + se);
                        else e.getLeft().add(se.toString());
                    }
                    if(Minecraft.getMinecraft().objectMouseOver != null) {
                        if (getLivingFromEntity(Minecraft.getMinecraft().objectMouseOver.entityHit) != null)
                            e.getLeft().add("Current Entity Name: " + getLivingFromEntity(Minecraft.getMinecraft().objectMouseOver.entityHit).getName());
                        try {
                            if (infernalChecker(getLivingFromEntity(Minecraft.getMinecraft().objectMouseOver.entityHit)) != null)
                                e.getLeft().add("Infernal Mob Mod Name: " + infernalChecker(getLivingFromEntity(Minecraft.getMinecraft().objectMouseOver.entityHit)));
                        } catch (NoSuchMethodError ignored) { }
                    }
                }
            }
        }
    }

    private static BlockPos roundedPos(EntityPlayer p) {
        return new BlockPos((Math.round(p.posX * 2) / 2.0), (Math.round(p.posY * 2) / 2.0), (Math.round(p.posZ * 2) / 2.0));
    }

    private static String infernalChecker(@Nullable EntityLiving m) {
        if(Loader.isModLoaded("infernalmobs") && m!=null) return InfernalMobsCore.getMobModifiers(m) == null ? null : InfernalMobsCore.getMobModifiers(m).getModName();
        return null;
    }

    private static EntityLiving getLivingFromEntity(Entity e) {
        if(e instanceof EntityLiving) return (EntityLiving) e;
        return null;
    }
}
