package annotation;

public class TestAnnotation {
    @Route(url = "/hello")
    public void hello() {
        System.out.println("Hello depuis la méthode annotée !");
    }

    @Route(url = "/bye")
    public void bye() {
        System.out.println("Au revoir !");
    }
}
