package xyz.fallenmc.risenboss.main;

import net.jitse.npclib.NPCLib;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.fallenmc.risenboss.main.commands.RisenCommands;
import xyz.fallenmc.risenboss.main.events.RisenEvents;
import xyz.fallenmc.risenboss.main.utils.BossBarUtil;

public class RisenMain extends JavaPlugin {
    private static RisenMain instance;
    private static NPCLib npcLib;
    public static NPCLib getNpcLib(){return npcLib;}
    public static RisenBoss currentBoss = null;

    public static RisenMain getInstance(){
        return instance;
    }

    public void onEnable(){
        instance = this;
        npcLib = new NPCLib(this);
        RisenCommands commands = new RisenCommands();
        getCommand("rise").setExecutor(commands);
        Bukkit.getPluginManager().registerEvents(new RisenEvents(), this);

        new Thread(() -> {
            while(true) {
                for(String s : BossBarUtil.getPlayers()) {
                    Player o = Bukkit.getPlayer(s);
                    if(o != null) BossBarUtil.teleportBar(o);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
