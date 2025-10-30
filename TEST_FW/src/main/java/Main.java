import annotation.Controller;
import util.ControllerScanner;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("Recherche des contrôleurs dans tous les packages...");
            var controllers = ControllerScanner.findControllers();
            
            if (controllers.isEmpty()) {
                System.out.println("Aucun contrôleur trouvé.");
            } else {
                System.out.println("\nContrôleurs trouvés (" + controllers.size() + ") :");
                for (var controller : controllers) {
                    System.out.println("- " + controller.getName());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la recherche des contrôleurs :");
            e.printStackTrace();
        }
    }
}
