package mods.thecomputerizer.musictriggers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import mods.thecomputerizer.musictriggers.Constants;
import mods.thecomputerizer.musictriggers.MusicTriggers;
import mods.thecomputerizer.musictriggers.client.audio.Channel;
import mods.thecomputerizer.musictriggers.client.audio.ChannelManager;
import mods.thecomputerizer.musictriggers.client.data.Trigger;
import mods.thecomputerizer.musictriggers.client.gui.GuiSuperType;
import mods.thecomputerizer.musictriggers.client.gui.instance.Instance;
import mods.thecomputerizer.musictriggers.config.ConfigDebug;
import mods.thecomputerizer.musictriggers.mixin.BossBarHudAccessor;
import mods.thecomputerizer.musictriggers.mixin.InGameHudAccessor;
import mods.thecomputerizer.musictriggers.util.PacketHandler;
import mods.thecomputerizer.musictriggers.util.packets.PacketBossInfo;
import mods.thecomputerizer.theimpossiblelibrary.util.client.AssetUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import org.apache.logging.log4j.Level;

import java.util.*;

public class EventsClient {
    public static ResourceLocation IMAGE_CARD = null;
    public static boolean isWorldRendered;
    public static float fadeCount = 1000;
    public static Boolean activated = false;
    public static long timer=0;
    public static int GuiCounter = 0;
    public static int reloadCounter = 0;
    public static boolean ismoving;
    public static String lastAdvancement;
    public static boolean advancement;
    public static Player PVPTracker;
    public static boolean renderDebug = true;
    public static boolean zone = false;
    public static boolean firstPass = false;
    public static int x1 = 0;
    public static int y1 = 0;
    public static int z1 = 0;
    public static int x2 = 0;
    public static int y2 = 0;
    public static int z2 = 0;
    private static int bossBarCounter = 0;
    public static final HashMap<String, Boolean> commandMap = new HashMap<>();

    public static SoundInstance playSound(SoundInstance sound) {
        SimpleSoundInstance silenced = new SimpleSoundInstance(sound.getLocation(), SoundSource.MUSIC, Float.MIN_VALUE*1000, 1F,
                false, 0, SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true);
        for(String s : ConfigDebug.BLOCKED_MOD_MUSIC) {
            if(sound.getLocation().toString().contains(s) && sound.getSource()==SoundSource.MUSIC) {
                if(!ConfigDebug.PLAY_NORMAL_MUSIC || ChannelManager.overridingMusicIsPlaying()) return silenced;
            }
        }
        for(String s : ConfigDebug.BLOCKED_MOD_RECORDS) {
            if(sound.getLocation().toString().contains(s) && sound.getSource()==SoundSource.RECORDS) {
                if(!ConfigDebug.PLAY_NORMAL_MUSIC || ChannelManager.overridingMusicIsPlaying()) return silenced;
            }
        }
        return sound;
    }

    public static void onAdvancement(Advancement adv) {
        lastAdvancement = adv.getId().toString();
        advancement = true;
    }

    public static boolean commandHelper(Trigger trigger) {
        String id = trigger.getParameter("identifier");
        return commandMap.containsKey(id) && commandMap.get(id);
    }

    public static void commandFinish(Trigger trigger) {
        String id = trigger.getParameter("identifier");
        commandMap.put(id,false);
    }

    public static void onDisconnect() {
        isWorldRendered=false;
    }

    public static void initReload() {
        Component reload = AssetUtil.genericLang(Constants.MODID,"misc","reload_start").withStyle(ChatFormatting.RED)
                        .withStyle(ChatFormatting.ITALIC);
        if(Objects.nonNull(Minecraft.getInstance().player))
            Minecraft.getInstance().player.sendMessage(reload,Minecraft.getInstance().player.getUUID());
        reloadCounter = 5;
        ChannelManager.reloading = true;
        MusicTriggers.savedMessages.clear();
        MusicTriggers.logExternally(Level.INFO,"Reloading Music...");
    }


    public static void onKeyInput() {
        if(Minecraft.getInstance().player!=null) {
            //BlockPos pos = MusicPicker.roundedPos(Minecraft.getMinecraft().player);
            if(!zone) {
                Minecraft.getInstance().setScreen(Instance.createTestGui());
                //Minecraft.getMinecraft().player.sendMessage(new TextComponentString("\u00A74\u00A7o"+I18n.translateToLocal("misc.musictriggers.reload_start")));
                //reloadCounter = 5;
                //ChannelManager.reloading = true;
            }
            /*
            else if(!firstPass) {
                x1 = pos.getX();
                y1 = pos.getY();
                z1 = pos.getZ();
                firstPass = true;
                Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.MUSIC, 1f, 1f, pos));
            } else {
                x2 = pos.getX();
                y2 = pos.getY();
                z2 = pos.getZ();
                int temp;
                if(x1>x2) {
                    temp=x1;
                    x1=x2;
                    x2=temp;
                }
                if(y1>y2) {
                    temp=y1;
                    y1=y2;
                    y2=temp;
                }
                if(z1>z2) {
                    temp=z1;
                    z1=z2;
                    z2=temp;
                }
                firstPass = false;
                zone = false;
                Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(SoundEvents.BLOCK_ANVIL_BREAK, SoundCategory.MUSIC, 1f, 1f, pos));
                String compiledZoneCoords = x1+","+y1+","+z1+","+x2+","+y2+","+z2;
                parentScreen.holder.editTriggerInfoParameter(parentScreen.songCode, parentScreen.trigger, parentScreen.scrollingSongs.index, compiledZoneCoords);
                Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            }
             */
        }
    }

    public static void onTick() {
        if(!Minecraft.getInstance().isPaused() && !(Minecraft.getInstance().screen instanceof GuiSuperType) && !renderDebug) renderDebug = true;
        if(reloadCounter>0) {
            reloadCounter-=1;
            if(reloadCounter==1) {
                ChannelManager.reloadAllChannels();
                Component reload = AssetUtil.genericLang(Constants.MODID,"misc","reload_finished")
                        .withStyle(ChatFormatting.GREEN).withStyle(ChatFormatting.ITALIC);
                if(Objects.nonNull(Minecraft.getInstance().player))
                    Minecraft.getInstance().player.sendMessage(reload,Minecraft.getInstance().player.getUUID());
                IMAGE_CARD = null;
                fadeCount = 1000;
                timer = 0;
                activated = false;
                ismoving = false;
                ChannelManager.reloading = false;
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static void debugInfo(PoseStack matrix) {
        if(ConfigDebug.SHOW_DEBUG && renderDebug) {
            List<String> lines = new ArrayList<>();
            lines.add("Music Triggers Debug Information");
            for(Channel channel : ChannelManager.getAllChannels())
                if(channel.curPlayingName()!=null)
                    lines.add("Channel["+channel.getChannelName()+"] Current Song: "+channel.curPlayingName());
            if(!ConfigDebug.CURRENT_SONG_ONLY) {
                int displayCount = 0;
                for(Channel channel : ChannelManager.getAllChannels()) {
                    if(!channel.formatSongTime().matches("No song playing")) lines.add("Channel["+channel.getChannelName()+"] Current Song Time: " + channel.formatSongTime());
                    if(channel.formattedFadeOutTime()!=null) lines.add("Channel["+channel.getChannelName()+"] Fading Out: "+channel.formattedFadeOutTime());
                    if(channel.formattedFadeInTime()!=null) lines.add("Channel["+channel.getChannelName()+"] Fading In: "+channel.formattedFadeInTime());
                }
                for(Channel channel : ChannelManager.getAllChannels()) {
                    if (!channel.getPlayableTriggers().isEmpty()) {
                        StringBuilder s = new StringBuilder();
                        for (Trigger trigger : channel.getPlayableTriggers()) {
                            String name = trigger.getNameWithID();
                            if (Minecraft.getInstance().font.width(s + " " + name) > 0.75f *
                                    Minecraft.getInstance().getWindow().getGuiScaledWidth()) {
                                if (displayCount == 0) {
                                    lines.add("Channel["+channel.getChannelName()+"] Playable Events: " + s);
                                    displayCount++;
                                } else lines.add(s.toString());
                                s = new StringBuilder();
                            }
                            s.append(" ").append(name);
                        }
                        if (displayCount == 0) lines.add("Channel["+channel.getChannelName()+"] Playable Events: " + s);
                        else lines.add(s.toString());
                    }
                    displayCount = 0;
                }
                StringBuilder sm = new StringBuilder();
                sm.append("minecraft");
                for (String ev : ConfigDebug.BLOCKED_MOD_MUSIC) {
                    if(Minecraft.getInstance().font.width(sm+" "+ev)>0.75f*Minecraft.getInstance().getWindow().getGuiScaledWidth()) {
                        if(displayCount==0) {
                            lines.add("Blocked Mods: " + sm);
                            displayCount++;
                        } else lines.add(sm.toString());
                        sm = new StringBuilder();
                    }
                    sm.append(" ").append(ev);
                }
                if(displayCount==0) lines.add("Blocked Mods: " + sm);
                else lines.add(sm.toString());
                displayCount=0;
                Minecraft mc = Minecraft.getInstance();
                Player player = mc.player;
                net.minecraft.world.level.Level level = player.level;
                if(player!=null && level!=null) {
                    lines.add("Current Biome: " + level.getBiome(roundedPos(player)).unwrapKey().get().location());
                    lines.add("Current Dimension: " + level.dimension().location());
                    lines.add("Current Total Light: " + level.getRawBrightness(roundedPos(player), 0));
                    lines.add("Current Block Light: " + level.getBrightness(LightLayer.BLOCK, roundedPos(player)));
                    if (MusicPicker.effectList != null && !MusicPicker.effectList.isEmpty()) {
                        StringBuilder se = new StringBuilder();
                        for (String ev : MusicPicker.effectList) {
                            if(Minecraft.getInstance().font.width(se+" "+ev)>0.75f*Minecraft.getInstance().getWindow().getGuiScaledWidth()) {
                                if(displayCount==0) {
                                    lines.add("Effect List: " + se);
                                    displayCount++;
                                } else lines.add(se.toString());
                                se = new StringBuilder();
                            }
                            se.append(" ").append(ev);
                        }
                        if(displayCount==0) lines.add("Effect List: " + se);
                        else lines.add(se.toString());
                    }
                    if(Minecraft.getInstance().crosshairPickEntity != null) {
                        if (getLivingFromEntity(Minecraft.getInstance().crosshairPickEntity) != null)
                            lines.add("Current Entity Name: " + getLivingFromEntity(Minecraft.getInstance()
                                    .crosshairPickEntity).getName());
                    }
                }
            }
            int top = 2;
            for (String msg : lines) {
                GuiComponent.fill(matrix, 1, top - 1, 2 + Minecraft.getInstance().font.width(msg) + 1,
                        top + Minecraft.getInstance().font.lineHeight - 1, -1873784752);
                Minecraft.getInstance().font.draw(matrix, msg, 2, top, 14737632);
                top += Minecraft.getInstance().font.lineHeight;
            }
        }
    }

    private static BlockPos roundedPos(Player p) {
        return new BlockPos((Math.round(p.getX() * 2) / 2.0), (Math.round(p.getY() * 2) / 2.0), (Math.round(p.getZ() * 2) / 2.0));
    }


    public static void renderBoss() {
        if(Objects.nonNull(Minecraft.getInstance().player)) {
            if (bossBarCounter % 11 == 0) {
                Map<UUID, LerpingBossEvent> bossbars =
                        ((BossBarHudAccessor) ((InGameHudAccessor) Minecraft.getInstance().gui).getBossOverlay()).getEvents();
                for (UUID u : bossbars.keySet()) {
                    LerpingBossEvent bar = bossbars.get(u);
                    PacketHandler.sendToServer(new PacketBossInfo(bar.getName().getString(),bar.getProgress(),
                            Minecraft.getInstance().player.getStringUUID()));
                }
                bossBarCounter = 0;
            }
            bossBarCounter++;
        }
    }
    private static LivingEntity getLivingFromEntity(Entity e) {
        if (e instanceof LivingEntity) return (LivingEntity) e;
        return null;
    }
}
