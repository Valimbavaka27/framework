package com.framework.servlet;

import com.framework.mapping.AnnotationStore;
import com.framework.mapping.MappingStore;
import com.framework.model.ModelView;
import com.framework.scanner.ControllerScanner;
import com.framework.annotation.RequestParam;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

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
                // ======================= SPRINT 7 : DISTINCTION GET / POST =======================
            // Sprint 7 demande que le framework gère séparément les méthodes HTTP :
            // - GET  : Affichage, listing, formulaire, lecture
            // - POST : Insertion, traitement de formulaire, enregistrement
            // findMapping(path, method) permet d'associer une URL + une méthode HTTP 
        if (path.isEmpty() || "/".equals(path)) path = "/";
        String method = req.getMethod().toUpperCase();

        AnnotationStore route = mappingStore.findMapping(path, method);

        if (route == null) {
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
            return;
        }

        try {
            Object controller = route.getControllerClass().getDeclaredConstructor().newInstance();
            Method m = route.getMethod();
            m.setAccessible(true);

            // ========== SPRINT 6-TER : INJECTION AUTOMATIQUE DES {id} SANS -parameters ==========
            Map<String, String> pathParams = extractPathParams(route.getUrl(), path);

            Parameter[] parameters = m.getParameters();
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                String value = null;
                String paramName = null;
                boolean required = true;
                String defaultValue = "";

                // Cas 1 : @RequestParam présent → on utilise son nom
                if (param.isAnnotationPresent(RequestParam.class)) {
                    RequestParam rp = param.getAnnotation(RequestParam.class);
                    paramName = rp.value();
                    required = rp.required();
                    defaultValue = rp.defaultValue();
                }
                // Cas 2 : AUCUNE annotation → on prend le i-ème placeholder de l'URL
                else if (!pathParams.isEmpty()) {
                    int placeholderIndex = 0;
                    for (String key : pathParams.keySet()) {
                        if (placeholderIndex == i) {
                            paramName = key;
                            break;
                        }
                        placeholderIndex++;
                    }
                }

                // Récupération de la valeur avec ordre de priorité
                if (paramName != null) {
                    value = pathParams.get(paramName);                    // 1. Dans l'URL
                    if (value == null) value = req.getParameter(paramName); // 2. Dans query/form
                }

                // Gestion required / defaultValue
                if (value == null || value.isEmpty()) {
                    if (required) {
                        String name = (paramName != null) ? paramName : "paramètre " + i;
                        throw new IllegalArgumentException("Paramètre requis manquant : " + name);
                    }
                    value = defaultValue;
                }

                args[i] = convert(value, param.getType());
            }

            Object result = m.invoke(controller, args);
            // ===================================================================================

            // Gestion du retour
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
                resp.sendError(500, "Type de retour non supporté");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().println("<h2 style='color:red'>Erreur 500</h2><pre>");
            e.printStackTrace(resp.getWriter());
            resp.getWriter().println("</pre>");
        }
    }

    // Méthode magique du Sprint 6-ter
   // SPRINT 6-TER : INJECTION AUTOMATIQUE des paramètres dynamiques {id}
    private Map<String, String> extractPathParams(String pattern, String actualPath) {
        Map<String, String> params = new HashMap<>();
        String[] pat = pattern.split("/");
        String[] act = actualPath.split("/");

        if (pat.length != act.length) return params;

        for (int i = 0; i < pat.length; i++) {
            if (pat[i].startsWith("{") && pat[i].endsWith("}")) {
                String name = pat[i].substring(1, pat[i].length() - 1);
                params.put(name, act[i]);
            }
        }
        return params;
    }

    // Conversion (inchangée)
    private Object convert(String value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType == String.class) return value;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(value);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(value);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(value);
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value) || "on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "1".equals(value);
        }
        return value;
    }
}