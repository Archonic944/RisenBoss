package xyz.fallenmc.risenboss.main.abilities;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTEntity;
import de.tr7zw.nbtapi.NBTItem;
import me.zach.DesertMC.GameMechanics.Events;
import me.zach.DesertMC.Utils.ActionBar.ActionBarUtils;
import me.zach.DesertMC.Utils.Particle.ParticleEffect;
import me.zach.DesertMC.Utils.StringUtils.StringUtil;
import org.bukkit.*;

import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import xyz.fallenmc.risenboss.main.RisenMain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public enum RisenAbility implements Listener {
    TEST("Test", "This is a test ability, hopefully the test succeeds!", Material.DIODE, 10, false){
        @Override
        protected void activationInternal(Player player) {
            player.sendMessage("Abilities system works!");
        }
    },
    SPRINT("Sprint", "Activate this ability to break into a quick sprint, doubling your move speed! For 4 seconds, anyone caught in the path of your whirlwind of speed is damaged and knocked away.", Material.FEATHER, 20, false){
        protected void activationInternal(Player player){
            float walkSpeed = player.getWalkSpeed();
            player.setWalkSpeed(walkSpeed * 2);
            BukkitTask sprintTask = Bukkit.getScheduler().runTaskTimer(RisenMain.getInstance(), () -> {
                for(Entity knockedEntity : player.getNearbyEntities(0.1, 0.1, 0.1)){
                    Damageable toDamage = canDamage(knockedEntity);
                    if(toDamage != null) {
                        toDamage.damage(10);
                        knockedEntity.setVelocity(knockedEntity.getVelocity().multiply(2));
                        knockedEntity.getWorld().playSound(knockedEntity.getLocation(), Sound.ZOMBIE_WOODBREAK, 10, 0.85f);
                        UUID uuid = knockedEntity.getUniqueId();
                        Events.invincible.add(uuid);
                        Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> Events.invincible.remove(uuid), 15);
                        ParticleEffect.SMOKE_NORMAL.display(0, 2, 0, 1, 1, player.getLocation(), 15);
                    }
                }
            }, 1, 1);
            Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> {
                sprintTask.cancel();
                player.setWalkSpeed(walkSpeed);
            }, 80);
        }
    },
    DODGE("Duck and Weave", "When this ability is activated, you'll have supreme agility for 7 seconds, granting you a 50% chance to dodge hits while crouching.", Material.ARROW, 25, true){
        boolean active = false;
        @EventHandler(priority = EventPriority.HIGH)
        public void onHit(EntityDamageByEntityEvent event){
            Entity entity = event.getEntity();
            if(active && entity.getUniqueId().equals(RisenMain.currentBoss.getUUID())){
                if(ThreadLocalRandom.current().nextBoolean()){
                    if(entity instanceof Player) ((Player) entity).playSound(entity.getLocation(), Sound.ENDERDRAGON_WINGS, 10, 1.1f);
                    Entity damager = event.getDamager();
                    if(damager instanceof Player) ((Player) damager).playSound(damager.getLocation(), Sound.ENDERDRAGON_WINGS, 10, 1.1f);
                    event.setCancelled(true);
                }
            }
        }

        @Override
        protected void activationInternal(Player player) {
            active = true;
            Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> active = false, 140);
        }
    },
    GROUND_SLAM("Ground Slam", "This ability, when used, first launches you high in the air, the shoots you back on the ground, sending out a shockwave damaging anyone caught in the impact!", Material.GRASS, 20, false){
        @Override
        protected void activationInternal(Player player){
            Vector velocity = player.getVelocity();
            Location location = player.getLocation();
            player.setVelocity(velocity.setY(velocity.getY() + 4));
            player.getWorld().playSound(location, Sound.ENDERDRAGON_WINGS, 10, 0.8f);
            Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> {
                player.setVelocity(player.getVelocity().setY(-5));
                new BukkitRunnable(){
                    public void run(){
                        if(((Entity) player).isOnGround()){
                            player.getWorld().playSound(location, Sound.DIG_GRASS, 10, 0.9f);
                            ParticleEffect.BlockData data = new ParticleEffect.BlockData(location.getBlock().getType(), (byte) 0);
                            for(int i = 0; i<10; i++){
                                ParticleEffect.BLOCK_CRACK.display(data, (float) Math.random() * 2, (float) Math.random() * 2,  (float) Math.random() * 2, 1, 1, location, 10);
                            }
                            for(Entity entity : player.getNearbyEntities(6, 2, 6)) {
                                Damageable toDamage = canDamage(entity);
                                if(toDamage != null){
                                    toDamage.damage(5.9);
                                    Vector entityVelocity = toDamage.getVelocity();
                                    toDamage.setVelocity(entityVelocity.setY(entityVelocity.getY() + 1));
                                }
                            }
                            cancel();
                        }
                    }
                }.runTaskTimer(RisenMain.getInstance(), 0, 1);
            }, 20);
        }
    },

    REJUVENATE("Rejuvenate", "Hold down this ability to hone your focus in such a way that, if you are left undisturbed for 8 seconds, you completely regain all of your health.", Material.POTION, 45, true){
        final HashMap<UUID, BukkitRunnable> rejMap = new HashMap<>();
        @Override
        protected void activationInternal(Player player){
            float[] count = {0};
            float walkSpeed = player.getWalkSpeed();
            float flySpeed = player.getFlySpeed();
            float[] tone = {1};
            Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + player.getName() + " is rejuvenating!");
            BukkitRunnable rejRunnable = new BukkitRunnable() {
                public void run() {
                    if(count[0] < 8) {
                        player.playSound(player.getLocation(), Sound.NOTE_STICKS, 10, tone[0]);
                        tone[0] = tone[0] + 0.01f;
                        count[0] = count[0] + 0.05f;
                        player.setFlySpeed(player.getFlySpeed() - 0.0001f);
                        player.setWalkSpeed(player.getWalkSpeed() - 0.0001f);
                        ActionBarUtils.sendActionBar(player, ChatColor.GOLD + ChatColor.BOLD.toString() + "Rejuvinating... " + ChatColor.GOLD.toString() + count[0] + ChatColor.GRAY + " | " + ChatColor.RED + ChatColor.UNDERLINE + "Don't switch items!");
                    }else{
                        player.setWalkSpeed(walkSpeed);
                        player.setFlySpeed(flySpeed);
                        player.setHealth(player.getMaxHealth());
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 10, 1);
                        ParticleEffect.SPELL_INSTANT.display(0, 0, 0, 1, 10, player.getLocation(), 15);
                    }
                }
                public void cancel(){
                    super.cancel();
                    ActionBarUtils.sendActionBar(player, ChatColor.RED + "Rejuvenation CANCELLED!");
                    player.playSound(player.getLocation(), Sound.NOTE_BASS, 10, 1);
                }
            };
            rejRunnable.runTaskTimer(RisenMain.getInstance(), 0, 1);
            rejMap.put(player.getUniqueId(), rejRunnable);
        }

        @EventHandler
        public void hotbarSwitchCancel(PlayerItemHeldEvent event){
            cancelRejuvenation(event.getPlayer());
        }
        @EventHandler
        public void damageCancel(EntityDamageByEntityEvent event){
            Entity entity = event.getEntity();
            if(entity instanceof Player) cancelRejuvenation((Player) entity);
        }
        private void cancelRejuvenation(Player player){
            BukkitRunnable rejRunnable = rejMap.remove(player.getUniqueId());
            if(rejRunnable != null) rejRunnable.cancel();
        }
    };

    private static Damageable canDamage(Entity entity){
        NBTEntity nbt = new NBTEntity(entity);
        return !(nbt.getBoolean("Invulnerable")) && entity instanceof Damageable ? (Damageable) entity : null;
    }

    protected abstract void activationInternal(Player player);

    public final ItemStack hotbarItem;
    public final String name;
    public final int cooldown;

    RisenAbility(String name, String description, Material icon, int cooldownSecs, boolean registerEvents){
        ItemStack hotbarItem = new ItemStack(icon);
        ItemMeta hotbarMeta = hotbarItem.getItemMeta();
        hotbarMeta.setDisplayName(ChatColor.GOLD + name + ChatColor.DARK_GRAY + " (Right Click)");
        ArrayList<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(StringUtil.wrapLore(ChatColor.GRAY + description));
        lore.add(ChatColor.DARK_GRAY + "Cooldown: " + cooldownSecs + " seconds");
        hotbarMeta.setLore(lore);
        hotbarItem.setItemMeta(hotbarMeta);
        NBTItem hotbarNBT = new NBTItem(hotbarItem);
        hotbarNBT.setBoolean("Unbreakable", true);
        NBTCompound hotbarAttributes = hotbarNBT.addCompound("CustomAttributes");
        hotbarAttributes.setString("ABILITY", name());
        this.hotbarItem = hotbarNBT.getItem();
        this.name = name;
        cooldown = cooldownSecs;
        if(registerEvents) Bukkit.getPluginManager().registerEvents(this, RisenMain.getInstance());
    }

    public String toString(){
        return name;
    }
}
