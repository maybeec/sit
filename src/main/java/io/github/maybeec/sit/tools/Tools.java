package io.github.maybeec.sit.tools;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

public class Tools {
    
    private static enum TYPE {
        MIN, MAX;
    }
    
    public static List<Integer[]> reduceOffsets(int reduction, List<Integer[]> occurences) {
        for (int i = 0; i < occurences.size(); i++) {
            occurences.get(i)[0] -= reduction;
            occurences.get(i)[1] -= reduction;
        }
        return occurences;
    }
    
    public static int getMaxOffset(List<Integer[]> occurences) {
        return getOffset(TYPE.MAX, occurences);
    }
    
    public static int getMinOffset(List<Integer[]> occurences) {
        return getOffset(TYPE.MIN, occurences);
    }
    
    private static int getOffset(TYPE type, List<Integer[]> occurences) {
        int result = -1;
        if (type == TYPE.MIN) {
            result = occurences.get(0)[0];
        } else if (type == TYPE.MAX) {
            result = occurences.get(0)[1];
        }
        
        if (result == -1) return result;
        
        for (Integer[] arr : occurences) {
            if (type == TYPE.MIN && arr[0] < result) {
                result = arr[0];
            } else if (type == TYPE.MAX && arr[1] > result) {
                result = arr[1];
            }
        }
        return result;
    }
    
    public static Collection<Integer[]> sort(List<Integer[]> occurences) {
        
        TreeMap<Integer, Integer[]> sortedMap = new TreeMap<Integer, Integer[]>();
        
        for (Integer[] i : occurences) {
            sortedMap.put(i[0], i);
        }
        
        return sortedMap.values();
    }
    
    public static <T> boolean listContains(List<T[]> list, T[] array) {
        for (T[] t : list) {
            if (Arrays.equals(array, t)) {
                return true;
            }
        }
        return false;
    }
}
