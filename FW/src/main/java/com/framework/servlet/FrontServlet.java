package com.framework.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {

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
