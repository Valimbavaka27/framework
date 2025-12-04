package com.framework.mapping;

import java.util.*;

public class MappingStore {
    private final Map<String, AnnotationStore> mappings = new HashMap<>();
    private final List<AnnotationStore> dynamicMappings = new ArrayList<>();

    public void addMapping(String key, AnnotationStore store) {
        if (key.contains("{") && key.contains("}")) {
            dynamicMappings.add(store);
        } else {
            mappings.put(key, store);
        }
    }

    public AnnotationStore findMapping(String requestPath, String httpMethod) {
        String exactKey = httpMethod + ":" + requestPath;
        if (mappings.containsKey(exactKey)) {
            return mappings.get(exactKey);
        }

        for (AnnotationStore store : dynamicMappings) {
            if (store.getHttpMethod().equals(httpMethod) && urlsMatch(requestPath, store.getUrl())) {
                return store;
            }
        }
        return null;
    }

    private boolean urlsMatch(String requestPath, String pattern) {
        String[] req = requestPath.split("/");
        String[] pat = pattern.split("/");

        if (req.length != pat.length) return false;

        for (int i = 0; i < req.length; i++) {
            if (pat[i].startsWith("{") && pat[i].endsWith("}")) {
                continue; // paramètre dynamique
            }
            if (!pat[i].equals(req[i])) {
                return false;
            }
        }
        return true;
    }

    public Map<String, AnnotationStore> getAllMappings() {
        Map<String, AnnotationStore> all = new HashMap<>(mappings);
        dynamicMappings.forEach(store -> all.put(store.getHttpMethod() + ":" + store.getUrl(), store));
        return all;
    }
} 