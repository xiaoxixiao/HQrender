package hqrender.globalstate;

import com.google.gson.Gson;
import hqrender.export.HQItemInfo;
import hqrender.export.ItemData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.Language;
import net.minecraft.item.ItemStack;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static hqrender.ClientProxy.SLEEP_TICK;

public class GlobalState {
    public static int currentListIndex = 0;
    public static boolean isProcessingItems = false;
    public static List<List<ItemStack>> nestedItemList;
    public static int sleepTick = SLEEP_TICK;
    public static Minecraft minecraft;
    public static List<HQItemInfo> itemInfoList;
    public static List<String> modList;
    public static Language savedLang;
    public static Gson gson;
    public static HQItemInfo itemData;
    public static String identifier;
    public static boolean getBase64;
    public static PrintWriter pw = null;
    public static List<HQItemInfo> itemListForJson;
    public static File export;
    public static boolean isCreatePW = false;
}
