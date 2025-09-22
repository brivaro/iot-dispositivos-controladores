package dispositivo;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONException;
import org.json.JSONObject;

public class ControladorSemaforico {

    // Tiempos de duración para cada estado del semáforo (en milisegundos)
    private static final long TIEMPO_VERDE = 5000;
    private static final long TIEMPO_AMARILLO = 2000;
    private static final long TIEMPO_TODO_ROJO = 1000;
    private static final int PUERTO_WEB = 8088; // Puerto para la interfaz web

    private static MqttClient clienteMqtt;
    
    // Usamos un ConcurrentHashMap para guardar el estado de forma segura entre hilos
    // El hilo principal actualiza el estado, y el hilo del servidor web lo lee.
    private static Map<String, String> estadosSemaforos = new ConcurrentHashMap<>();

    /**
     * Método de ayuda para enviar un comando a una función específica de un dispositivo.
     * @param idDispositivo El ID del dispositivo objetivo.
     * @param idFuncion El ID de la función (f1, f2, f3).
     * @param accion La acción a realizar (encender, apagar).
     */
    private static void enviarComando(String idDispositivo, String idFuncion, String accion) {
        // Construimos el topic MQTT según la estructura definida
        String topic = String.format("dispositivo/%s/funcion/%s/comandos", idDispositivo, idFuncion);
        
        // Creamos el payload en formato JSON
        JSONObject payload = new JSONObject();
        try {
            payload.put("accion", accion);
        } catch (JSONException e) {
            // Esto es muy improbable que suceda con estas claves, pero se maneja la excepción.
            e.printStackTrace();
            return; // Salimos del método si falla la creación del JSON
        }
        MqttMessage mensaje = new MqttMessage(payload.toString().getBytes());
        mensaje.setQos(0); // QoS 0 es suficiente para comandos no críticos
        mensaje.setRetained(false);
        try {
            System.out.printf("-> Enviando a '%s': funcion '%s', accion '%s'\n", idDispositivo, idFuncion, accion);
            clienteMqtt.publish(topic, mensaje);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Método para cambiar el estado de un semáforo completo.
     * @param idDispositivo El ID del semáforo a cambiar.
     * @param color El color al que debe cambiar ("ROJO", "AMARILLO", "VERDE").
     */
    private static void cambiarSemaforo(String idDispositivo, String color) {
        System.out.printf("Cambiando semáforo %s a %s\n", idDispositivo, color);
        
        // **ACTUALIZAMOS EL ESTADO INTERNO**
        // El mapa de estados se usa para la interfaz web
        String idKey = estadosSemaforos.get("id1").equals(idDispositivo) ? "estado1" : "estado2";
        estadosSemaforos.put(idKey, color);

        switch (color.toUpperCase()) {
            case "ROJO":
                enviarComando(idDispositivo, "f1", "encender"); // f1 = rojo
                enviarComando(idDispositivo, "f2", "apagar");   // f2 = amarillo
                enviarComando(idDispositivo, "f3", "apagar");   // f3 = verde
                break;
            case "AMARILLO":
                enviarComando(idDispositivo, "f1", "apagar");
                enviarComando(idDispositivo, "f2", "encender");
                enviarComando(idDispositivo, "f3", "apagar");
                break;
            case "VERDE":
                enviarComando(idDispositivo, "f1", "apagar");
                enviarComando(idDispositivo, "f2", "apagar");
                enviarComando(idDispositivo, "f3", "encender");
                break;
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Uso: java ControladorSemaforico <broker-mqtt> <id-dispositivo-1> <id-dispositivo-2>");
            System.out.println("Uso: java ControladorSemaforico tcp://tambori.dsic.upv.es:10083 ttmi050 ttmi051");
            return;
        }

        String broker = args[0];
        String idDispositivo1 = args[1];
        String idDispositivo2 = args[2];

        // Guardamos los IDs en nuestro mapa de estado
        estadosSemaforos.put("id1", idDispositivo1);
        estadosSemaforos.put("id2", idDispositivo2);
        estadosSemaforos.put("estado1", "ROJO"); // Estado inicial
        estadosSemaforos.put("estado2", "ROJO"); // Estado inicial

        // --- Iniciar Servidor Web ---
        try {
            ServidorWebSemaforo web = new ServidorWebSemaforo(PUERTO_WEB, estadosSemaforos);
            web.iniciar();
        } catch (IOException e) {
            System.err.println("Error al iniciar el servidor web.");
            e.printStackTrace();
            return;
        }

        // --- Conexión al Broker MQTT ---
        try {
            clienteMqtt = new MqttClient(broker, "ControladorSemaforico_" + UUID.randomUUID().toString(), new MqttDefaultFilePersistence("/tmp"));
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println("Conectando al broker: " + broker);
            clienteMqtt.connect(connOpts);
            System.out.println("Conectado.");
        } catch (MqttException e) {
            e.printStackTrace();
            return;
        }

        // --- Bucle principal del controlador ---
        try {
            // 1. Estado inicial: Poner ambos semáforos en rojo
            System.out.println("Iniciando secuencia: Ambos semáforos en ROJO.");
            cambiarSemaforo(idDispositivo1, "ROJO");
            cambiarSemaforo(idDispositivo2, "ROJO");
            Thread.sleep(TIEMPO_TODO_ROJO);

            while (true) {
                // Secuencia para el Semáforo 1
                cambiarSemaforo(idDispositivo1, "VERDE"); // 2. Pongo en verde al dispositivo 1
                Thread.sleep(TIEMPO_VERDE);
                
                cambiarSemaforo(idDispositivo1, "AMARILLO"); // 3. Pongo en amarillo al dispositivo 1
                Thread.sleep(TIEMPO_AMARILLO);

                cambiarSemaforo(idDispositivo1, "ROJO"); // 4. Pongo en rojo al dispositivo 1
                Thread.sleep(TIEMPO_TODO_ROJO); // Pausa de seguridad

                // Secuencia para el Semáforo 2
                cambiarSemaforo(idDispositivo2, "VERDE"); // 5. Pongo en verde al dispositivo 2
                Thread.sleep(TIEMPO_VERDE);

                cambiarSemaforo(idDispositivo2, "AMARILLO"); // 6. Pongo en amarillo al dispositivo 2
                Thread.sleep(TIEMPO_AMARILLO);
                
                cambiarSemaforo(idDispositivo2, "ROJO"); // 7. Pongo en rojo al dispositivo 2
                Thread.sleep(TIEMPO_TODO_ROJO); // Pausa de seguridad
                
                // 8. Volver a 2 (el bucle while se encarga de esto)
            }
        } catch (InterruptedException e) {
            System.out.println("Controlador de semáforo interrumpido.");
        } finally {
            // Desconectar el cliente MQTT al terminar
            if (clienteMqtt != null && clienteMqtt.isConnected()) {
                try {
                    clienteMqtt.disconnect();
                    System.out.println("Desconectado del broker.");
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}