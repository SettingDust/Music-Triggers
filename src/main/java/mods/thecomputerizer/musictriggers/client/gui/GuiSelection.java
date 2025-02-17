package mods.thecomputerizer.musictriggers.client.gui;

import mods.thecomputerizer.musictriggers.client.Translate;
import mods.thecomputerizer.musictriggers.client.audio.ChannelManager;
import mods.thecomputerizer.musictriggers.client.gui.instance.Instance;
import mods.thecomputerizer.theimpossiblelibrary.util.client.GuiUtil;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.util.BiConsumer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.vecmath.Point2i;
import javax.vecmath.Point4i;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GuiSelection extends GuiSuperType {

    private final Supplier<List<Element>> elementSupplier;
    private List<Element> elementCache;
    private final Consumer<List<Element>> multiSelectHandler;
    private final List<Element> searchedElements;
    private final List<Element> selectedElements;
    private final String customTitle;
    private final boolean canDelete;
    private final boolean canSort;
    private final boolean canMultiSelect;
    private final ButtonSuperType[] bottomButtons;
    private int numElements;
    private int verticalSpace;
    private int scrollPos;
    private boolean canScrollDown;
    private int elementHover;
    private int sortType;
    private boolean deleteMode;
    private boolean hasEdits;

    public GuiSelection(GuiSuperType parent, GuiType type, Instance configInstance, String customTitle, boolean canDelete,
                        boolean canSort, Supplier<List<Element>> elementSupplier, ButtonSuperType ... bottomButtons) {
        this(parent,type,configInstance,customTitle,canDelete,canSort,false,elementSupplier,null,bottomButtons);
    }

    public GuiSelection(GuiSuperType parent, GuiType type, Instance configInstance, String customTitle,
                        boolean canDelete, boolean canSort, boolean canMultiSelect,
                        Supplier<List<Element>> elementSupplier, Consumer<List<Element>> multiSelectHandler,
                        ButtonSuperType ... bottomButtons) {
        super(parent, type, configInstance);
        this.elementSupplier = elementSupplier;
        this.multiSelectHandler = multiSelectHandler;
        this.searchedElements = new ArrayList<>();
        this.selectedElements = new ArrayList<>();
        this.customTitle = customTitle;
        this.canDelete = canDelete;
        this.canSort = canSort;
        this.canMultiSelect = canMultiSelect;
        this.bottomButtons = bottomButtons;
        this.elementHover = -1;
        this.deleteMode = false;
        this.sortType = 0;
    }

    private void calculateScrollSize() {
        this.scrollPos = 0;
        int textSlot = mc.fontRenderer.FONT_HEIGHT+(this.spacing*2);
        int totalHeight = this.height-((this.spacing*3)+textSlot+48);
        int runningHeight = (textSlot*2)-this.spacing;
        int runningTotal = 1;
        while(runningHeight+textSlot<totalHeight) {
            runningTotal++;
            runningHeight+=textSlot;
        }
        this.numElements = runningTotal;
        this.canScrollDown = this.numElements < this.searchedElements.size();
        this.verticalSpace = (totalHeight-runningHeight)/2;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.elementCache = this.elementSupplier.get();
        enableSearch();
        int index = Integer.MIN_VALUE;
        for (Element element : this.elementCache) {
            if (index == Integer.MIN_VALUE)
                index = element.getIndex();
            else index = element.adjustOriginalIndex(index);
        }
        String displayName;
        int width;
        int left = 16;
        if(this.canDelete) {
            displayName = Translate.guiGeneric(false, "button", "delete_mode");
            width = this.fontRenderer.getStringWidth(displayName) + 8;
            String finalDisplayName = displayName;
            addSuperButton(createBottomButton(displayName, width, 2,
                    Translate.guiNumberedList(3, "button", "delete_mode", "desc"),
                    (screen, button, mode) -> {
                        TextFormatting color = mode == 1 ? TextFormatting.WHITE : TextFormatting.RED;
                        this.deleteMode = color==TextFormatting.RED;
                        button.updateDisplay(color + finalDisplayName,this.fontRenderer);
                    }), left);
            left += (width + 16);
        }
        if(this.canSort) {
            displayName = Translate.guiGeneric(false, "button", "sort", "original");
            width = this.fontRenderer.getStringWidth(displayName) + 8;
            addSuperButton(createBottomButton(displayName, width, 3,
                    Translate.guiNumberedList(3, "button", "sort", "desc"),
                    (screen, button, mode) -> {
                        this.sortType = mode - 1;
                        TextFormatting color = mode == 1 ? TextFormatting.WHITE : mode == 2 ? TextFormatting.GRAY : TextFormatting.DARK_GRAY;
                        button.updateDisplay(color + sortElements(),this.fontRenderer);
                    }), left);
            left += (width + 16);
        }
        for(ButtonSuperType button : this.bottomButtons) {
            addSuperButton(button,left);
            left += (button.width + 16);
        }
        updateSearch();
    }
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();
        if(scroll!=0) {
            if(scroll<1) {
                if (this.canScrollDown) {
                    this.scrollPos++;
                    this.canScrollDown = this.numElements + this.scrollPos + 1 < this.searchedElements.size();
                }
            } else if(this.scrollPos>0) {
                this.scrollPos--;
                this.canScrollDown = true;
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0 && this.elementHover>=0) {
            Element element = this.searchedElements.get(this.elementHover);
            if(this.deleteMode) {
                if(element.onDelete(mouseX<=this.width/2)) {
                    playGenericClickSound();
                    this.hasEdits = true;
                    this.searchedElements.remove(element);
                    this.elementCache.remove(element);
                    save();
                }
            } else {
                if (this.canMultiSelect) {
                    if (this.selectedElements.contains(element)) {
                        this.selectedElements.remove(element);
                        element.setSelected(false);
                    } else {
                        this.selectedElements.add(element);
                        element.setSelected(true);
                    }
                }
                else {
                    for (Element element1 : searchedElements)
                        element1.onClick(this, mouseX <= this.width / 2);
                }
            }
        }
    }

    @Override
    protected void keyTyped(char c, int key) {
        super.keyTyped(c, key);
        if(isKeyValid(c, key))
            for(Element element : this.searchedElements) {
                if(element.onType(key == Keyboard.KEY_BACK, c)) this.hasEdits = true;
            }
        updateSearch();
    }

    @Override
    protected void updateSearch() {
        this.searchedElements.clear();
        for(Element element : this.elementCache)
            if(checkSearch(element))
                this.searchedElements.add(element);
        calculateScrollSize();
    }

    private boolean checkSearch(Element element) {
        if(element instanceof MonoElement)
            return checkSearch(element.getDisplay(false));
        else return checkSearch(element.getDisplay(true)) || checkSearch(element.getDisplay(false));
    }

    public String sortElements() {
        if(this.sortType<=0) {
            this.searchedElements.sort(Comparator.comparingInt(Element::getIndex));
            return Translate.guiGeneric(false,"button","sort","original");
        }
        this.searchedElements.sort(Comparator.comparing(element -> element.getDisplay(true)));
        if(this.sortType>1) {
            Collections.reverse(this.searchedElements);
            return Translate.guiGeneric(false,"button","sort","reverse");
        }
        return Translate.guiGeneric(false,"button","sort","alphabetical");
    }

    @Override
    protected void drawStuff(int mouseX, int mouseY, float partialTicks) {
        MutableInt top = new MutableInt(this.spacing+24);
        GuiUtil.drawBox(new Point2i(0,top.getValue()),this.width,this.height-this.spacing*2-48,black(196),this.zLevel);
        Point2i start = new Point2i(0, top.getValue());
        Point2i end = new Point2i(this.width, top.getValue());
        GuiUtil.drawLine(start,end,white(192), 1f, this.zLevel);
        drawCenteredString(this.fontRenderer,this.customTitle,this.width/2,top.addAndGet(this.spacing),GuiUtil.WHITE);
        top.add(fontRenderer.FONT_HEIGHT+this.spacing+this.verticalSpace);
        boolean hoverAny = false;
        int index = 0;
        int bottom = this.height-(this.spacing+24);
        for(Element element : this.searchedElements) {
            if(index>=this.scrollPos) {
                if(element.renderElement(this,this.fontRenderer,mouseX,mouseY,top,this.spacing,this.zLevel)) {
                    hoverAny = true;
                    this.elementHover = index;
                }
                if ((bottom - top.getValue()) < (this.spacing + this.fontRenderer.FONT_HEIGHT)) break;
            }
            index++;
        }
        if(!hoverAny) this.elementHover = -1;
        start.setY(this.height-this.spacing-24);
        end.setY(this.height-this.spacing-24);
        GuiUtil.drawLine(start,end,white(192), 1f, this.zLevel);
        if(this.searchedElements.size()>this.numElements) drawScrollBar();
        if(hoverAny) {
            for (Element element : this.searchedElements) {
                if(element==this.searchedElements.get(this.elementHover)) {
                    List<String> hoverLines = element.getHoverLines(mouseX <= this.width / 2);
                    if(!hoverLines.isEmpty()) drawHoveringText(hoverLines, mouseX, mouseY);
                }
            }
        }
    }

    private void drawScrollBar() {
        float height = this.height-(this.spacing*2)-48;
        float indices = this.searchedElements.size()-this.numElements;
        float perIndex = height/indices;
        int top = (int)(24+spacing+(perIndex*this.scrollPos));
        int x = this.width-1;
        Point2i start = new Point2i(x, top);
        if(perIndex<1) perIndex = 1;
        Point2i end = new Point2i(x, (int)(top+perIndex));
        GuiUtil.drawLine(start,end,white(192), 2f, this.zLevel);
    }

    @Override
    public void parentUpdate() {
        super.parentUpdate();
        if(Objects.nonNull(this.multiSelectHandler)) this.multiSelectHandler.accept(this.selectedElements);
        save();
        this.elementCache = this.elementSupplier.get();
        updateSearch();
    }

    @Override
    protected void save() {
        for(Element element: this.elementCache) element.onSave();
        if(this.hasEdits)
            this.madeChange(true);
        updateSearch();
    }

    public static abstract class Element {
        private int index;
        protected boolean isSelected;

        protected boolean hover;

        public Element(int index) {
            this.index = index;
            this.isSelected = false;
        }

        public int adjustOriginalIndex(int previous) {
            if(this.index<=previous) this.index = previous+1;
            return this.index;
        }

        public int getIndex() {
            return this.index;
        }

        public void setSelected(boolean selected) {}

        public boolean isHover() {
            return this.hover;
        }

        public abstract boolean renderElement(GuiSuperType parent, FontRenderer font, int mouseX, int mouseY,
                                              MutableInt top, int spacing, float zLevel);

        public abstract String getDisplay(boolean isLeft);

        public abstract List<String> getHoverLines(boolean isLeft);

        public abstract boolean onType(boolean backspace, char c);

        public abstract void onClick(GuiSelection parent, boolean isLeft);

        public abstract boolean onDelete(boolean isLeft);

        public abstract void onSave();
    }

    public static class MonoElement extends Element {
        private final String id;
        private final String display;
        private final List<String> hoverText;
        private final Consumer<GuiSelection> onClick;
        private final Consumer<String> onDelete;
        private boolean multiSelect;

        public MonoElement(String id, int index, String display, Consumer<String> onDelete) {
            this(id,index,display,new ArrayList<>(),null,onDelete);
        }

        public MonoElement(String id, int index, String display, List<String> hoverText) {
            this(id,index,display,hoverText,null,null);
        }

        public MonoElement(String id, int index, String display, List<String> hoverText, Consumer<GuiSelection> onClick) {
            this(id,index,display,hoverText,onClick,null);
        }

        public MonoElement(String id, int index, String display, List<String> hoverText, Consumer<GuiSelection> onClick,
                           Consumer<String> onDelete) {
            super(index);
            this.id = id;
            this.display = display;
            this.hoverText = hoverText;
            this.onClick = onClick;
            this.onDelete = onDelete;
            this.multiSelect = false;
        }

        public String getID() {
            return this.id;
        }

        @Override
        public void setSelected(boolean selected) {
            this.multiSelect = selected;
        }

        @Override
        public boolean renderElement(GuiSuperType parent, FontRenderer font, int mouseX, int mouseY, MutableInt top,
                                     int spacing, float zLevel) {
            boolean hover = parent.mouseHover(new Point2i(0, top.getValue()),mouseX,mouseY,
                    parent.width,font.FONT_HEIGHT+spacing*2);
            boolean isLeft = mouseX<=parent.width/2;
            int textColor = GuiUtil.WHITE;
            if (hover || this.multiSelect) {
                textColor = GuiUtil.makeRGBAInt(200, 200, 200, 255);
                GuiUtil.drawBox(new Point2i(0, top.getValue()), parent.width, font.FONT_HEIGHT+spacing*2,
                        new Point4i(64, 64, 46, 96), zLevel);
            }
            parent.drawCenteredString(font,getDisplay(isLeft),parent.width/2, top.addAndGet(spacing), textColor);
            top.add(spacing+font.FONT_HEIGHT);
            this.hover = hover;
            return hover;
        }

        @Override
        public String getDisplay(boolean isLeft) {
            return this.display;
        }

        @Override
        public List<String> getHoverLines(boolean isLeft) {
            return this.hoverText;
        }

        @Override
        public boolean onType(boolean backspace, char c) {
            return false;
        }

        @Override
        public void onClick(GuiSelection parent, boolean isLeft) {
            if(this.hover && Objects.nonNull(this.onClick)) {
                this.onClick.accept(parent);
                parent.playGenericClickSound();
            }
            this.isSelected = hover;
        }

        @Override
        public boolean onDelete(boolean isLeft) {
            if(Objects.isNull(this.onDelete)) return false;
            this.onDelete.accept(this.id);
            return true;
        }

        @Override
        public void onSave() {}
    }

    public static class DualElement extends Element {
        private String key;
        private String val;
        private final List<String> hoverTextKey;
        private final List<String> hoverTextVal;
        private final Consumer<String> onDelete;
        private final BiConsumer<String, String> onSave;
        private boolean keySelected;

        public DualElement(String key, String val, int index, List<String> hoverTextKey, List<String> hoverTextVal,
                           Consumer<String> onDelete, BiConsumer<String, String> onSave) {
            super(index);
            this.key = key;
            this.val = val;
            this.hoverTextKey = hoverTextKey;
            this.hoverTextVal = hoverTextVal;
            this.onDelete = onDelete;
            this.onSave = onSave;
            this.keySelected = false;
        }

        @Override
        public boolean renderElement(GuiSuperType parent, FontRenderer font, int mouseX, int mouseY, MutableInt top,
                                     int spacing, float zLevel) {
            boolean hover = parent.mouseHover(new Point2i(0, top.getValue()),mouseX,mouseY,
                    parent.width,font.FONT_HEIGHT+spacing*2);
            boolean isLeft = mouseX<=parent.width/2;
            int keyColor = GuiUtil.WHITE;
            int valColor = GuiUtil.WHITE;
            char keyExtra = this.isSelected && this.keySelected ? ChannelManager.blinker : ' ';
            char valExtra = this.isSelected  && !this.keySelected ? ChannelManager.blinker : ' ';
            if (hover) {
                if(isLeft) {
                    keyColor = GuiUtil.makeRGBAInt(200, 200, 200, 255);
                    GuiUtil.drawBox(new Point2i(0, top.getValue()), parent.width/2, font.FONT_HEIGHT + spacing * 2,
                            new Point4i(64, 64, 46, 96), zLevel);
                } else {
                    valColor = GuiUtil.makeRGBAInt(200, 200, 200, 255);
                    GuiUtil.drawBox(new Point2i(parent.width/2, top.getValue()), parent.width/2, font.FONT_HEIGHT + spacing * 2,
                            new Point4i(64, 64, 46, 96), zLevel);
                }
            }
            parent.drawCenteredString(font,getDisplay(true)+keyExtra,parent.width/4, top.addAndGet(spacing), keyColor);
            parent.drawCenteredString(font,getDisplay(false)+valExtra,parent.width-(parent.width/4), top.getValue(), valColor);
            top.add(spacing+font.FONT_HEIGHT);
            this.hover = hover;
            return hover;
        }

        @Override
        public String getDisplay(boolean isLeft) {
            return isLeft ? this.key : this.val;
        }

        @Override
        public List<String> getHoverLines(boolean isLeft) {
            return isLeft ? this.hoverTextKey : this.hoverTextVal;
        }

        @Override
        public boolean onType(boolean backspace, char c) {
            if(this.isSelected) {
                if (this.keySelected) {
                    if (backspace) {
                        if (!this.key.isEmpty()) {
                            this.key = this.key.substring(0, this.key.length() - 1);
                            return true;
                        }
                    } else {
                        this.key += c;
                        return true;
                    }
                } else {
                    if (backspace) {
                        if (!this.val.isEmpty()) {
                            this.val = this.val.substring(0, this.val.length() - 1);
                            return true;
                        }
                    } else {
                        this.val += c;
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void onClick(GuiSelection parent, boolean isLeft) {
            this.isSelected = this.hover;
            this.keySelected = isLeft;
        }

        @Override
        public boolean onDelete(boolean isLeft) {
            if(Objects.isNull(this.onDelete)) return false;
            if(isLeft) {
                this.onDelete.accept(this.key);
                return true;
            }
            this.val = "";
            return false;
        }

        @Override
        public void onSave() {
            this.onSave.accept(this.key,this.val);
        }
    }
}
