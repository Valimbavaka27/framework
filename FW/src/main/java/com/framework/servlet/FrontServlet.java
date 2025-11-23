package com.framework.servlet;

import com.framework.mapping.AnnotationStore;
import com.framework.mapping.MappingStore;
import com.framework.scanner.ControllerScanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {

    public static final String MAPPING_STORE_KEY = "mappingStore";

    @Override
    public void init() throws ServletException {
        super.init();
        
        try {
            // Récupère le package à scanner depuis web.xml
            String packageToScan = getServletContext().getInitParameter("controller-package");
            
            if (packageToScan == null || packageToScan.isEmpty()) {
                throw new ServletException("Le paramètre 'controller-package' n'est pas défini dans web.xml");
            }

            System.out.println("Scan du package: " + packageToScan);
            
            // Scanne le package et crée le MappingStore
            MappingStore mappingStore = ControllerScanner.scanPackage(packageToScan);
            
            // Enregistre le MappingStore dans le ServletContext
            getServletContext().setAttribute(MAPPING_STORE_KEY, mappingStore);
            
            System.out.println("MappingStore initialisé avec " + 
                mappingStore.getAllMappings().size() + " mappings");
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Erreur lors de l'initialisation du FrontServlet", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRequest(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRequest(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRequest(req, resp);
    }

    private void handleRequest(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

    String path = req.getRequestURI().substring(req.getContextPath().length());
    String httpMethod = req.getMethod().toUpperCase();

    MappingStore mappingStore = (MappingStore) getServletContext().getAttribute(MAPPING_STORE_KEY);

    if (mappingStore == null) {
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "MappingStore non initialisé");
        return;
    }

    AnnotationStore annotationStore = mappingStore.findMapping(path, httpMethod);

    if (annotationStore != null) {
        try {
            // Instanciation du contrôleur
            Object controller = annotationStore.getClazz().getDeclaredConstructor().newInstance();
            Method method = annotationStore.getMethod();
            method.setAccessible(true);

            // Exécution de la méthode
            Object result = method.invoke(controller);

            // NOUVELLE CONDITION DEMANDÉE : si retour = String → on l'écrit directement
            if (result instanceof String) {
                String responseText = (String) result;

                // Tu as dit : on veut toujours text/plain (sauf si tu changes d'avis plus tard)
                resp.setContentType("text/plain; charset=UTF-8");
                resp.setCharacterEncoding("UTF-8");

                PrintWriter out = resp.getWriter();
                out.print(responseText);   // pas de println() → pas de saut de ligne en trop
                out.flush();
                return; // très important : on arrête tout ici
            }

            // Si ce n'est PAS une String → on garde le comportement de debug actuel
            resp.setContentType("text/plain; charset=UTF-8");
            PrintWriter out = resp.getWriter();
            out.println("========== MAPPING TROUVÉ ==========");
            out.println("URL: " + path);
            out.println("Méthode HTTP: " + httpMethod);
            out.println("Classe: " + annotationStore.getClazz().getName());
            out.println("Méthode: " + method.getName());
            out.println("Retour: " + (result != null ? result : "void"));
            out.println("====================================");

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Erreur dans le contrôleur : " + e.getCause());
        }

    } else {
        servirRessource(req, resp, path);
    }
}
    private void servirRessource(HttpServletRequest req, HttpServletResponse resp, String path)
            throws ServletException, IOException {

        if (path == null || path.isEmpty() || "/".equals(path)) {
            path = "/index.html";
        }

        // Cherche le fichier directement dans webapp/
        String realPath = getServletContext().getRealPath(path);
        File file = new File(realPath);

        if (file.exists() && file.isFile()) {
            // Détecte le type MIME et renvoie le fichier
            resp.setContentType(getServletContext().getMimeType(file.getName()));
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = resp.getOutputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        } else {
            // Si aucune ressource n'est trouvée
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().println("URL demandée : bsauigsazui " + path);
            resp.getWriter().println("Aucune ressource trouvée");
        }
    }
}