package xyz.fallenmc.risenboss.main.abilities;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTEntity;
import de.tr7zw.nbtapi.NBTItem;
import me.zach.DesertMC.GameMechanics.Events;
import me.zach.DesertMC.Utils.Particle.ParticleEffect;
import me.zach.DesertMC.Utils.StringUtils.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;

import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import xyz.fallenmc.risenboss.main.RisenMain;

import java.util.ArrayList;
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
                        knockedEntity.getWorld().playSound(knockedEntity.getLocation(), Sound.DIG_WOOD, 10, 0.85f);
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
            player.setVelocity(velocity.setY(velocity.getY() + 4));
            player.getWorld().playSound(player.getLocation(), Sound.ENDERDRAGON_WINGS, 10, 0.8f);
            Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> {
                player.setVelocity(player.getVelocity().setY(-5));
                Entity playerEntity = player;
                new BukkitRunnable(){
                    public void run(){
                        if(playerEntity.isOnGround()){
                            player.getWorld().playSound(player.getLocation(), Sound.DIG_GRASS, 10, 0.9f);
                            for(int i = 0; i<10; i++){
                                ParticleEffect.BLOCK_CRACK.display(new ParticleEffect.BlockData(Material.GRASS, (byte) 1), (float) Math.random() * 2, (float) Math.random() * 2,  (float) Math.random() * 2, 1, 1, player.getLocation(), 10);
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
