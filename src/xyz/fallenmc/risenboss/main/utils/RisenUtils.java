package xyz.fallenmc.risenboss.main.utils;

import me.zach.DesertMC.Utils.structs.Pair;
import me.zach.DesertMC.DesertMain;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import xyz.fallenmc.risenboss.main.BossActivationInventory;
import xyz.fallenmc.risenboss.main.BossPreferences;
import xyz.fallenmc.risenboss.main.RisenMain;
import xyz.fallenmc.risenboss.main.abilities.RisenAbility;

import java.util.*;

public class RisenUtils {

    public static BossPreferences getPreferences(Player player){
        return new BossPreferences(RisenAbility.values());
    }

    public static void openBossActivationInventory(Player player){
        Inventory inv;
        if(bossIsReady(player)){
            inv = BossActivationInventory.READY.getInventory();
        } else inv = BossActivationInventory.NOT_READY;
        player.openInventory(inv);
    }

    public static boolean bossIsReady(Player player){
        DesertMain main = DesertMain.getInstance;
        return main.getConfig().getBoolean("players." + player.getUniqueId() + ".boss.ready");
    }

    public static void setBossReady(Player player, boolean ready){
        DesertMain main = DesertMain.getInstance;
        main.getConfig().set("players." + player.getUniqueId() + ".boss.ready", ready);
        main.saveConfig();
    }

    public static boolean isBoss(UUID uuid) {
        if(RisenMain.currentBoss == null) return false;
        else return RisenMain.currentBoss.getUUID().equals(uuid);
    }

    public static <T> List<T> collectFirsts(Pair<T, ?>[] pairs){
        List<T> firsts = new ArrayList<>();
        for(Pair<T, ?> pair : pairs){
            firsts.add(pair.first);
        }
        return firsts;
    }

    public static <T> List<T> collectSeconds(Pair<?, T>[] pairs){
        List<T> seconds = new ArrayList<>();
        for(Pair<?, T> pair : pairs){
            seconds.add(pair.second);
        }
        return seconds;
    }
}
