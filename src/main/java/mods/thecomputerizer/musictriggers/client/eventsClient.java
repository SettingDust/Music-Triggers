package mods.thecomputerizer.musictriggers.client;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import mods.thecomputerizer.musictriggers.MusicTriggers;
import mods.thecomputerizer.musictriggers.config.configDebug;
import mods.thecomputerizer.musictriggers.config.configTitleCards;
import mods.thecomputerizer.musictriggers.util.CustomTick;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiWinGame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import javax.vecmath.Vector4f;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = MusicTriggers.MODID, value = Side.CLIENT)
public class eventsClient {
    public static ResourceLocation IMAGE_CARD = null;
    public static int curImageIndex;
    public static boolean isWorldRendered;
    public static float fadeCount = 1000;
    public static float startDelayCount = 0;
    public static Boolean activated = false;
    public static long timer=0;
    public static String GUIName;
    public static int GuiCounter = 0;
    private static int reloadCounter = 0;
    public static boolean ismoving;
    public static List<ResourceLocation> pngs = new ArrayList<>();
    public static int movingcounter = 0;
    public static String lastAdvancement;
    public static boolean advancement;
    public static EntityPlayer PVPTracker;

    @SubscribeEvent
    public static void playSound(PlaySoundEvent e) {

        PositionedSoundRecord silenced = new PositionedSoundRecord(e.getSound().getSoundLocation(), SoundCategory.MUSIC, Float.MIN_VALUE, 1F, false, 1, ISound.AttenuationType.LINEAR, 0F, 0F, 0F);
        if(e.getSound().getSoundLocation().getResourceDomain().matches(MusicTriggers.MODID) && ((e.getManager().isSoundPlaying(MusicPlayer.curMusic) && MusicPlayer.fromRecord==null) || MusicPlayer.playing)) {
            e.setResultSound(silenced);
        }
        for(String s : configDebug.blockedmods) {
            if(e.getSound().getSoundLocation().toString().contains(s) && e.getSound().getCategory()==SoundCategory.MUSIC) {
                if(!(MusicPlayer.curMusic==null && configDebug.SilenceIsBad)) {
                    e.setResultSound(silenced);
                }
            }
        }
        if(e.getSound().getSoundLocation().toString().contains("minecraft") && e.getSound().getCategory()==SoundCategory.MUSIC) {
            if(!(MusicPlayer.curMusic==null && configDebug.SilenceIsBad)) {
                e.setResultSound(silenced);
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent e) {
        if(e.getEntityLiving() instanceof EntityPlayer && e.getSource().getTrueSource() instanceof EntityPlayer) {
            if (e.getEntityLiving() == MusicPicker.player) {
                PVPTracker = (EntityPlayer)e.getSource().getTrueSource();
                MusicPicker.setPVP = true;
            }
            else if(e.getSource().getTrueSource() == MusicPicker.player) {
                PVPTracker = (EntityPlayer)e.getEntityLiving();
                MusicPicker.setPVP = true;
            }
        }
    }

    @SubscribeEvent
    public static void guiScreen(GuiScreenEvent e) {
        if(!(e.getGui() instanceof GuiWinGame)) {
            GUIName = e.getGui().toString();
        }
        else {
            GUIName = "CREDITS";
        }
        if (configDebug.ShowGUIName) {
            e.getGui().drawHoveringText(e.getGui().toString(), 0, 0);
        }
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent e) {
        lastAdvancement = e.getAdvancement().getId().toString();
        advancement = true;
    }

    @SubscribeEvent
    public static void worldRender(RenderWorldLastEvent e) {
        isWorldRendered=true;
    }

    @SubscribeEvent
    public static void clientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        MusicPicker.mc.getSoundHandler().stopSounds();
        isWorldRendered=false;
        MusicPicker.player=null;
    }

    @SubscribeEvent
    public static void customTick(CustomTick ev) {
        if(configTitleCards.imagecards.get(curImageIndex)!=null) {
            if (timer > configTitleCards.imagecards.get(curImageIndex).getTime()) {
                activated = false;
                timer = 0;
                ismoving = false;
                movingcounter = 0;
            }
            if (ismoving) {
                if (timer % configTitleCards.imagecards.get(curImageIndex).getDelay() == 0) {
                    movingcounter++;
                    if (movingcounter >= pngs.size()) {
                        movingcounter = 0;
                    }
                }
                IMAGE_CARD = pngs.get(movingcounter);
            }
            if (activated) {
                timer++;
                startDelayCount++;
                if (startDelayCount > 0) {
                    if (fadeCount > 1) {
                        fadeCount -= configTitleCards.imagecards.get(curImageIndex).getFadeIn();
                        if (fadeCount < 1) {
                            fadeCount = 1;
                        }
                    }
                }
            } else {
                if (fadeCount < 1000) {
                    fadeCount += configTitleCards.imagecards.get(curImageIndex).getFadeOut();
                    if (fadeCount > 1000) {
                        fadeCount = 1000;
                        ismoving = false;
                    }
                }
                startDelayCount = 0;
            }
        }
    }

    @SubscribeEvent
    public static void imageCards(RenderGameOverlayEvent.Post e) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if(e.getType()== RenderGameOverlayEvent.ElementType.ALL) {
            ScaledResolution res = e.getResolution();
            if (player != null && configTitleCards.imagecards.get(curImageIndex)!=null) {
                int x = res.getScaledWidth();
                int y = res.getScaledHeight();
                Vector4f color = new Vector4f(1, 1, 1, 1);
                if (fadeCount != 1000 && IMAGE_CARD!=null) {
                    float opacity = (int) (17 - (fadeCount / 80));
                    opacity = (opacity * 1.15f) / 15;
                    GlStateManager.enableBlend();
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(0, 0, 0);
                    GlStateManager.scale((0.25f*((float)x/(float)y))*(configTitleCards.imagecards.get(curImageIndex).getScaleX()/100f),0.25f*(configTitleCards.imagecards.get(curImageIndex).getScaleY()/100f),1f);
                    GlStateManager.color(color.getX(), color.getY(), color.getZ(), Math.max(0, Math.min(0.95f, opacity)));
                    mc.getTextureManager().bindTexture(IMAGE_CARD);
                    float x_translation = (((1f/0.140625f)*.5f)*(1f/(configTitleCards.imagecards.get(curImageIndex).getScaleX()/100f))*(x+configTitleCards.imagecards.get(curImageIndex).getHorizontal()));
                    GuiScreen.drawModalRectWithCustomSizedTexture((int)(x_translation-(x*0.496)),
                            (int)((y+configTitleCards.imagecards.get(curImageIndex).getVertical())/4f),x,y,x,y,x,y);
                    GlStateManager.color(1F, 1F, 1F, 1);
                    GlStateManager.popMatrix();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent e) {
        if(MusicPlayer.RELOAD.isKeyDown()) {
            Minecraft.getMinecraft().getSoundHandler().stopSounds();
            MusicPicker.player.sendMessage(new TextComponentString("\u00A74\u00A7oReloading Music... This may take a while!"));
            MusicPlayer.reloading = true;
            reloadCounter = 5;
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if(reloadCounter>0) {
            reloadCounter-=1;
            if(reloadCounter==1) {
                reload.readAndReload();
                MusicPicker.player.sendMessage(new TextComponentString("\u00A7a\u00A7oFinished!"));
                IMAGE_CARD = null;
                fadeCount = 1000;
                timer = 0;
                activated = false;
                ismoving = false;
                MusicPlayer.cards = true;
                MusicPlayer.reloading = false;
            }
        }
    }

    @SubscribeEvent
    public static void debugInfo(RenderGameOverlayEvent.Text e) {
        if(configDebug.ShowDebugInfo && isWorldRendered) {
            if(MusicPlayer.curTrackHolder!=null) {
                e.getLeft().add("Music Triggers Current song: " + MusicPlayer.curTrackHolder);
            }
            if(!configDebug.ShowJustCurSong) {
                if(MusicPicker.playableList!=null && !MusicPicker.playableList.isEmpty()) {
                    StringBuilder s = new StringBuilder();
                    for (String ev : MusicPicker.playableList) {
                        s.append(" ").append(ev);
                    }
                    e.getLeft().add("Music Triggers Playable Events:" + s);
                }
                StringBuilder sm = new StringBuilder();
                sm.append("minecraft");
                for (String ev : configDebug.blockedmods) {
                    sm.append(" ").append(ev);
                }
                e.getLeft().add("Music Triggers Current Blocked Mods: " + sm);
                if(MusicPicker.player!=null && MusicPicker.world!=null) {
                    e.getLeft().add("Music Triggers Current Biome: " + MusicPicker.world.getBiome(MusicPicker.player.getPosition()).getRegistryName());
                    e.getLeft().add("Music Triggers Current Dimension: " + MusicPicker.player.dimension);
                    e.getLeft().add("Music Triggers Current Total Light: " + MusicPicker.world.getLight(MusicPicker.roundedPos(MusicPicker.player), true));
                    e.getLeft().add("Music Triggers Current Block Light: " + MusicPicker.world.getLightFor(EnumSkyBlock.BLOCK, MusicPicker.roundedPos(MusicPicker.player)));

                    if (MusicPicker.effectList != null && !MusicPicker.effectList.isEmpty()) {
                        StringBuilder se = new StringBuilder();
                        for (String ev : MusicPicker.effectList) {
                            se.append(" ").append(ev);
                        }
                        e.getLeft().add("Music Triggers Current Effect List:" + se);
                    }
                    if(Minecraft.getMinecraft().objectMouseOver != null) {
                        if (getLivingFromEntity(Minecraft.getMinecraft().objectMouseOver.entityHit) != null) {
                            e.getLeft().add("Music Triggers Current Entity Name: " + getLivingFromEntity(Minecraft.getMinecraft().objectMouseOver.entityHit).getName());
                        }
                        try {
                            if (infernalChecker(getLivingFromEntity(Minecraft.getMinecraft().objectMouseOver.entityHit)) != null) {
                                e.getLeft().add("Music Triggers Infernal Mob Mod Name: " + infernalChecker(getLivingFromEntity(Minecraft.getMinecraft().objectMouseOver.entityHit)));
                            }
                        } catch (NoSuchMethodError ignored) {
                        }
                    }
                }
            }
        }
    }

    @Optional.Method(modid = "infernalmobs")
    private static String infernalChecker(@Nullable EntityLiving m) {
        if(m==null) {
            return null;
        }
        return InfernalMobsCore.getMobModifiers(m)==null ? null : InfernalMobsCore.getMobModifiers(m).getModName();
    }

    private static EntityLiving getLivingFromEntity(Entity e) {
        if(e instanceof EntityLiving) {
            return (EntityLiving) e;
        }
        else return null;
    }
}
