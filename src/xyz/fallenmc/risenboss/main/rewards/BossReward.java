package xyz.fallenmc.risenboss.main.rewards;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import xyz.fallenmc.risenboss.main.data.RisenData;

import java.util.Map;
//TODO refactor this class to be used for all player rewards! (whew, where do I even start?? not now, that's for sure, since i'm on a really tight deadline)
public class BossReward {
    public final int amount;
    public final RewardType type;

    public BossReward(RewardType type, int amount){
        this.amount = amount;
        this.type = type;
    }

    public void grant(Player player, RisenData data){
        if(type.grant(player, amount)){
            Map<String, Integer> earnedRewards = data.getRewards();
            String typeStr = type.name();
            int thisEarnedAmount = earnedRewards.getOrDefault(typeStr, 0);
            thisEarnedAmount += amount;
            earnedRewards.put(typeStr, thisEarnedAmount);
        }else{
            player.sendMessage(ChatColor.RED + "Could not grant " + this + ChatColor.RED + ": " + type.failedMessage + "!");
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 10, 1);
        }
    }

    public String toString(){
        return type.name + " x" + amount;
    }
}