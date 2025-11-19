package com.framework.mapping;

import java.lang.reflect.Method;

public class AnnotationStore {
    private Class<?> clazz;
    private Method method;
    private String fullUrl;
    private String httpMethod; // GET, POST, etc.

    public AnnotationStore(Class<?> clazz, Method method, String fullUrl, String httpMethod) {
        this.clazz = clazz;
        this.method = method;
        this.fullUrl = fullUrl;
        this.httpMethod = httpMethod;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public String getFullUrl() {
        return fullUrl;
    }

    public void setFullUrl(String fullUrl) {
        this.fullUrl = fullUrl;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
}