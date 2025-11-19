package com.framework.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.lang.reflect.Method;
import com.framework.annotation.ControllerScanner;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {

    @Override
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
        servirRessource(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        servirRessource(req, resp);
    }

    private void servirRessource(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Récupère le chemin demandé après le contexte, ex: /a.html
        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (path == null || path.isEmpty() || "/".equals(path)) {
            // Page d’accueil par défaut
            path = "/index.html";
        }

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
            }
        } else {
            // Si le fichier n’existe pas
            resp.setContentType("text/plain");
            resp.getWriter().println("URL demandée : " + path);
            resp.getWriter().println("Aucune ressource trouvée dans webapp/");
        }
    }
}
