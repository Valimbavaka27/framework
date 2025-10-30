package util;

import annotation.Controller;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ControllerScanner {
    
    public static List<Class<?>> findControllers() throws ClassNotFoundException, IOException {
        List<Class<?>> controllers = new ArrayList<>();
        String basePath = "src/main/java/";
        File baseDir = new File(basePath);
        
        if (baseDir.exists()) {
            scanDirectory(baseDir, "", controllers);
        } else {
            // Si on est dans un JAR ou autre, on essaie avec le classloader
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
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        controllers.add(clazz);
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
                // Nettoyer le nom de la classe pour les chemins Windows
                className = className.replace("\\", ".").replace("/", ".");
                if (className.startsWith(".")) {
                    className = className.substring(1);
                }
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        controllers.add(clazz);
                    }
                } catch (NoClassDefFoundError | ClassNotFoundException e) {
                    // Ignorer les classes qui ne peuvent pas être chargées
                }
            }
        }
    }
}
