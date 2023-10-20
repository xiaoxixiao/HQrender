package hqrender.keybind;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
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
        // 获取物品纹理地图
        TextureMap itemTextureMap = Minecraft.getMinecraft().getTextureMapBlocks();

        // 随机挑选一个物品纹理
        IIcon randomIcon = getRandomItemIcon(itemTextureMap);

        // 这里假设你已经有了渲染 IIcon 到 framebuffer 并保存为 PNG 的函数
        renderIconToFramebufferAndSave(randomIcon);
    }

    private IIcon getRandomItemIcon(TextureMap itemTextureMap) {
        ArrayList<Item> items = new ArrayList<>(GameData.getItemRegistry().getKeys());
        // 创建一个 Random 对象
        Random random = new Random();
        // 随机选择一个物品
        Item randomItem = items.get(random.nextInt(items.size()));
        // 获取该物品的IIcon
        return randomItem.getIconFromDamage(0);
    }

    public void renderIconToFramebufferAndSave(IIcon icon) {
        Minecraft mc = Minecraft.getMinecraft();
        int textureWidth = 16;  // 假设纹理是 16x16 的
        int textureHeight = 16;

        // 创建一个 framebuffer
        int framebufferId = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferId);

        // 创建一个纹理来储存 framebuffer 的数据
        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, textureWidth, textureHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);

        // 把纹理绑定到 framebuffer
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, textureId, 0);

        // 设置 OpenGL 参数
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        // 绑定物品纹理
        mc.getTextureManager().bindTexture(TextureMap.locationItemsTexture);

        // 渲染 IIcon 到 framebuffer
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(icon.getMinU(), icon.getMinV());
        GL11.glVertex2f(0, 0);
        GL11.glTexCoord2f(icon.getMaxU(), icon.getMinV());
        GL11.glVertex2f(textureWidth, 0);
        GL11.glTexCoord2f(icon.getMaxU(), icon.getMaxV());
        GL11.glVertex2f(textureWidth, textureHeight);
        GL11.glTexCoord2f(icon.getMinU(), icon.getMaxV());
        GL11.glVertex2f(0, textureHeight);
        GL11.glEnd();

        // 从 framebuffer 中读取数据
        ByteBuffer buffer = ByteBuffer.allocateDirect(textureWidth * textureHeight * 4);
        GL11.glReadPixels(0, 0, textureWidth, textureHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        // 将 ByteBuffer 转换为 BufferedImage
        BufferedImage image = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < textureHeight; y++) {
            for (int x = 0; x < textureWidth; x++) {
                int i = (y * textureWidth + x) * 4;
                int r = buffer.get(i) & 0xFF;
                int g = buffer.get(i + 1) & 0xFF;
                int b = buffer.get(i + 2) & 0xFF;
                int a = buffer.get(i + 3) & 0xFF;
                image.setRGB(x, textureHeight - y - 1, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        // 保存 BufferedImage 为 PNG 文件
        File outputfile = new File("saved_icon.png");
        try {
            ImageIO.write(image, "png", outputfile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 删除纹理和 framebuffer
        GL11.glDeleteTextures(textureId);
        GL30.glDeleteFramebuffers(framebufferId);
    }
}
