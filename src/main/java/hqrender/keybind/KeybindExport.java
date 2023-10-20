package hqrender.keybind;

import java.io.IOException;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import hqrender.export.ExportUtils;
import hqrender.globalstate.GlobalState;
import hqrender.render.FBOHelper;

public class KeybindExport {

    public final KeyBinding key;
    public FBOHelper fbo;

    public KeybindExport() {
        key = new KeyBinding(I18n.format("itemrender.key.export"), Keyboard.KEY_I, "Item Render");
        ClientRegistry.registerKeyBinding(key);
    }

    @SubscribeEvent // 订阅事件
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        // 如果当前打开的是聊天界面，那么直接返回，不执行后面的代码
        if (FMLClientHandler.instance()
            .isGUIOpen(GuiChat.class)) return;

        // 检查特定的按键是否被按下
        if (key.isPressed()) {
            try {
                // 如果按键被按下，尝试执行 exportMods 方法
                ExportUtils.INSTANCE.exportMods();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
