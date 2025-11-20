package com.framework.scanner;

import com.framework.annotation.Controller;
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

    /**
     * Scanne un package pour trouver les classes annotées avec @Controller
     */
    public static MappingStore scanPackage(String packageName) throws Exception {
        MappingStore mappingStore = new MappingStore();
        List<Class<?>> classes = findClasses(packageName);

        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Controller.class)) {
                processController(clazz, mappingStore);
            }
        }

        return mappingStore;
    }

    /**
     * Nouvelle méthode robuste : trouve toutes les classes du package,
     * que ce soit dans un dossier ou dans un JAR
     */
    private static List<Class<?>> findClasses(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();

            if ("file".equals(protocol)) {
                // Cas 1 : classes dans WEB-INF/classes ou dossier décompacté
                String filePath = URLDecoder.decode(resource.getFile(), "UTF-8");
                File directory = new File(filePath);
                if (directory.exists() && directory.isDirectory()) {
                    classes.addAll(findClassesInDirectory(directory, packageName));
                }

            } else if ("jar".equals(protocol)) {
                // Cas 2 : classes dans un JAR (ex: FrameworkServlet.jar)
                String jarPath = resource.getPath();
                // Format : file:/chemin/monapp.war!/WEB-INF/lib/monjar.jar!/com/mon/package
                int separator = jarPath.indexOf("!");
                if (separator == -1) continue;

                String jarFilePath = jarPath.substring(5, separator); // enlève "file:"
                jarFilePath = URLDecoder.decode(jarFilePath, "UTF-8");

                try (JarFile jarFile = new JarFile(jarFilePath)) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String entryName = entry.getName();

                        if (entryName.startsWith(path + "/")
                                && entryName.endsWith(".class")
                                && !entry.isDirectory()) {

                            String className = entryName
                                    .substring(0, entryName.length() - 6)
                                    .replace('/', '.');
                            classes.add(Class.forName(className));
                        }
                    }
                }
            }
        }

        return classes;
    }

    /**
     * Parcours récursif d'un répertoire de classes
     */
    private static List<Class<?>> findClassesInDirectory(File directory, String packageName)
            throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) return classes;

        File[] files = directory.listFiles();
        if (files == null) return classes;

        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClassesInDirectory(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                classes.add(Class.forName(className));
            }
        }
        return classes;
    }

    /**
     * Traite une classe annotée @Controller (inchangée)
     */
    private static void processController(Class<?> clazz, MappingStore mappingStore) {
        Controller controllerAnnotation = clazz.getAnnotation(Controller.class);
        String controllerUrl = controllerAnnotation.url();

        if (!controllerUrl.startsWith("/")) {
            controllerUrl = "/" + controllerUrl;
        }
        if (controllerUrl.endsWith("/")) {
            controllerUrl = controllerUrl.substring(0, controllerUrl.length() - 1);
        }

        for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(com.framework.annotation.Path.class)) {
                com.framework.annotation.Path pathAnnotation = method.getAnnotation(com.framework.annotation.Path.class);
                String methodUrl = pathAnnotation.url();
                String httpMethod = pathAnnotation.method().toUpperCase();

                if (!methodUrl.startsWith("/")) {
                    methodUrl = "/" + methodUrl;
                }

                String fullUrl = controllerUrl + methodUrl;
                fullUrl = fullUrl.replaceAll("/+", "/");
                if (fullUrl.isEmpty()) fullUrl = "/";

                String key = httpMethod + ":" + fullUrl;

                AnnotationStore annotationStore = new AnnotationStore(clazz, method, fullUrl, httpMethod);
                mappingStore.addMapping(key, annotationStore);

                System.out.println("Mapping enregistré: " + key + " -> " + clazz.getName() + "." + method.getName());
            }
        }
    }
}