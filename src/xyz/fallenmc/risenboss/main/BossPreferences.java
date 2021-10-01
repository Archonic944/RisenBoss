package xyz.fallenmc.risenboss.main;

import xyz.fallenmc.risenboss.main.abilities.RisenAbility;

import java.util.*;

public class BossPreferences {
    final List<RisenAbility> enabledAbilities = new ArrayList<>();
    public BossPreferences(RisenAbility[] enabled){
        Set<RisenAbility> clearDuplicates = new HashSet<>(Arrays.asList(enabled));
        enabledAbilities.addAll(clearDuplicates);
    }

}