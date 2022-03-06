package xyz.fallenmc.risenboss.main.rewards;

import itempackage.Items;
import me.zach.DesertMC.Utils.Config.ConfigUtils;
import me.zach.DesertMC.Utils.MiscUtils;
import me.zach.artifacts.gui.inv.ArtifactData;
import me.zach.databank.saver.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public enum RewardType {
    SPECIAL_HAMMER(ChatColor.LIGHT_PURPLE + "Special Hammer", "Full inventory (item(s) dropped)"){
        protected boolean grant(Player player, int amount){
            Inventory inventory = player.getInventory();
            boolean sucess = true;
            for(int i = 0; i<amount; i++){
                ItemStack hammer = Items.getSpecialHammer();
                if(sucess){
                    if(inventory.firstEmpty() == -1) sucess = false;
                    else inventory.addItem(hammer);
                }else{
                    Item droppedHammer = player.getWorld().dropItem(player.getLocation(), hammer);
                    MiscUtils.setOwner(droppedHammer, player);
                }
            }
            return sucess;
        }
    },
    DIAMOND_HAMMER(ChatColor.AQUA + "Diamond Hammer", "Full inventory (item(s) dropped)"){
        protected boolean grant(Player player, int amount){
            Inventory inventory = player.getInventory();
            boolean sucess = true;
            for(int i = 0; i<amount; i++){
                ItemStack hammer = Items.getDiamondHammer();
                if(sucess){
                    if(inventory.firstEmpty() == -1) sucess = false;
                    else inventory.addItem(hammer);
                }else{
                    Item droppedHammer = player.getWorld().dropItem(player.getLocation(), hammer);
                    MiscUtils.setOwner(droppedHammer, player);
                }
            }
            return sucess;
        }
    },
    IRON_HAMMER(ChatColor.AQUA + "Iron Hammer", " Full Inventory (item dropped with owner)"){
        protected boolean grant(Player player, int amount){
            Inventory inventory = player.getInventory();
            boolean sucess = true;
            for(int i = 0; i<amount; i++){
                ItemStack hammer = Items.getSpecialHammer();
                if(sucess){
                    if(inventory.firstEmpty() == -1) sucess = false;
                    else inventory.addItem(hammer);
                }else{
                    Item droppedHammer = player.getWorld().dropItem(player.getLocation(), hammer);
                    MiscUtils.setOwner(droppedHammer, player);
                }
            }
            return sucess;
        }
    },
    GEMS(ChatColor.GREEN + "Gems") {
        public boolean grant(Player player, int amount){
            ConfigUtils.addGems(player, amount);
            return true;
        }
    },
    SOULS(ChatColor.LIGHT_PURPLE + "Souls") {
        public boolean grant(Player player, int amount){
            ConfigUtils.addSouls(player, amount);
            return true;
        }
    },
    EXP(ChatColor.BLUE + "EXP") {
        public boolean grant(Player player, int amount){
            ConfigUtils.addXP(player, ConfigUtils.findClass(player.getUniqueId()), amount);
            return true;
        }
    },
    UPGRADE_STONES(ChatColor.GOLD + "Upgrade Stones") {
        protected boolean grant(Player player, int amount){
            PlayerData data = ConfigUtils.getData(player);
            ArtifactData artifactData = data.getArtifactData();
            artifactData.addUStones(amount);
            return true;
        }
    };

    String name;
    String failedMessage;

    RewardType(String name, String failedMessage){
        this.name = name;
        this.failedMessage = failedMessage;
    }

    RewardType(String name){
        this(name, "Unknown error! Contact a staff member!");
    }

    protected abstract boolean grant(Player player, int amount);

    public String toString(){
        return name;
    }
}

