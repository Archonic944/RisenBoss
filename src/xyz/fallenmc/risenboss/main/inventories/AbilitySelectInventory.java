package xyz.fallenmc.risenboss.main.inventories;

import com.google.common.collect.BiMap;
import me.zach.DesertMC.Utils.MiscUtils;
import me.zach.DesertMC.Utils.gui.GUIHolder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.fallenmc.risenboss.main.BossPreferences;
import xyz.fallenmc.risenboss.main.abilities.RisenAbility;
import xyz.fallenmc.risenboss.main.utils.RisenUtils;

import java.util.*;

public class AbilitySelectInventory implements GUIHolder {
    BiMap<Integer, RisenAbility> abilityMap;
    List<RisenAbility> workingSelected;
    public static final int MINIMUM_ABILITY_SLOTS = 3;
    public static final int WINS_PER_ABILITY_SLOT = 15;
    public static final int MAX_ABILITY_SLOTS = 6;
    final int abilitySlots;
    final int winsToNext;
    final Inventory inventory = Bukkit.getServer().createInventory(this, 54);
    StringBuilder nextSlotProgress = new StringBuilder();
    ItemStack abilitySlotsItem;
    final UUID uuid;

    public AbilitySelectInventory(Player player){
        uuid = player.getUniqueId();
        //setting variables
        abilitySlots = RisenUtils.getAbilitySlots(player);
        winsToNext = RisenUtils.getWinsToNextAbilitySlot(player);
        //filling inventory
        for(int i = 0; i<inventory.getSize(); i++){
            inventory.setItem(i,MiscUtils.getEmptyPane());
        }
        if(abilitySlots >= MAX_ABILITY_SLOTS) nextSlotProgress.append(ChatColor.YELLOW).append("You've reached the max!");
        else{
            nextSlotProgress.append(ChatColor.GRAY).append("Next slot progress: ");
            int wins = WINS_PER_ABILITY_SLOT - winsToNext;
            nextSlotProgress.append(ChatColor.GREEN);
            for(int i = 0; i < wins; i++){
                nextSlotProgress.append("▮");
            }
            nextSlotProgress.append(ChatColor.WHITE);
            while(nextSlotProgress.length() < WINS_PER_ABILITY_SLOT) nextSlotProgress.append("▯");
            nextSlotProgress.append(ChatColor.YELLOW).append(" (").append(wins).append("/").append(WINS_PER_ABILITY_SLOT).append(") wins");
        }
        abilitySlotsItem = MiscUtils.generateItem(
                Material.ITEM_FRAME,
                ChatColor.YELLOW + "Ability Slots",
                Arrays.asList(
                        "",
                        ChatColor.GRAY + "You currently have " + ChatColor.YELLOW + abilitySlots + ChatColor.GRAY + " ability slots.",
                        nextSlotProgress.toString()),
                (byte) -1,
                1);
        inventory.setItem(49, abilitySlotsItem);
        BossPreferences preferences = RisenUtils.getPreferences(Bukkit.getPlayer(uuid));
        workingSelected = preferences.enabledAbilities;
        for(int currentSlot = 0, stage = 0, i = 0; i < RisenAbility.VALUES.length; ){
            RisenAbility ability = RisenAbility.VALUES[i];
            ItemStack selectionItem = ability.hotbarItem.clone();
            ItemMeta selectionMeta = selectionItem.getItemMeta();
            selectionMeta.setDisplayName(selectionMeta.getDisplayName().replace(ChatColor.GRAY + " (Right Click)", ""));
            selectionMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            List<String> lore = selectionMeta.getLore();
            boolean isSelected = preferences.enabledAbilities.contains(ability);
            if(isSelected){
                lore.add(ChatColor.GREEN + "Click to disable!");
                selectionMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, false);
            }else lore.add(ChatColor.YELLOW + "Click to enable");
            selectionMeta.setLore(lore);
            selectionItem.setItemMeta(selectionMeta);
            currentSlot = (currentSlot + (stage == 3 ? 11 : (stage % 2 == 0 ? 9 : -3)));
            abilityMap.put(currentSlot, ability);
            inventory.setItem(currentSlot, selectionItem);
            stage = (stage == 4 ? 0 : stage + 1);
            i++;
        }
    }

    public Inventory getInventory(){
        return inventory;
    }

    public void inventoryClick(Player player, int slot, ItemStack clickedItem, ClickType clickType, InventoryClickEvent event){
        event.setCancelled(true);
        RisenAbility ability = abilityMap.get(slot);
        if(ability != null){
            boolean isSelected = !workingSelected.contains(ability);
            ItemMeta meta = clickedItem.getItemMeta();
            List<String> lore = meta.getLore();
            if(isSelected){
                workingSelected.add(ability);
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 10, 1);
                meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, false);
                lore.set(lore.size() - 1, ChatColor.GREEN + "Click to disable!");
                if(workingSelected.size() > abilitySlots){
                    ItemStack toDeselectItem = inventory.getItem(abilityMap.inverse().get(workingSelected.get(0)));
                    toDeselectItem.removeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL);
                    ItemMeta toDeselectMeta = toDeselectItem.getItemMeta();
                    List<String> deselectLore = toDeselectMeta.getLore();
                    deselectLore.set(0, ChatColor.YELLOW + "Click to select");
                    toDeselectMeta.setLore(deselectLore);
                    toDeselectItem.setItemMeta(toDeselectMeta);
                    workingSelected.remove(0);
                }
            }else{
                workingSelected.remove(ability);
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 10, 0.9f);
                meta.removeEnchant(Enchantment.PROTECTION_ENVIRONMENTAL);
                lore.set(lore.size() - 1, ChatColor.YELLOW + "Click to enable");
            }
            meta.setLore(lore);
            clickedItem.setItemMeta(meta);
        }
    }


    /**
     * Finalizes, trims, and saves the player's ability configuration.
     */
    public void inventoryClose(Player player, Inventory inventory, InventoryCloseEvent event){
        //saving, trimming to max ability slots for safety
        RisenAbility[] abilitiesTrimmed = Arrays.copyOf(workingSelected.toArray(new RisenAbility[0]), abilitySlots);
        RisenUtils.setPreferences(new BossPreferences(abilitiesTrimmed), player);
    }

    private static class SelectionEntry {
        final RisenAbility ability;
        boolean isSelected;

        public SelectionEntry(RisenAbility ability, boolean isSelected){
            this.ability = ability;
            this.isSelected = isSelected;
        }
    }
}
