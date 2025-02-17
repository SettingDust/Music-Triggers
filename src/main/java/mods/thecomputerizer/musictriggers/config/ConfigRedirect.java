package mods.thecomputerizer.musictriggers.config;

import mods.thecomputerizer.musictriggers.MusicTriggers;
import mods.thecomputerizer.theimpossiblelibrary.util.file.FileUtil;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.Level;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigRedirect {

    private final File FILE;
    public final Map<String,String> urlMap = new HashMap<>();
    public final Map<String, ResourceLocation> resourceLocationMap = new HashMap<>();

    public ConfigRedirect(File file) {
        boolean exists = file.exists();
        this.FILE = exists ? file : FileUtil.generateNestedFile(file,false);
        if(!exists) FileUtil.writeLinesToFile(this.FILE,headerLines(),false);
    }

    private List<String> headerLines() {
        return Arrays.asList("Format this like name = url",
                "If you are trying to redirect to an already registered resource location Format it like name == location instead",
                "Any lines with Format in the name or = not present will not be read in",
                "Make sure each new entry is on a new line",
                "Here are 2 examples:",
                "thx = https://youtu.be/z3Q4WBpCXhs",
                "title == minecraft:sounds/music/menu/menu1.ogg");
    }

    public void parse() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(this.FILE));
            String line = br.readLine();
            while (line != null) {
                if(!line.contains("Format") && line.contains(" = ") && !line.contains(" == ")) {
                    urlMap.put(line.substring(0,line.indexOf('=')-1).trim(),line.substring(line.indexOf('=')+1).trim());
                } else if (!line.contains("Format") && line.contains("==")) {
                    String resource = line.substring(line.indexOf('=') + 2).trim();
                    try {
                        resourceLocationMap.put(line.substring(0,line.indexOf('=')-1), new ResourceLocation(resource));
                    } catch (Exception ignored) {
                        MusicTriggers.logExternally(Level.ERROR,"Resource location {} was invalid!", resource);
                    }
                }
                line = br.readLine();
            }
            br.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
