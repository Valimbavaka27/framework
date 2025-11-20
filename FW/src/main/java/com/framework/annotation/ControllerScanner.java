package com.framework.annotation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Method;
import java.lang.annotation.Annotation;

public class ControllerScanner {
    
    public static List<Class<?>> findControllers() throws ClassNotFoundException, IOException {
        List<Class<?>> controllers = new ArrayList<>();
        String basePath = "src/main/java/";
        File baseDir = new File(basePath);
        
        if (baseDir.exists()) {
            scanDirectory(baseDir, "", controllers);
        } else {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources("");
            
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File directory = new File(resource.getFile());
                if (directory.exists()) {
                    findControllersInDirectory(directory, "", controllers);
                }
            }
        }
        
        return controllers;
    }
    
    private static void scanDirectory(File directory, String packageName, List<Class<?>> controllers) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String newPackage = packageName.isEmpty() ? 
                    file.getName() : 
                    packageName + "." + file.getName();
                scanDirectory(file, newPackage, controllers);
            } else if (file.getName().endsWith(".java")) {
                try {
                    String className = (packageName + "." + file.getName().replace(".java", ""))
                                        .replace("src.main.java.", "")
                                        .replace("src\\main\\java\\", "")
                                        .replace("src/main/java/", "")
                                        .replace("\\", ".");
                    
                    if (className.startsWith(".")) {
                        className = className.substring(1);
                    }
                    
                    Class<?> clazz = Class.forName(className);
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends Annotation> controllerAnnotation = (Class<? extends Annotation>) Class.forName("com.framework.annotation.Controller");
                        if (clazz.isAnnotationPresent(controllerAnnotation)) {
                            controllers.add(clazz);
                        }
                    } catch (ClassNotFoundException ignore) {
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Classe non trouvée: " + e.getMessage());
                }
            }
        } 
    }
    
    private static void findControllersInDirectory(File directory, String packageName, List<Class<?>> controllers) 
            throws ClassNotFoundException {
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                findControllersInDirectory(file, packageName + "." + file.getName(), controllers);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                className = className.replace("\\", ".").replace("/", ".");
                if (className.startsWith(".")) {
                    className = className.substring(1);
                }
                try {
                    Class<?> clazz = Class.forName(className);
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends Annotation> controllerAnnotation = (Class<? extends Annotation>) Class.forName("com.framework.annotation.Controller");
                        if (clazz.isAnnotationPresent(controllerAnnotation)) {
                            controllers.add(clazz);
                        }
                    } catch (ClassNotFoundException ignore) {
                    }
                } catch (NoClassDefFoundError | ClassNotFoundException e) {
                }
            }
        }
    }

    public static Map<String, Method> scanRoutes(String basePackage) throws IOException {
        Map<String, Method> routes = new HashMap<>();
        String packagePath = basePackage == null ? "" : basePackage.replace('.', '/');

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(packagePath);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File directory = new File(resource.getFile());
            if (directory.exists() && directory.isDirectory()) {
                collectRoutesInDirectory(directory, basePackage == null ? "" : basePackage, routes);
            }
        }

        return routes;
    }

    private static void collectRoutesInDirectory(File directory, String packageName, Map<String, Method> routes) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String subPkg = packageName == null || packageName.isEmpty()
                        ? file.getName()
                        : packageName + "." + file.getName();
                collectRoutesInDirectory(file, subPkg, routes);
            } else if (file.getName().endsWith(".class")) {
                String className = (packageName == null || packageName.isEmpty())
                        ? file.getName().substring(0, file.getName().length() - 6)
                        : packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                className = className.replace('\\', '.').replace('/', '.');
                if (className.startsWith(".")) className = className.substring(1);
                try {
                    Class<?> clazz = Class.forName(className);
                    for (Method m : clazz.getDeclaredMethods()) {
                        if (m.isAnnotationPresent(Route.class)) {
                            Route r = m.getAnnotation(Route.class);
                            String url = r.url();
                            routes.put(url, m);
                        }
                    }
                } catch (Throwable ignore) {
                }
            }
        }
    }
}
