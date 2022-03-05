package xyz.fallenmc.risenboss.main.abilities;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import me.zach.DesertMC.ClassManager.CoruManager.EventsForCorruptor;
import me.zach.DesertMC.GameMechanics.Events;
import me.zach.DesertMC.Prefix;
import me.zach.DesertMC.Utils.ActionBar.ActionBarUtils;
import me.zach.DesertMC.Utils.Config.ConfigUtils;
import me.zach.DesertMC.Utils.MiscUtils;
import me.zach.DesertMC.Utils.Particle.ParticleEffect;
import me.zach.DesertMC.Utils.PlayerUtils;
import me.zach.DesertMC.Utils.StringUtils.StringUtil;
import me.zach.DesertMC.Utils.packet.wrappers.WrapperPlayServerEntityTeleport;
import me.zach.DesertMC.Utils.structs.Pair;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import xyz.fallenmc.risenboss.main.RisenBoss;
import xyz.fallenmc.risenboss.main.RisenMain;
import xyz.fallenmc.risenboss.main.utils.RisenUtils;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;


public enum RisenAbility implements Listener {
    SPRINT("Sprint", "Activate this ability to break into a quick sprint, doubling your move speed! For 5 seconds, anyone caught in the path of your whirlwind of speed is dealt 2 damage and knocked away.", Material.FEATHER, (byte) -1, 20, false, true, "scout", "1.5x damage"){
        final HashMap<UUID, BukkitRunnable> active = new HashMap<>();
        final float WALK_SPEED_BONUS = 0.15f;
        protected boolean activationInternal(boolean multiply, RisenBoss boss){
            Player player = boss.getPlayer();
            UUID uuid = player.getUniqueId();
            player.setWalkSpeed(player.getWalkSpeed() + WALK_SPEED_BONUS);
            BukkitRunnable sprintTask = new BukkitRunnable(){
                int count = 0;
                public void run(){
                    if(count < 100){
                        for(Damageable toDamage : MiscUtils.getNearbyDamageables(player, 0.5)){
                            PlayerUtils.trueDamage(toDamage, multiply ? 2 : 3, player);
                            toDamage.setVelocity(toDamage.getVelocity().multiply(2));
                            toDamage.getWorld().playSound(toDamage.getLocation(), Sound.ZOMBIE_WOODBREAK, 10, 0.85f);
                            UUID uuid = toDamage.getUniqueId();
                            Events.invincible.add(uuid);
                            Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> Events.invincible.remove(uuid), 15);
                            ParticleEffect.SMOKE_NORMAL.display(0, 0, 0, 1, 1, player.getLocation(), 15);
                        }
                        count++;
                    }else cancel();
                }

                public void cancel(){
                    super.cancel();
                    player.setWalkSpeed(player.getWalkSpeed() - WALK_SPEED_BONUS);
                    active.remove(uuid);
                }
            };
            sprintTask.runTaskTimer(RisenMain.getInstance(), 0, 1);
            active.put(uuid, sprintTask);
            return true;
        }

        public void abort(UUID uuid){
            BukkitRunnable activeRunnable = active.get(uuid);
            if(activeRunnable != null) activeRunnable.cancel();
        }
    },
    DODGE("Duck and Weave", "When this ability is activated, you'll have supreme agility for 7 seconds, granting you a 50% chance to dodge hits while crouching.", Material.ARROW, (byte) -1, 25, true, true, "scout", "+15% dodge chance"){
        final HashMap<UUID, Boolean> activePlayers = new HashMap<>(); //boolean: whether to multiply
        @EventHandler(priority = EventPriority.LOW)
        public void onHit(EntityDamageByEntityEvent event){
            Entity entity = event.getEntity();
            UUID uuid = entity.getUniqueId();
            Boolean multiply;
            if(RisenUtils.isBoss(uuid) && (multiply = activePlayers.get(uuid)) != null){
                if(Math.random() < (multiply ? 0.65 : 0.5)){
                    if(entity instanceof Player)
                        ((Player) entity).playSound(entity.getLocation(), Sound.ENDERDRAGON_WINGS, 10, 1.1f);
                    Entity damager = event.getDamager();
                    if(damager instanceof Player)
                        ((Player) damager).playSound(damager.getLocation(), Sound.ENDERDRAGON_WINGS, 10, 1.1f);
                    event.setCancelled(true);
                }
            }
        }

        protected boolean activationInternal(boolean multiply, RisenBoss player) {
            UUID uuid = player.getUUID();
            activePlayers.put(uuid, multiply);
            Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> activePlayers.remove(uuid), 140);
            return true;
        }

        public void abort(UUID uuid){
            activePlayers.remove(uuid);
        }
    },
    GROUND_SLAM("Ground Slam", "This ability, when used, first launches you high in the air, the shoots you back on the ground, sending out a shockwave damaging anyone caught in the impact!", Material.GRASS, (byte) -1, 20, false, true, "tank", "1.4x damage"){
        HashMap<UUID, BukkitRunnable> gsMap = new HashMap<>();
        protected boolean activationInternal(boolean multiply, RisenBoss boss){
            Player player = boss.getPlayer();
            Vector velocity = player.getVelocity();
            float added = 1.4f;
            player.setVelocity(velocity.setY(velocity.getY() + added));
            player.getWorld().playSound(player.getLocation(), Sound.ENDERDRAGON_WINGS, 10, 0.8f);
            BukkitRunnable gs = new BukkitRunnable(){ //down velocity triggers when player starts falling (or cancels after 1k ticks)
                int fallCheckCount = 0;
                public void run(){
                    if(player.getVelocity().getY() <= 0){
                        player.setVelocity(onlyAddXAndZ(player.getVelocity(), player.getLocation().getDirection(), 0.8).setY(player.getVelocity().getY() - added));
                        new BukkitRunnable() {
                            final Location playerLocation = player.getLocation();
                            int count = 0;
                            public void run(){
                                if(((Entity) player).isOnGround()){
                                    player.getWorld().playSound(playerLocation, Sound.BLAZE_HIT, 10, 0.8f);
                                    ParticleEffect.BlockData data = new ParticleEffect.BlockData(playerLocation.clone().subtract(0, 1, 0).getBlock().getType(), (byte) 0);
                                    ParticleEffect.BLOCK_CRACK.display(data, 0, 0, 0, 1, 10, playerLocation, 10);
                                    for(Damageable toDamage : MiscUtils.getNearbyDamageables(player, 5, 2, 5)){
                                        PlayerUtils.trueDamage(toDamage, multiply ? 6 : 8.4, player);
                                        Vector entityVelocity = toDamage.getVelocity();
                                        toDamage.setVelocity(entityVelocity.setY(entityVelocity.getY() + 0.8));
                                    }
                                    cancel();
                                }else if(count > 1000) cancel();
                                else count++;
                            }
                        }.runTaskTimer(RisenMain.getInstance(), 0, 1);
                        cancel();
                    }else if(fallCheckCount > 500) cancel();
                    else fallCheckCount++;
                }

                public void cancel(){
                    gsMap.remove(player.getUniqueId());
                    if(Bukkit.getScheduler().isCurrentlyRunning(getTaskId())) super.cancel();
                }
            };
            gsMap.put(player.getUniqueId(), gs);
            gs.runTaskTimer(RisenMain.getInstance(), 0, 2);
            return true;
        }

        public void abort(UUID uuid){
            BukkitRunnable groundSlamRunnable = gsMap.get(uuid);
            if(groundSlamRunnable != null) groundSlamRunnable.cancel();
        }

        private Vector onlyAddXAndZ(Vector vec1, Vector vec2, double multiplier){
            return vec1.setX(vec1.getX() + vec2.getX() * multiplier).setZ(vec1.getZ() + vec2.getZ() * multiplier);
        }
    },
    REJUVENATE("Rejuvenate", "Hold down this ability to hone your focus in such a way that, if you are left undisturbed for 8 seconds, you completely regain all of your health. Plus a couple absorption hearts for good measure.", Material.RED_ROSE, (byte) -1, 45, true, true, "tank", "-2s rejuvenation time"){
        final HashMap<UUID, BukkitRunnable> rejMap = new HashMap<>();
        final float SPEED_REMOVAL_FACTOR = 0.0001f;
        final ParticleEffect.ParticleColor HEAL_COLOR = new ParticleEffect.OrdinaryColor(Color.fromRGB(249, 152, 168));
        protected boolean activationInternal(boolean multiply, RisenBoss boss){
            Player player = boss.getPlayer();
            Bukkit.getServer().broadcastMessage(Prefix.RISEN_BOSS.toString() + ChatColor.YELLOW + " " + player.getDisplayName() + ChatColor.YELLOW + " is rejuvenating!");
            BukkitRunnable rejRunnable = new BukkitRunnable() {
                float count = 0;
                float tone = 0.9f;
                final DecimalFormat secondsFormatter = new DecimalFormat();
                final int rejuvenateTime = multiply ? 6 : 8;
                {
                    secondsFormatter.setMinimumFractionDigits(2);
                    secondsFormatter.setMaximumFractionDigits(2);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * rejuvenateTime, 4));
                }
                public void run() {
                    if(count < rejuvenateTime) {
                        ActionBarUtils.sendActionBar(player, ChatColor.GOLD + ChatColor.BOLD.toString() +
                                "Rejuvenating... " + ChatColor.GOLD + secondsFormatter.format(count) + ChatColor.GRAY + " | " + ChatColor.RED + ChatColor.UNDERLINE + "Don't switch items!");
                        player.playSound(player.getLocation(), Sound.NOTE_STICKS, 10, tone);
                        tone += 0.0035f;
                        count += 0.05f;
                        player.setWalkSpeed(player.getWalkSpeed() - SPEED_REMOVAL_FACTOR);
                    }else{
                        ActionBarUtils.sendActionBar(player, ChatColor.BOLD + ChatColor.GOLD.toString() + "FULLY REJUVENATED!");
                        player.setHealth(player.getMaxHealth());
                        PlayerUtils.addAbsorption(player, 4);
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 10, 1);
                        Location location = player.getLocation();
                        for(int i = 0; i<10; i++)
                            ParticleEffect.SPELL_MOB.display(HEAL_COLOR, location, 25);
                        Bukkit.getServer().broadcastMessage(Prefix.RISEN_BOSS + " " + boss.rankColor + boss.name + ChatColor.GOLD + " fully rejuvenated! Be faster next time!");
                        endRej();
                    }
                }
                public void cancel(){
                    ActionBarUtils.sendActionBar(player, ChatColor.GOLD + "Rejuvenation" + ChatColor.RED  + " CANCELLED" + ChatColor.GOLD + "!");
                    player.playSound(player.getLocation(), Sound.NOTE_BASS, 10, 1);
                    endRej();
                }
                private void endRej(){
                    super.cancel();
                    float removedSpeed = SPEED_REMOVAL_FACTOR * (count / 0.05f);
                    player.setWalkSpeed(player.getWalkSpeed() + removedSpeed);
                    player.removePotionEffect(PotionEffectType.SLOW);
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
        @EventHandler(priority = EventPriority.MONITOR)
        public void onHit(EntityDamageEvent event){
            Entity entity = event.getEntity();
            if(entity instanceof Player && !event.isCancelled()) cancelRejuvenation((Player) entity);
        }
        public void cancelRejuvenation(Player player){
            abort(player.getUniqueId());
        }
        public void abort(UUID uuid){
            BukkitRunnable rejRunnable = rejMap.get(uuid);
            if(rejRunnable != null) rejRunnable.cancel();
        }
    },
    HOT_TUB("Hot Tub", "Creates a relaxing lava pool under you that heals health instead of damaging you.", Material.LAVA_BUCKET, (byte) -1, 40, true, true, "corrupter", "1.4x heal amount"){
        final ParticleEffect.OrdinaryColor HEAL_COLOR = new ParticleEffect.OrdinaryColor(Color.fromRGB(255, 107, 252));
        final HashMap<UUID, Pair<Pair<Location, Material>[], Float>> htMap = new HashMap<>();
        @EventHandler
        public void heal(EntityDamageEvent event){
            Entity entity = event.getEntity();
            if(RisenUtils.isBoss(entity.getUniqueId())){
                EntityDamageEvent.DamageCause cause = event.getCause();
                if(cause == EntityDamageEvent.DamageCause.LAVA || cause == EntityDamageEvent.DamageCause.FIRE_TICK || cause == EntityDamageEvent.DamageCause.FIRE){
                    Pair<Pair<Location, Material>[], Float> htPairs = htMap.get(entity.getUniqueId());
                    if(htPairs != null){
                        List<Location> locs = RisenUtils.collectFirsts(htPairs.first);
                        for(Location location : locs){
                            Block block = location.getBlock();
                            if(entity.getLocation().getBlock().equals(block)){
                                event.setCancelled(true);
                                Damageable damageable = MiscUtils.canDamage(entity);
                                if(damageable != null && damageable.getHealth() + htPairs.second < damageable.getMaxHealth()) {
                                    LivingEntity living = (LivingEntity) damageable;
                                    living.heal(htPairs.second);
                                    for(int i = 0; i < 3; i++)
                                        ParticleEffect.SPELL_MOB.display(HEAL_COLOR, event.getEntity().getLocation(), 75);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        @SuppressWarnings("unchecked")
        protected boolean activationInternal(boolean multiply, RisenBoss boss){
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
            htMap.put(uuid, new Pair<>(htPairs, multiply ? 0.14f : 0.1f));
            Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> {
                abort(uuid);
            }, 100);
            return true;
        }

        public void abort(UUID uuid){
            Player player = Bukkit.getPlayer(uuid);
            if(player != null && player.isOnline()) player.setFireTicks(0);
            Pair<Pair<Location, Material>[], Float> htEntry = htMap.remove(uuid);
            if(htEntry != null){
                Pair<Location, Material>[] htPairs = htEntry.first;
                for(Pair<Location, Material> pair : htPairs){
                    pair.first.getBlock().setType(pair.second);
                }
            }
        }
    },
    RETALIATE("Retaliate", "Using this ability will enact long-overdue revenge on your foes by inflicting Hellfire on the player who has garnered the most boss damage for 4 seconds!", Material.FLINT_AND_STEEL, (byte) -1, 30, false, true, "corrupter", "+1 second hellfire duration"){
        HashMap<UUID, UUID> abortMap = new HashMap<>();
        protected boolean activationInternal(boolean multiply, RisenBoss boss){
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
            int hfSeconds = multiply ? 5 : 4;
            UUID bossId = boss.getUUID();
            abortMap.put(bossId, topDmgPlayer.getUniqueId());
            topDmgPlayer.sendMessage(ChatColor.RED + boss.name + " used " + ChatColor.GOLD + this + ChatColor.RED + " on you, which inflicts Hellfire for " + hfSeconds + " seconds!");
            EventsForCorruptor.INSTANCE.hf(topDmgPlayer, hfSeconds * 20);
            Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> abortMap.remove(bossId), hfSeconds * 20);
            return true;
        }

        public void abort(UUID uuid){
            UUID hfPlayer = abortMap.get(uuid);
            if(hfPlayer != null){
                Player player = Bukkit.getPlayer(hfPlayer);
                if(player != null && player.isOnline()){
                    player.setFireTicks(0);
                }
            }
        }
    },
    SPELL_BOMB("Spell Bomb",  "This ability shoots a flying potion out in the direction you're looking in, which, after a small duration, explodes, coating a 5-block radius in a dangerous, fast damaging cloud of... something.", Material.POTION, (byte) 16386, 20, false, true, "wizard", "Faster explosions"){
        final HashMap<UUID, BukkitRunnable> abortMap = new HashMap<>();
        protected boolean activationInternal(boolean multiply, RisenBoss boss) {
            UUID uuid = boss.getUUID();
            Player player = boss.getPlayer();
            ItemStack item = new ItemStack(Material.INK_SACK, 1, (short) 12);
            NBTItem itemNBT = new NBTItem(item);
            itemNBT.addCompound("CustomAttributes").setBoolean("NO_PICKUP", true);
            item = itemNBT.getItem();
            Location eyeLoc = player.getEyeLocation();
            Item droppedItem = player.getWorld().dropItem(eyeLoc, item);
            droppedItem.setVelocity(eyeLoc.getDirection().normalize().multiply(0.7));
            BukkitRunnable runnable = new BukkitRunnable(){
                int count = 0;
                boolean queued = false;
                BukkitRunnable other = null;
                public void run(){
                    count++;
                    if(queued){
                        if(droppedItem.isOnGround()){
                            //explode the spell bomb
                            other = new BukkitRunnable(){
                                int explodeCount = 0;
                                public void run(){
                                    if(explodeCount < 8){
                                        for(Damageable toDamage : MiscUtils.getNearbyDamageables(droppedItem, 4.5))
                                            PlayerUtils.trueDamage(toDamage, 5, Bukkit.getPlayer(uuid));
                                        MiscUtils.spawnFirework(droppedItem.getLocation(), 0, false, false, FireworkEffect.Type.BALL, Color.AQUA);
                                        explodeCount++;
                                    }else{
                                        aCancel();
                                    }
                                }
                            };
                            other.runTaskTimer(RisenMain.getInstance(), 0, multiply ? 8 : 10);
                            super.cancel();
                        }else if(count > 200){
                            cancel();
                        }
                    }else if(count >= 20){
                        queued = true;
                    }else if(!droppedItem.isOnGround() && count > 1)
                        ParticleEffect.CRIT.display(0, 0, 0, 0, 1, droppedItem.getLocation(), 50);
                }

                void aCancel(){ //dammit java (if anyone else is reading this and knows of another way to access a method of an anonymous class from within another anonymous inner class of the same superclass please lmk)
                    cancel();
                }

                public void cancel(){
                    if(Bukkit.getScheduler().isCurrentlyRunning(getTaskId())) super.cancel();
                    droppedItem.remove();
                    if(other != null && Bukkit.getScheduler().isCurrentlyRunning(other.getTaskId())) other.cancel();
                    abortMap.remove(uuid);
                }
            };
            runnable.runTaskTimer(RisenMain.getInstance(), 0, 2);
            abortMap.put(uuid, runnable);
            return true;
        }

        public void abort(UUID uuid){
            BukkitRunnable runnable = abortMap.get(uuid);
            if(runnable != null)
                runnable.cancel();
        }
    },
    /*Broken! May or may not fix later. But for now, it's probably going unused.*/DUMMY("Dummy!", "Trick your opponents with this ability by placing a dummy version of yourself, and turning you invisible for 2 seconds!", Material.LEATHER_CHESTPLATE, (byte) -1, 27, true, true, "wizard", "1.5x duration"){
        final HashMap<UUID, BukkitRunnable> dummyMap = new HashMap<>();
        protected boolean activationInternal(boolean multiply, RisenBoss boss){
            UUID uuid = boss.getUUID();
            Player player = boss.getPlayer();
            int entityId = player.getEntityId();
            PacketListener listener = new PacketListener() {
                ListeningWhitelist whitelist = ListeningWhitelist.newBuilder().types(PacketType.Play.Server.REL_ENTITY_MOVE, PacketType.Play.Server.REL_ENTITY_MOVE_LOOK, PacketType.Play.Server.ENTITY_TELEPORT).build();
                public void onPacketSending(PacketEvent event){
                    if(!event.getPlayer().getUniqueId().equals(uuid) && event.getPacket().getIntegers().read(0) == entityId)
                        event.setCancelled(true);
                }

                public void onPacketReceiving(PacketEvent event){

                }

                public ListeningWhitelist getSendingWhitelist(){
                    return whitelist;
                }

                public ListeningWhitelist getReceivingWhitelist(){
                    return ListeningWhitelist.EMPTY_WHITELIST;
                }

                public Plugin getPlugin(){
                    return RisenMain.getInstance();
                }
            };
            Events.invincible.add(uuid);
            ProtocolLibrary.getProtocolManager().addPacketListener(listener);
            Location prev = player.getLocation();
            BukkitRunnable runnable = new BukkitRunnable(){
                int count = 0;
                final int maxTicks = multiply ? 40 : 60;
                public void run(){
                    if(!player.isOnline()){
                        cancel();
                    }else{
                        if(count < maxTicks){
                            ParticleEffect.SPELL_MOB_AMBIENT.display(0f, 0.5f, 0f, 0.2f, 3, prev, player);
                            count++;
                        }else cancel();
                    }
                }

                public void cancel(){
                    super.cancel();
                    dummyMap.remove(uuid);
                    ProtocolLibrary.getProtocolManager().removePacketListener(listener);
                    if(player.isOnline()){
                        WrapperPlayServerEntityTeleport packet = WrapperPlayServerEntityTeleport.create(entityId, player.getLocation());
                        for(Player p : Bukkit.getServer().getOnlinePlayers()){
                            if(!p.getUniqueId().equals(uuid)) packet.sendPacket(p);
                        }
                        ParticleEffect.CLOUD.display(0, 1f, 0, 0, 25, prev, 75);
                    }
                    Events.invincible.remove(uuid);
                    Bukkit.getServer().broadcastMessage(Prefix.RISEN_BOSS + " " + player.getDisplayName() + ChatColor.GRAY + " stopped using " + ChatColor.GOLD + "Dummy" + ChatColor.GRAY + "!");
                }
            };
            runnable.runTaskTimer(RisenMain.getInstance(), 0, 1);
            dummyMap.put(uuid, runnable);
            player.sendMessage(ChatColor.YELLOW + "You used " + ChatColor.GOLD + "Dummy" + ChatColor.YELLOW + "!");
            return true;
        }

        public void abort(UUID uuid){
            BukkitRunnable runnable = dummyMap.get(uuid);
            if(runnable != null) runnable.cancel();
        }
    },
    DEAL("[Hyperlink Blocked]", "HEY! YOU    YOU [Brilliant Little Sponge] ! LET'S MAKE A  DEAL!!\n     FOR A [5 seconds closer to your death] LIMTED TIME ONLY, YOU CAN WATCH YOUR FUTURE [Shrivel And Die] BEFORE YOUR VERY OWN  TWO LITTLE EYES!        EVERY HIT YOU TAKE AGAINST THIS BOSS HAS A [50% Off! Act Now!] CHANCE TO MAKE YOUR [Nightmares] COME   TRUE,, WITH TWO TIMES THE  DEATH AND PERIL!! ACT NOW ! FAST! WILD DEALS!" + ChatColor.YELLOW + "*NOTE: IF YOUR SWING DOESN'T GET DOUBLED [Like You Know You've Always Deserved!], A SMALL, [[Tiny Fee]] OF YOUR      [[Delicious Kromer]] WILL BE FUNNELLED DIRECTLY TO [Risen Boss']'s ACCOUNT! YOU HAVE BEEN WARNED! BE CAREFUL! NO! EXCERSIZE [No]   CAUTION!" + ChatColor.GRAY + "ACT FAST! TAKE    THE DEAL!   PLEASE! I NEED IT!  THE KROMER!    IT HAS TO BE [Mine. All Mine.] FOR YOU!  THE DEAL!     MY [Little] SPONGE! IT WILL [Ruin] YOUR LIFE!  I'M BEGGING [And Pleading! I Need You! Only You! Now! Fast!]   ! PLEASE!", Material.EMERALD, (byte) -1, 28, true, false, "wizard", "ONE POINT TWO FIVE TIMES THE [[Delicious Kromer]]"){
        final HashMap<UUID, Boolean> kromerActive = new HashMap<>();
        final int WAIT_COUNT = 8;
        protected boolean activationInternal(boolean multiply, RisenBoss boss){
            Player player = boss.getPlayer();
            player.getServer().broadcastMessage(ChatColor.RED.toString() + ChatColor.BOLD + "WARNING! " + ChatColor.GRAY + "For the next " + ChatColor.YELLOW + "5" + ChatColor.GRAY + " seconds, every hit you take against the current" + ChatColor.GREEN + " Risen Boss " + ChatColor.GRAY + "(" + boss.rankColor + boss.name + ChatColor.GRAY + ") will either be " + ChatColor.YELLOW +  "doubled as a 50% chance" + ChatColor.GRAY + ", or result in " + ChatColor.RED + "a small amount of your REAL GEMS " + ChatColor.GRAY + "being transferred from your balance to theirs! " + ChatColor.RED + "BE CAREFUL!");
            World playerWorld = player.getWorld();
            int[] taskId = {-1};
            taskId[0] = Bukkit.getScheduler().runTaskTimer(RisenMain.getInstance(),
                    new Runnable() {
                        int count = 0;
                        public void run(){
                            if(count < WAIT_COUNT){
                                playerWorld.playSound(boss.getPlayer().getLocation(), Sound.NOTE_STICKS, 9, 1);
                                count++;
                            }else{
                                playerWorld.playSound(boss.getPlayer().getLocation(), Sound.NOTE_PLING, 10, 1);
                                kromerActive.put(boss.getUUID(), multiply);
                                Bukkit.getScheduler().cancelTask(taskId[0]);
                            }
                        }
                    }
            , 0, 10).getTaskId();
            Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> kromerActive.remove(boss.getUUID()), 100 + 10 * WAIT_COUNT);
            return false;
        }

        @EventHandler(priority = EventPriority.HIGH /*don't want this to call after the kill check, it could screw some things up*/)
        public void onHit(EntityDamageByEntityEvent event){
            Entity damager = event.getDamager();
            Entity entity = event.getEntity();
            if(damager instanceof Player && entity instanceof Player){
                Boolean kromerEntry = kromerActive.get(entity.getUniqueId());
                if(kromerEntry != null && RisenUtils.isBoss(entity.getUniqueId())){
                    Player player = (Player) entity;
                    Player playerDamager = (Player) damager;
                    if(ThreadLocalRandom.current().nextBoolean()){
                        event.setDamage(event.getDamage() * 2);
                        playerDamager.sendMessage(ChatColor.YELLOW + ChatColor.BOLD.toString() + "BIG SHOT! " + ChatColor.WHITE + ChatColor.BOLD + "==>" + ChatColor.GREEN + " Doubled damage to Risen Boss!");
                        playerDamager.playSound(playerDamager.getLocation(), Sound.SUCCESSFUL_HIT, 10, 1);
                        player.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + "UH OH! " + ChatColor.WHITE + ChatColor.BOLD + "==>" + ChatColor.YELLOW + " Took 2x damage from " + playerDamager.getDisplayName());
                        player.playSound(player.getLocation(), Sound.NOTE_BASS, 10,1);
                    }else{
                        int gemsAmount = ConfigUtils.getGems(playerDamager) / (kromerEntry ? 175 : 200);
                        if(gemsAmount > 0){
                            ConfigUtils.deductGems(playerDamager, gemsAmount);
                            player.sendMessage(ChatColor.YELLOW + ChatColor.BOLD.toString() + "BIG SHOT! " + ChatColor.WHITE + ChatColor.BOLD + "==>" + ChatColor.GREEN + " Graciously \"borrowed\" " + gemsAmount + (gemsAmount == 1 ? "gem" : "gems") + " from " + MiscUtils.getRankColor(playerDamager) + playerDamager.getName());
                            player.playSound(player.getLocation(), Sound.SUCCESSFUL_HIT, 10, 1);
                            playerDamager.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + "UH OH! " + ChatColor.WHITE + ChatColor.BOLD + "==>" + ChatColor.YELLOW + " You gave " + player.getDisplayName() + " " + ChatColor.YELLOW + gemsAmount + " gems! Read chat messages next time!");
                            playerDamager.sendTitle("", ChatColor.RED + "-" + gemsAmount + " gems!");
                            playerDamager.playSound(playerDamager.getLocation(), Sound.NOTE_BASS, 10, 1);
                        }
                    }
                }
            }
        }

        public void abort(UUID uuid){
            kromerActive.remove(uuid);
        }
    },
    COIN_FLIP("Quantum Coinflip", "Exists in all possible states until observed.\nScenario A- All damage dealt against you is doubled.\nScenario B- All damage against you is reflected doubly towards the attacker.\n" + ChatColor.DARK_GRAY + "Observed for 5 seconds. 50/50.", Material.DOUBLE_PLANT, (byte) -1, 25, true, false, "wizard", "30/70"){
        HashMap<UUID, Boolean> active = new HashMap<>(); // true - scenario A
        protected boolean activationInternal(boolean multiply, RisenBoss boss){
            Player player = boss.getPlayer();
            boolean result = ThreadLocalRandom.current().nextBoolean();
            new BukkitRunnable(){
                int count = 0;
                public void run(){
                    if(!player.isOnline()){
                        cancel();
                        abort(player.getUniqueId());
                    }else{
                        if(count == 0){
                            Bukkit.getServer().broadcastMessage(ChatColor.BLUE + "Scenario A" + ChatColor.GRAY + " - All damage against the Risen Boss is doubled.");
                            player.getWorld().playSound(player.getLocation(), Sound.NOTE_STICKS, 20, 1);
                        }else if(count == 1){
                            Bukkit.getServer().broadcastMessage(ChatColor.BLUE + "Scenario B" + ChatColor.GRAY + " - All damage against the Risen Boss is reflected doubly against you.");
                            player.getWorld().playSound(player.getLocation(), Sound.NOTE_STICKS, 20, 1.1f);
                        }else if(count == 2){
                            Bukkit.getServer().broadcastMessage(ChatColor.RED + "Lasts for 5 seconds. The Risen Boss will be pre-informed.");
                            player.getWorld().playSound(player.getLocation(), Sound.NOTE_STICKS, 20, 1.2f);
                        }else if(count == 3){
                            player.getWorld().playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 20, 1);
                            //TODO start
                        }
                        count++;
                    }
                }
            };
            return true;
        }

        public void abort(UUID uuid){
            active.remove(uuid);
        }
    };
    

    protected abstract boolean activationInternal(boolean multiply, RisenBoss boss);

    public static List<RisenAbility> VALUES = Arrays.stream(values()).filter((ability) -> ability.include).sorted().collect(Collectors.toList());
    public static List<RisenAbility> getStartAbilities(int abilitySlots){
        List<RisenAbility> abilities = new ArrayList<>(VALUES);
        MiscUtils.trimList(abilities, abilitySlots);
        return abilities;
    }

    public final ItemStack hotbarItem;
    public final String name;
    public final int cooldown;
    protected final String multiClass;
    private final boolean include;

    RisenAbility(String name, String description, Material icon, byte dataValue, int cooldownSecs, boolean registerEvents, boolean include, String multiClass, String amplifier){
        ItemStack hotbarItem =  dataValue == (byte) -1 ? new ItemStack(icon) : new ItemStack(icon, 1, dataValue);
        ItemMeta hotbarMeta = hotbarItem.getItemMeta();
        hotbarMeta.setDisplayName(ChatColor.GOLD + name + ChatColor.GRAY + " (Right Click)");
        ArrayList<String> lore = new ArrayList<>();
        String ampString = ChatColor.YELLOW + "With " + StringUtil.capitalizeFirst(multiClass) + " class: " + ChatColor.GREEN + amplifier;
        lore.add("");
        lore.addAll(StringUtil.wrapLore(ChatColor.GRAY + description, ampString.length()));
        lore.add(ChatColor.DARK_GRAY + "Cooldown: " + cooldownSecs + " seconds");
        lore.add("");
        lore.add(ampString);
        hotbarMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_POTION_EFFECTS);
        hotbarMeta.setLore(lore);
        hotbarItem.setItemMeta(hotbarMeta);
        NBTItem hotbarNBT = new NBTItem(hotbarItem);
        hotbarNBT.setBoolean("Unbreakable", true);
        NBTCompound hotbarAttributes = hotbarNBT.addCompound("CustomAttributes");
        hotbarAttributes.setString("ABILITY", name());
        this.name = name;
        this.multiClass = multiClass;
        this.hotbarItem = hotbarNBT.getItem();
        cooldown = cooldownSecs;
        this.include = include;
        if(registerEvents) Bukkit.getPluginManager().registerEvents(this, RisenMain.getInstance());
    }

    public void onHit(EntityDamageByEntityEvent event){

    }

    public String toString(){
        return name;
    }

    protected abstract void abort(UUID uuid);
}