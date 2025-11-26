package com.framework.scanner;

import com.framework.annotation.Controller;
import com.framework.annotation.Path;
import com.framework.mapping.AnnotationStore;
import com.framework.mapping.MappingStore;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ControllerScanner {

    public static MappingStore scan(String packageName) throws Exception {
        MappingStore store = new MappingStore();
        List<Class<?>> classes = findClasses(packageName);

        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Controller.class)) {
                registerController(clazz, store);
            }
        }
        return store;
    }

private static void registerController(Class<?> clazz, MappingStore store) {
    Controller ctrl = clazz.getAnnotation(Controller.class);
    String baseUrl = ctrl.url();                     // ← url()
    if (!baseUrl.startsWith("/")) baseUrl = "/" + baseUrl;
    if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

    for (var method : clazz.getDeclaredMethods()) {
        if (method.isAnnotationPresent(Path.class)) {
            Path path = method.getAnnotation(Path.class);
            String methodUrl = path.url();           // ← url()
            String httpMethod = path.method();       // ← method()

            if (!methodUrl.startsWith("/")) methodUrl = "/" + methodUrl;
            String fullUrl = (baseUrl + methodUrl).replaceAll("/+", "/");
            if ("/".equals(fullUrl)) fullUrl = "/";

            String key = httpMethod.toUpperCase() + ":" + fullUrl;
            store.addMapping(key, new AnnotationStore(clazz, method, fullUrl, httpMethod.toUpperCase()));

            System.out.println("Mapped " + key + " → " + clazz.getSimpleName() + "." + method.getName() + "()");
        }
    }
}

    private static List<Class<?>> findClasses(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = cl.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("file".equals(resource.getProtocol())) {
                String filePath = URLDecoder.decode(resource.getFile(), "UTF-8");
                classes.addAll(findInDirectory(new File(filePath), packageName));
            } else if ("jar".equals(resource.getProtocol())) {
                String jarPath = resource.getPath();
                int sep = jarPath.indexOf("!");
                String jarFile = jarPath.substring(5, sep);
                jarFile = URLDecoder.decode(jarFile, "UTF-8");
                try (JarFile jar = new JarFile(jarFile)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry e = entries.nextElement();
                        if (e.getName().startsWith(path + "/") && e.getName().endsWith(".class")) {
                            String className = e.getName().replace("/", ".").substring(0, e.getName().length() - 6);
                            classes.add(Class.forName(className));
                        }
                    }
                }
            }
        }
        return classes;
    }

    private static List<Class<?>> findInDirectory(File dir, String packageName) throws ClassNotFoundException {
        List<Class<?>> list = new ArrayList<>();
        if (!dir.exists()) return list;
        File[] files = dir.listFiles();
        if (files == null) return list;

        for (File f : files) {
            if (f.isDirectory()) {
                list.addAll(findInDirectory(f, packageName + "." + f.getName()));
            } else if (f.getName().endsWith(".class")) {
                String className = packageName + "." + f.getName().substring(0, f.getName().length() - 6);
                list.add(Class.forName(className));
            }
        }
        return list;
    }
}