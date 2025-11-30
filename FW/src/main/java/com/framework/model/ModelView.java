package com.framework.model;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    private String view;
    private final Map<String, Object> data = new HashMap<>();

    public ModelView() {}

    public ModelView(String view) {
        this.view = view;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public ModelView addObject(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    // Raccourci pratique
    public ModelView add(String key, Object value) {
        return addObject(key, value);
    }
}