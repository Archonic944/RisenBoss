package xyz.fallenmc.risenboss.main.events;

import me.zach.DesertMC.GameMechanics.Events;
import me.zach.DesertMC.Utils.Config.ConfigUtils;
import me.zach.DesertMC.Utils.MiscUtils;
import me.zach.DesertMC.Utils.StringUtils.StringUtil;
import me.zach.DesertMC.Utils.nbt.NBTUtil;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import xyz.fallenmc.risenboss.main.RisenBoss;
import xyz.fallenmc.risenboss.main.RisenMain;
import xyz.fallenmc.risenboss.main.abilities.Ability;
import xyz.fallenmc.risenboss.main.utils.BossBarUtil;
import xyz.fallenmc.risenboss.main.utils.RisenUtils;

import java.util.HashMap;
import java.util.UUID;

public class RisenEvents implements Listener {
    private final HashMap<UUID, Integer> rejuvenateMap = new HashMap<>();

    private boolean isBoss(UUID uuid) {
        if (RisenMain.currentBoss == null) return false;
        else return RisenMain.currentBoss.getUUID().equals(uuid);
    }

    @EventHandler
    public void cancelBossPickup(PlayerPickupItemEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (isBoss(uuid)) event.setCancelled(true);
    }

    @EventHandler
    public void cancelBossDrop(PlayerDropItemEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if(isBoss(uuid)) event.setCancelled(true);
    }

    @EventHandler
    public void useAbility(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if(isBoss(player.getUniqueId())){
            String id = NBTUtil.getCustomAttr(player.getItemInHand(), "ID");
            Ability ability = RisenMain.currentBoss.abilityInstances.get(id);
            if(ability != null){
                ability.activate(RisenMain.currentBoss);
            }
        }
    }

    @EventHandler
    public void removeBar(PlayerQuitEvent event) {
        BossBarUtil.removeBar(event.getPlayer());
    }

    @EventHandler
    public void activateBoss(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        String id = NBTUtil.getCustomAttr(item, "ID");
        Player player = (Player) event.getWhoClicked();
        if (id.equals("RISEN_ACTIVATOR")) {
            player.closeInventory();
            Integer ks = Events.ks.get(player.getUniqueId());
            if (ks != null) {
                if (ks >= 50) {
                    if (RisenMain.currentBoss == null) {
                        Location spawn = ConfigUtils.getSpawn("boss");
                        if (spawn == null) {
                            player.sendMessage(ChatColor.RED + "Uh-oh! We couldn't turn you into a risen boss because the boss spawn point hasn't been set yet!\nPlease tell a server owner or administrator to do this immediately!");
                            player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 10, 1);
                        } else {
                            RisenBoss boss = new RisenBoss(player, RisenUtils.getPreferences(player));
                            player.teleport(spawn);
                            player.getServer().broadcastMessage(ChatColor.RED + ChatColor.BOLD.toString() + "RISEN BOSS " + boss.rankColor + player.getName() + ChatColor.GRAY + " became a risen boss! Fight them to earn rewards!");
                            StringUtil.sendCenteredWrappedMessage(player,
                                    new StringUtil.ChatWrapper('=', ChatColor.YELLOW, true, false),
                                    ChatColor.GREEN + ChatColor.MAGIC.toString() + "/" + ChatColor.GREEN + ChatColor.BOLD + "YOU HAVE ARISEN" + ChatColor.GREEN + ChatColor.MAGIC + "\\\n",
                                    ChatColor.GREEN + "You became a risen boss!",
                                    ChatColor.GREEN + "Use abilities by right clicking the hotbar items!",
                                    ChatColor.GREEN + "Survive for 10 minutes and deal damage to gain the best rewards!\n",
                                    ChatColor.GOLD + ChatColor.BOLD.toString() + "GOOD LUCK!");
                            for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
                                otherPlayer.playSound(otherPlayer.getLocation(), Sound.WITHER_SPAWN, 10, 1);
                                otherPlayer.playSound(otherPlayer.getLocation(), Sound.ANVIL_LAND, 7, 1);
                            }
                            MiscUtils.spawnFirework(player.getLocation(), 0, false, false, FireworkEffect.Type.BALL, Color.GREEN, Color.YELLOW);
                        }
                    }
                }
            }
        }
    }
}