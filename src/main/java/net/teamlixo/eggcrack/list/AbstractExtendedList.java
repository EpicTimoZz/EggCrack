package net.teamlixo.eggcrack.list;

import java.util.Iterator;
import java.util.List;

public abstract class AbstractExtendedList<T> implements ExtendedList<T> {
    private final List<T> list;

    public AbstractExtendedList(List<T> list) {
        this.list = list;
    }

    @Override
    public Iterator iterator(boolean looping) {
        if (looping)
            return new LoopedIterator(this);
        else
            return list.iterator();
    }

    @Override
    public void add(T object) {
        list.add(object);
    }

    @Override
    public void remove(T object) {
        list.remove(object);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public void clear() {
        list.clear();
    }

    private void remove(int i) {
        list.remove(i);
    }

    private T get(int i) {
        return list.get(i);
    }

    private class LoopedIterator implements Iterator {
        private final Object lock = new Object();
        private final ExtendedList<T> list;
        private volatile int idx = 0;

        public LoopedIterator(ExtendedList<T> list) {
            this.list = list;
        }

        @Override
        public boolean hasNext() {
            return list.size() > 0;
        }

        @Override
        public T next() {
            synchronized (lock) {
                idx ++;
                if (idx >= list.size()) idx = 0;
                return AbstractExtendedList.this.get(idx);
            }
        }

        @Override
        public void remove() {
            synchronized (lock) {
                AbstractExtendedList.this.remove(idx);
                idx --;
            }
        }
    }
}
