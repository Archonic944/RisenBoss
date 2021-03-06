package xyz.fallenmc.risenboss.main.abilities;

import me.zach.DesertMC.Utils.ActionBar.ActionBarUtils;
import me.zach.DesertMC.Utils.Config.ConfigUtils;
import me.zach.DesertMC.Prefix;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.fallenmc.risenboss.main.RisenBoss;
import xyz.fallenmc.risenboss.main.RisenMain;

import java.util.UUID;


public class Ability {
    public RisenAbility ability;
    public Ability(RisenAbility ability){
        this.ability = ability;
    }

    public void activate(RisenBoss boss){
        Player player = boss.getPlayer();
        if(!cd){
            if(ability.activationInternal(ConfigUtils.findClass(boss.getUUID()).equals(ability.multiClass), boss)) {
                if(!ability.equals(RisenAbility.REJUVENATE) && !ability.equals(RisenAbility.DUMMY)){
                    Bukkit.getServer().broadcastMessage(Prefix.RISEN_BOSS.toString() + ChatColor.YELLOW + " " + player.getName() + " used " + ChatColor.GOLD + ability.name + ChatColor.YELLOW + "!");
                    for(UUID uuid : boss.damagers.keySet()){
                        Player p = Bukkit.getPlayer(uuid);
                        p.playSound(p.getLocation(), Sound.ENDERDRAGON_GROWL, 10, 1.05f);
                    }
                }
                cooldown(player);
            }else player.playSound(player.getLocation(), Sound.NOTE_BASS, 10, 1);
        }else player.sendMessage(ChatColor.RED + "This ability is on cooldown for " + cdLeft + " more seconds!");
    }

    public boolean cd = false;
    private int cdLeft = 0;

    private void cooldown(Player player){
        cd = true;
        cdLeft = ability.cooldown;
        new BukkitRunnable() {
            public void run() {
                if (cdLeft == 0){
                    cd = false;
                    ActionBarUtils.sendActionBar(player, ChatColor.GREEN + "You can use " + ChatColor.GOLD + ChatColor.BOLD + ability.name + ChatColor.GREEN + "!");
                    player.playSound(player.getLocation(), Sound.NOTE_PLING, 10, 1.1f);
                    cancel();
                }else if(cdLeft < 0){
                    cancel();
                }else cdLeft--;
            }
        }.runTaskTimer(RisenMain.getInstance(), 20, 20);
    }

    public void abort(RisenBoss boss){
        ability.abort(boss.getUUID());
        cdLeft = -1;
    }
}