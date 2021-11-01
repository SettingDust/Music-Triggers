package mods.thecomputerizer.musictriggers.client;

import de.ellpeck.nyx.capabilities.NyxWorld;
import de.ellpeck.nyx.lunarevents.BloodMoon;
import de.ellpeck.nyx.lunarevents.HarvestMoon;
import de.ellpeck.nyx.lunarevents.StarShower;
import lumien.bloodmoon.Bloodmoon;
import mods.thecomputerizer.musictriggers.common.SoundHandler;
import mods.thecomputerizer.musictriggers.config;
import mods.thecomputerizer.musictriggers.configDebug;
import mods.thecomputerizer.musictriggers.util.RegistryHandler;
import mods.thecomputerizer.musictriggers.util.packet;
import net.darkhax.gamestages.GameStageHelper;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Optional;
import org.orecruncher.dsurround.client.weather.Weather;

import java.util.*;

public class MusicPicker {
    public static Minecraft mc;
    public static EntityPlayer player;
    public static World world;

    public static HashMap<String, String[]> dynamicSongs = new HashMap<>();
    public static HashMap<String, Integer> dynamicPriorities = new HashMap<>();
    public static HashMap<String, Integer> dynamicFade = new HashMap<>();

    public static List<String> playableList = new ArrayList<>();
    public static List<String> titleCardEvents = new ArrayList<>();

    public static int curFade = 0;
    public static boolean shouldChange = false;

    public static String[] playThese() {
        if (!MusicPlayer.fading) {
            titleCardEvents = new ArrayList<>();
        }
        mc = Minecraft.getMinecraft();
        player = mc.player;
        if (player != null) {
            world = player.getEntityWorld();
        }
        if (player == null) {
            return config.menu.menuSongs;
        }
        List<String> res = comboChecker(priorityHandler(playableEvents()));
        if (res != null && !res.isEmpty()) {
            dynamicSongs = new HashMap<>();
            dynamicPriorities = new HashMap<>();
            dynamicFade = new HashMap<>();
            return res.toArray(new String[0]);
        }
        dynamicSongs = new HashMap<>();
        dynamicPriorities = new HashMap<>();
        dynamicFade = new HashMap<>();
        curFade = config.generic.genericFade;
        return config.generic.genericSongs;
    }

    @SuppressWarnings("rawtypes")
    public static List<String> comboChecker(String st) {
        if (st == null) {
            return null;
        }
        List<String> playableSongs = new ArrayList<>();
        for (String s : dynamicSongs.get(st)) {
            for (Map.Entry<String, List<String>> stringListEntry : SoundHandler.songCombos.entrySet()) {
                String checkThis = ((Map.Entry) stringListEntry).getKey().toString();
                if (s.matches(checkThis)) {
                    if (playableList.containsAll(SoundHandler.songCombos.get(s)) && SoundHandler.songCombos.get(s).size() != 1) {
                        playableSongs.add(s.substring(1));
                        if (!titleCardEvents.contains(st)) {
                            titleCardEvents.addAll(SoundHandler.songCombos.get(s));
                        }
                    }
                }
            }
        }
        if (playableSongs.isEmpty()) {
            for (String s : dynamicSongs.get(st)) {
                if (!s.startsWith("@")) {
                    playableSongs.add(s);
                    if (!titleCardEvents.contains(st)) {
                        titleCardEvents.add(st);
                    }
                }
            }
        }
        if (playableSongs.isEmpty()) {
            List<String> tryAgain = playableList;
            tryAgain.remove(st);
            playableList = tryAgain;
            if (playableList.isEmpty()) {
                return null;
            }
            playableSongs = comboChecker(priorityHandler(playableList));
        }
        return playableSongs;
    }

    public static String priorityHandler(List<String> sta) {
        if (sta == null) {
            return null;
        }
        int highest = -100;
        String trueHighest = "";
        for (String list : sta) {
            if (dynamicPriorities.get(list) > highest && !Arrays.asList(dynamicSongs.get(list)).isEmpty()) {
                highest = dynamicPriorities.get(list);
                trueHighest = list;
            }
        }
        while (dynamicSongs.get(trueHighest) == null) {
            sta.remove(trueHighest);
            if (sta.isEmpty()) {
                return null;
            }
            for (String list : sta) {
                if (dynamicPriorities.get(list) > highest) {
                    highest = dynamicPriorities.get(list);
                    trueHighest = list;
                }
            }
        }
        if (dynamicFade != null && !dynamicFade.isEmpty()) {
            if (dynamicFade.get(trueHighest) != null) {
                curFade = dynamicFade.get(trueHighest);
            } else {
                curFade = 0;
            }
        }
        return trueHighest;
    }

    @SuppressWarnings("rawtypes")
    public static List<String> playableEvents() {
        List<String> events = new ArrayList<>();
        double time = (double) world.getWorldTime() / 24000.0;
        if (time > 1) {
            time = time - (long) time;
        }
        if (time < 0.54166666666) {
            events.add("day");
            dynamicSongs.put("day", config.day.daySongs);
            dynamicPriorities.put("day", config.day.dayPriority);
            dynamicFade.put("day", config.day.dayFade);
        } else {
            if (SoundHandler.nightSongs.get(0)!=null) {
                events.add("night" + 0);
                String[] dimSongsArray;
                if(SoundHandler.nightSongsString.get((world.getMoonPhase()+1))!=null && !SoundHandler.nightSongsString.get((world.getMoonPhase()+1)).isEmpty()) {
                    dimSongsArray = new String[(SoundHandler.nightSongsString.get(0).size()) + (SoundHandler.nightSongsString.get((world.getMoonPhase()+1)).size())];
                }
                else {
                    dimSongsArray = new String[(SoundHandler.nightSongsString.get(0).size())];
                }
                List<String> tempNight = new ArrayList<>();
                if(SoundHandler.nightSongsString.get((world.getMoonPhase()+1))!=null && !SoundHandler.nightSongsString.get((world.getMoonPhase()+1)).isEmpty()) {
                    tempNight.addAll(SoundHandler.nightSongsString.get((world.getMoonPhase()+1)));
                }
                tempNight.addAll(SoundHandler.nightSongsString.get(0));
                dynamicSongs.put("night" + 0, tempNight.toArray(dimSongsArray));
                dynamicPriorities.put("night" + 0, config.night.nightPriority);
                dynamicFade.put("night" + 0, SoundHandler.nightFade.get(0));
            }
            else {
                if (SoundHandler.nightSongs.get((world.getMoonPhase()+1))!=null) {
                    events.add("night" + (world.getMoonPhase()+1));
                    String[] dimSongsArray = new String[SoundHandler.nightSongsString.get((world.getMoonPhase()+1)).size()];
                    dynamicSongs.put("night" + (world.getMoonPhase()+1), SoundHandler.nightSongsString.get((world.getMoonPhase()+1)).toArray(dimSongsArray));
                    dynamicPriorities.put("night"+ (world.getMoonPhase()+1), config.night.nightPriority);
                    dynamicFade.put("night" + (world.getMoonPhase()+1), SoundHandler.nightFade.get((world.getMoonPhase()+1)));
                }
            }
        }
        if (time < 0.54166666666 && time >= 0.5) {
            events.add("sunset");
            dynamicSongs.put("sunset", config.sunset.sunsetSongs);
            dynamicPriorities.put("sunset", config.sunset.sunsetPriority);
            dynamicFade.put("sunset", config.sunset.sunsetFade);
        } else if (time >= 0.95833333333 && time < 1) {
            events.add("sunrise");
            dynamicSongs.put("sunrise", config.sunrise.sunriseSongs);
            dynamicPriorities.put("sunrise", config.sunrise.sunrisePriority);
            dynamicFade.put("sunrise", config.sunrise.sunriseFade);
        }
        if(configDebug.LightLevel) {
            player.sendMessage(new TextComponentString("Current light level: "+world.getLight(roundedPos(player))));
        }
        if (world.getLight(roundedPos(player)) <= config.light.lightLevel) {
            events.add("light");
            dynamicSongs.put("light", config.light.lightSongs);
            dynamicPriorities.put("light", config.light.lightPriority);
            dynamicFade.put("light", config.light.lightFade);
        }
        if (player.posY < config.deepUnder.deepUnderLevel && !world.canSeeSky(roundedPos(player))) {
            events.add("deepUnder");
            dynamicSongs.put("deepUnder", config.deepUnder.deepUnderSongs);
            dynamicPriorities.put("deepUnder", config.deepUnder.deepUnderPriority);
            dynamicFade.put("deepUnder", config.deepUnder.deepUnderFade);
        }
        if (player.posY < config.underground.undergroundLevel && !world.canSeeSky(roundedPos(player))) {
            events.add("underground");
            dynamicSongs.put("underground", config.underground.undergroundSongs);
            dynamicPriorities.put("underground", config.underground.undergroundPriority);
            dynamicFade.put("underground", config.underground.undergroundFade);
        }
        if (player.posY < config.inVoid.inVoidLevel) {
            events.add("inVoid");
            dynamicSongs.put("inVoid", config.inVoid.inVoidSongs);
            dynamicPriorities.put("inVoid", config.inVoid.inVoidPriority);
            dynamicFade.put("inVoid", config.inVoid.inVoidFade);
        }
        if (player.posY >= config.high.highLevel) {
            events.add("high");
            dynamicSongs.put("high", config.high.highSongs);
            dynamicPriorities.put("high", config.high.highPriority);
            dynamicFade.put("high", config.high.highFade);
        }
        if (world.isRaining()) {
            events.add("raining");
            dynamicSongs.put("raining", config.raining.rainingSongs);
            dynamicPriorities.put("raining", config.raining.rainingPriority);
            dynamicFade.put("raining", config.raining.rainingFade);
            if (world.canSnowAt(player.getPosition(), true)) {
                events.add("snowing");
                dynamicSongs.put("snowing", config.snowing.snowingSongs);
                dynamicPriorities.put("snowing", config.snowing.snowingPriority);
                dynamicFade.put("snowing", config.snowing.snowingFade);
            }
        }
        if (world.isThundering()) {
            events.add("storming");
            dynamicSongs.put("storming", config.storming.stormingSongs);
            dynamicPriorities.put("storming", config.storming.stormingPriority);
            dynamicFade.put("storming", config.storming.stormingFade);
        }
        if (player.getHealth() < player.getMaxHealth() * (config.lowHP.lowHPLevel)) {
            events.add("lowHP");
            dynamicSongs.put("lowHP", config.lowHP.lowHPSongs);
            dynamicPriorities.put("lowHP", config.lowHP.lowHPPriority);
            dynamicFade.put("lowHP", config.lowHP.lowHPFade);
        }
        if (player.isDead) {
            events.add("dead");
            dynamicSongs.put("dead", config.dead.deadSongs);
            dynamicPriorities.put("dead", config.dead.deadPriority);
            dynamicFade.put("dead", config.dead.deadFade);
        }
        if (player.isSpectator()) {
            events.add("spectator");
            dynamicSongs.put("spectator", config.spectator.spectatorSongs);
            dynamicPriorities.put("spectator", config.spectator.spectatorPriority);
            dynamicFade.put("spectator", config.spectator.spectatorFade);
        }
        if (player.isCreative()) {
            events.add("creative");
            dynamicSongs.put("creative", config.creative.creativeSongs);
            dynamicPriorities.put("creative", config.creative.creativePriority);
            dynamicFade.put("creative", config.creative.creativeFade);
        }
        if (player.isRiding()) {
            events.add("riding");
            dynamicSongs.put("riding", config.riding.ridingSongs);
            dynamicPriorities.put("riding", config.riding.ridingPriority);
            dynamicFade.put("riding", config.riding.ridingFade);
        }
        if (world.getBlockState(roundedPos(player)).getMaterial()==Material.WATER && world.getBlockState(roundedPos(player).up()).getMaterial()==Material.WATER) {
            events.add("underwater");
            dynamicSongs.put("underwater", config.underwater.underwaterSongs);
            dynamicPriorities.put("underwater", config.underwater.underwaterPriority);
            dynamicFade.put("underwater", config.underwater.underwaterFade);
        }
        for (EntityMob ent : world.getEntitiesWithinAABB(EntityMob.class, new AxisAlignedBB(player.posX - 16, player.posY - 8, player.posZ - 16, player.posX + 16, player.posY + 8, player.posZ + 16))) {
            if (ent.serializeNBT().getString("Owner").matches(player.getName())) {
                events.add("pet");
                dynamicSongs.put("pet", config.pet.petSongs);
                dynamicPriorities.put("pet", config.pet.petPriority);
                dynamicFade.put("pet", config.pet.petFade);
                break;
            }
        }
        if (configDebug.DimensionChecker && eventsClient.isWorldRendered) {
            player.sendMessage(new TextComponentString("Current dimension: "+player.dimension));
        }
        if (SoundHandler.dimensionSongs.get(player.dimension) != null) {
            events.add("dimension" + player.dimension);
            String[] dimSongsArray = new String[SoundHandler.dimensionSongsString.get(player.dimension).size()];
            dynamicSongs.put("dimension" + player.dimension, SoundHandler.dimensionSongsString.get(player.dimension).toArray(dimSongsArray));
            dynamicPriorities.put("dimension" + player.dimension, SoundHandler.dimensionPriorities.get(player.dimension));
            dynamicFade.put("dimension" + player.dimension, SoundHandler.dimensionFade.get(player.dimension));
        }
        if (configDebug.BiomeChecker && eventsClient.isWorldRendered) {
            player.sendMessage(new TextComponentString("Current dimension: "+Objects.requireNonNull(world.getBiome(player.getPosition()).getRegistryName())));
        }
        if (SoundHandler.biomeSongs!=null && !SoundHandler.biomeSongs.isEmpty()) {
            for (Map.Entry<String, List<String>> stringListEntry : SoundHandler.biomeSongsString.entrySet()) {
                String biomeRegex = ((Map.Entry) stringListEntry).getKey().toString();
                if (Objects.requireNonNull(world.getBiome(player.getPosition()).getRegistryName()).toString().contains(biomeRegex)) {
                    String biomeName = Objects.requireNonNull(world.getBiome(player.getPosition()).getRegistryName()).toString();
                    events.add(biomeName);
                    String[] biomeSongsArray = new String[SoundHandler.biomeSongsString.get(biomeName).size()];
                    dynamicSongs.put(biomeName, SoundHandler.biomeSongsString.get(biomeName).toArray(biomeSongsArray));
                    dynamicPriorities.put(biomeName, SoundHandler.biomePriorities.get(biomeName));
                    dynamicFade.put(biomeName, SoundHandler.biomeFade.get(biomeName));
                }
            }
        }
        if (mc.isSingleplayer()) {
            WorldServer nworld = Objects.requireNonNull(mc.getIntegratedServer()).getWorld(player.dimension);
            for (Map.Entry<String, List<String>> stringListEntry : SoundHandler.structureSongsString.entrySet()) {
                String structName = ((Map.Entry) stringListEntry).getKey().toString();
                if (nworld.getChunkProvider().isInsideStructure(world, structName, player.getPosition())) {
                    events.add("structure:" + structName);
                    String[] structureSongsArray = new String[SoundHandler.structureSongsString.get(structName).size()];
                    dynamicSongs.put("structure:" + structName, SoundHandler.structureSongsString.get(structName).toArray(structureSongsArray));
                    dynamicPriorities.put("structure:" + structName, SoundHandler.structurePriorities.get(structName));
                    dynamicFade.put("structure:" + structName, SoundHandler.structureFade.get(structName));
                }
            }
        } else {
            for (Map.Entry<String, List<String>> stringListEntry : SoundHandler.structureSongsString.entrySet()) {
                String structName = ((Map.Entry) stringListEntry).getKey().toString();
                RegistryHandler.network.sendToServer(new packet.packetMessage(structName, player.getPosition(), player.dimension, player.getUniqueID()));
                if (fromServer.inStructure.containsKey(structName)) {
                    if (fromServer.inStructure.get(structName)) {
                        events.add("structure:" + structName);
                        String[] structureSongsArray = new String[SoundHandler.structureSongsString.get(structName).size()];
                        dynamicSongs.put("structure:" + structName, SoundHandler.structureSongsString.get(structName).toArray(structureSongsArray));
                        dynamicPriorities.put("structure:" + structName, SoundHandler.structurePriorities.get(structName));
                        dynamicFade.put("structure:" + structName, SoundHandler.structureFade.get(structName));
                    }
                }
            }
        }
        for (Map.Entry<String, List<String>> stringListEntry : SoundHandler.mobSongsString.entrySet()) {
            String mobName = ((Map.Entry) stringListEntry).getKey().toString();
            double range = SoundHandler.mobRange.get(mobName);
            List<EntityLiving> mobTempList = world.getEntitiesWithinAABB(EntityLiving.class, new AxisAlignedBB(player.posX - range, player.posY - (range / 2), player.posZ - range, player.posX + range, player.posY + (range / 2), player.posZ + range));
            List<EntityLiving> mobList = new ArrayList<>();
            for (EntityLiving e : mobTempList) {
                if (e instanceof EntityMob || e instanceof EntityDragon) {
                    mobList.add(e);
                }
            }
            if (mobName.matches("MOB")) {
                if (mobList.size() >= SoundHandler.mobNumber.get(mobName)) {
                    events.add(mobName);
                    String[] mobSongsArray = new String[SoundHandler.mobSongsString.get(mobName).size()];
                    dynamicSongs.put(mobName, SoundHandler.mobSongsString.get(mobName).toArray(mobSongsArray));
                    dynamicPriorities.put(mobName, SoundHandler.mobPriorities.get(mobName));
                    dynamicFade.put(mobName, SoundHandler.mobFade.get(mobName));
                }
            } else {
                int mobCounter = 0;
                for (EntityLiving e : mobTempList) {
                    if (e.getName().matches(mobName)) {
                        mobCounter++;
                    }
                }
                if (mobCounter >= SoundHandler.mobNumber.get(mobName)) {
                    events.add(mobName);
                    String[] mobSongsArray = new String[SoundHandler.mobSongsString.get(mobName).size()];
                    dynamicSongs.put(mobName, SoundHandler.mobSongsString.get(mobName).toArray(mobSongsArray));
                    dynamicPriorities.put(mobName, SoundHandler.mobPriorities.get(mobName));
                    dynamicFade.put(mobName, SoundHandler.mobFade.get(mobName));
                }
            }
        }
        if(!SoundHandler.zonesSongs.isEmpty()) {
            for (Map.Entry<String, List<String>> stringListEntry : SoundHandler.zonesSongsString.entrySet()) {
                String zoneRange = ((Map.Entry) stringListEntry).getKey().toString();
                String[] broken = SoundHandler.stringBreaker(zoneRange);
                BlockPos bp = player.getPosition();
                int x1 = Integer.parseInt(broken[0]);
                int y1 = Integer.parseInt(broken[1]);
                int z1 = Integer.parseInt(broken[2]);
                int x2 = Integer.parseInt(broken[3]);
                int y2 = Integer.parseInt(broken[4]);
                int z2 = Integer.parseInt(broken[5]);
                if(bp.getX()>x1 && bp.getX()<x2 && bp.getY()>y1 && bp.getY()<y2 && bp.getZ()>z1 && bp.getZ()<z2) {
                    events.add(zoneRange);
                    String[] zonesSongsArray = new String[SoundHandler.zonesSongsString.get(zoneRange).size()];
                    dynamicSongs.put(zoneRange, SoundHandler.zonesSongsString.get(zoneRange).toArray(zonesSongsArray));
                    dynamicPriorities.put(zoneRange, SoundHandler.zonesPriorities.get(zoneRange));
                    dynamicFade.put(zoneRange, SoundHandler.zonesFade.get(zoneRange));
                }
            }
        }
        if(!SoundHandler.effectSongs.isEmpty()) {
            for(PotionEffect p : player.getActivePotionEffects()) {
                if(SoundHandler.effectSongsString.containsKey(p.getEffectName())) {
                    if(configDebug.EffectList) {
                        player.sendMessage(new TextComponentString(p.getEffectName()));
                    }
                    events.add(p.getEffectName());
                    String[] zonesSongsArray = new String[SoundHandler.effectSongsString.get(p.getEffectName()).size()];
                    dynamicSongs.put(p.getEffectName(), SoundHandler.effectSongsString.get(p.getEffectName()).toArray(zonesSongsArray));
                    dynamicPriorities.put(p.getEffectName(), SoundHandler.effectPriorities.get(p.getEffectName()));
                    dynamicFade.put(p.getEffectName(), SoundHandler.effectFade.get(p.getEffectName()));
                }
            }
        }
        try {
            List<String> whitelist = stageWhitelistChecker();
            List<String> blacklist = stageBlacklistChecker();
            if (!whitelist.isEmpty()) {
                events.addAll(whitelist);
            }
            if (!blacklist.isEmpty()) {
                events.addAll(blacklist);
            }
        } catch (NoSuchMethodError ignored) {
        }
        try {
            boolean bloodmoon = bloodmoon();
            if (bloodmoon) {
                events.add("bloodmoon");
            }
        } catch (NoSuchMethodError ignored) {
        }
        try {
            boolean nyxbloodmoon = nyxbloodmoon();
            if (nyxbloodmoon) {
                events.add("bloodmoon");
            }
        } catch (NoSuchMethodError ignored) {
        }
        try {
            boolean nyxharvestmoon = nyxharvestmoon();
            if (nyxharvestmoon) {
                events.add("harvestmoon");
            }
        } catch (NoSuchMethodError ignored) {
        }
        try {
            boolean nyxfallingstars = nyxfallingstars();
            if (nyxfallingstars) {
                events.add("fallingstars");
            }
        } catch (NoSuchMethodError ignored) {
        }
        try {
            if(dynamicrain()!=null) {
                int rainIntensity = Integer.parseInt(Objects.requireNonNull(dynamicrain()));
                events.add("Rain Intensity"+rainIntensity);
            }
        } catch (NoSuchMethodError ignored) {
        }

        playableList = events;

        if (events.size() >= 1 && configDebug.PlayableEvents && eventsClient.isWorldRendered) {
            for (int i=0;i<events.size();i++) {
                player.sendMessage(new TextComponentString("Playable events ["+i+"]: "+events.get(i)));
            }
        }
        return events;
    }

    @SuppressWarnings("rawtypes")
    @Optional.Method(modid = "gamestages")
    private static List<String> stageWhitelistChecker() {
        List<String> events = new ArrayList<>();
        for (Map.Entry<String, List<String>> stringListEntry : SoundHandler.gamestageSongsStringWhitelist.entrySet()) {
            String stageName = ((Map.Entry) stringListEntry).getKey().toString();
            String temp = stageName;
            if (temp.startsWith("@")) {
                temp = temp.substring(1);
            }
            if (GameStageHelper.clientHasStage(player, temp)) {
                events.add(stageName + "true");
                String[] gamestageSongsArray = new String[SoundHandler.gamestageSongsStringWhitelist.get(stageName).size()];
                dynamicSongs.put(stageName + "true", SoundHandler.gamestageSongsStringWhitelist.get(stageName).toArray(gamestageSongsArray));
                dynamicPriorities.put(stageName + "true", SoundHandler.gamestagePrioritiesWhitelist.get(stageName));
                dynamicFade.put(stageName + "true", SoundHandler.gamestageFadeWhitelist.get(stageName));
            }
        }
        return events;
    }

    @SuppressWarnings("rawtypes")
    @Optional.Method(modid = "gamestages")
    private static List<String> stageBlacklistChecker() {
        List<String> events = new ArrayList<>();
        for (Map.Entry<String, List<String>> stringListEntry : SoundHandler.gamestageSongsStringBlacklist.entrySet()) {
            String stageName = ((Map.Entry) stringListEntry).getKey().toString();
            String temp = stageName;
            if (temp.startsWith("@")) {
                temp = temp.substring(1);
            }
            if (!GameStageHelper.clientHasStage(player, temp)) {
                events.add(stageName + "false");
                String[] gamestageSongsArray = new String[SoundHandler.gamestageSongsStringBlacklist.get(stageName).size()];
                dynamicSongs.put(stageName + "false", SoundHandler.gamestageSongsStringBlacklist.get(stageName).toArray(gamestageSongsArray));
                dynamicPriorities.put(stageName + "false", SoundHandler.gamestagePrioritiesBlacklist.get(stageName));
                dynamicFade.put(stageName + "true", SoundHandler.gamestageFadeBlacklist.get(stageName));
            }
        }
        return events;
    }

    @Optional.Method(modid = "bloodmoon")
    private static boolean bloodmoon() {
        if (Bloodmoon.proxy.isBloodmoon()) {
            dynamicSongs.put("bloodmoon", config.bloodmoon.bloodmoonSongs);
            dynamicPriorities.put("bloodmoon", config.bloodmoon.bloodmoonPriority);
            dynamicFade.put("bloodmoon", config.bloodmoon.bloodmoonFade);
            return true;
        }
        return false;
    }

    @Optional.Method(modid = "nyx")
    private static boolean nyxbloodmoon() {
        if (NyxWorld.get(player.getEntityWorld()).currentEvent instanceof BloodMoon) {
            dynamicSongs.put("bloodmoon", config.bloodmoon.bloodmoonSongs);
            dynamicPriorities.put("bloodmoon", config.bloodmoon.bloodmoonPriority);
            dynamicFade.put("bloodmoon", config.bloodmoon.bloodmoonFade);
            return true;
        }
        return false;
    }

    @Optional.Method(modid = "nyx")
    private static boolean nyxharvestmoon() {
        if (NyxWorld.get(player.getEntityWorld()).currentEvent instanceof HarvestMoon) {
            dynamicSongs.put("harvestmoon", config.harvestmoon.harvestmoonSongs);
            dynamicPriorities.put("harvestmoon", config.harvestmoon.harvestmoonPriority);
            dynamicFade.put("harvestmoon", config.harvestmoon.harvestmoonFade);
            return true;
        }
        return false;
    }

    @Optional.Method(modid = "nyx")
    private static boolean nyxfallingstars() {
        if (NyxWorld.get(player.getEntityWorld()).currentEvent instanceof StarShower) {
            dynamicSongs.put("fallingstars", config.fallingstars.fallingstarsSongs);
            dynamicPriorities.put("fallingstars", config.fallingstars.fallingstarsPriority);
            dynamicFade.put("fallingstars", config.fallingstars.fallingstarsFade);
            return true;
        }
        return false;
    }

    @Optional.Method(modid = "dsurround")
    private static String dynamicrain() {
        for (Map.Entry<Integer, List<String>> integerListEntry : SoundHandler.rainintensitySongsString.entrySet()) {
            int intensity = integerListEntry.getKey();
            if (Weather.getIntensityLevel()>(float)intensity/100F) {
                String[] rainIntensityArray = new String[SoundHandler.rainintensitySongsString.get(intensity).size()];
                dynamicSongs.put("Rain Intensity"+intensity, SoundHandler.rainintensitySongsString.get(intensity).toArray(rainIntensityArray));
                dynamicPriorities.put("Rain Intensity"+intensity, config.rainintensity.rainintensityPriority);
                dynamicFade.put("Rain Intensity"+intensity, config.rainintensity.rainintensityFade);
                return intensity+"";
            }
        }
        return null;
    }

    public static BlockPos roundedPos(EntityPlayer p) {
        return new BlockPos((Math.round(p.posX*2)/2.0),(Math.round(p.posY*2)/2.0),(Math.round(p.posZ*2)/2.0));
    }
}