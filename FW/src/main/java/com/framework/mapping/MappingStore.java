package com.framework.mapping;

import java.util.HashMap;
import java.util.Map;

public class MappingStore {
    private Map<String, AnnotationStore> mappings;

    public MappingStore() {
        this.mappings = new HashMap<>();
    }

    public void addMapping(String key, AnnotationStore annotationStore) {
        this.mappings.put(key, annotationStore);
    }

    public AnnotationStore getMapping(String key) {
        return this.mappings.get(key);
    }

    public Map<String, AnnotationStore> getAllMappings() {
        return this.mappings;
    }

    /**
     * Trouve un mapping correspondant à l'URL et la méthode HTTP
     * Supporte les URLs avec paramètres dynamiques comme /controller/{id}
     */
    public AnnotationStore findMapping(String requestUrl, String httpMethod) {
        // Recherche exacte d'abord
        String exactKey = httpMethod + ":" + requestUrl;
        if (mappings.containsKey(exactKey)) {
            return mappings.get(exactKey);
        }

        // Recherche avec paramètres dynamiques
        for (Map.Entry<String, AnnotationStore> entry : mappings.entrySet()) {
            String mappingKey = entry.getKey();
            
            // Vérifie si la méthode HTTP correspond
            if (!mappingKey.startsWith(httpMethod + ":")) {
                continue;
            }

            // Extrait l'URL du mapping
            String mappingUrl = mappingKey.substring((httpMethod + ":").length());
            
            if (urlsMatch(requestUrl, mappingUrl)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Compare deux URLs en tenant compte des paramètres dynamiques
     * Exemple: /controller/1 correspond à /controller/{id}
     */
    private boolean urlsMatch(String requestUrl, String mappingUrl) {
        String[] requestParts = requestUrl.split("/");
        String[] mappingParts = mappingUrl.split("/");

        if (requestParts.length != mappingParts.length) {
            return false;
        }

        for (int i = 0; i < requestParts.length; i++) {
            String mappingPart = mappingParts[i];
            String requestPart = requestParts[i];

            // Si c'est un paramètre dynamique (commence par { et termine par })
            if (mappingPart.startsWith("{") && mappingPart.endsWith("}")) {
                continue; // Ce segment correspond toujours
            }

            // Sinon, les segments doivent être identiques
            if (!mappingPart.equals(requestPart)) {
                return false;
            }
        }

        return true;
    }
}