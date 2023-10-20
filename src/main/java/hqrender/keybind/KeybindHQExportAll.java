package hqrender.keybind;

import com.google.gson.GsonBuilder;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import hqrender.ClientProxy;
import hqrender.export.HQItemInfo;
import hqrender.export.ItemList;
import hqrender.globalstate.GlobalState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.input.Keyboard;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static hqrender.ClientProxy.*;
import static hqrender.HqRender.LOG;
import static hqrender.globalstate.GlobalState.*;

public class KeybindHQExportAll {


    public final KeyBinding key;


    public KeybindHQExportAll() {
        key = new KeyBinding(I18n.format("itemrender.key.hqexport"), Keyboard.KEY_NUMPAD9, "Item Render");
        ClientRegistry.registerKeyBinding(key);
    }

    @SubscribeEvent // 订阅事件
    public void onKeyInput(InputEvent.KeyInputEvent event) throws FileNotFoundException, UnsupportedEncodingException {
        // 如果当前打开的是聊天界面，那么直接返回，不执行后面的代码
        if (FMLClientHandler.instance()
            .isGUIOpen(GuiChat.class)) return;

        // 检查特定的按键是否被按下
        if (key.isPressed()) {
            int totalItems = ItemList.items.size(); // 获取物品总数

            LOG.info("总数:"+totalItems+"\n");
            // 在聊天框输出
            String msg1 = "§6§l############################################################";
            String msg2 = "§6§l#      §c§l总数: §a" + totalItems + "                                    §6§l#";
            String msg3 = "§6§l############################################################";

            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg1));
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg2));
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg3));

            GlobalState.isProcessingItems = true; // 设置状态为正在处理

            LOG.info("\n\n开始处理\n\n");

            if (totalItems == 0) {
                // 列表为空
                LOG.warn("ItemList is empty!");
            }
            initWaitTick();
        }
    }

    public void initWaitTick() throws FileNotFoundException, UnsupportedEncodingException {
        // 获取所有的物品，得到一个列表
        List<ItemStack> allItems = ItemList.items;

        // 以 TICK_PER_ITEMS 个为单位分割列表，最后得到一个嵌套的列表
        nestedItemList = new ArrayList<>();
        int chunkSize = TICK_PER_ITEMS;
        for (int i = 0; i < allItems.size(); i += chunkSize) {
            int end = Math.min(allItems.size(), i + chunkSize);
            nestedItemList.add(allItems.subList(i, end));
        }
        minecraft = FMLClientHandler.instance().getClient();
        // 初始化用于存储这个列表物品数据的列表，清空数据
        if (itemInfoList != null) {
            itemInfoList.clear();
        } else if (itemInfoList == null) {
            itemInfoList = new ArrayList<>();
        }
        // 初始化modList
        modList = new ArrayList<String>();
        // 保存当前语言
        savedLang = minecraft.getLanguageManager().getCurrentLanguage();
        // 初始化Gson对象
        gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

        itemListForJson = new ArrayList<>();



    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) throws IOException {
        // 如果是正在处理状态
        if (GlobalState.isProcessingItems) {
            // 如果超出了范围
            if (currentListIndex >= nestedItemList.size() ) {
                // 设置为终止停止循环刻执行后面的逻辑
                isProcessingItems = false;
                // 初始化当前的列表标签
                currentListIndex = 0;
                String mcDir = Minecraft.getMinecraft().mcDataDir.getAbsolutePath();
                String exportDir = mcDir + File.separator + "export" + File.separator;

                String msg1 = "§6§l############################################################";
                String msg2 = "§6§l#      §c§l任务完成!      §6§l#";
                String msg3 = "§6§l#   §a§l导出位置:        §6§l#";
                String msg4 = "§6§l#     " + exportDir + "     §6§l#";
                String msg5 = "§6§l############################################################";

                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg1));
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg2));
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg3));
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg4));
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg5));

            } else {  // 如果在范围内
                // 如果休眠计数器大于0
                if (sleepTick > 0) {
                    // 递减
                    sleepTick--;
                    // 如果在范围内 且 休眠计数器递减到0
                }
            }
            if ( sleepTick == 0 && isProcessingItems) {
                LOG.info(currentListIndex +"index \n\nsize"+nestedItemList.size());
                // -----------------------------------------------------------------------------------------------------
                // ------------------------核心逻辑部位-------------------------------------------------------------------
                // -----------------------------------------------------------------------------------------------------
                // 重设休眠时间
                sleepTick = SLEEP_TICK;
                // 向前移动一位

                // 设置本次循环的文件保存
                String filePath = SAVE_ROOT_PLACE + currentListIndex + FILE_SUFFIX;
                //
                // 重要逻辑处理
                //
                // 处理当前 currentListIndex 序数下标的列表
                List<ItemStack> currentItemList = nestedItemList.get(currentListIndex);
                // 开始遍历当前列表
                for (ItemStack itemStack : currentItemList) {
                    identifier = itemStack.getUnlocalizedName() + "@" + itemStack.getItemDamage();
                    if (itemStack != null && !(getItemOwner(itemStack).equals("minecraft") && !ClientProxy.exportVanillaItems) && !(ClientProxy.blacklist.contains(identifier)));

                    // 初始化存储小列表的对象
                    itemData = new HQItemInfo(itemStack);
                    // 小列表存入大列表中
                    itemInfoList.add(itemData);
                    if (!modList.contains(getItemOwner(itemStack))) modList.add(getItemOwner(itemStack));
                }

                currentListIndex++;

                for (String modid : modList) {
                    // �����ﴴ��һ�� List ���ڴ�� ItemData ����
                    /*List<HQItemInfo> itemListForJson = new ArrayList<>();*/

                    export = new File(
                        minecraft.mcDataDir,
                        String.format("export/" + modid + "_item.json", modid.replaceAll("[^A-Za-z0-9()\\[\\]]", "")));
                    if (!export.getParentFile()
                        .exists()) {
                        export.getParentFile()
                            .mkdirs();
                    }
                    if (!export.exists()) {
                        export.createNewFile();
                    }
                    /* PrintWriter pw = new PrintWriter(export, "UTF-8"); */
                    if (!isCreatePW) {
                        pw = new PrintWriter(export, "UTF-8");
                        isCreatePW =true;
                    }

                    for (HQItemInfo data : itemInfoList) {
                        if (modid.equals(getItemOwner(data.getItemStack()))) {
                            itemListForJson.add(data);
                        }
                    }

                    // ʹ�� Gson ������ List ת��Ϊ JSON �ַ�������һ����д�뵽�ļ�
                    String jsonStr = gson.toJson(itemListForJson);
                    /*PrintWriter pw = new PrintWriter(export, "UTF-8");*/
                    pw.println(jsonStr);
                    pw.flush();  // 强制写入
                }
                if (currentListIndex+1 == nestedItemList.size() ) {
                    pw.close();
                    isCreatePW = false;
                }
            }
        }
    }

    public String getItemOwner(ItemStack itemStack) {
        GameRegistry.UniqueIdentifier uniqueIdentity = GameRegistry.findUniqueIdentifierFor(itemStack.getItem());
        return uniqueIdentity == null ? "unnamed" : uniqueIdentity.modId;
    }
}
            /*// 如果大于了 nestedItemList 列表 重置为初始状态
            if (currentListIndex >= nestedItemList.size()) {
                GlobalState.isProcessingItems = false;
                currentListIndex = 0;
                // 如果在范围内则进行处理
            } else {
                if (sleepTick > 0) {
                    sleepTick--;
                } else if (sleepTick == 0) {
                    // 重置休眠计时器
                    sleepTick = 40;
                    //
                    // 重要逻辑处理
                    //
                    // 定义该次 filePath 的位置
                    String filePath = SAVE_ROOT_PLACE + currentListIndex + FILE_SUFFIX;
                    // 处理当前 currentListIndex 序数下标的列表
                    List<ItemStack> currentItemList = nestedItemList.get(currentListIndex);
                    // 向前移动一位
                    currentListIndex++;

                    // 遍历
                    for (ItemStack itemStack : currentItemList) {
                        // 在这里对每个itemStack进行处理 -> 改为在ItemInfo中处理，只需传入 itemStack 对象即可

                        HQItemInfo itemInfo = new HQItemInfo(itemStack);
                        itemInfoList.add(itemInfo);
                    }

                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    BufferedWriter bufferedWriter = null;
                    try {
                        FileOutputStream fos = new FileOutputStream(filePath);
                        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");  // 指定编码为UTF-8
                        bufferedWriter = new BufferedWriter(osw);

                        gson.toJson(itemInfoList, bufferedWriter);
                        bufferedWriter.flush(); // 确保所有数据都被写入文件
                    } catch (IOException e) {
                        // 打印异常堆栈信息以供调试
                        e.printStackTrace();
                        LOG.error("写入文件失败： " + e.getMessage()); // 使用日志库记录错误信息
                    } finally {
                        try {
                            if (bufferedWriter != null) {
                                bufferedWriter.close();
                            }
                        } catch (IOException e) {
                            // 再次捕获异常并打印
                            e.printStackTrace();
                            LOG.error("关闭文件流失败： " + e.getMessage()); // 使用日志库记录错误信息
                        }
                    }
                }
            }*/
