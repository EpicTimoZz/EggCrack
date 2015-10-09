package net.teamlixo.eggcrack.list;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class AbstractExtendedList<T> implements ExtendedList<T> {
    private final List<T> list;

    public AbstractExtendedList(List<T> list) {
        this.list = list;
    }

    @Override
    public Iterator iterator(boolean looping) {
        if (looping)
            return new LoopedIterator(this, true);
        else
            return new LoopedIterator(this, false);
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

    public class LoopedIterator implements Iterator {
        private final Object lock = new Object();
        private final ExtendedList<T> list;
        private volatile int idx = 0;
        private final boolean looping;

        public LoopedIterator(ExtendedList<T> list, boolean b) {
            this.list = list;
            this.looping = b;
        }

        @Override
        public boolean hasNext() {
            return looping ? list.size() > 0 : idx + 1 < list.size();
        }

        @Override
        public T next() {
            synchronized (lock) {
                idx ++;
                if (idx >= list.size()) {
                    if (looping) idx = 0;
                    else throw new NoSuchElementException();
                }
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

        public float getProgress() {
            return looping ? -1F : (float)idx / (float)list.size();
        }
    }
}
