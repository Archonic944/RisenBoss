package xyz.fallenmc.risenboss.main.inventories;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.fallenmc.risenboss.main.abilities.RisenAbility;
import xyz.fallenmc.risenboss.main.utils.RisenUtils;
import xyz.fallenmc.risenboss.main.data.RisenData;

import java.util.*;

public class AbilitySelectInventory implements GUIHolder {
    private static final char PROGRESS_TICK = '\u258A';
    BiMap<Integer, RisenAbility> abilityMap = HashBiMap.create();
    List<RisenAbility> workingSelected;
    final int abilitySlots;
    final int winsToNext;
    final Inventory inventory = Bukkit.getServer().createInventory(this, 54);
    String nextSlotProgress;
    int abilitiesItemSlot = 49;
    final UUID uuid;

    public AbilitySelectInventory(Player player){
        uuid = player.getUniqueId();
        //setting variables
        RisenData data = RisenUtils.getData(player);
        abilitySlots = data.getAbilitySlots();
        winsToNext = data.getWinsToNextSlot();
        //filling inventory
        for(int i = 0; i<inventory.getSize(); i++){
            inventory.setItem(i,MiscUtils.getEmptyPane());
        }
        this.nextSlotProgress = nextSlotProgress(winsToNext, abilitySlots);
        workingSelected = data.getAbilities();
        ItemStack abilitiesItem = MiscUtils.generateItem(
                Material.ITEM_FRAME,
                ChatColor.YELLOW + "Selected Abilities",
                getAbilitiesText(),
                (byte) -1,
                1);
        inventory.setItem(abilitiesItemSlot, abilitiesItem);
        for(int currentSlot = 1, stage = 0, i = 0; i < RisenAbility.VALUES.size(); i++){
            RisenAbility ability = RisenAbility.VALUES.get(i);
            ItemStack selectionItem = ability.hotbarItem.clone();
            ItemMeta selectionMeta = selectionItem.getItemMeta();
            boolean isSelected = workingSelected.contains(ability);
            selectionMeta.setDisplayName((isSelected ? ChatColor.AQUA : ChatColor.WHITE) + ability.name);
            List<String> lore = selectionMeta.getLore();
            if(isSelected){
                lore.add(ChatColor.GREEN + "Click to disable!");
                selectionMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, false);
            }else lore.add(ChatColor.YELLOW + "Click to enable");
            selectionMeta.setLore(lore);
            selectionItem.setItemMeta(selectionMeta);
            inventory.setItem(currentSlot, selectionItem);
            abilityMap.put(currentSlot, ability);
            currentSlot = (currentSlot + (stage == 3 ? 12 : (stage % 2 == 0 ? 9 : -3)));
            stage = (stage == 3 ? 0 : stage + 1);
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
                if(workingSelected.size() + 1 > abilitySlots){
                    RisenAbility toDeselectAbility = workingSelected.get(0);
                    ItemStack toDeselectItem = inventory.getItem(abilityMap.inverse().get(toDeselectAbility));
                    toDeselectItem.removeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL);
                    ItemMeta toDeselectMeta = toDeselectItem.getItemMeta();
                    List<String> deselectLore = toDeselectMeta.getLore();
                    deselectLore.set(deselectLore.size() - 1, ChatColor.YELLOW + "Click to enable");
                    toDeselectMeta.setDisplayName(ChatColor.WHITE + toDeselectAbility.name);
                    toDeselectMeta.setLore(deselectLore);
                    toDeselectItem.setItemMeta(toDeselectMeta);
                    workingSelected.remove(0);
                }
                workingSelected.add(ability);
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 10, 1);
                meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, false);
                meta.setDisplayName(ChatColor.AQUA + ability.name);
                lore.set(lore.size() - 1, ChatColor.GREEN + "Click to disable!");
                updateInventory();
            }else{
                workingSelected.remove(ability);
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 10, 0.9f);
                meta.removeEnchant(Enchantment.PROTECTION_ENVIRONMENTAL);
                meta.setDisplayName(ChatColor.WHITE + ability.name);
                lore.set(lore.size() - 1, ChatColor.YELLOW + "Click to enable");
                updateInventory();
            }
            meta.setLore(lore);
            clickedItem.setItemMeta(meta);
        }
    }

    private void updateInventory(){
        ItemMeta abilitiesMeta = getAbilitiesItem().getItemMeta();
        abilitiesMeta.setLore(getAbilitiesText());
        getAbilitiesItem().setItemMeta(abilitiesMeta);
    }

    private List<String> getAbilitiesText(){
        ArrayList<String> desc = new ArrayList<>();
        desc.add("");
        if(workingSelected.isEmpty()) desc.add(ChatColor.GRAY + "None");
        else{
            for(int i = 0; i<abilitySlots; i++){
                if(i < workingSelected.size()){
                    RisenAbility ability = workingSelected.get(i);
                    desc.add(ChatColor.DARK_GRAY + "- " + ChatColor.AQUA + ability);
                }else desc.add(ChatColor.DARK_GRAY + "-");
            }
        }
        desc.add("");
        desc.add(nextSlotProgress);
        return desc;
    }

    private ItemStack getAbilitiesItem(){
        return inventory.getItem(abilitiesItemSlot);
    }


    /**
     * Finalizes, trims, and saves the player's ability configuration.
     */
    public void inventoryClose(Player player, Inventory inventory, InventoryCloseEvent event){
        //saving, trimming to max ability slots for safety
        MiscUtils.trimList(workingSelected, abilitySlots);
        if(workingSelected.size() == 0) workingSelected.add(RisenAbility.VALUES.get(0));
        RisenUtils.getData(player).setAbilities(workingSelected);
        player.playSound(player.getLocation(), Sound.NOTE_STICKS, 10, 1);
    }

    public static String nextSlotProgress(int winsToNext, int abilitySlots){
        StringBuilder nextSlotProgress = new StringBuilder();
        if(abilitySlots >= RisenUtils.MAX_ABILITY_SLOTS) nextSlotProgress.append(ChatColor.YELLOW).append("You've reached the max amount of ability slots!!");
        else{
            int wins = RisenUtils.WINS_PER_ABILITY_SLOT - winsToNext;
            nextSlotProgress.append(ChatColor.GREEN);
            for(int i = 0; i < wins; i++){
                nextSlotProgress.append(PROGRESS_TICK);
            }
            nextSlotProgress.append(ChatColor.RED);
            while(ChatColor.stripColor(nextSlotProgress.toString()).length() - 1 < RisenUtils.WINS_PER_ABILITY_SLOT){
                nextSlotProgress.append(PROGRESS_TICK);
            }
            nextSlotProgress.append(ChatColor.YELLOW).append(" (").append(wins).append("/").append(RisenUtils.WINS_PER_ABILITY_SLOT).append(" wins)");
            String beginText = ChatColor.GRAY + "Next slot progress: ";
            nextSlotProgress.insert(0, beginText);
        }
        return nextSlotProgress.toString();
    }
}