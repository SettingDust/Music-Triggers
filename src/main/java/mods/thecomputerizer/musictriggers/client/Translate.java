package mods.thecomputerizer.musictriggers.client;

import mods.thecomputerizer.musictriggers.Constants;
import mods.thecomputerizer.musictriggers.client.data.Trigger;
import mods.thecomputerizer.theimpossiblelibrary.common.toml.Table;
import mods.thecomputerizer.theimpossiblelibrary.util.client.AssetUtil;
import net.minecraft.client.resources.I18n;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Translate {

    /*
     * Gets the identifier of a trigger id
     */
    public static String triggerID(Table trigger) {
        String id = trigger.getValOrDefault("identifier","not_set");
        return id.matches("not_set") ? trigger.getValOrDefault("id","not_set") : id;
    }

    /*
     * Formats the translation of a trigger with an identifier.
    */
    public static String triggerWithID(String triggerIdentifier) {
        String[] split = triggerIdentifier.split("-", 2);
        return guiGeneric(false,"trigger",split[0])+"-"+ split[1];
    }

    /*
     * Converts a collection of strings into a single bracketed string.
    */
    public static String condenseList(Collection<?> things) {
        StringBuilder builder = new StringBuilder();
        int checkMax = 0;
        for(Object element : things) {
            builder.append("[").append(element).append("]");
            checkMax++;
            if(checkMax>things.size()) builder.append(" ");
        }
        return builder.toString();
    }

    private static String formatMillis(long millis) {
        return String.format("%d:%02d:%03d", TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis)-TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)),
                        millis-TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis)));
    }

    /*
     * Converts a collection of triggers into a single line for use in hover text specifically for toggles.
    */
    public static String toggleTriggers(Collection<String> triggers) {
        return condenseList(triggers.stream().map(Translate::triggerWithID).collect(Collectors.toList()));
    }

    /*
     * Parameter name translation. Also handles when triggers have unique descriptions for certain parameters.
    */
    public static String parameter(String ... stuff) {
        String key = guiGeneric(true,stuff);
        return I18n.hasKey(key) ? libHook(key,null) :
        guiGeneric(false,ArrayUtils.remove(stuff,3));
    }

    /*
     * Allows for custom song names to display or uses the registered name if no key is set.
    */
    public static String songInstance(String name) {
        return libHook(buildLangKey("audio",name,"name"),name);
    }

    /*
     * Translation for any title or single line string that includes a song name.
    */
    public static String withSongInstance(String name, String ... elements) {
        return guiGeneric(false,elements)+" "+ songInstance(name);
    }

    /*
     * Translation for any title or single line string that includes a generic non song name string
     */
    public static String withOther(String extra, String ... elements) {
        return guiGeneric(false,elements)+" "+ extra;
    }

    /*
     * Generic hover translations that need to be returned as a list but only have 1 element.
    */
    public static List<String> singletonHover(String ... elements) {
        return Collections.singletonList(guiGeneric(false, elements));
    }

    /*
     * Generic hover translations that need to be returned as a list but only have 1 element which needs an extra string
     */
    public static List<String> singletonHoverExtra(String extra, String ... elements) {
        return Collections.singletonList(guiGeneric(false, elements)+" "+extra);
    }

    /*
     * Hover translation for song instances.
    */
    public static List<String> songHover(int loadOrder, List<String> triggers) {
        return Arrays.asList(guiGeneric(false,"selection","song","load")+" "+loadOrder,
                guiGeneric(false,"selection","song","triggers")+" "+
                        condenseList(triggers));
    }

    /*
     * Hover translation for song info within instances.
    */
    public static List<String> songInfoHover(Table audio) {
        return Arrays.asList(guiGeneric(false,"parameter","song_info","volume","name")+ ": "+
                        audio.getValOrDefault("volume",1f),
                guiGeneric(false,"parameter","song_info","pitch","name")+ ": "+ audio.getValOrDefault("pitch",1f));
    }

    /*
     * Hover translation for potential triggers.
    */
    public static List<String> potentialTriggerHover(String triggerName, boolean hasTrigger, Collection<String> ids) {
        List<String> translated = new ArrayList<>();
        boolean needsID = Trigger.isParameterAccepted(triggerName,"identifier");
        if(needsID) translated.add(guiGeneric(false,"selection","trigger","needs_id"));
        if(!hasTrigger) translated.add(guiGeneric(false,"selection","trigger","not_registered"));
        else {
            if(needsID) translated.add(guiGeneric(false,"selection","trigger", "registered_id")+" "+
                    condenseList(ids));
            else translated.add(guiGeneric(false,"selection","trigger","is_registered"));
        }
        return translated;
    }

    /*
     * Hover translation for a trigger instance.
    */
    public static List<String> triggerElementHover(Table trigger) {
        if(trigger.getName().matches("menu") || trigger.getName().matches("generic") ||
                trigger.getName().matches("loading"))
            return new ArrayList<>();
        if(Trigger.isParameterAccepted(trigger.getName(),"identifier"))
            return Arrays.asList(guiGeneric(false,"trigger","identifier")+ ": "+triggerID(trigger),
                    guiGeneric(false,"trigger","priority")+ ": "+trigger.getValOrDefault("priority",1));
        return Collections.singletonList(guiGeneric(false,"trigger","priority")+": "+
                trigger.getValOrDefault("priority",1));
    }

    public static List<String> loopHover(Table loop) {
        return Arrays.asList(withOther(formatMillis(loop.getValOrDefault("from",0L)),"selection","loop","from"),
                withOther(formatMillis(loop.getValOrDefault("to",0L)),"selection","loop","to"));
    }

    /*
     * Hover translation for a title card
     */
    public static List<String> hoverLinesTitle(Table title) {
        int titles = title.getValOrDefault("titles",new ArrayList<String>()).size();
        int subtitles = title.getValOrDefault("subtitles",new ArrayList<String>()).size();
        List<String> triggers = title.getValOrDefault("triggers",new ArrayList<>());
        return triggers.isEmpty() ?
                Arrays.asList(titles+" "+guiGeneric(false,"selection","title","titles"),
                        subtitles+" "+guiGeneric(false,"selection","title","subtitles")) :
                Arrays.asList(titles+" "+guiGeneric(false,"selection","title","titles"),
                        subtitles+" "+guiGeneric(false,"selection","title","subtitles"),
                        condenseList(triggers));
    }

    /*
     * Hover translation for an image card
     */
    public static List<String> hoverLinesImage(Table image) {
        String name = image.getValOrDefault("name","missing_name");
        List<String> triggers = image.getValOrDefault("triggers",new ArrayList<>());
        return triggers.isEmpty() ? Collections.singletonList(name) : Arrays.asList(name,condenseList(triggers));
    }

    /*
     * Hover translation for a toggle instance.
    */
    public static List<String> hoverLinesToggle(Table toggle) {
        return Arrays.asList(guiGeneric(false,"selection","toggle","load_order")+" "+toggle.getArrIndex(),
                toggle.getTablesByName("from").size()+" "+
                        guiGeneric(false,"selection","toggles","from"),
                toggle.getTablesByName("to").size()+" "+
                        guiGeneric(false,"selection","toggles","to"));
    }

    /*
     * Hover translation for a trigger within a toggle instance.
    */
    public static List<String> hoverLinesTrigger(Table trigger) {
        return Arrays.asList(guiGeneric(false, "selection","toggle", "load_order") + " " + trigger.getParent().getArrIndex(),
                guiGeneric(false, "toggle", "triggers") + " " +
                        toggleTriggers(trigger.getValOrDefault("triggers", new ArrayList<>())));
    }

    /*
     * Hover translation for a target within a toggle instance.
    */
    public static List<String> hoverLinesTarget(Table target) {
        return Arrays.asList(guiGeneric(false, "selection","toggle", "load_order") + " " + target.getParent().getArrIndex(),
                guiGeneric(false, "toggle", "triggers") + " " +
                        toggleTriggers(target.getValOrDefault("triggers", new ArrayList<>())));
    }

    /*
     * Generic numbered list used for hover translations that are split into multiple lines for aesthetic purposes.
    */
    public static List<String> guiNumberedList(int numLines, String ... elements) {
        return IntStream.range(0, numLines).mapToObj(i -> libHook(guiGeneric(true,elements)+(i+1),null))
                .collect(Collectors.toList());
    }

    /*
     * Translation for potential song entries.
    */
    public static String selectionSong(String song, String ... elements) {
        String key = buildLangKey("gui",elements);
        return I18n.hasKey(key) ? libHook(key,null) : song;
    }

    /*
     * Returns the key if asKey is true or otherwise the actual translation.
    */
    public static String guiGeneric(boolean asKey, String ... elements) {
        String key = buildLangKey("gui",elements);
        return asKey ? key : libHook(key, null);
    }

    /*
     * Builds a lang key from a variable of elements assuming a base of a category and the modid.
    */
    @SuppressWarnings("SameParameterValue")
    private static String buildLangKey(String category, String ... extras) {
        StringBuilder builder = new StringBuilder();
        builder.append(category).append(".").append(Constants.MODID);
        List<String> fixed = Arrays.stream(extras).filter(Objects::nonNull).collect(Collectors.toList());
        if(fixed.isEmpty()) return builder.toString();
        builder.append(".");
        int checkMax = 1;
        for(String extra : fixed) {
            builder.append(extra);
            if(checkMax<fixed.size()) {
                builder.append(".");
                checkMax++;
            }
        }
        return builder.toString();
    }

    /*
     * This will assume the name ending has already been handled. The lang key will only get checked for validity if the
     * fallback value is nonnull. In that case the fallback is returned if the key is not found.
    */
    private static String libHook(String key, String fallback) {
        if(I18n.hasKey(key) || fallback==null)
            return AssetUtil.customLang(key,false);
        return fallback;
    }
}
