package xyz.fallenmc.risenboss.main;

import de.tr7zw.nbtapi.NBTItem;
import itempackage.Items;
import me.zach.DesertMC.ClassManager.TravellerEvents;
import me.zach.DesertMC.GameMechanics.Events;
import me.zach.DesertMC.GameMechanics.hitbox.HitboxListener;
import me.zach.DesertMC.Prefix;
import me.zach.DesertMC.Utils.Config.ConfigUtils;
import me.zach.DesertMC.Utils.MiscUtils;
import me.zach.DesertMC.Utils.Particle.ParticleEffect;
import me.zach.DesertMC.Utils.PlayerUtils;
import me.zach.DesertMC.Utils.StringUtils.StringUtil;
import me.zach.DesertMC.Utils.nbt.NBTUtil;
import me.zach.DesertMC.Utils.structs.Pair;
import me.zach.artifacts.gui.helpers.ArtifactUtils;
import me.zach.artifacts.gui.inv.ArtifactData;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.inventivetalent.bossbar.BossBarAPI;
import xyz.fallenmc.risenboss.main.abilities.Ability;
import xyz.fallenmc.risenboss.main.abilities.RisenAbility;
import xyz.fallenmc.risenboss.main.data.RisenData;
import xyz.fallenmc.risenboss.main.inventories.AbilitySelectInventory;
import xyz.fallenmc.risenboss.main.rewards.BossReward;
import xyz.fallenmc.risenboss.main.rewards.RewardType;
import xyz.fallenmc.risenboss.main.utils.RisenUtils;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


public final class RisenBoss implements Listener {
    public static final BossBarAPI.Color BAR_COLOR = BossBarAPI.Color.RED;
    public static final DecimalFormat fallenPieceDefenseBonusFormatter = new DecimalFormat();
    static{
        fallenPieceDefenseBonusFormatter.setMaximumFractionDigits(2);
    }
    public static final int HEALTH_BONUS = 16;
    public final HashMap<String, Ability> abilityInstances = new HashMap<>();
    public final String rankColor;
    public final HashMap<UUID, Double> damagers = new HashMap<>();
    private final boolean removedFromNotifs;
    RisenData data;
    final String SEPARATOR = StringUtil.getCenteredLine(ChatColor.DARK_GRAY + "---------");
    public RisenBoss(Player player){
        data = RisenUtils.getData(player);
        RisenMain.alreadyUsed.remove(player.getUniqueId());
        List<RisenAbility> abilities = data.getAbilities();
        uuid = player.getUniqueId();
        name = player.getName();
        //saving inventory
        prevPlayerInventory = player.getInventory().getContents();
        // deselecting/saving previously selected artifacts
        ArtifactData ad = ConfigUtils.getAD(player);
        previousArtifacts = ad.getSelected();
        ad.setSelected(ArtifactUtils.sized(4));
        //setting rankColor for quick access
        rankColor = MiscUtils.getRankColor(uuid);
        PlayerInventory playerInventory = player.getInventory();
        //replacing inventory
        playerInventory.clear();
        playerInventory.setItem(0, Items.getRisenBlade());
        for(int i = 2, j = 0; i < 9 && j < abilities.size(); i++, j++){
            RisenAbility ability = abilities.get(j);
            playerInventory.setItem(i, ability.hotbarItem);
        }
        //preparing ability instances map
        for(RisenAbility risenAbility : abilities){
            abilityInstances.put(risenAbility.name(), new Ability(risenAbility));
        }
        //temporarily removing from Traveller notifications because of action bar cooldown messages
        removedFromNotifs = TravellerEvents.blockNotifs.remove(uuid);
        //setting max health, resetting traveller
        TravellerEvents.resetTraveller(player);
        player.setMaxHealth(player.getMaxHealth() + HEALTH_BONUS);
        player.setHealth(player.getMaxHealth());
        //initializing timers and other things
        initRunnables();
        Bukkit.getPluginManager().registerEvents(this, RisenMain.getInstance());
        for(Player otherPlayer : Bukkit.getServer().getOnlinePlayers()){
            setBar(otherPlayer);
        }
    }

    private final List<Integer> previousArtifacts;
    private static final int secondsToReach = 600;
    public final String name;
    private int damageTaken = 0;
    private int secondsLeft = secondsToReach;
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
        if(!damager.equals(uuid))
            damagers.put(damager, damagers.containsKey(damager) ? damagers.get(damager) + damage : damage);
        bossDamage(damage);
    }

    public void bossDamage(double damage){
        damageTaken += damage;
        refreshBarHealth();
    }

    public void bossAttack(double damage){
        damageDealt += damage;
    }

    private void initRunnables(){
        Player player = getPlayer();
        timer = timerInit();
        callout = calloutInit(player.getDisplayName());
    }

    public void endBoss(EndReason reason){
        RisenMain.currentBoss = null; //have existential crisis
        for(Ability ability : abilityInstances.values()){
            ability.abort(this);
        }
        HandlerList.unregisterAll(this);
        timer.cancel();
        callout.cancel();
        Player player = getPlayer();
        player.playSound(player.getLocation(), Sound.ENDERDRAGON_DEATH, 10, 1);
        //health removal handled by player respawn
        for(Player p : Bukkit.getServer().getOnlinePlayers()){
            BossBarAPI.removeBar(p);
        }
        player.getInventory().setContents(prevPlayerInventory);
        ConfigUtils.getAD(player).setSelected(previousArtifacts);
        for(ItemStack armor : player.getEquipment().getArmorContents()){
            if(armor != null && armor.getType() != Material.AIR){
                NBTItem nbt = new NBTItem(armor);
                if(NBTUtil.getCustomAttrString(nbt, "ID").startsWith("FALLEN_")){
                    float defense = NBTUtil.getCustomAttrFloat(nbt, "DEFENSE", 0);
                    if(defense < 15){
                        float newDefense = defense + 0.25f;
                        NBTUtil.checkCustomAttr(nbt).setFloat("DEFENSE", newDefense);
                        ItemMeta newMeta = nbt.getItem().getItemMeta();
                        List<String> lore = new ArrayList<>(newMeta.getLore());
                        for(int i = 0, loreSize = lore.size(); i < loreSize; i++){
                            String str = lore.get(i);
                            //this physically hurts me, but I don't have time to make a better item system
                            if(ChatColor.stripColor(str).contains("Current defense bonus:")){
                                lore.set(i, ChatColor.GOLD + "Current defense bonus: " + ChatColor.RED + fallenPieceDefenseBonusFormatter.format(newDefense) + "%");
                            }
                        }
                        newMeta.setLore(lore);
                        armor.setItemMeta(newMeta);
                    }
                }else Bukkit.getLogger().warning("Armor piece for player " + player.getUniqueId() + " not Fallen piece when ending Risen Boss!\nInstead, it was " + armor);
            }else Bukkit.getLogger().warning("Player " + player.getUniqueId() + " not wearing an armor piece when risen boss ended!");
        }
        String[] message;
        Pair<Player, Double>[] damagersSorted = getDamagersSorted();
        List<BossReward> rewards = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if(removedFromNotifs && reason != EndReason.BOSS_QUIT) TravellerEvents.blockNotifs.add(uuid);
        boolean bossPlayed = damageDealt > 20;
        if(reason.won){
            String nextSlotMessage = null;
            if(data.getAbilitySlots() < RisenUtils.MAX_ABILITY_SLOTS){
                data.setWinsToNextSlot(data.getWinsToNextSlot() - 1);
                nextSlotMessage = AbilitySelectInventory.nextSlotProgress(data.getWinsToNextSlot(), data.getAbilitySlots());
                if(data.getWinsToNextSlot() <= 0){
                    data.setAbilitySlots(data.getAbilitySlots() + 1);
                    if(data.getAbilitySlots() >= RisenUtils.MAX_ABILITY_SLOTS) nextSlotMessage = ChatColor.AQUA.toString() + ChatColor.BOLD + "ABILITY SLOTS MAXED! " + ChatColor.YELLOW + "(" + RisenUtils.MAX_ABILITY_SLOTS + "/" + RisenUtils.MAX_ABILITY_SLOTS + ")";
                    player.playSound(player.getLocation(), Sound.LEVEL_UP, 10, 1.05f);
                    data.setWinsToNextSlot(RisenUtils.WINS_PER_ABILITY_SLOT);
                }
            }
            if(bossPlayed) rewards.add(new BossReward(RewardType.SPECIAL_HAMMER, 1));
            if(random.nextBoolean()) rewards.add(new BossReward(RewardType.DIAMOND_HAMMER, bossPlayed ? 2 : 1));
            int bossUpgradeStonesEarned = bossPlayed ? 4 + random.nextInt(2) : 3;
            rewards.add(new BossReward(RewardType.UPGRADE_STONES, bossUpgradeStonesEarned));
            int gemsCap = 9000 + random.nextInt(50);
            int gemsMin = 500 - random.nextInt(20);
            int bossGemsEarned = Math.max(Math.min((int) (random.nextDouble(8, 9) * (damageDealt * (bossPlayed ? 9 : 5))), gemsCap), gemsMin); //cap at ~9000, min at ~500 (if the boss didn't play, reduce amount)
            rewards.add(new BossReward(RewardType.GEMS, bossGemsEarned));
            int soulsCap = 400 + random.nextInt(30);
            int soulsMin = 30 - random.nextInt(5);
            int bossSoulsEarned = Math.max(Math.min((int) (random.nextDouble(3.2, 4) * (damageDealt * (bossPlayed ? 0.9 : 0.5))), soulsCap), soulsMin); //cap at ~400, min at ~30 (if the boss didn't play, reduce amount)
            rewards.add(new BossReward(RewardType.SOULS, bossSoulsEarned));
            int expCap = 12000 + random.nextInt(3000);
            int expMin = 2000 - random.nextInt(200);
            int bossEXPEarned = Math.max(Math.min((int) (random.nextDouble(10, 11) * (damageDealt * (bossPlayed ? 9 : 7))), expCap), expMin); //cap at 12000-15000, min at ~2000
            rewards.add(new BossReward(RewardType.EXP, bossEXPEarned));
            message = endMessage(rewards, damageDealt, -1, ChatColor.GREEN, "RISEN BOSS WINS!", "Congratulations!", nextSlotMessage);
        }else{
            int timeSurvived = secondsToReach - secondsLeft;
            if(timeSurvived > 300){
                boolean survivedFor7m = timeSurvived > 490;
                if(bossPlayed || random.nextBoolean()) rewards.add(new BossReward(RewardType.DIAMOND_HAMMER, 1));
                if(survivedFor7m && bossPlayed && random.nextBoolean()) rewards.add(new BossReward(RewardType.SPECIAL_HAMMER, 1));
                int upgradeStones = 0;
                float uStoneChance = survivedFor7m ? 0.8f : 0.4f;
                if(!bossPlayed) uStoneChance /= 2f;
                for(int i = 0; i<2; i++)
                    if(random.nextDouble() > 1 - uStoneChance) upgradeStones++;
                if(upgradeStones > 0){
                    rewards.add(new BossReward(RewardType.UPGRADE_STONES, upgradeStones));
                }
            }
            rewards.add(new BossReward(RewardType.IRON_HAMMER, 1));
            int bossGemsEarned = (bossPlayed ? 3 : 5) * timeSurvived;
            rewards.add(new BossReward(RewardType.GEMS, bossGemsEarned));
            int bossSoulsEarned = timeSurvived / (bossPlayed ? 2 : 3);
            rewards.add(new BossReward(RewardType.SOULS, bossSoulsEarned));
            int expMin = 1000 - random.nextInt(100);
            int bossEXPEarned = Math.max(timeSurvived * (bossPlayed ? 15 : 10), expMin);
            rewards.add(new BossReward(RewardType.EXP, bossEXPEarned));
            message = endMessage(rewards, damageDealt, -1, ChatColor.RED, "RISEN BOSS LOSES...", "Better luck next time!", null);
        }
        grantAllTo(rewards, player, data);
        player.sendMessage(message); //TODO queue message if boss leaves
        if(!damagers.isEmpty()){
            for(Pair<Player, Double> damagerPair : damagersSorted){
                Player damager = damagerPair.first;
                if(damager != null){
                    damager.playSound(damager.getLocation(), Sound.ENDERDRAGON_DEATH, 10, 1);
                    List<BossReward> damagerRewards = new ArrayList<>();
                    int place = MiscUtils.indexOf(damagersSorted, damagerPair);
                    if(place < 3){
                        int upgradeStonesAmount = random.nextInt(1, 3);
                        if(place < 2){
                            upgradeStonesAmount += random.nextInt(1, 3);
                            damagerRewards.add(new BossReward(RewardType.IRON_HAMMER, 1));
                            if(place < 1){
                                upgradeStonesAmount += random.nextInt(1, 4);
                                damagerRewards.add(0, new BossReward(RewardType.DIAMOND_HAMMER, 1));
                                if(random.nextDouble() < 0.15)
                                    rewards.add(0, new BossReward(RewardType.SPECIAL_HAMMER, 1));
                            }
                        }
                        damagerRewards.add(new BossReward(RewardType.UPGRADE_STONES, upgradeStonesAmount));
                    }
                    int reversePlace = damagersSorted.length - place;
                    int damagerGemsAmount = reversePlace * 100 + damageDealt * 8;
                    if(damagerGemsAmount > 0) damagerRewards.add(new BossReward(RewardType.GEMS, damagerGemsAmount));
                    int damagerSoulsAmount = reversePlace * 8 + damageDealt / 5;
                    if(damagerSoulsAmount > 0) damagerRewards.add(new BossReward(RewardType.SOULS, damagerSoulsAmount));
                    int damagerEXPAmount = reversePlace * 170 + damageDealt * 10;
                    if(damagerEXPAmount > 0) damagerRewards.add(new BossReward(RewardType.EXP, damagerEXPAmount));
                    damager.sendMessage(endMessage(rewards, damagerPair.second, place + 1, reason.won ? ChatColor.RED : ChatColor.GREEN, reason.won ? "RISEN BOSS WINS" : "RISEN BOSS VANQUISHED", reason.won ? "You fought well (probably)!" : "Spectacular fight!", null));
                    grantAllTo(damagerRewards, damager, RisenUtils.getData(damager));
                }
            }
        }
    }

    private BukkitTask timerInit(){
        return new BukkitRunnable(){
            @Override
            public void run() {
                secondsLeft--;
                for(Player player : Bukkit.getServer().getOnlinePlayers()){
                    if(BossBarAPI.hasBar(player)){
                        BossBarAPI.setMessage(player, barText());
                    }
                }
                if(secondsLeft <= 0){
                    Events.respawn(getPlayer());
                    endBoss(EndReason.TIMER_FINISHED);
                    cancel();
                }
            }
        }.runTaskTimer(RisenMain.getInstance(), 20, 20);
    }
    //broken: BossBarAPI resets bar health when message is set
    public void refreshBarHealth(){
        /*
        float healthFloat = barProgress();
        for(Player p : Bukkit.getServer().getOnlinePlayers()){
            if(BossBarAPI.hasBar(p)){
                BossBarAPI.setHealth(p, healthFloat);
            }
        }
         */
    }

    private BukkitTask flamesInit(){
        return Bukkit.getScheduler().runTaskTimer(RisenMain.getInstance(), () -> {
            float xOffset = (float) MiscUtils.trueRandom();
            float yOffset = (float) MiscUtils.trueRandom();
            float zOffset = (float) MiscUtils.trueRandom();
            Location auraLocation = getPlayer().getLocation().clone().add(xOffset, yOffset, zOffset);
            ParticleEffect.FLAME.display(-xOffset, -yOffset,-zOffset,0.8f, 1, auraLocation, 75);
        }, 0, 7);
    }

    private BukkitTask calloutInit(String playerName){
        return Bukkit.getScheduler().runTaskTimer(RisenMain.getInstance(), () -> {
            Location location = getPlayer().getLocation();
            Bukkit.getServer().broadcastMessage(Prefix.RISEN_BOSS + " " + playerName + ChatColor.GRAY + " is at " + ChatColor.YELLOW + "(" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")" + ChatColor.YELLOW + "!" + ChatColor.GRAY + " Come and get them!");
            MiscUtils.spawnFirework(location, 2, false, true, FireworkEffect.Type.BURST, Color.YELLOW);
        }, 800, 800);
    }

    private List<String> damagersMessage(Pair<Player, Double>[] top){
        ArrayList<String> message = new ArrayList<>();
        //sometimes, microptimizations just tack off speed, development and CPU.
        if(0 < top.length) message.add(ChatColor.RED + ChatColor.BOLD.toString() + "1st " + top[0].first.getDisplayName() + ChatColor.GRAY + " - " + ChatColor.GOLD + ((int) top[0].second.doubleValue()) + ChatColor.GRAY + " damage");
        if(1 < top.length) message.add(ChatColor.GOLD + ChatColor.BOLD.toString() + "2nd " + top[1].first.getDisplayName() + ChatColor.GRAY + " - " + ChatColor.GOLD + ((int) top[1].second.doubleValue()) + ChatColor.GRAY + " damage");
        if(2 < top.length) message.add(ChatColor.YELLOW + ChatColor.BOLD.toString() + "3rd " + top[2].first.getDisplayName() + ChatColor.GRAY + " - " + ChatColor.GOLD + ((int) top[2].second.doubleValue()) + ChatColor.GRAY + " damage");
        centerList(message);
        return message;
    }

    private void grantAllTo(List<BossReward> rewards, Player player, RisenData data){
        for(BossReward reward : rewards){
            reward.grant(player, data);
        }
    }

    private List<String> rewardsMessage(List<BossReward> rewards){
        List<String> message = new ArrayList<>();
        message.add(StringUtil.getCenteredLine(ChatColor.GOLD + "Rewards"));
        message.add("");
        for(BossReward reward : rewards){
            message.add(ChatColor.GRAY + "   ■ " + reward);
        }
        return message;
    }

    private String[] endMessage(List<BossReward> rewards, double damageDealt, int place, ChatColor color, String header, String footer, String nextSlotMessage){
        final StringUtil.ChatWrapper wrapper = new StringUtil.ChatWrapper('=', color, true, false);
        List<String> message = new ArrayList<>();
        message.add(wrapper.toString());
        message.add(color + ChatColor.BOLD.toString() + header);
        message.add("");
        message.add(ChatColor.GOLD + "Damage dealt: " + ChatColor.RED + (int) damageDealt);
        message.add(ChatColor.GOLD + "Boss' damage taken: " + ChatColor.RED + damageTaken);
        if(place > 0) message.add(ChatColor.GRAY + "Your place: " + ChatColor.GOLD + place);
        else if(nextSlotMessage != null) message.add(nextSlotMessage);
        centerList(message);
        addSection(message, damagersMessage(getDamagersSorted()));
        addSection(message, rewardsMessage(rewards));
        message.add("");
        message.add(StringUtil.getCenteredLine(color + footer));
        message.add(StringUtil.getCenteredLine(wrapper.toString()));
        return message.toArray(new String[0]);
    }

    private void addSection(List<String> message, List<String> section){
        if(!section.isEmpty()){
            message.add("");
            message.add(SEPARATOR);
            message.addAll(section);
        }
    }

    private void centerList(List<String> list){
        String[] dumb = StringUtil.getCenteredMessage(list.toArray(new String[0]));
        for(int i = 0; i<dumb.length; i++){
            list.set(i, dumb[i]);
        }
    }

    private String barText(){
        int minutesLeft = Math.floorDiv(secondsLeft, 60);
        int secondsWithoutMinutes = secondsLeft % 60;
        return net.md_5.bungee.api.ChatColor.GOLD + net.md_5.bungee.api.ChatColor.BOLD.toString() + "RISEN BOSS: " + rankColor + net.md_5.bungee.api.ChatColor.BOLD + name + " " + net.md_5.bungee.api.ChatColor.YELLOW +  String.format("%d:%02d", minutesLeft, secondsWithoutMinutes);
    }
    //broken: bar resets health when setting bar message
    private float barProgress(){
        return 100;
        /*
        Player player = getPlayer();
        return ((float) player.getHealth() / (float) player.getMaxHealth()) * 100;
         */
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        setBar(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        if(event.getWhoClicked().getUniqueId().equals(uuid))
            event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event){
        if(event.getPlayer().getUniqueId().equals(uuid))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMove(PlayerMoveEvent event){
        if(event.getPlayer().getUniqueId().equals(uuid)){
            Location to = event.getTo();
            Location from = event.getFrom();
            if(to.equals(from)) return;
            if(HitboxListener.isInSafeZone(to) && !HitboxListener.isInSafeZone(from)){
                event.setTo(from);
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.NOTE_BASS, 10, 1);
            }
        }
    }

    private void setBar(Player player){
        TextComponent component = new TextComponent(barText());
        if(!BossBarAPI.hasBar(player)) BossBarAPI.addBar(player, component, BAR_COLOR, BossBarAPI.Style.PROGRESS, barProgress());
    }

    @SuppressWarnings("unchecked")
    public Pair<Player, Double>[] getDamagersSorted(){
        if(damagers.isEmpty()) return new Pair[0];
        else{
            System.out.println("not empty");
            List<Map.Entry<UUID, Double>> damagersSorted = MiscUtils.sortValues(damagers);
            int pairsSize = Math.min(damagersSorted.size(), 3);
            Pair<Player, Double>[] damagersPairs = (Pair<Player, Double>[]) new Pair[pairsSize];
            for(int i = 0; i < pairsSize; i++){
                damagersPairs[i] = new Pair<>(Bukkit.getPlayer(damagersSorted.get(i).getKey()), damagersSorted.get(i).getValue());
            }
            return damagersPairs;
        }
    }

    @SuppressWarnings("unused")
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