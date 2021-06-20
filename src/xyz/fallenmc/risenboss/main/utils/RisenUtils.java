package xyz.fallenmc.risenboss.main.utils;

import org.bukkit.entity.Player;
import xyz.fallenmc.risenboss.main.BossPreferences;
import xyz.fallenmc.risenboss.main.abilities.RisenAbility;

import java.util.*;


public class RisenUtils {
    public static BossPreferences getPreferences(Player player){
        return new BossPreferences(RisenAbility.TEST);
    }
    public static void setBossReady(Player player, boolean ready){
        //TODO do this
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static HashMap sortValues(HashMap<?, Integer> map){
        List<Map.Entry<?, Integer>> list = new LinkedList<>(map.entrySet());
        list.sort((e1, e2) -> ((Comparable<Integer>) ((e1)).getValue()).compareTo(e2.getValue()));
        HashMap sortedHashMap = new LinkedHashMap();
        for(Object o : list){
            Map.Entry<?, Integer> entry = (Map.Entry<?, Integer>) o;
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }
        return sortedHashMap;
    }
}