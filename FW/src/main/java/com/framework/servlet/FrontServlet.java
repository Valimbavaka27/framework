package com.framework.servlet;

import com.framework.mapping.AnnotationStore;
import com.framework.mapping.MappingStore;
import com.framework.scanner.ControllerScanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
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

        // Récupère le chemin demandé après le contexte
        String path = req.getRequestURI().substring(req.getContextPath().length());
        String httpMethod = req.getMethod().toUpperCase();

        // Récupère le MappingStore du ServletContext
        MappingStore mappingStore = (MappingStore) getServletContext().getAttribute(MAPPING_STORE_KEY);

        if (mappingStore == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().println("Erreur: MappingStore non initialisé");
            return;
        }

        // Cherche le mapping correspondant
        AnnotationStore annotationStore = mappingStore.findMapping(path, httpMethod);

        if (annotationStore != null) {
            // Mapping trouvé - affiche les informations
            resp.setContentType("text/plain; charset=UTF-8");
            PrintWriter out = resp.getWriter();
            
            out.println("========== MAPPING TROUVÉ ==========");
            out.println("URL demandée: " + path);
            out.println("Méthode HTTP: " + httpMethod);
            out.println("------------------------------------");
            out.println("Classe: " + annotationStore.getClazz().getName());
            out.println("Méthode: " + annotationStore.getMethod().getName());
            out.println("URL complète du mapping: " + annotationStore.getFullUrl());
            out.println("====================================");
            
        } else {
            // Aucun mapping trouvé - essaie de servir un fichier statique
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