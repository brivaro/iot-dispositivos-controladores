package dispositivo;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import org.json.JSONObject;

public class ServidorWeb {

    private final int puerto;
    // Usamos un mapa para mantener el estado actual de los semáforos.
    // Este mapa será compartido con el controlador principal.
    private final Map<String, String> estadosSemaforos;

    public ServidorWeb(int puerto, Map<String, String> estadosSemaforos) {
        this.puerto = puerto;
        this.estadosSemaforos = estadosSemaforos;
    }

    public void iniciar() throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        
        // El contexto "/" servirá nuestro fichero index.html
        servidor.createContext("/", new ManejadorDeFicheros());
        
        // El contexto "/status" devolverá el estado actual en formato JSON
        servidor.createContext("/status", new ManejadorDeEstado());
        
        servidor.setExecutor(null); // Usamos el gestor de hilos por defecto
        servidor.start();
        System.out.println("Servidor web iniciado en http://localhost:" + puerto);
    }

    // Clase interna para manejar las peticiones al endpoint de estado
    class ManejadorDeEstado implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            // Creamos un objeto JSON con el estado actual
            String respuestaJson = new JSONObject(estadosSemaforos).toString();
            
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.sendResponseHeaders(200, respuestaJson.getBytes().length);
            OutputStream os = t.getResponseBody();
            os.write(respuestaJson.getBytes());
            os.close();
        }
    }

    // Clase interna para servir el fichero index.html
    class ManejadorDeFicheros implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String respuestaHtml = "";
            try {
                // Leemos el fichero index.html del disco
                respuestaHtml = new String(Files.readAllBytes(Paths.get("controlador semafórico/index.html")));
                t.getResponseHeaders().set("Content-Type", "text/html");
                t.sendResponseHeaders(200, respuestaHtml.getBytes().length);
            } catch (IOException e) {
                respuestaHtml = "Error: No se encontra el fichero index.html.";
                System.err.println(respuestaHtml);
                t.sendResponseHeaders(404, respuestaHtml.getBytes().length);
            }
            
            OutputStream os = t.getResponseBody();
            os.write(respuestaHtml.getBytes());
            os.close();
        }
    }
}