package hqrender.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Random;

public class RenderHandler {
    private ItemStack currentItem = null;

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (currentItem != null) {
            renderItemOnScreen(currentItem);
        }
    }

    public void setRandomItem(Item item) {
        currentItem = new ItemStack(item);
    }

    public Item hqGetRandomItem() {
        Random rand = new Random();
        Object[] items = Item.itemRegistry.getKeys().toArray();
        return (Item) Item.itemRegistry.getObject(items[rand.nextInt(items.length)]);
    }

    private void renderItemOnScreen(ItemStack item) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderItem renderItem = new RenderItem();

        GL11.glPushMatrix();

        // 设置渲染的位置和大小
        int x = mc.displayWidth / 2;
        int y = mc.displayHeight / 2;

        // 缩放、平移等都可以在这里进行，例如:
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(2.0F, 2.0F, 2.0F); // 这将使物品图标放大两倍

        // 使用Minecraft的封装方法来渲染物品图标
        renderItem.renderItemIntoGUI(mc.fontRenderer, mc.getTextureManager(), item, 0, 0);

        GL11.glPopMatrix();
    }
}
