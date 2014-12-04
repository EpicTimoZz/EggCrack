package net.teamlixo.eggcrack.list;

import java.util.Iterator;

public interface ExtendedList<T> {
    public Iterator<T> iterator(boolean looping);
    public void add(T object);
    public void remove(T object);
    public void clear();
    public int size();
}
