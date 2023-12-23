package io.github.karlchanel.nacosgrayrouting.gray;


import java.util.HashMap;
import java.util.Map;


public class WeightRandomUtils {
    public static <T> WeightMeta<T> buildWeightMeta(final Map<T, Integer> weightMap) {
        if(weightMap.isEmpty()){
            return null;
        }
        final int size = weightMap.size();
        Object[] nodes = new Object[size];
        int[] weights = new int[size];
        int index = 0;
        int weightAdder = 0;
        for (Map.Entry<T, Integer> each : weightMap.entrySet()) {
            nodes[index] = each.getKey();
            weights[index++] = (weightAdder = weightAdder + each.getValue());
        }
        return new WeightMeta<T>((T[]) nodes, weights);
    }

    public static void main(String[] args) {
        Map<String,Integer> map = new HashMap<>();
        map.put("v1",1);
        map.put("v2",2);
        WeightMeta<String> nodes = WeightRandomUtils.buildWeightMeta(map);
        for(int i = 0; i < 10; i++){
            new Thread(()->{
                System.out.println(nodes.random());
            }).start();
        }

    }
}