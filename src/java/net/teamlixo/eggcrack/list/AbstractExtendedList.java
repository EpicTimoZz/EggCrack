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

    private class LoopedIterator<T> implements Iterator<T> {
        private ExtendedList<T> list;
        private Iterator<T> iterator;

        public LoopedIterator(ExtendedList<T> list) {
            this.list = list;
        }

        @Override
        public boolean hasNext() {
            if (iterator == null || !iterator.hasNext())
                this.iterator = list.iterator(false);

            synchronized (iterator) {
                return iterator.hasNext();
            }
        }

        @Override
        public T next() {
            synchronized (iterator) {
                return iterator.next();
            }
        }

        @Override
        public void remove() {
            synchronized (iterator) {
                iterator.remove();
            }
        }
    }
}
