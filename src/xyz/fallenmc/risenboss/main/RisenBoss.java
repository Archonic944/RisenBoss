package xyz.fallenmc.risenboss.main;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import itempackage.Items;
import me.zach.DesertMC.ClassManager.TravellerEvents;
import me.zach.DesertMC.Utils.MiscUtils;
import me.zach.DesertMC.Utils.Particle.ParticleEffect;
import me.zach.DesertMC.Utils.RankUtils.RankEvents;
import me.zach.DesertMC.Utils.StringUtils.StringUtil;
import net.jitse.npclib.api.NPC;
import net.jitse.npclib.api.skin.Skin;
import net.jitse.npclib.api.state.NPCSlot;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import xyz.fallenmc.risenboss.main.abilities.Ability;
import xyz.fallenmc.risenboss.main.abilities.RisenAbility;
import xyz.fallenmc.risenboss.main.utils.BossBarUtil;

import java.util.*;


public final class RisenBoss {
    public final HashMap<String, Ability> abilityInstances = new HashMap<>();
    public final String rankColor;
    HashMap<UUID, Double> damagers = new HashMap<>();
    public RisenBoss(Player player, BossPreferences abilities){
        RisenMain.currentBoss = this;
        //saving inventory
        prevPlayerInventory = player.getInventory().getContents();
        Plugin fallenMain = Bukkit.getPluginManager().getPlugin("Fallen");
        uuid = player.getUniqueId();
        //setting rankColor for quick access
        if(RankEvents.rankSession.containsKey(uuid)) rankColor = RankEvents.rankSession.get(uuid).c.toString();
        else rankColor = "§7";
        //preparing dummy player npc for the wizard class's "Dummy!" ability
        dummyPlayer = RisenMain.getNpcLib().createNPC(Collections.singletonList(rankColor + player.getName()));
        //getting skin signature and id from the player
        EntityPlayer NMSplayer = ((CraftPlayer) player).getHandle();
        GameProfile profile = NMSplayer.getProfile();
        Property property = profile.getProperties().get("textures").iterator().next();
        String value = property.getValue();
        String signature = property.getSignature();
        dummyPlayer.setSkin(new Skin(value, signature));
        //setting slots on the npc to match the player's armor (as well as getting the players inventory)
        PlayerInventory playerInventory = player.getInventory();
        dummyPlayer.setItem(NPCSlot.BOOTS, playerInventory.getBoots());
        dummyPlayer.setItem(NPCSlot.LEGGINGS, playerInventory.getLeggings());
        dummyPlayer.setItem(NPCSlot.CHESTPLATE, playerInventory.getChestplate());
        dummyPlayer.setItem(NPCSlot.HELMET, playerInventory.getHelmet());
        //replacing inventory
        playerInventory.clear();
        playerInventory.setItem(27, Items.getRisenBlade());
        int j = 0;
        for(int i = 29; i < 36; i++){
            if(j < abilities.enabledAbilities.size()){
                RisenAbility ability = abilities.enabledAbilities.get(j);
                playerInventory.setItem(i, ability.hotbarItem);
                j++;
            }else break;
        }
        //preparing ability instances map
        for(RisenAbility risenAbility : abilities.enabledAbilities){
            abilityInstances.put(risenAbility.name(), new Ability(risenAbility));
        }
        //temporarily removing from Traveller notifications because of action bar cooldown messages
        TravellerEvents.blockNotifs.remove(uuid);
        //boss message
        Bukkit.broadcastMessage(rankColor + name + " " + ChatColor.GOLD + "just because a RISEN BOSS! Fight them to gain rewards!");
        //setting max health
        player.setMaxHealth(player.getMaxHealth() + 16);
        //initializing timers and other things
        initRunnables();
    }



    public final String name = getPlayer().getName();
    private final NPC dummyPlayer;
    private int damageTaken = 0;
    private static final int secondsToReach = 600;
    private int secondsLeft = secondsToReach;
    private BukkitTask fallenFlames;
    private BukkitTask timer;
    private BukkitTask callout;

    public int getDamageDealt() {
        return damageDealt;
    }

    private int damageDealt = 0;
    final ItemStack[] prevPlayerInventory;

    public int getDamageTaken() {
        return damageTaken;
    }


    public ItemStack[] getPrevPlayerInventory() {
        return prevPlayerInventory;
    }

    public UUID getUUID() {
        return uuid;
    }

    public Player getPlayer(){
        return Bukkit.getPlayer(uuid);
    }

    private final UUID uuid;

    public void bossDamage(UUID damager, double damage){
        if(!damager.equals(uuid)){
            damagers.put(damager, damagers.containsKey(damager) ? damagers.get(damager) + damage : damage);
            damageTaken += damage;
            Player player = getPlayer();
            float healthFloat = (float) (player.getHealth() / player.getMaxHealth());
            for(UUID uuid : BossBarUtil.getPlayers()){
                BossBarUtil.updateHealth(Bukkit.getPlayer(uuid), healthFloat);
            }
        }
    }

    public void bossHit(double damage){
        damageDealt += damage;
        Player player = getPlayer();
        float healthFloat = (float) (player.getHealth() / player.getMaxHealth());
        for(UUID uuid : BossBarUtil.getPlayers()){
            BossBarUtil.updateHealth(Bukkit.getPlayer(uuid), healthFloat);
        }
    }


    private void initRunnables(){
        Player player = getPlayer();
        Location location = player.getLocation();
        flamesInit(location);
        timerInit(player);
        calloutInit(location, rankColor + player.getName());
    }

    public void endBoss(EndReason reason){
        fallenFlames.cancel();
        dummyPlayer.destroy();
        timer.cancel();
        callout.cancel();
        BossBarUtil.clearAllBars();
        Player player = getPlayer();
        player.getInventory().setContents(prevPlayerInventory);
        if(reason.won){
            //TODO do this gabriel!
            StringUtil.sendCenteredWrappedMessage(player, new StringUtil.ChatWrapper('*', ChatColor.GREEN, true, false), ChatColor.GREEN + "YOU WIN!");

        }
    }

    private void timerInit(Player player){
        timer = new BukkitRunnable(){
            @Override
            public void run() {
                secondsLeft--;
                int minutesLeft = Math.floorDiv(secondsLeft, 60);
                int secondsWithoutMinutes = secondsLeft % 60;
                Set<UUID> bossBars = BossBarUtil.getPlayers();
                String text = ChatColor.GOLD + ChatColor.BOLD.toString() + "RISEN BOSS: " + rankColor + ChatColor.BOLD + player.getName() + ChatColor.YELLOW +  String.format("%d:%02d", minutesLeft, secondsWithoutMinutes);
                for(Player player : Bukkit.getOnlinePlayers()){
                    if(bossBars.contains(player.getUniqueId())){
                        BossBarUtil.updateText(player, text);
                    }else BossBarUtil.setBar(player, text, (float) (player.getHealth() / player.getMaxHealth()));
                }
                if(secondsLeft <= 0){
                    endBoss(EndReason.TIMER_FINISHED);
                    cancel();
                }
            }
        }.runTaskTimer(RisenMain.getInstance(), 20, 20);
    }

    private void flamesInit(Location location){
        fallenFlames = Bukkit.getScheduler().runTaskTimer(RisenMain.getInstance(), () -> {
            float xOffset = (float) Math.random() * 2;
            float yOffset = (float) Math.random() * 2;
            float zOffset = (float) Math.random() * 2;
            Location auraLocation = location.clone().add(xOffset, yOffset, zOffset);
            ParticleEffect.FLAME.display(-xOffset, -yOffset, -zOffset, 1.4f, 1, auraLocation, 75);
        }, 0, 7);
    }

    private void calloutInit(Location location, String playerName){
        callout = Bukkit.getScheduler().runTaskTimer(RisenMain.getInstance(), () -> {
            Bukkit.getServer().broadcastMessage(playerName + ChatColor.GRAY + " is at " + ChatColor.YELLOW + "(" + location.getX() + ", " + location.getY() + ", " + location.getZ() + ")" + ChatColor.YELLOW + "!" + ChatColor.GRAY + " Come and get them!");
            MiscUtils.spawnFirework(location, 5, false, true, FireworkEffect.Type.BURST, Color.YELLOW);
        }, 800, 800);
    }

    public enum EndReason{
        TIMER_FINISHED(true),
        BOSS_VANQUISHED(false),
        UNKNOWN_WIN(true),
        UNKNOWN_LOSS(false),
        BOSS_QUIT(false);

        public final boolean won;
        EndReason(boolean bossWon){
            won = bossWon;
        }
    }
}