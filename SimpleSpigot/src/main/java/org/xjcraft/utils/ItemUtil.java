package org.xjcraft.utils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Created by Ree on 2017/9/11.
 */
public class ItemUtil {
    public static ItemStack[] removeItem(Player player, ItemStack itemStack, int number) throws Exception {
        ItemStack[] bag = player.getInventory().getContents();
        if (getItemNumber(player, itemStack) < number) {
            throw new Exception("not enough items!");
        }
        for (ItemStack i : bag) {
            if (i != null) {
                itemStack.setAmount(i.getAmount());
                if (i.hashCode() == itemStack.hashCode()) {
                    if (i.getAmount() <= number) {
                        number = number - i.getAmount();
                        i.setType(Material.AIR);
                    } else {
                        i.setAmount(i.getAmount() - number);
                        number = 0;
                    }
                    if (number == 0) {
                        break;
                    }
                }
            }
        }
        player.getInventory().setContents(bag);
//        if (number != 0) {
//            throw new Exception("not enough items!");
//        }
        return bag;
    }

    public static int getItemNumber(Player player, ItemStack itemStack) {
        ItemStack[] bag = player.getInventory().getContents();
        int itemCount = 0;
        for (ItemStack i : bag) {
            if (i != null) {
                int amount = i.getAmount();
                itemStack.setAmount(amount);
                if (itemStack.getType() == i.getType() && itemStack.getDurability() == itemStack.getDurability()) {
                    itemCount = itemCount + amount;
                }
            }
        }
        return itemCount;
    }
}
