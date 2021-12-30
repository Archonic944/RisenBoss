package xyz.fallenmc.risenboss.main.utils;

import me.zach.DesertMC.Utils.Config.ConfigUtils;
import me.zach.DesertMC.Utils.reflection.ReflectionUtils;
import me.zach.DesertMC.Utils.structs.Pair;
import net.jitse.npclib.api.NPC;
import net.jitse.npclib.internal.NPCBase;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.fallenmc.risenboss.main.data.RisenData;
import xyz.fallenmc.risenboss.main.inventories.BossActivationInventory;
import xyz.fallenmc.risenboss.main.RisenMain;
import xyz.fallenmc.risenboss.main.abilities.RisenAbility;

import java.util.*;

public class RisenUtils {
    private static final String dashWrap = ChatColor.GREEN.toString() + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "-----------------------------------------";
    private static final String halfDashWrap = dashWrap.substring(0, dashWrap.length() / 2 - 4);
    public static final int MINIMUM_ABILITY_SLOTS = 3;
    public static final int WINS_PER_ABILITY_SLOT = 15;
    public static final int MAX_ABILITY_SLOTS = 6;

    public static List<RisenAbility> getPreferences(Player player){
        return new ArrayList<>();
    }

    public static void openBossActivationInventory(Player player){
        Inventory inv;
        if(getData(player).isBossReady()){
            inv = new BossActivationInventory(player).getInventory();
        }else inv = BossActivationInventory.getNotReady();
        player.openInventory(inv);
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

    public static void teleportNPC(NPC npc, Location location){
        npc.setLocation(location);
        boolean isOnGround = Math.floor(location.getY()) == location.getY() && !location.clone().subtract(0, 1, 0).getBlock().getType().isSolid() || location.getBlock().getType() == Material.SNOW;
        PacketPlayOutEntityTeleport teleport = new PacketPlayOutEntityTeleport();
        ReflectionUtils.setValues(teleport, new String[]{"a", "b", "c", "d", "e", "f", "g"}, new Object[]{npc.getId(), Math.floor(location.getX() * 32), Math.floor(location.getY() * 32), Math.floor(location.getZ() * 32), (byte)((int)(location.getYaw() * 256.0F / 360.0F)), (byte)((int)(location.getPitch() * 256.0F / 360.0F)), isOnGround});
        for(UUID uuid : ((NPCBase) npc).getShown()){
            EntityPlayer craftPlayer = ((CraftPlayer) Bukkit.getPlayer(uuid)).getHandle();
            craftPlayer.playerConnection.sendPacket(teleport);
        }
    }

    public static void activateBossReady(Player player){
        RisenUtils.getData(player).setBossReady(true);
        player.sendMessage(halfDashWrap + ChatColor.GREEN + ChatColor.BOLD + "↑PREPARE TO RISE↑" + halfDashWrap +
                ChatColor.DARK_GREEN + "\n   Congrats! You hit a 50 streak with Fallen Armor, so now you get to become a RISEN BOSS! To activate it, retrieve your Fallen Core from the Wongo the Wither, and right click it to activate it!\n"
                + dashWrap + "-");
        player.playSound(player.getLocation(), Sound.WITHER_SPAWN, 10, 1);
        RisenMain.alreadyUsed.add(player.getUniqueId());
        RisenUtils.openBossActivationInventory(player);
    }

    public static RisenData getData(Player player){
        return ConfigUtils.getData(player.getUniqueId()).getRisenData();
    }
}