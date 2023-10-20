package hqrender.cient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ChatComponentText;

public class HQMessage {

    public static void HqMessage(String title, String Message) {
        String baseMsg = "§6§l############################################";
        String content = "§6§l#      §c§l" + title + "§a" + Message;

        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(baseMsg));
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(content));
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(baseMsg));
    }
}
