package xyz.fallenmc.risenboss.main.events;

import me.zach.DesertMC.Utils.MiscUtils;
import me.zach.DesertMC.Utils.nbt.NBTUtil;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.*;
import xyz.fallenmc.risenboss.main.RisenBoss;
import xyz.fallenmc.risenboss.main.RisenMain;
import xyz.fallenmc.risenboss.main.abilities.Ability;
import xyz.fallenmc.risenboss.main.abilities.RisenAbility;
import xyz.fallenmc.risenboss.main.inventories.AbilitySelectInventory;
import xyz.fallenmc.risenboss.main.utils.BossBarUtil;
import xyz.fallenmc.risenboss.main.utils.RisenUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

public class RisenEvents implements Listener {
    private final HashMap<UUID, Integer> rejuvenateMap = new HashMap<>();

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
        String abilityName = NBTUtil.getCustomAttrString(player.getItemInHand(), "ABILITY");
        if(RisenUtils.isBoss(player.getUniqueId())){
            Ability ability = RisenMain.currentBoss.abilityInstances.get(abilityName);
            if(ability != null){
                ability.activate(RisenMain.currentBoss);
            }
        }else if(!abilityName.equals("null")) player.setItemInHand(null);
    }

    @EventHandler
    public void removeBar(PlayerQuitEvent event){
        BossBarUtil.removeBar(event.getPlayer());
    }

    @EventHandler
    public void cancelBucketPour(PlayerBucketEmptyEvent event){
        event.setCancelled(true);
    }


    @EventHandler
    public void cancelDuckAndWeaveDrink(PlayerItemConsumeEvent event){
        if(NBTUtil.hasCustomKey(event.getItem(), "ABILITY")) event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.RED + "Nice try.");
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.NOTE_BASS, 10, 1);
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
    public void cancelAbilityPlace(BlockPlaceEvent event){
        if(NBTUtil.hasCustomKey(event.getItemInHand(), "ABILITY")) event.setCancelled(true);
    }

    @EventHandler
    public void setBossSaveValues(PlayerJoinEvent event){
        UUID uuid = event.getPlayer().getUniqueId();
        //TODO ensure boss save values
    }
}