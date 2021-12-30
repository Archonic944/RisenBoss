package xyz.fallenmc.risenboss.main;

import net.jitse.npclib.NPCLib;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.fallenmc.risenboss.main.commands.RisenCommands;
import xyz.fallenmc.risenboss.main.events.RisenEvents;
import xyz.fallenmc.risenboss.main.utils.BossBarUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RisenMain extends JavaPlugin {
    private static NPCLib npcLib;
    public static NPCLib getNpcLib(){return npcLib;}
    public static RisenBoss currentBoss = null;
    public static Set<UUID> alreadyUsed = new HashSet<>();
    private static RisenMain instance;

    public static Plugin getInstance(){
        return instance;
    }

    public void onEnable(){
        instance = this;
        npcLib = new NPCLib(this);
        RisenCommands commands = new RisenCommands();
        getCommand("rise").setExecutor(commands);
        getCommand("endboss").setExecutor(commands);
        Bukkit.getPluginManager().registerEvents(new RisenEvents(), this);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            Set<UUID> bossBars = BossBarUtil.getPlayers();
            for(UUID uuid : bossBars) {
                Player o = Bukkit.getPlayer(uuid);
                if(o != null) BossBarUtil.teleportBar(o);
            }
        }, 20, 20);
    }
}
