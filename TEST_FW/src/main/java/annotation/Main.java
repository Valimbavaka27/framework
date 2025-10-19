package annotation;

import java.lang.reflect.Method;

public class Main {
    public static void main(String[] args) throws Exception {
        TestAnnotation test = new TestAnnotation();

        for (Method method : TestAnnotation.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Route.class)) {
                Route route = method.getAnnotation(Route.class);
                System.out.println("Méthode : " + method.getName() + " | URL : " + route.url());
                method.invoke(test);
            }
        }
    }
}
