package com.jimm0063.magi.document.utilities.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class ModuleRegistry {

    private final Map<String, TextModule> modules = new LinkedHashMap<>();

    public ModuleRegistry(List<TextModule> modules) {
        for (TextModule module : modules) {
            TextModule previous = this.modules.putIfAbsent(module.id(), module);
            if (previous != null) {
                throw new IllegalStateException("Duplicate module id: " + module.id());
            }
        }
    }

    public Optional<TextModule> find(String id) {
        return Optional.ofNullable(modules.get(id));
    }

    public Collection<TextModule> all() {
        return modules.values();
    }
}
