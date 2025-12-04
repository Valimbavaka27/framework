package com.framework.servlet;

import com.framework.mapping.AnnotationStore;
import com.framework.mapping.MappingStore;
import com.framework.model.ModelView;
import com.framework.scanner.ControllerScanner;
import com.framework.annotation.RequestParam;  // ← Nouvelle annotation
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

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
        if (path.isEmpty() || "/".equals(path)) path = "/";
        String method = req.getMethod().toUpperCase();
        AnnotationStore route = mappingStore.findMapping(path, method);

        if (route != null) {
            try {
                Object controller = route.getControllerClass().getDeclaredConstructor().newInstance();
                Method m = route.getMethod();
                m.setAccessible(true);

                // =============== SPRINT 6-BIS : @RequestParam ===============
                Parameter[] parameters = m.getParameters();
                Object[] args = new Object[parameters.length];

                for (int i = 0; i < parameters.length; i++) {
                    Parameter param = parameters[i];

                    if (param.isAnnotationPresent(RequestParam.class)) {
                        RequestParam rp = param.getAnnotation(RequestParam.class);
                        String paramName = rp.value();
                        String value = req.getParameter(paramName);

                        // Gestion du required + defaultValue
                        if (value == null || value.isEmpty()) {
                            if (rp.required()) {
                                throw new IllegalArgumentException("Paramètre requis manquant ou vide : " + paramName);
                            } else {
                                value = rp.defaultValue();
                                if ("".equals(value)) {
                                    value = null; // si defaultValue est vide → on met null
                                }
                            }
                        }

                        args[i] = convert(value, param.getType());
                    } else {
                        // Si pas d'annotation → null (ou tu peux lever une exception si tu veux être strict)
                        args[i] = null;
                    }
                }

                Object result = m.invoke(controller, args);
                // ============================================================

                // Gestion du retour de la méthode
                if (result instanceof String s) {
                    resp.setContentType("text/html;charset=UTF-8");
                    resp.getWriter().print(s);
                }
                else if (result instanceof ModelView mv) {
                    if (mv.getData() != null) {
                        mv.getData().forEach(req::setAttribute);
                    }
                    String viewPath = mv.getView();
                    if (!viewPath.startsWith("/")) viewPath = "/" + viewPath;
                    if (!viewPath.endsWith(".jsp")) viewPath += ".jsp";
                    viewPath = "/WEB-INF/views" + viewPath;
                    req.getRequestDispatcher(viewPath).forward(req, resp);
                }
                else {
                    resp.sendError(500, "Type de retour non supporté : " + (result == null ? "null" : result.getClass()));
                }

            } catch (Exception e) {
                e.printStackTrace();
                resp.setContentType("text/html;charset=UTF-8");
                resp.getWriter().println("<h2 style='color:red'>Erreur 500</h2><pre>");
                e.printStackTrace(resp.getWriter());
                resp.getWriter().println("</pre>");
            }
        } else {
            // Fichiers statiques ou 404
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

    // Méthode de conversion (inchangée mais améliorée un peu)
    private Object convert(String value, Class<?> targetType) {
        if (value == null) return null;

        if (targetType == String.class) return value;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(value);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(value);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(value);
        if (targetType == float.class || targetType == Float.class) return Float.parseFloat(value);
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value) || "on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "1".equals(value);
        }
        return value; // fallback
    }
}