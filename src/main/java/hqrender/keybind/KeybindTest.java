package hqrender.keybind;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import hqrender.render.FBOHelper;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;

import java.util.Random;

import static hqrender.cient.HQMessage.HqMessage;
import static hqrender.render.Renderer.renderItem;

public class KeybindTest {
    public final KeyBinding key;
    public FBOHelper fbo = new FBOHelper(128);
    public RenderItem renderItem = new RenderItem();

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
        ItemStack itemStackRandom = randomItem();
        // 获取物品的名称
        String ItemUnlocalizedName = itemStackRandom.getUnlocalizedName();

        HqMessage("抽取到了", ItemUnlocalizedName);

        renderItem(itemStackRandom, fbo, "", renderItem);
    }

    public ItemStack randomItem() {
        Random random = new Random();
        // 获取所有已注册的物品
        Object[] registeredItems = Item.itemRegistry.getKeys().toArray();
        // 从列表中随机选择一个物品
        Item randomItem = (Item) Item.itemRegistry.getObject(registeredItems[random.nextInt(registeredItems.length)]);
        // 销毁对象(伪)
        random = null;
        return new ItemStack(randomItem, 1);
    }
}
