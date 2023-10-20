package hqrender.keybind;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameData;
import hqrender.cient.HQMessage;
import hqrender.render.RenderHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static hqrender.cient.HQMessage.HqMessage;

public class KeybindTest {
    public final KeyBinding key;

    public KeybindTest() {
        key = new KeyBinding(I18n.format("itemrender.key.test"), Keyboard.KEY_NUMPAD8, "Item Render");
        ClientRegistry.registerKeyBinding(key);
    }

    @SubscribeEvent // 订阅事件
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        // 如果当前打开的是聊天界面，那么直接返回，不执行后面的代码
        if (FMLClientHandler.instance()
            .isGUIOpen(GuiChat.class)) return;

        // 检查特定的按键是否被按下
        if (key.isPressed()){
            ItemRender();
        }
    }

    public void ItemRender() {
        RenderHandler render = new RenderHandler();
        // 随机挑选一个物品纹理
        Item randomItem = render.hqGetRandomItem();
        // 设置渲染物品并保持渲染
        render.setRandomItem(randomItem);

        // 获取物品的名称
        String ItemUnlocalizedName = randomItem.getUnlocalizedName();

        HqMessage("抽取到了", ItemUnlocalizedName);

    }
}
