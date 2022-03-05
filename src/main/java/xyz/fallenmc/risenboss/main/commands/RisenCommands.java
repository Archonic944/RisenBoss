package xyz.fallenmc.risenboss.main.commands;

import me.zach.DesertMC.GameMechanics.Events;
import me.zach.DesertMC.Utils.MiscUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.fallenmc.risenboss.main.RisenBoss;
import xyz.fallenmc.risenboss.main.RisenMain;
import xyz.fallenmc.risenboss.main.utils.RisenUtils;

public class RisenCommands implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String alias, String[] args){
        if(command.getName().equalsIgnoreCase("rise")){
            if((commandSender instanceof Player && MiscUtils.isAdmin((Player) commandSender)) || commandSender.hasPermission("admin")){
                Player target;
                if(args.length >= 1) target = Bukkit.getPlayer(args[0]);
                else if(commandSender instanceof Player) target = (Player) commandSender;
                else return false;
                if(target != null){
                    RisenUtils.activateBossReady(target);
                }else{
                    commandSender.sendMessage(ChatColor.RED + "That player either isn't online, or doesn't exist!");
                }
            }
        }else if(command.getName().equalsIgnoreCase("endboss")){
            if((commandSender instanceof Player && MiscUtils.isAdmin((Player) commandSender)) || commandSender.hasPermission("admin") && args.length > 0){
                RisenBoss currentBoss = RisenMain.currentBoss;
                if(currentBoss == null) commandSender.sendMessage(ChatColor.RED + "No risen boss is currently active!");
                else{
                    RisenBoss.EndReason reason = null;
                    if(args[0].equalsIgnoreCase("win") || args[0].equalsIgnoreCase("won"))
                        reason = RisenBoss.EndReason.UNKNOWN_WIN;
                    else if(args[0].equalsIgnoreCase("loss") || args[0].equalsIgnoreCase("lost") || args[0].equalsIgnoreCase("lose"))
                        reason = RisenBoss.EndReason.UNKNOWN_LOSS;
                    if(reason == null) return false;
                    else{
                        Events.respawn(currentBoss.getPlayer());
                        currentBoss.endBoss(reason);
                        commandSender.sendMessage(ChatColor.YELLOW + "Ended " + currentBoss.name + "'s Risen Boss session with reason " + reason.name());
                    }
                }
            }
        }
        return true;
    }
}