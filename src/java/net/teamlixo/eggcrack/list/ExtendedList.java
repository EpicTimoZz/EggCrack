package net.teamlixo.eggcrack.list;

import java.util.Iterator;

/**
 * Provides looping iterators to the extended list object; used when looping through an array of proxies, among other
 * things.
 * @param <T> Type of ExtendedList to create.
 */
public interface ExtendedList<T> {
    public Iterator<T> iterator(boolean looping);
    public void add(T object);
    public void remove(T object);
    public void clear();
    public int size();
}
