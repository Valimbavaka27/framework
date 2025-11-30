package com.framework.servlet;

import com.framework.mapping.AnnotationStore;
import com.framework.mapping.MappingStore;
import com.framework.model.ModelView;
import com.framework.scanner.ControllerScanner;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.*;
import java.lang.reflect.Method;

@WebServlet("/")
public class FrontServlet extends HttpServlet {

    private MappingStore mappingStore;

    @Override
    public void init() throws ServletException {
        String packageName = getServletContext().getInitParameter("controller-package");
        if (packageName == null || packageName.trim().isEmpty()) {
            throw new ServletException("Paramètre 'controller-package' manquant dans web.xml");
        }

        try {
            mappingStore = ControllerScanner.scan(packageName.trim());
            getServletContext().setAttribute("mappingStore", mappingStore);
            System.out.println("Framework initialisé : " + mappingStore.getAllMappings().size() + " routes chargées");
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Échec du scan des controllers", e);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.isEmpty()) path = "/";
        String method = req.getMethod().toUpperCase();

        AnnotationStore route = mappingStore.findMapping(path, method);

        if (route != null) {
            try {
                Object controller = route.getControllerClass().getDeclaredConstructor().newInstance();
                Method m = route.getMethod();
                m.setAccessible(true);
                Object result = m.invoke(controller);

                if (result instanceof String s) {
                    resp.setContentType("text/plain;charset=UTF-8");
                    resp.getWriter().print(s);
                } else if (result instanceof ModelView mv) {
                    // NOUVELLE PARTIE : injection des données
                    if (mv.getData() != null) {
                        for (var entry : mv.getData().entrySet()) {
                            req.setAttribute(entry.getKey(), entry.getValue());
                        }
                    }

                    String viewPath = mv.getView();
                    if (!viewPath.startsWith("/")) {
                        viewPath = "/" + viewPath;
                    }
                    if (!viewPath.endsWith(".jsp")) {
                        viewPath += ".jsp";
                    }

                    req.getRequestDispatcher(viewPath).forward(req, resp);
                } else {
                    resp.sendError(500, "Type de retour non supporté");
                }
            } catch (Exception e) {
                e.printStackTrace();
                resp.sendError(500, "Erreur dans le controller : " + e.getCause());
            }
        } else {
            // Fichier statique ou 404
            String realPath = getServletContext().getRealPath(path);
            File file = new File(realPath);
            if (file.exists() && file.isFile()) {
                String mime = getServletContext().getMimeType(file.getName());
                resp.setContentType(mime != null ? mime : "application/octet-stream");
                try (InputStream in = new FileInputStream(file);
                     OutputStream out = resp.getOutputStream()) {
                    in.transferTo(out);
                }
            } else {
                resp.sendError(404, "Page non trouvée : " + path);
            }
        }
    }
}