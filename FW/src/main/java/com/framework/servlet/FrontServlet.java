package com.framework.servlet;

import com.framework.mapping.AnnotationStore;
import com.framework.mapping.MappingStore;
import com.framework.model.ModelView;
import com.framework.scanner.ControllerScanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;

import java.util.Map;
import java.lang.reflect.Method;
import com.framework.annotation.ControllerScanner;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Method;

public class FrontServlet extends HttpServlet {

    public static final String MAPPING_STORE_KEY = "mappingStore";

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            String packageToScan = getServletContext().getInitParameter("controller-package");
            if (packageToScan == null || packageToScan.trim().isEmpty()) {
                throw new ServletException("Paramètre 'controller-package' manquant dans web.xml");
            }

            System.out.println("Scan du package: " + packageToScan);
            MappingStore mappingStore = ControllerScanner.scanPackage(packageToScan.trim());
            getServletContext().setAttribute(MAPPING_STORE_KEY, mappingStore);

            System.out.println("MappingStore initialisé avec " + mappingStore.getAllMappings().size() + " mappings");

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Erreur init FrontServlet", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String pkg = null;
        if (config != null) {
            pkg = config.getInitParameter("scanPackage");
        }
        if (pkg == null || pkg.isEmpty()) {
            if (getServletContext() != null) {
                pkg = getServletContext().getInitParameter("scanPackage");
            }
        }
        try {
            Map<String, Method> mappings = ControllerScanner.scanRoutes(pkg);
            getServletContext().setAttribute("routeMappings", mappings);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }

    private void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String path = req.getRequestURI().substring(req.getContextPath().length());
        String httpMethod = req.getMethod().toUpperCase();

        MappingStore mappingStore = (MappingStore) getServletContext().getAttribute(MAPPING_STORE_KEY);
        if (mappingStore == null) {
            resp.sendError(500, "MappingStore non initialisé");
            return;
        }

        AnnotationStore annotationStore = mappingStore.findMapping(path, httpMethod);

        if (annotationStore != null) {
            try {
                Object controller = annotationStore.getClazz().getDeclaredConstructor().newInstance();
                Method method = annotationStore.getMethod();
                method.setAccessible(true);
                Object result = method.invoke(controller);

                
                if (result instanceof String) {
                    resp.setContentType("text/plain; charset=UTF-8");
                    resp.getWriter().print((String) result);

                } else if (result instanceof ModelView) {
                    ModelView mv = (ModelView) result;
                    String jspPath = "/" + mv.getView();   // JSP à la racine
                    req.getRequestDispatcher(jspPath).forward(req, resp);

                } else {
                    // Si c'est void ou autre chose → on va sur index.jsp
                    req.getRequestDispatcher("/index.jsp").forward(req, resp);
        @SuppressWarnings("unchecked")
        Map<String, Method> routeMappings = (Map<String, Method>) getServletContext().getAttribute("routeMappings");
        if (routeMappings != null) {
            Method m = routeMappings.get(path);
            if (m != null) {
                resp.setContentType("text/plain");
                resp.getWriter().println("URL supportée : " + path);
                resp.getWriter().println("Classe : " + m.getDeclaringClass().getName());
                resp.getWriter().println("Méthode : " + m.getName());
                return;
            }
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

            } catch (Exception e) {
                e.printStackTrace();
                resp.sendError(500, "Erreur contrôleur: " + e.getMessage());
            }
        } else {
            // Aucun mapping → page d'accueil
            req.getRequestDispatcher("/index.jsp").forward(req, resp);
        }
    }
}