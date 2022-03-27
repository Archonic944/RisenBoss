package xyz.fallenmc.risenboss.main.events;

import me.zach.DesertMC.Utils.MiscUtils;
import me.zach.DesertMC.Utils.nbt.NBTUtil;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
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
    public void regainHealth(EntityRegainHealthEvent event){
        if(RisenUtils.isBoss(event.getEntity().getUniqueId()))
            RisenMain.currentBoss.refreshBarHealth();
    }

    @EventHandler
    public void useAbility(PlayerInteractEvent event){
        if(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK){
            Player player = event.getPlayer();
            ItemStack item = event.getItem();
            String abilityName = NBTUtil.getCustomAttrString(item, "ABILITY");
            if(RisenUtils.isBoss(player.getUniqueId())){
                Ability ability = RisenMain.currentBoss.abilityInstances.get(abilityName);
                if(ability != null){
                    boolean cancelledBefore = event.useItemInHand() == Event.Result.DENY;
                    event.setUseItemInHand(Event.Result.DENY);
                    if(!cancelledBefore) player.updateInventory();
                    ability.activate(RisenMain.currentBoss);
                }else if(!abilityName.equals("null")){
                    event.setCancelled(true);
                }
            }else if(!abilityName.equals("null")) player.setItemInHand(null);
            else if(NBTUtil.getCustomAttrString(item, "ID").equals("RISEN_BLADE")) player.setItemInHand(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void dropFallenPiece(PlayerDropItemEvent event){
        Player player = event.getPlayer();
        if(NBTUtil.getCustomAttrString(event.getItemDrop().getItemStack(), "ID").startsWith("FALLEN")){
            if(!MiscUtils.isAdmin(player)){
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You can't drop that item.\n" + ChatColor.GRAY + "Trust me, you don't want to.");
            }else{
                player.sendMessage(ChatColor.YELLOW + "You're an admin, so I let you drop that item.\n" + ChatColor.GRAY + "Don't think you'll be so lucky next time.");
            }
        }
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
    public void bossHit(EntityDamageByEntityEvent event){
        if(RisenMain.currentBoss != null){
            UUID entityUUID = event.getEntity().getUniqueId();
            UUID damagerUUID = event.getDamager().getUniqueId();
            double dmg = event.getDamage();
            if(RisenUtils.isBoss(damagerUUID))
                RisenMain.currentBoss.bossAttack(dmg);
            else if(RisenUtils.isBoss(entityUUID) && event.getDamager() instanceof Player){
                RisenMain.currentBoss.bossDamage(damagerUUID, dmg);
                System.out.println("boss damage " + dmg + " from " + event.getEntity());
            }
        }
    }

    @EventHandler
    public void bossDamageNaturally(EntityDamageEvent event){
        UUID uuid = event.getEntity().getUniqueId();
        if(RisenUtils.isBoss(uuid)){
            RisenMain.currentBoss.bossDamage(event.getDamage());
        }
    }
}