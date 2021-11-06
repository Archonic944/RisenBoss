package xyz.fallenmc.risenboss.main.abilities;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import me.zach.DesertMC.ClassManager.CoruManager.EventsForCorruptor;
import me.zach.DesertMC.GameMechanics.Events;
import me.zach.DesertMC.Utils.ActionBar.ActionBarUtils;
import me.zach.DesertMC.Utils.Config.ConfigUtils;
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
import org.github.paperspigot.Title;
import xyz.fallenmc.risenboss.main.RisenBoss;
import xyz.fallenmc.risenboss.main.RisenMain;
import xyz.fallenmc.risenboss.main.utils.RisenUtils;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


public enum RisenAbility implements Listener {
    SPRINT("Sprint", "Activate this ability to break into a quick sprint, doubling your move speed! For 4 seconds, anyone caught in the path of your whirlwind of speed is dealt 2 damage and knocked away.", Material.FEATHER, 20, false, true, "scout", "1.5x damage"){
        protected boolean activationInternal(boolean multiply, RisenBoss boss){
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
    DODGE("Duck and Weave", "When this ability is activated, you'll have supreme agility for 7 seconds, granting you a 50% chance to dodge hits while crouching.", Material.ARROW, 25, true, true, "scout", "+15% dodge chance"){
        final HashMap<UUID, Boolean> activePlayers = new HashMap<>();
        @EventHandler(priority = EventPriority.HIGH)
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
    },
    GROUND_SLAM("Ground Slam", "This ability, when used, first launches you high in the air, the shoots you back on the ground, sending out a shockwave damaging anyone caught in the impact!", Material.GRASS, 20, false, true, "tank", "1.4x damage"){
        protected boolean activationInternal(boolean multiply, RisenBoss boss){
            Player player = boss.getPlayer();
            Vector velocity = player.getVelocity();
            player.setVelocity(velocity.setY(velocity.getY() + 2.5));
            player.getWorld().playSound(player.getLocation(), Sound.ENDERDRAGON_WINGS, 10, 0.8f);
            Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> {
                player.setVelocity(player.getVelocity().setY(-5).add(player.getLocation().getDirection().multiply(1.2)));
                new BukkitRunnable(){
                    final Location playerLocation = player.getLocation();
                    int count = 0;
                    public void run(){
                        if(((Entity) player).isOnGround()){
                            player.getWorld().playSound(playerLocation, Sound.BLAZE_HIT, 10, 0.8f);
                            ParticleEffect.BlockData data = new ParticleEffect.BlockData(playerLocation.clone().subtract(0, 1, 0).getBlock().getType(), (byte) 0);
                            ParticleEffect.BLOCK_CRACK.display(data, 0, 0, 0, 1, 10, playerLocation, 10);
                            for(Damageable toDamage : MiscUtils.getNearbyDamageables(player, 6, 2, 6)){
                                PlayerUtils.trueDamage(toDamage, multiply ? 6 : 8.4, player);
                                Vector entityVelocity = toDamage.getVelocity();
                                toDamage.setVelocity(entityVelocity.setY(entityVelocity.getY() + 1));
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
    REJUVENATE("Rejuvenate", "Hold down this ability to hone your focus in such a way that, if you are left undisturbed for 8 seconds, you completely regain all of your health.", Material.YELLOW_FLOWER, 45, true, true, "tank", "-1s rejuvenation time"){
        final HashMap<UUID, BukkitRunnable> rejMap = new HashMap<>();
        final ParticleEffect.ParticleColor HEAL_COLOR = new ParticleEffect.OrdinaryColor(Color.fromRGB(249, 152, 168));
        protected boolean activationInternal(boolean multiply, RisenBoss boss){
            Player player = boss.getPlayer();
            Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + player.getName() + " is rejuvenating!");
            BukkitRunnable rejRunnable = new BukkitRunnable() {
                float count = 0;
                final float walkSpeed = player.getWalkSpeed();
                final float flySpeed = player.getFlySpeed();
                float tone = 1;
                final DecimalFormat secondsFormatter = new DecimalFormat();
                final int rejuvenateTime = multiply ? 7 : 8;
                {
                    secondsFormatter.setMinimumFractionDigits(2);
                    secondsFormatter.setMaximumFractionDigits(2);
                }
                public void run() {
                    if(count < rejuvenateTime) {
                        ActionBarUtils.sendActionBar(player, ChatColor.GOLD + ChatColor.BOLD.toString() +
                                "Rejuvenating... " + ChatColor.GOLD + secondsFormatter.format(count) + ChatColor.GRAY + " | " + ChatColor.RED + ChatColor.UNDERLINE + "Don't switch items!");
                        player.playSound(player.getLocation(), Sound.NOTE_STICKS, 10, tone);
                        tone += 0.004f;
                        count += 0.05f;
                        player.setFlySpeed(player.getFlySpeed() - 0.02f);
                        player.setWalkSpeed(player.getWalkSpeed() - 0.02f);
                    }else{
                        ActionBarUtils.sendActionBar(player, ChatColor.BOLD + ChatColor.GOLD.toString() + "FULLY REJUVENATED!");
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
    HOT_TUB("Hot Tub", "Creates a relaxing lava pool under you that heals 1 health instead of damaging you.", Material.LAVA_BUCKET, 40, true, true, "corrupter", "1.4x heal amount"){
        final ParticleEffect.OrdinaryColor HEAL_COLOR = new ParticleEffect.OrdinaryColor(Color.fromRGB(255, 107, 252));
        final HashMap<UUID, Pair<Pair<Location, Material>[], Double>> htMap = new HashMap<>();
        @EventHandler
        public void heal(EntityDamageEvent event){
            Entity entity = event.getEntity();
            if(RisenUtils.isBoss(entity.getUniqueId())){
                EntityDamageEvent.DamageCause cause = event.getCause();
                if(cause == EntityDamageEvent.DamageCause.LAVA || cause == EntityDamageEvent.DamageCause.FIRE_TICK || cause == EntityDamageEvent.DamageCause.FIRE){
                    Pair<Pair<Location, Material>[], Double> htPairs = htMap.get(entity.getUniqueId());
                    if(htPairs != null){
                        List<Location> locs = RisenUtils.collectFirsts(htPairs.first);
                        for(Location location : locs){
                            Block block = location.getBlock();
                            if(entity.getLocation().getBlock().equals(block)){
                                event.setCancelled(true);
                                Damageable damageable = MiscUtils.canDamage(entity);
                                if(damageable != null && damageable.getHealth() != damageable.getMaxHealth())
                                    damageable.setHealth(damageable.getHealth() + htPairs.second);
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
            htMap.put(uuid, new Pair<>(htPairs, multiply ? 0.14 : 0.1));
            Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> {
                for(Pair<Location, Material> pair : htPairs){
                    pair.first.getBlock().setType(pair.second);
                }
                Bukkit.getPlayer(uuid).setFireTicks(0);
            }, 100);
            return true;
        }
    },
    RETALIATE("Retaliate", "Using this ability will enact long-overdue revenge on your foes by inflicting Hellfire on the player who has garnered the most boss damage for 4 seconds!", Material.FLINT_AND_STEEL, 30, false, true, "corrupter", "+1 second"){
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
            topDmgPlayer.sendMessage(ChatColor.RED + boss.name + " used " + ChatColor.GOLD + this + ChatColor.RED + " on you, which inflicts Hellfire for" + hfSeconds + "seconds!");
            EventsForCorruptor.INSTANCE.hf(topDmgPlayer, hfSeconds * 20);
            return true;
        }
    },
    SPELL_BOMB("Spell Bomb",  "This ability shoots a flying potion out in the direction you're looking in, which, after a small duration, explodes, coating a 5-block radius in a dangerous, fast damaging cloud of... something.", Material.POTION, 20, false, true, "wizard", "1.2x explosion rate"){
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
                            }.runTaskTimer(RisenMain.getInstance(), 0, multiply ? 8 : 10);
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
    /*Broken! May or may not fix later. But for now, it's probably going unused.*/DUMMY("Dummy!", "Trick your opponents with this ability by placing a dummy version of yourself, and turning you invisible for 2 seconds!", Material.LEATHER_CHESTPLATE, 27, true, false, "wizard", "1"){
        final Set<UUID> npcSet = new HashSet<>();
        final Set<UUID> npcNoDamage = new HashSet<>();
        protected boolean activationInternal(boolean multiply, RisenBoss boss) {
            Player player = boss.getPlayer();
            boss.dummyPlayer.setItem(NPCSlot.MAINHAND, player.getInventory().getItem(0));
            RisenUtils.teleportNPC(boss.dummyPlayer, player.getLocation());
            for(Player p : Bukkit.getOnlinePlayers()){
                boss.dummyPlayer.show(p);
                if(!p.equals(player)) p.hidePlayer(player);
            }
            npcSet.add(boss.dummyPlayer.getUniqueId());
            Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> {
                for(Player p : Bukkit.getOnlinePlayers()){
                    boss.dummyPlayer.hide(p);
                    if(!p.equals(player)) p.showPlayer(player);
                    npcSet.remove(boss.dummyPlayer.getUniqueId());
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
                    npc.getWorld().playSound(npc.getLocation(), Sound.HURT_FLESH, 10, 1);
                    npcNoDamage.add(uuid);
                    Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> npcNoDamage.remove(uuid), 5);
                }
            }
        }
    },
    DEAL("[Hyperlink Blocked]", "HEY! YOU    YOU [Brilliant Little Sponge] ! LET'S MAKE A  DEAL!!\n     FOR A [5 seconds closer to your death] LIMTED TIME ONLY, YOU CAN WATCH YOUR FUTURE [Shrivel And Die] BEFORE YOUR VERY OWN  TWO LITTLE EYES!        EVERY HIT YOU TAKE AGAINST THIS BOSS HAS A [50% Off! Act Now!] CHANCE TO MAKE YOUR [Nightmares] COME   TRUE,, WITH TWO TIMES THE  DEATH AND PERIL!! ACT NOW ! FAST! WILD DEALS!" + ChatColor.YELLOW + "*NOTE: IF YOUR SWING DOESN'T GET DOUBLED [Like You Know You've Always Deserved!], A SMALL FEE OF   [0.5 percent! Tiny!] OF YOUR      [Highly Personal] KROMER WILL BE FUNNELLED DIRECTLY TO [Risen Boss']'s ACCOUNT! YOU HAVE BEEN WARNED! BE CAREFUL! NO! EXCERSIZE [No]   CAUTION!" + ChatColor.GRAY + "ACT FAST! TAKE    THE DEAL!   PLEASE! I NEED IT!  THE KROMER!    IT HAS TO BE [Mine. All Mine.] FOR YOU!  THE DEAL!     MY [Little] SPONGE! IT WILL [Ruin] YOUR LIFE!  I'M BEGGING [And Pleading! I Need You! Only You! Now! Fast!]   ! PLEASE!", Material.EMERALD, 28, true, true, "wizard", "ONE POINT TWO FIVE TIMES THE [[Delicious Kromer]]"){
        final HashMap<UUID, Boolean> kromerActive = new HashMap<>();
        final int WAIT_COUNT = 8;
        protected boolean activationInternal(boolean multiply, RisenBoss boss){
            Player player = boss.getPlayer();
            player.getServer().broadcastMessage(ChatColor.RED.toString() + ChatColor.BOLD + "WARNING! " + ChatColor.GRAY + "For the next " + ChatColor.YELLOW + "5" + ChatColor.GRAY + " seconds, every hit you take against the current" + ChatColor.GREEN + " Risen Boss " + ChatColor.GRAY + "(" + boss.rankColor + boss.name + ChatColor.GRAY + ") will either be" + ChatColor.YELLOW +  "doubled as a 50% chance" + ChatColor.GRAY + ", or result in " + ChatColor.RED + "a small amount of your REAL GEMS " + ChatColor.GRAY + "will be transferred from your balance to theirs! " + ChatColor.RED + "BE CAREFUL!");
            World playerWorld = player.getWorld();
            int[] taskId = {-1};
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(RisenMain.getInstance(),
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
            , 0, 10);
            taskId[0] = task.getTaskId();
            Bukkit.getScheduler().runTaskLater(RisenMain.getInstance(), () -> kromerActive.remove(boss.getUUID()), 100 + 10 * WAIT_COUNT);
            return false;
        }

        @EventHandler(priority = EventPriority.HIGH /*don't want this to call after the kill check, it could screw some things up*/)
        public void bigShot(EntityDamageByEntityEvent event){
            Entity damager = event.getDamager();
            Entity entity = event.getEntity();
            if(damager instanceof Player && entity instanceof Player){
                Boolean kromerEntry = kromerActive.get(entity.getUniqueId());
                if(kromerEntry != null){
                    Player player = (Player) entity;
                    Player playerDamager = (Player) damager;
                    if(ThreadLocalRandom.current().nextBoolean()){
                        event.setDamage(event.getDamage() * 2);
                        playerDamager.sendMessage(ChatColor.YELLOW + ChatColor.BOLD.toString() + "BIG SHOT! " + ChatColor.WHITE + ChatColor.BOLD + "==>" + ChatColor.GREEN + " Doubled damage to Risen Boss!");
                        playerDamager.playSound(playerDamager.getLocation(), Sound.SUCCESSFUL_HIT, 10, 1);
                        player.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + "UH OH! " + ChatColor.WHITE + ChatColor.BOLD + "==>" + ChatColor.YELLOW + " Took 2x damage from " + MiscUtils.getRankColor(playerDamager) + playerDamager.getName());
                        player.playSound(player.getLocation(), Sound.NOTE_BASS, 10,1);
                    }else{
                        int gemsAmount = ConfigUtils.getGems(playerDamager) / (kromerEntry ? 175 : 200);
                        if(gemsAmount > 0){
                            ConfigUtils.deductGems(playerDamager, gemsAmount);
                            player.sendMessage(ChatColor.YELLOW + ChatColor.BOLD.toString() + "BIG SHOT! " + ChatColor.WHITE + ChatColor.BOLD + "==>" + ChatColor.GREEN + " Graciously \"borrowed\" " + gemsAmount + (gemsAmount == 1 ? "gem" : "gems") + " from " + MiscUtils.getRankColor(playerDamager) + playerDamager.getName());
                            player.playSound(player.getLocation(), Sound.SUCCESSFUL_HIT, 10, 1);
                            playerDamager.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + "UH OH! " + ChatColor.WHITE + ChatColor.BOLD + "==>" + ChatColor.YELLOW + " You gave " + MiscUtils.getRankColor(player) + player.getName() + " " + ChatColor.YELLOW + gemsAmount + " gems! Read chat messages next time!");
                            playerDamager.sendTitle(new Title("", ChatColor.RED + "-" + gemsAmount + " gems!"));
                            playerDamager.playSound(playerDamager.getLocation(), Sound.NOTE_BASS, 10, 1);
                        }
                    }
                }
            }
        }
    };

    protected abstract boolean activationInternal(boolean multiply, RisenBoss boss);

    public static RisenAbility[] VALUES = Arrays.stream(values()).filter((ability) -> ability.include).sorted().toArray(RisenAbility[]::new);
    private static final RisenAbility[] START_ABILITIES_ARRAY = new RisenAbility[3];
    public static List<String> START_ABILITIES = new ArrayList<>();
    static{
        System.arraycopy(VALUES, 0, START_ABILITIES_ARRAY, 0, 3);
        for(RisenAbility ability : START_ABILITIES_ARRAY) START_ABILITIES.add(ability.name());
    }
    public final ItemStack hotbarItem;
    public final String name;
    public final int cooldown;
    protected final String multiClass;
    private final boolean include;

    RisenAbility(String name, String description, Material icon, int cooldownSecs, boolean registerEvents, boolean include, String multiClass, String amplifier){
        ItemStack hotbarItem = new ItemStack(icon);
        ItemMeta hotbarMeta = hotbarItem.getItemMeta();
        hotbarMeta.setDisplayName(ChatColor.GOLD + name + ChatColor.GRAY + " (Right Click)");
        ArrayList<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(StringUtil.wrapLore(ChatColor.GRAY + description));
        lore.add(ChatColor.YELLOW + "With " + StringUtil.capitalizeFirst(multiClass) + ChatColor.YELLOW + " class: " + ChatColor.GREEN + amplifier);
        lore.add(ChatColor.DARK_GRAY + "Cooldown: " + cooldownSecs + " seconds");
        hotbarMeta.setLore(lore);
        hotbarItem.setItemMeta(hotbarMeta);
        NBTItem hotbarNBT = new NBTItem(hotbarItem);
        hotbarNBT.setBoolean("Unbreakable", true);
        NBTCompound hotbarAttributes = hotbarNBT.addCompound("CustomAttributes");
        hotbarAttributes.setString("ABILITY", name());
        this.multiClass = multiClass;
        this.hotbarItem = hotbarNBT.getItem();
        this.name = name;
        cooldown = cooldownSecs;
        this.include = include;
        if(registerEvents) Bukkit.getPluginManager().registerEvents(this, RisenMain.getInstance());
    }

    public String toString(){
        return name;
    }
}