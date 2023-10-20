package hqrender;

import static hqrender.HqRender.LOG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hqrender.keybind.KeybindHQExportAll;
import hqrender.keybind.KeybindTest;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import org.lwjgl.opengl.GLContext;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import hqrender.export.ExportUtils;
import hqrender.export.ItemList;
import hqrender.keybind.KeybindExport;
import hqrender.keybind.KeybindWarn;

public class ClientProxy extends CommonProxy {

    static final String MODID = Tags.MODID;
    public static final int DEFAULT_MAIN_BLOCK_SIZE = 128;
    public static final int DEFAULT_GRID_BLOCK_SIZE = 32;
    public static final int DEFAULT_MAIN_ENTITY_SIZE = 512;
    public static final int DEFAULT_GRID_ENTITY_SIZE = 128;
    public static final int DEFAULT_PLAYER_SIZE = 1024;

    public static Configuration cfg;
    public static float renderScale = 1.0F;
    public static boolean gl32_enabled = false;
    public static int mainBlockSize = DEFAULT_MAIN_BLOCK_SIZE;
    public static int gridBlockSize = DEFAULT_GRID_BLOCK_SIZE;
    public static int mainEntitySize = DEFAULT_MAIN_ENTITY_SIZE;
    public static int gridEntitySize = DEFAULT_GRID_ENTITY_SIZE;
    public static int playerSize = DEFAULT_PLAYER_SIZE;
    public static boolean exportVanillaItems = true;
    public static boolean debugMode = false;
    public static List<String> blacklist = new ArrayList<String>();
    public static int TICK_PER_ITEMS = 100;
    public static String SAVE_ROOT_PLACE = "./export/";
    public static String FILE_SUFFIX = ".json";
    public static int SLEEP_TICK = 100;

    // Override CommonProxy methods here, if you want a different behaviour on the client (e.g. registering renders).
    // Don't forget to call the super methods as well.

    @Override
    public void preInit(FMLPreInitializationEvent event) { // 重写 FMLPreInitializationEvent 的 preInit 方法
        super.preInit(event); // 调用父类的 preInit 方法，进行必要的初始化
        // 检查当前环境是否是服务器端
        if (event.getSide()
            .isServer()) {
            LOG.error("Item Render is a client-only mod. Please remove this mod and restart your server."); // 如果是服务器端，输出错误信息并返回
            return;
        }
        // 检查是否支持 OpenGL 3.2
        // 获取 OpenGL 3.2 的支持情况，并存储在 gl32_enabled 变量中
        gl32_enabled = GLContext.getCapabilities().OpenGL32;
        // 配置文件相关
        // 获取建议的配置文件路径，并创建 Configuration 对象
        cfg = new Configuration(event.getSuggestedConfigurationFile());
        // 调用之前定义的 syncConfig 方法，用于从配置文件中读取设置
        mysyncConfig();
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) { // 重写FMLServerStartingEvent的serverStarting方法
        // 应该可以不用，暂时禁用掉
        // event.registerServerCommand(new CommandItemRender()); // 注册一个新的服务器命令CommandItemRender
    }

    @Override
    public void init(FMLInitializationEvent event) { // 重写FMLInitializationEvent的init方法
        super.init(event); // 调用父类的 init 方法，完成基础的初始化工作
        // 检查当前是否在服务器端
        if (event.getSide()
            .isServer()) {
            LOG.error("Item Render is a client-only mod. Please remove this mod and restart your server."); // 如果是服务器端，输出错误日志并返回
            return;
        }

        if (gl32_enabled) {
            ExportUtils.INSTANCE = new ExportUtils();

            FMLCommonHandler.instance()
                .bus()
                .register(new KeybindExport());

            FMLCommonHandler.instance()
                .bus()
                .register(new KeybindHQExportAll());

            FMLCommonHandler.instance()
                .bus()
                .register(new KeybindTest());

        } else {
            FMLCommonHandler.instance()
                .bus()
                .register(new KeybindWarn());
            LOG.error("[Item Render] OpenGL Error, please upgrade your drivers or system.");
        }
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event); // 调用父类的 postInit，如果有必要
        // 客户端专用的后初始化代码（如果有的话）

        if (event.getSide()
            .isClient()) ItemList.updateList();
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.modID.equals(Tags.MODID)) mysyncConfig();
    }

    private void mysyncConfig() {
        mainBlockSize = cfg.get(Configuration.CATEGORY_GENERAL, "RenderBlockMain", DEFAULT_MAIN_BLOCK_SIZE, I18n.format("itemrender.cfg.mainblock")).getInt();
        gridBlockSize = cfg.get(Configuration.CATEGORY_GENERAL, "RenderBlockGrid", DEFAULT_GRID_BLOCK_SIZE, I18n.format("itemrender.cfg.gridblock")).getInt();
        mainEntitySize = cfg.get(Configuration.CATEGORY_GENERAL, "RenderEntityMain", DEFAULT_MAIN_ENTITY_SIZE, I18n.format("itemrender.cfg.mainentity")).getInt();
        gridEntitySize = cfg.get(Configuration.CATEGORY_GENERAL, "RenderEntityGrid", DEFAULT_GRID_ENTITY_SIZE, I18n.format("itemrender.cfg.gridentity")).getInt();
        playerSize = cfg.get(Configuration.CATEGORY_GENERAL, "RenderPlayer", DEFAULT_PLAYER_SIZE, I18n.format("itemrender.cfg.player")).getInt();
        exportVanillaItems = cfg.get(Configuration.CATEGORY_GENERAL, "ExportVanillaItems", false, I18n.format("itemrender.cfg.vanilla")).getBoolean();
        debugMode = cfg.get(Configuration.CATEGORY_GENERAL, "DebugMode", false, I18n.format("itemrender.cfg.debug")).getBoolean();
        blacklist = Arrays.asList(cfg.get(Configuration.CATEGORY_GENERAL, "BlackList", new String[]{}, I18n.format("itemrender.cfg.blacklist")).getStringList());
        if (cfg.hasChanged())
            cfg.save();
        if (cfg.hasChanged()) cfg.save();
    }
}
