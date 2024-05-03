package org.xjcraft.utils.random;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class RandomGenerator<T> {
    double total;
    List<LinkedHashMap<Variable, Object>> lib = new ArrayList<>();

    public RandomGenerator() {
    }

    public T add(double rate, T object) {
        double v = total + rate;
        LinkedHashMap<Variable, Object> map = new LinkedHashMap<Variable, Object>() {{
            put(Variable.RATE, rate);
            put(Variable.VALUE, object);
        }};
        total = v;
        lib.add(map);
        return object;
    }

    public T remove(T object) {
        LinkedHashMap<Variable, Object> remove = null;
        for (LinkedHashMap<Variable, Object> map : lib) {
            if (map.get(Variable.VALUE) == object) {
                remove = map;
                break;
            }
        }
        if (remove != null) {
            lib.remove(remove);
            total -= (double) remove.get(Variable.RATE);
            return object;
        }
        return object;
    }

    public T getRandom() {
        double v = Math.random() * total;
        for (LinkedHashMap<Variable, Object> map : lib) {
            double o = (double) map.get(Variable.RATE);
            v -= o;
            if (v <= 0) {
                return (T) map.get(Variable.VALUE);
            }
        }
        return null;
    }

    public int size() {
        return lib.size();
    }

    enum Variable {
        RATE, VALUE
    }

}
