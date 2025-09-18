package dispositivo;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

class ServidorWebMaestroEsclavo {

    private final int puerto;
    private final Collection<Map<String, String>> estadosDispositivos;
    // --- NUEVO: Necesitamos una referencia al controlador para poder enviar comandos ---
    private final ControladorMaestroEsclavo controlador; 
    private final String maestroId;

    public ServidorWebMaestroEsclavo(int puerto, Collection<Map<String, String>> estados, ControladorMaestroEsclavo controlador, String maestroId) {
        this.puerto = puerto;
        this.estadosDispositivos = estados;
        this.controlador = controlador; // Guardamos la referencia
        this.maestroId = maestroId;     // Guardamos el ID del maestro
    }

    public void iniciar() throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        servidor.createContext("/", new ManejadorDeFicheros());
        servidor.createContext("/status", new ManejadorDeEstado());
        // --- NUEVO: Añadimos el endpoint para los comandos ---
        servidor.createContext("/command", new ManejadorDeComandos()); 
        servidor.setExecutor(null);
        servidor.start();
        System.out.println("Interfaz web iniciada en http://localhost:" + puerto);
    }

    // --- NUEVO HANDLER COMPLETO PARA LOS COMANDOS ---
    class ManejadorDeComandos implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (!"POST".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(405, -1); // 405 Method Not Allowed
                return;
            }
            try {
                InputStreamReader isr = new InputStreamReader(t.getRequestBody(), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                String body = br.readLine();
                JSONObject json = new JSONObject(body);
                String accion = json.getString("accion");

                // Usamos la referencia al controlador para enviar el comando MQTT al maestro
                controlador.enviarComando(maestroId, "f1", accion);

                String respuesta = "{\"status\":\"Comando '" + accion + "' enviado al maestro.\"}";
                t.getResponseHeaders().set("Content-Type", "application/json");
                t.sendResponseHeaders(200, respuesta.getBytes().length);
                OutputStream os = t.getResponseBody();
                os.write(respuesta.getBytes());
                os.close();
            } catch (Exception e) {
                String error = "{\"error\":\"Petición inválida.\"}";
                t.sendResponseHeaders(400, error.getBytes().length); // 400 Bad Request
                OutputStream os = t.getResponseBody();
                os.write(error.getBytes());
                os.close();
            }
        }
    }

    class ManejadorDeEstado implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String respuestaJson = new JSONArray(estadosDispositivos).toString();
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.sendResponseHeaders(200, respuestaJson.getBytes().length);
            OutputStream os = t.getResponseBody();
            os.write(respuestaJson.getBytes());
            os.close();
        }
    }

    class ManejadorDeFicheros implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                String respuestaHtml = new String(Files.readAllBytes(Paths.get("maestro esclavo/index.html")));
                t.getResponseHeaders().set("Content-Type", "text/html");
                t.sendResponseHeaders(200, respuestaHtml.getBytes().length);
                OutputStream os = t.getResponseBody();
                os.write(respuestaHtml.getBytes());
                os.close();
            } catch (IOException e) { 
                /* ... manejo de error ... */ 
            }
        }
    }
}