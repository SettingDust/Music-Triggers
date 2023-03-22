package mods.thecomputerizer.musictriggers.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import mods.thecomputerizer.musictriggers.MusicTriggers;
import mods.thecomputerizer.musictriggers.client.ClientEvents;
import mods.thecomputerizer.musictriggers.client.gui.instance.Instance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.util.TriConsumer;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class GuiSuperType extends Screen {

    protected final GuiSuperType parent;
    protected final GuiType type;
    private final Instance configInstance;
    private final List<ButtonSuperType> superButtons;
    private final String channel;
    private ButtonSuperType applyButton;
    protected int spacing;
    protected TextFieldWidget searchBar;

    public GuiSuperType(GuiSuperType parent, GuiType type, Instance configInstance) {
        this(parent,type,configInstance,null);
    }

    public GuiSuperType(GuiSuperType parent, GuiType type, Instance configInstance, @Nullable String channel) {
        super(new StringTextComponent(type.getId()));
        this.parent = parent;
        this.type = type;
        this.configInstance = configInstance;
        this.channel = channel;
        this.superButtons = new ArrayList<>();
        this.spacing = 16;
    }

    public Instance getInstance() {
        return this.configInstance;
    }

    public GuiSuperType getParent() {
        return this.parent;
    }

    public String getChannel() {
        return this.channel;
    }

    public Vector4f white(int alpha) {
        return new Vector4f(255,255,255,alpha);
    }

    public Vector4f black(int alpha) {
        return new Vector4f(0,0,0,alpha);
    }

    @Override
    public boolean keyPressed(int keyCode, int x, int y) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if(!getInstance().hasEdits()) {
                this.onClose();
            } else this.getMinecraft().setScreen(new GuiPopUp(this,GuiType.POPUP,getInstance(),"confirm"));
            return true;
        }
        boolean ret = this.searchBar.keyPressed(keyCode, x, y);
        if(ret) updateSearch();
        return ret;
    }

    @Override
    public boolean charTyped(char c, int mod) {
        boolean ret = this.searchBar.charTyped(c, mod);
        if(ret) updateSearch();
        return ret;
    }

    protected String backspace(String value) {
        if(!value.isEmpty()) return value.substring(0,value.length()-1);
        return value;
    }

    @Override
    public void init() {
        this.superButtons.clear();
        switch (Minecraft.getInstance().options.guiScale) {
            case 0: {
                this.spacing = 10;
                break;
            }
            case 1: {
                this.spacing = 24;
                break;
            }
            case 2: {
                this.spacing = 16;
                break;
            }
            case 3: {
                this.spacing = 12;
                break;
            }
        }
        ClientEvents.SHOULD_RENDER_DEBUG = false;
        for (ButtonType buttonHolder : this.type.getButtonHolders()) {
            if (buttonHolder.isNormal()) {
                ButtonSuperType button = buttonHolder.getNormalButton(this);
                if(buttonHolder.getID().contains("apply"))
                    this.applyButton = button;
                addSuperButton(button);
            }
        }
        if(this.configInstance.hasEdits() && Objects.nonNull(this.applyButton)) this.applyButton.setEnable(true);
        this.searchBar = new TextFieldWidget(this.font, this.width / 4, 8,this.width / 2, 16,
                new StringTextComponent("search_bar"));
        this.searchBar.setMaxLength(32500);
        this.searchBar.setVisible(false);
        this.searchBar.setEditable(false);
        this.searchBar.setValue("");
        this.children.add(this.searchBar);
    }

    protected void enableSearch() {
        this.searchBar.setEditable(true);
        this.searchBar.setVisible(true);
    }

    protected boolean checkSearch(String toCheck) {
        return toCheck.toLowerCase().contains(this.searchBar.getValue().toLowerCase());
    }

    protected void updateSearch() {

    }

    public ButtonSuperType createBottomButton(String name, int width, int modes, List<String> hoverText,
                                                 TriConsumer<GuiSuperType, ButtonSuperType, Integer> handler) {
        return new ButtonSuperType(0, this.height-24,width,16, modes,name,
                hoverText, handler,true);
    }

    private void addSuperButton(ButtonSuperType button) {
        this.superButtons.add(button);
    }

    protected void addSuperButton(ButtonSuperType button, int x) {
        if(x>=0) button.x=x;
        this.superButtons.add(button);
    }

    public void madeChange(boolean needReload) {
        if(!this.applyButton.active) {
            recursivelySetApply(this);
            getInstance().madeChanges(needReload);
        }
    }

    private void recursivelySetApply(GuiSuperType superScreen) {
        this.applyButton.setEnable(true);
        if(Objects.nonNull(superScreen.parent)) recursivelySetApply(superScreen.parent);
    }

    protected abstract void drawStuff(MatrixStack matrix, int mouseX, int mouseY, float partialTicks);

    @Override
    public void render(@Nonnull MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrix);
        drawStuff(matrix, mouseX, mouseY, partialTicks);
        for(ButtonSuperType superButton : this.superButtons)
            superButton.render(matrix,mouseX,mouseY,partialTicks);
        this.searchBar.render(matrix,mouseX,mouseY,partialTicks);
        for(ButtonSuperType superButton : this.superButtons) {
            if(Minecraft.getInstance().screen == this) {
                List<ITextComponent> hoverText = superButton.getHoverText(mouseX,mouseY);
                if(!hoverText.isEmpty()) renderComponentTooltip(matrix, hoverText, mouseX, mouseY);
            }
        }
    }

    public boolean mouseHover(Vector2f topLeft, int mouseX, int mouseY, int width, int height) {
        return mouseX>=topLeft.x && mouseX<topLeft.x+width && mouseY>=topLeft.y && mouseY<topLeft.y+height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if(this.searchBar.mouseClicked(mouseX, mouseY, mouseButton)) return true;
        boolean ret = false;
        if (mouseButton == 0)
            for (ButtonSuperType superButton : this.superButtons)
                if(superButton.handle(this, mouseX, mouseY))
                    ret = true;
        return ret;
    }

    public void saveAndDisplay(GuiSuperType next) {
        save();
        this.getMinecraft().setScreen(next);
    }

    public void parentUpdate() {
        madeChange(true);
    }

    protected abstract void save();

    public void saveAndClose(boolean reload) {
        save();
        if(reload) {
            if(this.configInstance.hasEdits())
                applyButton();
            MusicTriggers.logExternally(Level.INFO,"No in-game changes were detected - Loading file changes");
            ClientEvents.initReload();
            this.onClose();
        } else this.getMinecraft().setScreen(this.parent);
    }

    public void applyButton() {
        save();
        if(Objects.nonNull(this.parent)) {
            Minecraft.getInstance().setScreen(this.parent);
            this.parent.applyButton();
        }
        else {
            this.getInstance().writeAndReload();
            this.onClose();
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        ClientEvents.SHOULD_RENDER_DEBUG = true;
    }

    public void playGenericClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
