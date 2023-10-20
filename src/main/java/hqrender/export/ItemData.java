package hqrender.export;

import static hqrender.HqRender.LOG;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.registry.GameData;
import hqrender.ClientProxy;

public class ItemData {

    private String name;
    private String englishName;
    private String registerName;
    private int metadata;
    private String OredictList;
    private String CreativeTabName;
    private String type;
    private int maxStackSize;
    private int maxDurability;
    private String smallIcon;
    private String largeIcon;
    private transient ItemStack itemStack;

    public ItemData(ItemStack itemStack) {
        if (ClientProxy.debugMode) {
            LOG.info("Processing " + itemStack.getUnlocalizedName() + "@" + itemStack.getItemDamage());
        }
        this.name = null;
        this.englishName = null;
        this.registerName = GameData.getItemRegistry()
            .getNameForObject(itemStack.getItem());
        List<String> list = new ArrayList<String>();
        metadata = itemStack.getItemDamage();
        if (!isEmpty(itemStack)) {
            for (int i : OreDictionary.getOreIDs(itemStack)) {
                String ore = OreDictionary.getOreName(i);
                list.add(ore);
            }
            OredictList = list.toString();
        }
        this.CreativeTabName = null;
        this.type = ExportUtils.INSTANCE.getType(itemStack);
        this.maxStackSize = itemStack.getMaxStackSize();
        this.maxDurability = (itemStack.getMaxDamage() + 1);
        this.smallIcon = ExportUtils.INSTANCE.getSmallIcon(itemStack);
        this.largeIcon = ExportUtils.INSTANCE.getLargeIcon(itemStack);
        this.itemStack = itemStack;
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public void setCreativeName(String name) {
        this.CreativeTabName = name;
    }

    private boolean isEmpty(ItemStack stack) {
        // 检查是否是一个null对象或者代表空气的ItemStack
        if (Objects.equals(stack, new ItemStack((Item) null))) {
            return true;
        }
        // 检查物品是否非空且不是空气方块
        else if (stack.getItem() != null && stack.getItem() != Item.getItemFromBlock(Blocks.air)) {
            // 检查物品堆的大小是否为0或负数
            if (stack.stackSize <= 0) {
                return true;
            }
            // 检查物品的损坏值是否在一个特定范围之外
            else {
                return stack.getItemDamage() < -32768 || stack.getItemDamage() > 65535;
            }
        }
        // 其他情况视为一个空的ItemStack
        else {
            return true;
        }
    }

}
