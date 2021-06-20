package xyz.fallenmc.risenboss.main.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.fallenmc.risenboss.main.utils.RisenUtils;

public class RisenCommands implements CommandExecutor {
    private static final String dashWrap = ChatColor.GREEN.toString() + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "-----------------------------------------";
    private static final String halfDashWrap = dashWrap.substring(0, dashWrap.length() / 2 - 4);
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String alias, String[] args){
        if(command.getName().equalsIgnoreCase("rise")){
            if(commandSender.hasPermission("admin")){
                Player target;
                if(args.length == 1)target = Bukkit.getPlayer(args[0]);
                else if(commandSender instanceof Player) target = (Player) commandSender;
                else return false;
                if(target != null){
                    RisenUtils.setBossReady(target, true);
                    target.sendMessage(halfDashWrap + ChatColor.GREEN + ChatColor.BOLD + "↑PREPARE TO RISE↑" + halfDashWrap +
                            ChatColor.DARK_GREEN + "\n   Congrats! You hit a 50 streak with Fallen Armor, so now you get to become a RISEN BOSS! To activate it, retrieve your Fallen Core from the Wongo the Wither, and right click it to activate it!\n"
                            + dashWrap + "-");
                    target.playSound(target.getLocation(), Sound.WITHER_SPAWN, 10, 1);
                }else{
                    commandSender.sendMessage(ChatColor.RED + "That player either isn't online, or doesn't exist!");
                }
            }
        }
        return true;
    }
}
