package xyz.fallenmc.risenboss.main.utils;

import me.zach.DesertMC.DesertMain;
import me.zach.DesertMC.Utils.reflection.ReflectionUtils;
import me.zach.DesertMC.Utils.structs.Pair;
import net.jitse.npclib.api.NPC;
import net.jitse.npclib.internal.NPCBase;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import xyz.fallenmc.risenboss.main.inventories.BossActivationInventory;
import xyz.fallenmc.risenboss.main.BossPreferences;
import xyz.fallenmc.risenboss.main.RisenMain;
import xyz.fallenmc.risenboss.main.abilities.RisenAbility;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RisenUtils {
    public static BossPreferences getPreferences(Player player){
        List<String> abilityNames = RisenMain.getInstance().getConfig().getStringList("players." + player.getUniqueId() + ".boss.selected");
        RisenAbility[] abilities = new RisenAbility[abilityNames.size()];
        for(int i = 0; i<abilities.length; i++) abilities[i] = RisenAbility.valueOf(abilityNames.get(i));
        return new BossPreferences(abilities);
    }

    public static void setPreferences(BossPreferences preferences, Player player){
        RisenAbility[] abilities = new RisenAbility[preferences.enabledAbilities.size()];
        List<String> abilityNames = new ArrayList<>();
        for(int i = 0; i<abilities.length; i++) abilityNames.add(abilities[i].name());
        RisenMain.getInstance().getConfig().set("players." + player.getUniqueId() + ".boss.selected", abilityNames);
    }

    public static void openBossActivationInventory(Player player){
        Inventory inv;
        if(bossIsReady(player)){
            inv = BossActivationInventory.READY.getInventory();
        }else inv = BossActivationInventory.NOT_READY;
        player.openInventory(inv);
    }

    public static boolean bossIsReady(Player player){
        RisenMain main = RisenMain.getInstance();
        boolean ready = main.getConfig().getBoolean("players." + player.getUniqueId() + ".boss.ready");
        return ready;
    }

    public static void setBossReady(Player player, boolean ready){
        RisenMain main = RisenMain.getInstance();
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

    public static void setWinsToNextAbilitySlot(Player player, int wins){
        RisenMain main = RisenMain.getInstance();
        main.getConfig().set("players." + player.getUniqueId() + ".boss.tonextslot", wins);
        main.saveConfig();
    }

    public static int getWinsToNextAbilitySlot(Player player){
        RisenMain main = RisenMain.getInstance();
        return main.getConfig().getInt("players." + player.getUniqueId() + ".boss.tonext");
    }

    public static void setAbilitySlots(Player player, int slots){
        RisenMain main = RisenMain.getInstance();
        main.getConfig().set("players." + player.getUniqueId() + ".boss.abilityslots", slots);
        main.saveConfig();
    }

    public static int getAbilitySlots(Player player){
        RisenMain main = RisenMain.getInstance();
        return main.getConfig().getInt("players." + player.getUniqueId() + ".boss.abilityslots");
    }
}