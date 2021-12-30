package xyz.fallenmc.risenboss.main;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import itempackage.Items;
import me.zach.DesertMC.ClassManager.TravellerEvents;
import me.zach.DesertMC.DesertMain;
import me.zach.DesertMC.GameMechanics.Events;
import me.zach.DesertMC.Utils.MiscUtils;
import me.zach.DesertMC.Utils.Particle.ParticleEffect;
import me.zach.DesertMC.Utils.RankUtils.RankEvents;
import me.zach.DesertMC.Utils.StringUtils.StringUtil;
import me.zach.DesertMC.Utils.nbt.NBTUtil;
import me.zach.DesertMC.Utils.structs.Pair;
import net.jitse.npclib.api.NPC;
import net.jitse.npclib.api.skin.Skin;
import net.jitse.npclib.api.state.NPCSlot;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import xyz.fallenmc.risenboss.main.abilities.Ability;
import xyz.fallenmc.risenboss.main.abilities.RisenAbility;
import xyz.fallenmc.risenboss.main.data.RisenData;
import xyz.fallenmc.risenboss.main.inventories.AbilitySelectInventory;
import xyz.fallenmc.risenboss.main.rewards.BossReward;
import xyz.fallenmc.risenboss.main.rewards.RewardType;
import xyz.fallenmc.risenboss.main.utils.BossBarUtil;
import xyz.fallenmc.risenboss.main.utils.RisenUtils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


public final class RisenBoss {
    public final HashMap<String, Ability> abilityInstances = new HashMap<>();
    public final String rankColor;
    public final HashMap<UUID, Double> damagers = new HashMap<>();
    private final boolean removedFromNotifs;
    RisenData data;
    final String SEPARATOR = StringUtil.getCenteredLine(ChatColor.GRAY + "---------");
    public RisenBoss(Player player){
        data = RisenUtils.getData(player);
        RisenMain.alreadyUsed.remove(player.getUniqueId());
        List<RisenAbility> abilities = data.getAbilities();
        uuid = player.getUniqueId();
        name = player.getName();
        //saving inventory
        prevPlayerInventory = player.getInventory().getContents();
        Plugin fallenMain = DesertMain.getInstance;
        //setting rankColor for quick access
        if(RankEvents.rankSession.containsKey(uuid)) rankColor = RankEvents.rankSession.get(uuid).c.toString();
        else rankColor = "§7";
        //preparing dummy player npc for the wizard class's "Dummy!" ability
        dummyPlayer = RisenMain.getNpcLib().createNPC(Collections.singletonList(rankColor + name));
        //getting skin signature and id from the player
        EntityPlayer NMSPlayer = ((CraftPlayer) player).getHandle();
        GameProfile profile = NMSPlayer.getProfile();
        Property property = profile.getProperties().get("textures").iterator().next();
        String value = property.getValue();
        String signature = property.getSignature();
        dummyPlayer.setSkin(new Skin(value, signature));
        //setting slots on the npc to match the player's armor (as well as getting the players inventory)
        PlayerInventory playerInventory = player.getInventory();
        if(playerInventory.getBoots() != null) dummyPlayer.setItem(NPCSlot.BOOTS, playerInventory.getBoots());
        if(playerInventory.getLeggings() != null) dummyPlayer.setItem(NPCSlot.LEGGINGS, playerInventory.getLeggings());
        if(playerInventory.getChestplate() != null) dummyPlayer.setItem(NPCSlot.CHESTPLATE, playerInventory.getChestplate());
        if(playerInventory.getHelmet() != null) dummyPlayer.setItem(NPCSlot.HELMET, playerInventory.getHelmet());
        dummyPlayer.setLocation(player.getLocation());
        dummyPlayer.create();
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
        //boss message
        Bukkit.broadcastMessage(rankColor + name + " " + ChatColor.GOLD + "just because a RISEN BOSS! Fight them to gain rewards!");
        //setting max health
        player.setMaxHealth(player.getMaxHealth() + 16);
        player.setHealth(player.getMaxHealth());
        //initializing timers and other things
        initRunnables();
    }


    private static final int secondsToReach = 600;
    public final String name;
    public final NPC dummyPlayer;
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

    public void bossAttack(double damage){
        damageDealt += damage;
        Player player = getPlayer();
        float healthFloat = (float) (player.getMaxHealth() / player.getHealth());
        for(UUID uuid : BossBarUtil.getPlayers()){
            BossBarUtil.updateHealth(Bukkit.getPlayer(uuid), healthFloat);
        }
    }


    private void initRunnables(){
        Player player = getPlayer();
        timer = timerInit(player);
        callout = calloutInit(rankColor + player.getName());
    }

    public void endBoss(EndReason reason){
        dummyPlayer.destroy();
        timer.cancel();
        callout.cancel();
        Player player = getPlayer();
        for(Player p : Bukkit.getOnlinePlayers())
            if(!p.canSee(player)) p.showPlayer(player);
        BossBarUtil.clearAllBars();
        player.getInventory().setContents(prevPlayerInventory);
        for(ItemStack armor : player.getEquipment().getArmorContents()){
            NBTItem nbt = new NBTItem(armor);
            if(NBTUtil.getCustomAttrString(nbt, "ID").startsWith("FALLEN_")){
                float defense = NBTUtil.getCustomAttrFloat(nbt, "DEFENSE", 0);
                if(nbt.getFloat("DEFENSE") < 15){
                    float newDefense = defense + 0.25f;
                    nbt.getCompound("CustomAttributes").setFloat("DEFENSE", newDefense);
                    ItemMeta meta = armor.getItemMeta();
                    List<String> lore = new ArrayList<>(meta.getLore());
                    for(int i = 0, loreSize = lore.size(); i < loreSize; i++){
                        String str = lore.get(i);
                        //this physically hurts me, but I don't have time to make a better item system
                        if(ChatColor.stripColor(str).startsWith("Current defense bonus:"))
                            lore.set(i, str.replace(defense + "%", newDefense + "%"));
                    }
                }
            }else Bukkit.getLogger().warning("Armor piece for player " + player.getUniqueId() + " not Fallen piece when ending Risen Boss!\nInstead, it was " + armor);
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
                nextSlotMessage = AbilitySelectInventory.nextSlotProgress(data.getWinsToNextSlot(), data.getAbilitySlots());
                data.setWinsToNextSlot(data.getWinsToNextSlot() - 1);
                if(data.getWinsToNextSlot() == 0){
                    data.setAbilitySlots(data.getAbilitySlots() + 1);
                    nextSlotMessage += " " + ChatColor.BOLD + (data.getAbilitySlots() == RisenUtils.MAX_ABILITY_SLOTS ? "ABILITY SLOTS MAXED!" : "NEW SLOT UNLOCKED");
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
                            if(random.nextDouble() < 0.15) rewards.add(0, new BossReward(RewardType.SPECIAL_HAMMER, 1));
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
                damager.sendMessage(endMessage(rewards, damagerPair.second, place + 1, reason.won ? ChatColor.RED : ChatColor.GREEN, "RISEN BOSS LOSES", "You fought well (probably)!", null));
                grantAllTo(damagerRewards, damager, RisenUtils.getData(damager));
            }
        }
        RisenMain.currentBoss = null;
    }

    private BukkitTask timerInit(Player player){
        return new BukkitRunnable(){
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
            Bukkit.getServer().broadcastMessage(playerName + ChatColor.GRAY + " is at " + ChatColor.YELLOW + "(" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")" + ChatColor.YELLOW + "!" + ChatColor.GRAY + " Come and get them!");
            MiscUtils.spawnFirework(location, 3, false, true, FireworkEffect.Type.BURST, Color.YELLOW);
            MiscUtils.spawnFirework(location, 3, false, true, FireworkEffect.Type.BURST, Color.YELLOW);
        }, 800, 800);
    }

    private List<String> damagersMessage(Pair<Player, Double>[] top){
        ArrayList<String> message = new ArrayList<>();
        //sometimes, microptimizations just tack off speed, development and CPU.
        if(0 < top.length) message.add(ChatColor.RED + ChatColor.BOLD.toString() + "1st " + MiscUtils.getRankColor(top[0].first) + top[0].first.getName() + ChatColor.GRAY + " - " + ChatColor.RED + top[0].second + " damage");
        if(1 < top.length) message.add(ChatColor.GOLD + ChatColor.BOLD.toString() + "2nd " + MiscUtils.getRankColor(top[1].first) + top[1].first.getName() + ChatColor.GRAY + " - " + ChatColor.GOLD + top[1].second + " damage");
        if(2 < top.length) message.add(ChatColor.YELLOW + ChatColor.BOLD.toString() + "3rd " + MiscUtils.getRankColor(top[2].first) + top[2].first.getName() + ChatColor.GRAY + " - " + ChatColor.YELLOW + top[2].second + " damage");
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
        message.add(ChatColor.GOLD + "Damage dealt: " + ChatColor.RED + damageDealt);
        message.add(ChatColor.GOLD + "Boss' damage taken: " + ChatColor.RED + damageTaken);
        if(place > 0) message.add(ChatColor.GRAY + "Your place: " + ChatColor.YELLOW + place);
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

    @SuppressWarnings("unchecked")
    public Pair<Player, Double>[] getDamagersSorted(){
        List<Map.Entry<UUID, Double>> damagersSorted = MiscUtils.sortValues(damagers);
        int pairsSize = Math.min(damagersSorted.size(), 3);
        Pair<Player, Double>[] damagersPairs = (Pair<Player, Double>[]) new Pair[pairsSize];
        for(int i = 0; i<pairsSize; i++){
            damagersPairs[i] = new Pair<>(Bukkit.getPlayer(damagersSorted.get(i).getKey()), damagersSorted.get(i).getValue());
        }
        return damagersPairs;
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