package xyz.fallenmc.risenboss.main.events;

import me.zach.DesertMC.Utils.nbt.NBTUtil;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import xyz.fallenmc.risenboss.main.RisenBoss;
import xyz.fallenmc.risenboss.main.RisenMain;
import xyz.fallenmc.risenboss.main.abilities.Ability;
import xyz.fallenmc.risenboss.main.utils.RisenUtils;

import java.util.UUID;

public class RisenEvents implements Listener {
    @EventHandler
    public void cancelBossPickup(PlayerPickupItemEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (RisenUtils.isBoss(uuid)) event.setCancelled(true);
    }

    @EventHandler
    public void cancelBossDrop(PlayerDropItemEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if(RisenUtils.isBoss(uuid)) event.setCancelled(true);
    }

    @EventHandler
    public void bossQuit(PlayerQuitEvent event){
        UUID uuid = event.getPlayer().getUniqueId();
        if(RisenUtils.isBoss(uuid)){
            RisenMain.currentBoss.endBoss(RisenBoss.EndReason.BOSS_QUIT);
            System.out.println("RisenMain.currentBoss = " + RisenMain.currentBoss);
        }
    }

    @EventHandler
    public void useAbility(PlayerInteractEvent event){
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        String abilityName = NBTUtil.getCustomAttrString(item, "ABILITY");
        if(RisenUtils.isBoss(player.getUniqueId())){
            Ability ability = RisenMain.currentBoss.abilityInstances.get(abilityName);
            if(ability != null){
                ability.activate(RisenMain.currentBoss);
            }
        }else if(!abilityName.equals("null")) player.setItemInHand(null);
        else if(NBTUtil.getCustomAttrString(item, "ID").equals("RISEN_BLADE")) player.setItemInHand(null);
    }

    @EventHandler
    public void cancelBucketPour(PlayerBucketEmptyEvent event){
        event.setCancelled(true);
    }

    @EventHandler
    public void cancelDuckAndWeaveDrink(PlayerItemConsumeEvent event){
        if(NBTUtil.hasCustomKey(event.getItem(), "ABILITY")){
            event.getPlayer().sendMessage(ChatColor.RED + "Nice try.");
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.NOTE_BASS, 10, 1);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void cancelAbilityPotionThrow(PotionSplashEvent event){
        if(NBTUtil.hasCustomKey(event.getPotion().getItem(), "ABILITY")) event.setCancelled(true);
    }

    @EventHandler
    public void bossHit(EntityDamageByEntityEvent event){
        if(RisenMain.currentBoss != null){
            UUID entityUUID = event.getEntity().getUniqueId();
            UUID damagerUUID = event.getDamager().getUniqueId();
            double dmg = event.getDamage();
            if(RisenUtils.isBoss(damagerUUID))
                RisenMain.currentBoss.bossAttack(event.getDamage());
            else if(RisenUtils.isBoss(entityUUID))
                RisenMain.currentBoss.bossDamage(damagerUUID, dmg);
        }
    }

    @EventHandler
    public void bossDamageNaturally(EntityDamageEvent event){
        UUID uuid = event.getEntity().getUniqueId();
        if(RisenUtils.isBoss(uuid)){
            RisenMain.currentBoss.bossDamage(event.getDamage());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void cancelAbilityPlace(BlockPlaceEvent event){
        if(NBTUtil.hasCustomKey(event.getItemInHand(), "ABILITY")) event.setCancelled(true);
    }
}