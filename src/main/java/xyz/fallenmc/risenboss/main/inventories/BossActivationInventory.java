package xyz.fallenmc.risenboss.main.inventories;

import me.zach.DesertMC.Utils.Config.ConfigUtils;
import me.zach.DesertMC.Utils.MiscUtils;
import me.zach.DesertMC.Utils.StringUtils.StringUtil;
import me.zach.DesertMC.Utils.gui.GUIHolder;
import me.zach.DesertMC.Utils.nbt.NBTUtil;
import me.zach.DesertMC.Prefix;
import xyz.fallenmc.risenboss.main.data.RisenData;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.fallenmc.risenboss.main.RisenBoss;
import xyz.fallenmc.risenboss.main.RisenMain;
import xyz.fallenmc.risenboss.main.rewards.RewardType;
import xyz.fallenmc.risenboss.main.utils.RisenUtils;

import java.util.*;
import java.util.function.IntPredicate;

public class BossActivationInventory implements GUIHolder {
    private final int[][] borders = new int[3][];
    private final ItemStack[] borderPanes = new ItemStack[]{MiscUtils.generateItem(Material.STAINED_GLASS_PANE, " ", Collections.emptyList(), (byte) 2, 1), MiscUtils.generateItem(Material.STAINED_GLASS_PANE, " ", Collections.emptyList(), (byte) 5, 1), MiscUtils.generateItem(Material.STAINED_GLASS_PANE, " ", Collections.emptyList(), (byte) 3, 1)};
    Inventory inventory = Bukkit.getServer().createInventory(this, 54, "Activate Risen Boss");
    int centerRow = Math.floorDiv(inventory.getSize(), 2);
    final int abilitiesSlot = 3;
    final int coreSlot = 4;
    final int rewardsSlot = 5;

    BukkitRunnable animation = new BukkitRunnable(){
        public void run(){
            ItemStack replaced = borderPanes[0];
            for(int i = 1, j = 0; j<borderPanes.length; j++, i = (i + 1 >= borderPanes.length ? 0 : i + 1)){
                ItemStack setAfter = borderPanes[i];
                borderPanes[i] = replaced;
                replaced = setAfter;
            }
            for(int i = 0; i < borders.length; i++){
                int[] border = borders[i];
                ItemStack item = borderPanes[i];
                for(int slot : border){
                    inventory.setItem(slot, item);
                }
            }
        }
    };
    public BossActivationInventory(Player player){
        final ItemStack selectAbilities = getSelectAbilities();
        final ItemStack risenCore = getRisenCore();
        List<String> rewardsDesc = new ArrayList<>();
        RisenData data = RisenUtils.getData(player);
        for(Map.Entry<String, Integer> rewardEntry : data.getRewards().entrySet()){
            RewardType type = RewardType.valueOf(rewardEntry.getKey());
            String entryString = ChatColor.AQUA + " - " + type + ": " + rewardEntry.getValue();
            rewardsDesc.add(entryString);
        }
        if(rewardsDesc.isEmpty()) rewardsDesc.add(ChatColor.GRAY + "None so far");
        else rewardsDesc.add(0, "");
        ItemStack earnedRewardsItem = MiscUtils.generateItem(Material.DIAMOND, ChatColor.AQUA + "Earned Rewards", rewardsDesc, (byte) -1, 1);
        inventory.setItem(centerRow + abilitiesSlot, selectAbilities);
        inventory.setItem(centerRow + coreSlot, risenCore);
        inventory.setItem(centerRow + rewardsSlot, earnedRewardsItem);
        //calculating animation borders
        for(int i = 0; i<borders.length; i++){
            //resolving corners (ymin isn't necessary since it's always equal to xmin)
            int xmin = borders.length - i - 1;
            int xmax = xmin + 4 + i * 2;
            int ymax = xmin /*(ymin)*/ + 2 + i * 2;
            int[] border = new int[((xmax - xmin) + (ymax - xmin)) * 2 + 4];
            //filling perimeter x and y edges
            int[] xRange = MiscUtils.range(xmin, xmax + 1);
            int[] yRange = MiscUtils.range(xmin, ymax + 1);
            int borderIndex = 0;
            for(int j = 0; j<xRange.length; j++, borderIndex += 2){
                border[borderIndex] = getSlot(xRange[j], xmin);
                border[borderIndex + 1] = getSlot(xRange[j], ymax);
            }
            for(int j = 0; j<yRange.length; j++, borderIndex += 2){
                border[borderIndex] = getSlot(xmin, yRange[j]);
                border[borderIndex + 1] = getSlot(xmax, yRange[j]);
            }
            borders[i] = border;
        }
        //setting the initial border values
        for(int i = borders.length - 1; i >= 0; i--){
            int[] border = borders[i];
            for(int j = 0; j < border.length; j++){
                int slot = border[j];
                if(slot >= inventory.getSize()) continue;
                if(inventory.getItem(slot) != null && inventory.getItem(slot).getType() != Material.AIR){
                    //marking the invalid slot for removal
                    border[j] = -1;
                }else inventory.setItem(slot, borderPanes[i]);
            }
            borders[i] = Arrays.stream(border).filter(borderFilter).toArray();
        }
    }

    private static final GUIHolder NOT_READY_HOLDER = new GUIHolder(){
        public final Inventory NOT_READY = Bukkit.getServer().createInventory(NOT_READY_HOLDER, 45, "Activate Risen Boss");
        final int coreSlot = Math.floorDiv(NOT_READY.getSize(), 2) + 4;
        final int abilitiesSlot = NOT_READY.getSize() - 4;
        {
            final ItemStack selectAbilities = getSelectAbilities();
            final ItemStack notReadyItem = MiscUtils.generateItem(Material.INK_SACK,
                    ChatColor.RED + "Risen Core not ready",
                    StringUtil.wrapLore(ChatColor.YELLOW + "\nThis Risen Core will illuminate after you reach a 50 killstreak with Risen Armor. Once it's activated, come back here to turn into a Risen Boss!"),
                    (byte) 8,
                    1);
            //filling inventory
            ItemStack empty = MiscUtils.getEmptyPane();
            for(int i = 0; i<NOT_READY.getSize(); i++) NOT_READY.setItem(i, empty);
            NOT_READY.setItem(coreSlot, notReadyItem);
            NOT_READY.setItem(abilitiesSlot, selectAbilities);
        }
        public void inventoryClick(Player player, int slot, ItemStack clickedItem, ClickType clickType, InventoryClickEvent event){
            event.setCancelled(true);
            if(slot == coreSlot) player.playSound(player.getLocation(), Sound.NOTE_BASS, 10, 1.1f);
            else if(slot == abilitiesSlot) player.openInventory(new AbilitySelectInventory(player).getInventory());
        }
        public Inventory getInventory(){
            return NOT_READY;
        }
    };

    IntPredicate borderFilter = value -> value > -1 && value < inventory.getSize();

    public void inventoryOpen(Player player, Inventory inventory, InventoryOpenEvent event){
        animation.runTaskTimer(RisenMain.getInstance(), 5, 5);
    }

    public void inventoryClose(Player player, Inventory inventory, InventoryCloseEvent event){
        animation.cancel();
    }

    public void inventoryClick(Player player, int slot, ItemStack item, ClickType clickType, InventoryClickEvent event){
        event.setCancelled(true);
        if(slot == centerRow + coreSlot){
            player.closeInventory();
            RisenData data = RisenUtils.getData(player);
            if(data.isBossReady()){
                if(RisenMain.currentBoss == null){
                    Location spawn = ConfigUtils.getSpawn("boss");
                    if(spawn == null){
                        player.sendMessage(ChatColor.RED + "Uh-oh! We couldn't turn you into a risen boss because the boss spawn point hasn't been set yet!\nPlease tell a server owner or administrator to resolve this issue immediately!");
                        player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 10, 1);
                    }else{
                        for(ItemStack armor : player.getEquipment().getArmorContents()){
                            String id = NBTUtil.getCustomAttrString(armor, "ID");
                            if(!id.startsWith("FALLEN_")){
                                player.sendMessage(ChatColor.RED + "To become a risen boss, you must first be wearing the full Fallen Armor set!");
                                return;
                            }
                        }
                        RisenMain.currentBoss = new RisenBoss(player);
                        data.setBossReady(false);
                        player.teleport(spawn);
                        player.getServer().broadcastMessage(Prefix.RISEN_BOSS + " " + player.getDisplayName() + ChatColor.GRAY + " became a risen boss! Fight them to earn rewards!");
                        StringUtil.sendCenteredWrappedMessage(player,
                                new StringUtil.ChatWrapper('=', ChatColor.YELLOW, true, false),
                                ChatColor.GREEN + ChatColor.MAGIC.toString() + "/" + ChatColor.GREEN + ChatColor.BOLD + "YOU HAVE ARISEN" + ChatColor.GREEN + ChatColor.MAGIC + "\\\n",
                                ChatColor.GREEN + "You became a risen boss!",
                                ChatColor.GREEN + "Use abilities by right clicking the hotbar items!",
                                ChatColor.GREEN + "Survive for 10 minutes and deal damage to",
                                ChatColor.GREEN + "gain the best rewards!",
                                "",
                                ChatColor.GOLD + ChatColor.BOLD.toString() + "GOOD LUCK!");
                        for(Player otherPlayer : Bukkit.getOnlinePlayers()){
                            otherPlayer.playSound(otherPlayer.getLocation(), Sound.WITHER_SPAWN, 10, 1);
                            otherPlayer.playSound(otherPlayer.getLocation(), Sound.ANVIL_LAND, 7, 1);
                        }
                        MiscUtils.spawnFirework(player.getLocation(), 0, false, false, FireworkEffect.Type.BALL, Color.GREEN, Color.YELLOW);
                    }
                }else{
                    player.sendMessage(ChatColor.RED + "Sorry, you can't become a Risen Boss while one is already active.");
                }
            }
        }else if(slot == centerRow + abilitiesSlot){
            player.openInventory(new AbilitySelectInventory(player).getInventory());
        }
    }

    private static ItemStack getSelectAbilities(){
        return MiscUtils.generateItem(Material.POTION, ChatColor.YELLOW + "Select Risen Abilities", Collections.emptyList(), (byte) -1, 1);
    }

    private static ItemStack getRisenCore(){
        ItemStack risenCore = MiscUtils.generateItem(Material.INK_SACK, ChatColor.GOLD + "Risen Core", StringUtil.wrapLore(ChatColor.YELLOW.toString() + ChatColor.BOLD + "\nTHE TIME HAS COME\n" + ChatColor.GOLD + "The " + ChatColor.BOLD + "RISEN CORE" + ChatColor.GOLD + " has been activated! Click it fight a glorious battle as the powerful Risen Boss!"), (byte) 10, 1);
        risenCore.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        ItemMeta risenMeta = risenCore.getItemMeta();
        risenMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        risenCore.setItemMeta(risenMeta);
        return risenCore;
    }

    public static int getSlot(int x, int y){
        return y * 9 + x;
    }

    public Inventory getInventory(){
        return inventory;
    }

    public static Inventory getNotReady(){
        return NOT_READY_HOLDER.getInventory();
    }
}