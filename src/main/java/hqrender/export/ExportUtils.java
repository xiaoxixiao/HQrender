package hqrender.export;

import static hqrender.HqRender.LOG;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Language;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import hqrender.ClientProxy;
import hqrender.render.FBOHelper;
import hqrender.render.Renderer;

public class ExportUtils {

    public static ExportUtils INSTANCE;
    private FBOHelper fboSmall;
    private FBOHelper fboLarge;
    private RenderItem itemRenderer = new RenderItem();
    private List<ItemData> itemDataList = new ArrayList();

    public ExportUtils() {
        this.fboSmall = new FBOHelper(32);
        this.fboLarge = new FBOHelper(128);
    }

    public String getLocalizedName(ItemStack itemStack) {
        return itemStack.getDisplayName();
    }

    public String getType(ItemStack itemStack) {
        return (itemStack.getItem() instanceof ItemBlock) ? "Block" : "Item";
    }

    public String getSmallIcon(ItemStack itemStack) {
        return Renderer.getItemBase64(itemStack, this.fboSmall, this.itemRenderer);
    }

    public String getLargeIcon(ItemStack itemStack) {
        return Renderer.getItemBase64(itemStack, this.fboLarge, this.itemRenderer);
    }

    public String getItemOwner(ItemStack itemStack) {
        GameRegistry.UniqueIdentifier uniqueIdentity = GameRegistry.findUniqueIdentifierFor(itemStack.getItem());
        return uniqueIdentity == null ? "unnamed" : uniqueIdentity.modId;
    }

    public void exportMods() throws IOException {
        // 获取 Minecraft 客户端的实例并初始化列表
        Minecraft minecraft = FMLClientHandler.instance()
            .getClient();
        // 清除 itemDataList，为新的数据获取做准备
        itemDataList.clear();
        // 创建一个用于存放 mod ID 的列表
        List<String> modList = new ArrayList<String>();
        // 获取当前游戏使用的语言
        Language lang = minecraft.getLanguageManager()
            .getCurrentLanguage();

        // 初始化 Gson 对象，用于后续的 JSON 转换
        // 禁用 HTML 转义，并开启 pretty printing 格式化输出
        Gson gson = new GsonBuilder().disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
        // 创建 ItemData 和 identifier 变量，但还未初始化
        ItemData itemData = null;
        String identifier = null;

        getAllItemDataImage(identifier, itemData, modList);

        // 切换到简体中文语言环境
        // 获取当前语言设置
        Language currentLanguage = minecraft.getLanguageManager()
            .getCurrentLanguage();
        String currentLangCode = currentLanguage.getLanguageCode(); // 或者使用你需要的方法获取当前语言代码
        // 检查当前语言是否为中文
        if (!"zh_CN".equals(currentLangCode)) {
            // 切换到简体中文语言环境
            minecraft.getLanguageManager()
                .setCurrentLanguage(new Language("zh_CN", "中国", "简体中文", false));
            // 更新游戏设置以反映新的语言
            minecraft.gameSettings.language = "zh_CN";
            // 刷新游戏资源以应用新的语言设置
            minecraft.refreshResources();
            // 保存更改后的游戏设置
            minecraft.gameSettings.saveOptions();
        }
        // 遍历 itemDataList 中的每一个元素（这里是 ItemData 对象）
        for (ItemData data : itemDataList) {
            // 如果 debug 模式启用了，记录当前物品的相关信息
            if (ClientProxy.debugMode) LOG.info(
                "Adding Chinese name for " + data.getItemStack()
                    .getUnlocalizedName()
                    + "@"
                    + data.getItemStack()
                        .getItemDamage());
            // 调用 setName 方法来设置该物品的中文名称
            data.setName(this.getLocalizedName(data.getItemStack()));
            // 调用 setCreativeName 方法来设置该物品在创造模式标签页中的名称
            data.setCreativeName(getCreativeTabName(data));
        }

        // 切换到英文（美国）环境
        minecraft.getLanguageManager()
            .setCurrentLanguage(new Language("en_US", "US", "English", false));
        // 设置游戏语言设置为英文（美国）
        minecraft.gameSettings.language = "en_US";
        // 刷新游戏资源，以应用新的语言设置
        minecraft.refreshResources();
        // 禁用 Unicode 标志，一般用于字体渲染
        minecraft.fontRenderer.setUnicodeFlag(false);
        // 保存游戏设置
        minecraft.gameSettings.saveOptions();
        // 遍历 itemDataList 中的每一个元素（这里是 ItemData 对象）
        for (ItemData data : itemDataList) {
            // 如果 debug 模式启用了，记录当前物品的相关信息
            if (ClientProxy.debugMode) LOG.info(
                "Adding English name for " + data.getItemStack()
                    .getUnlocalizedName()
                    + "@"
                    + data.getItemStack()
                        .getItemDamage());
            // 调用 setEnglishName 方法来设置该物品的英文名称
            data.setEnglishName(this.getLocalizedName(data.getItemStack()));
        }

        // 将数据保存为 JSON 文件
        File export; // 创建一个 File 对象用于后续的文件操作
        for (String modid : modList) { // 遍历模组列表
            // 创建一个 ArrayList，用于临时存储与当前模组 modid 相关的 ItemData 对象
            List<ItemData> itemListForJson = new ArrayList<>();
            // 初始化 File 对象，指向要保存的 JSON 文件路径
            export = new File(
                minecraft.mcDataDir,
                String.format("export/" + modid + "_item.json", modid.replaceAll("[^A-Za-z0-9()\\[\\]]", "")));
            // 检查文件夹是否存在，如果不存在则创建
            if (!export.getParentFile()
                .exists()) {
                export.getParentFile()
                    .mkdirs();
            }
            // 检查文件是否存在，如果不存在则创建新文件
            if (!export.exists()) {
                export.createNewFile();
            }
            // 遍历所有的 ItemData 对象
            for (ItemData data : itemDataList) {
                // 如果 ItemData 对象属于当前遍历到的模组，则添加到 itemListForJson 列表中
                if (modid.equals(getItemOwner(data.getItemStack()))) {
                    itemListForJson.add(data);
                }
            }
            // 使用 Gson 库将 itemListForJson 列表转换为 JSON 格式的字符串
            String jsonStr = gson.toJson(itemListForJson);
            // 创建 PrintWriter 对象，并指定文件编码为 "UTF-8"
            PrintWriter pw = new PrintWriter(export, "UTF-8");
            // 将转换后的 JSON 字符串写入文件
            pw.println(jsonStr);
            // 关闭 PrintWriter，完成文件写入
            pw.close();
        }

        // 还原原始语言设置并刷新资源
        minecraft.getLanguageManager()
            .setCurrentLanguage(lang); // 将游戏语言设置回原始语言（lang 对象存储了原始语言信息）
        minecraft.gameSettings.language = lang.getLanguageCode(); // 将游戏的语言设置更新为原始语言的代码（例如："en_US"）
        minecraft.refreshResources(); // 刷新游戏资源，使新的语言设置生效
        minecraft.gameSettings.saveOptions(); // 保存游戏设置，以确保更改是持久的
    }

    private void getAllItemDataImage(String identifier, ItemData itemData, List<String> modList) {
        // 遍历所有的游戏物品
        for (ItemStack itemStack : ItemList.items) {
            // 跳过空的物品
            if (itemStack == null) continue;
            // 如果物品来自原版Minecraft，并且设置中不允许导出原版物品，则跳过
            if (getItemOwner(itemStack).equals("minecraft") && !ClientProxy.exportVanillaItems) continue;
            // 构建物品的标识符，通常是物品的非本地化名称与其损耗值（damage value）的组合
            identifier = itemStack.getUnlocalizedName() + "@" + itemStack.getItemDamage();
            // 如果该物品在黑名单内，则跳过
            if (ClientProxy.blacklist.contains(identifier)) continue;
            // 创建一个ItemData对象来保存物品的数据，并添加到itemDataList列表
            itemData = new ItemData(itemStack);
            itemDataList.add(itemData);

            // 如果物品所属的模组ID还没有被添加到modList列表中，则添加
            if (!modList.contains(getItemOwner(itemStack))) modList.add(getItemOwner(itemStack));
        }
    }

    private String getCreativeTabName(ItemData data) {
        if (data.getItemStack()
            .getItem()
            .getCreativeTab() != null) {
            return I18n.format(
                data.getItemStack()
                    .getItem()
                    .getCreativeTab()
                    .getTranslatedTabLabel(),
                new Object[0]);
        } else {
            return "";
        }
    }
}
