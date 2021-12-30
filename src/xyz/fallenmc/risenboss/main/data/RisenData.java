package xyz.fallenmc.risenboss.main.data;

import org.bson.codecs.pojo.annotations.BsonProperty;
import xyz.fallenmc.risenboss.main.abilities.RisenAbility;
import xyz.fallenmc.risenboss.main.utils.RisenUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.zach.databank.saver.Key.RisenKey;

public class RisenData {
    @BsonProperty(RisenKey.WINS_TO_NEXT_SLOT)
    private int winsToNextSlot = RisenUtils.WINS_PER_ABILITY_SLOT;
    @BsonProperty(RisenKey.ABILITY_SLOTS)
    private int abilitySlots = RisenUtils.MINIMUM_ABILITY_SLOTS;
    @BsonProperty(RisenKey.SELECTED_ABILITIES)
    private List<RisenAbility> abilities = RisenAbility.getStartAbilities(abilitySlots);
    @BsonProperty(RisenKey.EARNED_REWARDS)
    private Map<String, Integer> rewards = new HashMap<>();
    @BsonProperty(RisenKey.BOSS_READY)
    private boolean bossReady = false;

    public Map<String, Integer> getRewards(){
        return rewards;
    }

    public void setRewards(Map<String, Integer> rewards){
        this.rewards = rewards;
    }

    public int getWinsToNextSlot(){
        return winsToNextSlot;
    }

    public RisenData setWinsToNextSlot(int winsToNextSlot){
        this.winsToNextSlot = winsToNextSlot;
        return this;
    }

    public int getAbilitySlots(){
        return abilitySlots;
    }

    public RisenData setAbilitySlots(int abilitySlots){
        this.abilitySlots = abilitySlots;
        return this;
    }

    public List<RisenAbility> getAbilities(){
        return abilities;
    }

    public RisenData setAbilities(List<RisenAbility> abilities){
        this.abilities = abilities;
        return this;
    }

    public boolean isBossReady(){
        return bossReady;
    }

    public RisenData setBossReady(boolean bossReady){
        this.bossReady = bossReady;
        return this;
    }

    public RisenData(){

    }

    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof RisenData)) return false;
        RisenData risenData = (RisenData) o;
        return winsToNextSlot == risenData.winsToNextSlot && abilitySlots == risenData.abilitySlots && bossReady == risenData.bossReady && abilities.equals(risenData.abilities) && rewards.equals(risenData.rewards);
    }
}