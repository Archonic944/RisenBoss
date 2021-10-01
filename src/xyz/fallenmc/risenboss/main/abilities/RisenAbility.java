package xyz.fallenmc.risenboss.main.abilities;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import me.zach.DesertMC.ClassManager.CoruManager.EventsForCorruptor;
import me.zach.DesertMC.GameMechanics.Events;
import me.zach.DesertMC.Utils.ActionBar.ActionBarUtils;
import me.zach.DesertMC.Utils.MiscUtils;
import me.zach.DesertMC.Utils.Particle.ParticleEffect;
import me.zach.DesertMC.Utils.PlayerUtils;
import me.zach.DesertMC.Utils.StringUtils.StringUtil;
import me.zach.DesertMC.Utils.structs.Pair;

import net.jitse.npclib.api.NPC;
import net.jitse.npclib.api.events.NPCInteractEvent;
import net.jitse.npclib.api.state.NPCAnimation;
import net.jitse.npclib.api.state.NPCSlot;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import xyz.fallenmc.risenboss.main.RisenBoss;
import xyz.fallenmc.risenboss.main.RisenMain;
import xyz.fallenmc.risenboss.main.utils.RisenUtils;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public enum RisenAbility implements Listener {
    TEST("Test", "This is a test ability, hopefully the test succeeds!", Material.DIODE, 10, false){
        protected boolean activationInternal(RisenBoss boss){
            Player player = boss.getPlayer();
            player.sendMessage("Abilities system works!");
            return true;
        }
    },
    SPRINT("Sprint", "Activate this ability to break into a quick sprint, doubling your move speed! For 4 seconds, anyone caught in the path of your whirlwind of speed is damaged and knocked away.", Material.FEATHER, 20, false){
        protected boolean activationInternal(RisenBoss boss){
            Player player = boss.getPlayer();
            float walkSpeed = player.getWalkSpeed();
            player.setWalkSpeed(walkSpeed * 2);
            BukkitTask sprintTask = Bukkit.getScheduler().runTaskTimer(RisenMain.getInstance(), () -> {
                for(Damageable toDamage : MiscUtils.getNearbyDamageables(player, 0.5)){
                    toDamage.setVelocity(toDamage.getVelocity().multiply(2));
                    toDamage.getWorld().playSound(toDamage.getLocation(), Sound.ZOMBIE_WOODBREAK, 10, 0.85f);
                    UUID uuid = toDamage.getUniqueId();
                    Events.invincible.add(uuid);
                    Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> Events.invincible.remove(uuid), 15);
                    ParticleEffect.SMOKE_NORMAL.display(0, 2, 0, 2, 1, player.getLocation(), 15);
                }
            }, 1, 1);
            Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> {
                sprintTask.cancel();
                player.setWalkSpeed(walkSpeed);
            }, 80);
            return true;
        }
    },
    DODGE("Duck and Weave", "When this ability is activated, you'll have supreme agility for 7 seconds, granting you a 50% chance to dodge hits while crouching.", Material.ARROW, 25, true){
        final Set<UUID> activePlayers = new HashSet<>();
        @EventHandler(priority = EventPriority.HIGH)
        public void onHit(EntityDamageByEntityEvent event){
            Entity entity = event.getEntity();
            UUID uuid = entity.getUniqueId();
            if(activePlayers.contains(uuid) && RisenUtils.isBoss(uuid)){
                if(ThreadLocalRandom.current().nextBoolean()){
                    if(entity instanceof Player) ((Player) entity).playSound(entity.getLocation(), Sound.ENDERDRAGON_WINGS, 10, 1.1f);
                    Entity damager = event.getDamager();
                    if(damager instanceof Player) ((Player) damager).playSound(damager.getLocation(), Sound.ENDERDRAGON_WINGS, 10, 1.1f);
                    event.setCancelled(true);
                }
            }
        }

        protected boolean activationInternal(RisenBoss player) {
            UUID uuid = player.getUUID();
            activePlayers.add(uuid);
            Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> activePlayers.remove(uuid), 140);
            return true;
        }
    },
    GROUND_SLAM("Ground Slam", "This ability, when used, first launches you high in the air, the shoots you back on the ground, sending out a shockwave damaging anyone caught in the impact!", Material.GRASS, 20, false){
        protected boolean activationInternal(RisenBoss boss){
            Player player = boss.getPlayer();
            Vector velocity = player.getVelocity();
            Location location = player.getLocation();
            player.setVelocity(velocity.setY(velocity.getY() + 2.5));
            player.getWorld().playSound(location, Sound.ENDERDRAGON_WINGS, 10, 0.8f);
            Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> {
                player.setVelocity(player.getVelocity().setY(-5).add(player.getLocation().getDirection().multiply(1.2)));
                new BukkitRunnable(){
                    int count = 0;
                    public void run(){
                        if(((Entity) player).isOnGround()){
                            player.getWorld().playSound(location, Sound.BLAZE_HIT, 10, 0.8f);
                            ParticleEffect.BlockData data = new ParticleEffect.BlockData(location.getBlock().getType(), (byte) 0);
                            for(int i = 0; i<10; i++){
                                ParticleEffect.BLOCK_CRACK.display(data, (float) MiscUtils.trueRandom() * 2, (float) MiscUtils.trueRandom() * 2,  (float) MiscUtils.trueRandom() * 2, 1, 1, location, 10);
                            }
                            for(Entity entity : player.getNearbyEntities(6, 2, 6)) {
                                Damageable toDamage = MiscUtils.canDamage(entity);
                                if(toDamage != null){

                                    PlayerUtils.trueDamage(toDamage, 5.83, player);
                                    Vector entityVelocity = toDamage.getVelocity();
                                    toDamage.setVelocity(entityVelocity.setY(entityVelocity.getY() + 1));
                                }
                            }
                            cancel();
                        }else if(count > 1000) cancel();
                        else count++;
                    }
                }.runTaskTimer(RisenMain.getInstance(), 0, 1);
            }, 20);
            return true;
        }
    },
    REJUVENATE("Rejuvenate", "Hold down this ability to hone your focus in such a way that, if you are left undisturbed for 8 seconds, you completely regain all of your health.", Material.YELLOW_FLOWER, 45, true){
        final HashMap<UUID, BukkitRunnable> rejMap = new HashMap<>();
        final ParticleEffect.ParticleColor HEAL_COLOR = new ParticleEffect.OrdinaryColor(Color.fromRGB(249, 152, 168));
        protected boolean activationInternal(RisenBoss boss){
            Player player = boss.getPlayer();
            Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + player.getName() + " is rejuvenating!");
            BukkitRunnable rejRunnable = new BukkitRunnable() {
                float count = 0;
                final float walkSpeed = player.getWalkSpeed();
                final float flySpeed = player.getFlySpeed();
                float tone = 1;
                final DecimalFormat secondsFormatter = new DecimalFormat("#.##");
                public void run() {
                    if(count < 8) {
                        player.playSound(player.getLocation(), Sound.NOTE_STICKS, 10, tone);
                        tone += 0.004f;
                        count += 0.05f;
                        player.setFlySpeed(player.getFlySpeed() - 0.02f);
                        player.setWalkSpeed(player.getWalkSpeed() - 0.02f);
                        ActionBarUtils.sendActionBar(player, ChatColor.GOLD + ChatColor.BOLD.toString() +
                                "Rejuvenating... " + ChatColor.GOLD + secondsFormatter.format(count) + ChatColor.GRAY + " | " + ChatColor.RED + ChatColor.UNDERLINE + "Don't switch items!");
                    }else{
                        player.setHealth(player.getMaxHealth());
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 10, 1);
                        Location location = player.getLocation();
                        for(int i = 0; i<10; i++)
                            ParticleEffect.SPELL_MOB.display(HEAL_COLOR, location, 25);
                        Bukkit.getServer().broadcastMessage(boss.rankColor + boss.name + ChatColor.GOLD + " fully rejuvenated! Be faster next time!");
                        endRej();
                    }
                }
                public void cancel(){
                    ActionBarUtils.sendActionBar(player, ChatColor.RED + "Rejuvenation CANCELLED!");
                    player.playSound(player.getLocation(), Sound.NOTE_BASS, 10, 1);
                    endRej();
                }
                private void endRej(){
                    super.cancel();
                    player.setWalkSpeed(walkSpeed);
                    player.setFlySpeed(flySpeed);
                    rejMap.remove(player.getUniqueId());
                }
            };
            rejRunnable.runTaskTimer(RisenMain.getInstance(), 0, 1);
            rejMap.put(player.getUniqueId(), rejRunnable);
            return true;
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
        void cancelRejuvenation(Player player){
            BukkitRunnable rejRunnable = rejMap.get(player.getUniqueId());
            if(rejRunnable != null) rejRunnable.cancel();
        }
    },
    HOT_TUB("Hot Tub", "Creates a relaxing lava pool under you that heals 1 health instead of damaging you.", Material.LAVA_BUCKET, 40, true){
        final ParticleEffect.OrdinaryColor HEAL_COLOR = new ParticleEffect.OrdinaryColor(Color.fromRGB(255, 107, 252));
        final HashMap<UUID, Pair<Location, Material>[]> htMap = new HashMap<>();
        @EventHandler
        public void heal(EntityDamageEvent event){
            Entity entity = event.getEntity();
            if(RisenUtils.isBoss(entity.getUniqueId())){
                EntityDamageEvent.DamageCause cause = event.getCause();
                if(cause == EntityDamageEvent.DamageCause.LAVA || cause == EntityDamageEvent.DamageCause.FIRE_TICK || cause == EntityDamageEvent.DamageCause.FIRE){
                    Pair<Location, Material>[] htPairs = htMap.get(entity.getUniqueId());
                    if(htPairs != null){
                        List<Location> locs = RisenUtils.collectFirsts(htPairs);
                        for(Location location : locs){
                            Block block = location.getBlock();
                            if(entity.getLocation().getBlock().equals(block)){
                                event.setCancelled(true);
                                Damageable damageable = MiscUtils.canDamage(entity);
                                if(damageable != null && damageable.getHealth() != damageable.getMaxHealth())
                                    damageable.setHealth(damageable.getHealth() + 0.2);
                                for(int i = 0; i < 8; i++)
                                    ParticleEffect.SPELL_MOB.display(HEAL_COLOR, event.getEntity().getLocation(), 25);
                                break;
                            }
                        }
                    }
                }
            }
        }
        @SuppressWarnings("unchecked")
        protected boolean activationInternal(RisenBoss boss){
            Player player = boss.getPlayer();
            Location playerLocation = player.getLocation();
            Pair<Location, Material>[] htPairs = new Pair[4];
            Location[] htLocs = new Location[htPairs.length];
            htLocs[0] = playerLocation.clone().subtract(1, 0, 0);
            htLocs[1] = playerLocation.clone();
            htLocs[2] = playerLocation.clone().add(0, 0, 1);
            htLocs[3] = playerLocation.clone().add(-1, 0, 1);
            for(int i = 0; i<htPairs.length; i++){
                Location htLoc = htLocs[i];
                Block block = htLoc.getBlock();
                htPairs[i] = new Pair<>(htLoc, block.getType());
                if(MiscUtils.trueEmpty(block)){
                    block.setType(Material.STATIONARY_LAVA);
                }
            }
            UUID uuid = player.getUniqueId();
            htMap.put(uuid, htPairs);
            Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> {
                for(Pair<Location, Material> pair : htPairs){
                    pair.first.getBlock().setType(pair.second);
                }
                Bukkit.getPlayer(uuid).setFireTicks(0);
            }, 100);
            return true;
        }
    },
    RETALIATE("Retaliate", "Using this ability will enact long-overdue revenge on your foes by inflicting Hellfire on the player who has garnered the most boss damage for 4 seconds!", Material.FLINT_AND_STEEL, 30, false){
        protected boolean activationInternal(RisenBoss boss){
            Set<Map.Entry<UUID, Double>> entries = boss.damagers.entrySet();
            Iterator<Map.Entry<UUID, Double>> entryIterator = entries.iterator();
            if(!entryIterator.hasNext()){
                boss.getPlayer().sendMessage(ChatColor.RED + "Since nobody has damaged you yet, there is no one to retaliate against!");
                return false;
            }
            Map.Entry<UUID, Double> topDmg = entryIterator.next();
            while(entryIterator.hasNext()){
                Map.Entry<UUID, Double> entry = entryIterator.next();
                if(entry.getValue() > topDmg.getValue()) topDmg = entry;
            }
            Player topDmgPlayer = Bukkit.getPlayer(topDmg.getKey());
            topDmgPlayer.sendMessage(ChatColor.RED + boss.name + " used " + ChatColor.GOLD + this + ChatColor.RED + " on you, which inflicts Hellfire for 4 seconds!");
            EventsForCorruptor.INSTANCE.hf(topDmgPlayer, 80);
            return true;
        }
    },
    SPELL_BOMB("Spell Bomb",  "This ability shoots a flying potion out in the direction you're looking in, which, after a small duration, explodes, coating a 5-block radius in a dangerous, fast damaging cloud of... something.", Material.POTION, 20, false){
        protected boolean activationInternal(RisenBoss boss) {
            UUID uuid = boss.getUUID();
            Player player = boss.getPlayer();
            ItemStack item = new ItemStack(Material.INK_SACK, 1, (short) 12);
            NBTItem itemNBT = new NBTItem(item);
            itemNBT.addCompound("CustomAttributes").setBoolean("NO_PICKUP", true);
            item = itemNBT.getItem();
            Location eyeLoc = player.getEyeLocation();
            Item droppedItem = player.getWorld().dropItem(eyeLoc, item);
            droppedItem.setVelocity(eyeLoc.getDirection().normalize().multiply(0.7));
            new BukkitRunnable(){
                int count = 0;
                boolean queued = false;
                public void run(){
                    count++;
                    if(queued){
                        if(droppedItem.isOnGround()){
                            //explode the spell bomb
                            new BukkitRunnable(){
                                int explodeCount = 0;
                                public void run(){
                                    if(explodeCount < 8){
                                        for(Damageable toDamage : MiscUtils.getNearbyDamageables(droppedItem, 4.5))
                                            PlayerUtils.trueDamage(toDamage, 5, Bukkit.getPlayer(uuid));
                                        MiscUtils.spawnFirework(droppedItem.getLocation(), 0, false, false, FireworkEffect.Type.BALL, Color.AQUA);
                                        explodeCount++;
                                    }else{
                                        droppedItem.remove();
                                        cancel();
                                    }
                                }
                            }.runTaskTimer(RisenMain.getInstance(), 0, 10);
                            cancel();
                        }else if(count > 200){
                            droppedItem.remove();
                            cancel();
                        }
                    }else if(count >= 20){
                        queued = true;
                    }else if(!droppedItem.isOnGround() && count > 1)
                        ParticleEffect.CRIT.display(0, 0, 0, 0, 1, droppedItem.getLocation(), 50);
                }
            }.runTaskTimer(RisenMain.getInstance(), 0, 2);
            return true;
        }
    },
    DUMMY("Dummy!", "Trick your opponents with this ability by placing a dummy version of yourself, and turning you invisible for 2 seconds!", Material.LEATHER_CHESTPLATE, 27, true){
        final Set<UUID> npcSet = new HashSet<>();
        final Set<UUID> npcNoDamage = new HashSet<>();
        protected boolean activationInternal(RisenBoss boss) {
            Player player = boss.getPlayer();
            ItemStack itemInHand = player.getItemInHand();
            boss.dummyPlayer.setItem(NPCSlot.MAINHAND, itemInHand);
            boss.dummyPlayer.setLocation(player.getLocation());
            for(Player p : Bukkit.getOnlinePlayers()){
                boss.dummyPlayer.show(p);
                if(!p.equals(player)) p.hidePlayer(player);
            }
            Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> {
                for(Player p : Bukkit.getOnlinePlayers()){
                    boss.dummyPlayer.hide(p);
                    if(!p.equals(player)) p.showPlayer(player);
                }
            }, 80);
            return true;
        }

        @EventHandler
        public void playAnimation(NPCInteractEvent event){
            NPC npc = event.getNPC();
            UUID uuid = npc.getUniqueId();
            if(npcSet.contains(uuid) && !npcNoDamage.contains(uuid)){
                if(event.getClickType().equals(NPCInteractEvent.ClickType.LEFT_CLICK)){
                    npc.playAnimation(NPCAnimation.TAKE_DAMAGE);
                    npc.getWorld().playSound(npc.getLocation(), Sound.SUCCESSFUL_HIT, 10, 1);
                    npcNoDamage.add(uuid);
                    Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> npcNoDamage.remove(uuid), 4);
                }
            }
        }
    };

    protected abstract boolean activationInternal(RisenBoss boss);

    public final ItemStack hotbarItem;
    public final String name;
    public final int cooldown;

    RisenAbility(String name, String description, Material icon, int cooldownSecs, boolean registerEvents){
        ItemStack hotbarItem = new ItemStack(icon);
        ItemMeta hotbarMeta = hotbarItem.getItemMeta();
        hotbarMeta.setDisplayName(ChatColor.GOLD + name + ChatColor.GRAY + " (Right Click)");
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