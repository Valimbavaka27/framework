package com.framework.servlet;

import com.framework.mapping.AnnotationStore;
import com.framework.mapping.MappingStore;
import com.framework.model.ModelView;
import com.framework.scanner.ControllerScanner;

import jakarta.servlet.ServletException;
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