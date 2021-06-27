package xyz.fallenmc.risenboss.main.abilities;

import me.zach.DesertMC.Utils.ActionBar.ActionBarUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.fallenmc.risenboss.main.RisenBoss;
import xyz.fallenmc.risenboss.main.RisenMain;

public class Ability {

    public RisenAbility ability;
    public Ability(RisenAbility ability){
        this.ability = ability;
    }
    
    public void activate(RisenBoss boss){
        Player player = boss.getPlayer();
        if(!cd){
            ability.activationInternal(player);
            cooldown(player);
            if(!ability.equals(RisenAbility.REJUVENATE))
                Bukkit.getServer().broadcastMessage(boss.rankColor + player.getName() + ChatColor.YELLOW + " used " + ChatColor.GOLD + ability.name + ChatColor.YELLOW + "!");
        }else player.sendMessage(ChatColor.RED + "This ability is on cooldown for " + cdLeft + " more seconds!");
    }

    public boolean cd = false;
    private int cdLeft = 0;

    private void cooldown(Player player){
        cd = true;
        cdLeft = ability.cooldown;
        new BukkitRunnable() {
            public void run() {
                if (cdLeft <= 0){
                    cd = false;
                    ActionBarUtils.sendActionBar(player, ChatColor.GREEN + "You can use " + ChatColor.GOLD + ChatColor.BOLD + ability.name + ChatColor.GREEN + "!");
                    player.playSound(player.getLocation(), Sound.NOTE_PLING, 10, 1.1f);
                    cancel();
                }else cdLeft--;
            }
        }.runTaskTimer(RisenMain.getInstance(), 20, 20);
    }
}