package net.teamlixo.eggcrack.authentication.configuration;

import java.util.ArrayList;
import java.util.List;

public class ServiceConfiguration {
    private List<Option> optionList = new ArrayList<>();

    public <T> Option<T> register(Option<T> option) {
        optionList.add(option);
        return option;
    }

    public List<Option> getOptions() {
        return optionList;
    }

    public static class Option<T> {
        private final T defaultValue;
        private final String name;
        private volatile T value = null;

        public Option(String name, T defaultValue) {
            this.defaultValue = defaultValue;
            this.name = name;
        }

        public T getDefaultValue() {
            return defaultValue;
        }

        public String getName() {
            return name;
        }

        public T getValue() {
            return value == null ? defaultValue : value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public void unset() {
            setValue(null);
        }
    }
}
