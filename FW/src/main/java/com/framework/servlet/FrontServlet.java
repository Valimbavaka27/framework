package com.framework.servlet;

import com.framework.mapping.AnnotationStore;
import com.framework.mapping.MappingStore;
import com.framework.model.ModelView;
import com.framework.scanner.ControllerScanner;
import com.framework.annotation.Json;          // ← Nouvelle annotation
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
        if (path.isEmpty() || "/".equals(path)) path = "/";
        String method = req.getMethod().toUpperCase();

        AnnotationStore route = mappingStore.findMapping(path, method);

        if (route == null) {
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

            // ========== SPRINT 6-TER : INJECTION AUTOMATIQUE ==========
            Map<String, String> pathParams = extractPathParams(route.getUrl(), path);

            Parameter[] parameters = m.getParameters();
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                String value = null;
                String paramName = null;
                boolean required = true;
                String defaultValue = "";

                if (param.isAnnotationPresent(RequestParam.class)) {
                    RequestParam rp = param.getAnnotation(RequestParam.class);
                    paramName = rp.value();
                    required = rp.required();
                    defaultValue = rp.defaultValue();
                } else if (!pathParams.isEmpty()) {
                    int placeholderIndex = 0;
                    for (String key : pathParams.keySet()) {
                        if (placeholderIndex == i) {
                            paramName = key;
                            break;
                        }
                        placeholderIndex++;
                    }
                }

                if (paramName != null) {
                    value = pathParams.get(paramName);
                    if (value == null) value = req.getParameter(paramName);
                }

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

            // ======================= GESTION DU RETOUR =======================

            // SPRINT 9 : Si la méthode est annotée @Json → retour JSON API REST
            if (m.isAnnotationPresent(Json.class)) {
                resp.setContentType("application/json;charset=UTF-8");
                resp.setStatus(200);

                String jsonData = objectToJson(result);

                String jsonResponse = """
                    {
                        "status": "success",
                        "code": 200,
                        "data": %s
                    }
                    """.formatted(jsonData == null ? "null" : jsonData);

                resp.getWriter().print(jsonResponse);
            }
            // String → affiché directement
            else if (result instanceof String s) {
                resp.setContentType("text/html;charset=UTF-8");
                resp.getWriter().print(s);
            }
            // ModelView → forward JSP
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
            // Sprint 8 : Autre objet → result.jsp
            else {
                req.setAttribute("data", result);
                String defaultView = "/WEB-INF/views/result.jsp";
                req.getRequestDispatcher(defaultView).forward(req, resp);
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().println("<h2 style='color:red'>Erreur 500</h2><pre>");
            e.printStackTrace(resp.getWriter());
            resp.getWriter().println("</pre>");
        }
    }

    // SPRINT 6-TER
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

    // Conversion paramètre
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

    // ======================= SPRINT 9 : CONVERSION OBJET → JSON (maison, sans dépendance) =======================
    private String objectToJson(Object obj) {
        if (obj == null) return "null";

        if (obj instanceof String) {
            return "\"" + obj.toString().replace("\"", "\\\"") + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }

        if (obj instanceof java.util.Collection<?> coll) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : coll) {
                if (!first) sb.append(",");
                sb.append(objectToJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }

        if (obj instanceof java.util.Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey().toString().replace("\"", "\\\"")).append("\":");
                sb.append(objectToJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }

        // Objet personnalisé → via réflexion
        StringBuilder sb = new StringBuilder("{");
        java.lang.reflect.Field[] fields = obj.getClass().getDeclaredFields();
        boolean first = true;
        for (java.lang.reflect.Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (!first) sb.append(",");
                sb.append("\"").append(field.getName()).append("\":");
                sb.append(objectToJson(value));
                first = false;
            } catch (IllegalAccessException e) {
                // ignore
            }
        }
        sb.append("}");
        return sb.toString();
    }
}