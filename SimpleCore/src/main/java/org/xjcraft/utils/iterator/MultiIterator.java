package org.xjcraft.utils.iterator;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 语法糖，一次性遍历多个集合
 *
 * @param <T>
 */
public class MultiIterator<T> {
    Collection<T> temp = new ArrayList<>();

    /**
     * 添加集合
     *
     * @param collections
     * @return
     */
    public MultiIterator<T> add(Collection<T>... collections) {
        for (Collection<T> collection : collections) {
            if (collection != null) {
                temp.addAll(collection);
            }
        }
        return this;
    }

    /**
     * 便利集合，如果没集合参数则便利添加的集合
     *
     * @param callback
     * @param collections
     */
    public void iterator(IteratorCallback<T> callback, Collection<T>... collections) {
        if (collections.length == 0) {
            for (T t : temp) {
                try {
                    callback.onIterate(t);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            for (Collection<T> collection : collections) {
                if (collection != null) {
                    for (T t : collection) {

                        try {
                            callback.onIterate(t);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }


    public interface IteratorCallback<T> {
        void onIterate(T collection) throws Exception;
    }
}


