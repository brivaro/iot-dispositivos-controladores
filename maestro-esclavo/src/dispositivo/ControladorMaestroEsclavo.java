package dispositivo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONException;
import org.json.JSONObject;

public class ControladorMaestroEsclavo implements MqttCallback {

    private MqttClient clienteMqtt;
    private final String broker;
    private final String maestroId;
    private final List<String> esclavoIds;
    private static final int PUERTO_WEB = 8089; // Puerto para la nueva interfaz

    // Mapa para guardar el estado de TODOS los dispositivos (maestro y esclavos)
    private static Map<String, Map<String, String>> estadosDispositivos = new ConcurrentHashMap<>();

    public ControladorMaestroEsclavo(String broker, String maestroId, List<String> esclavoIds) {
        this.broker = broker;
        this.maestroId = maestroId;
        this.esclavoIds = esclavoIds;

        // Inicializar el mapa de estados
        estadosDispositivos.put(maestroId, new ConcurrentHashMap<>(Map.of("id", maestroId, "role", "master", "state", "OFF")));
        for (String esclavoId : esclavoIds) {
            estadosDispositivos.put(esclavoId, new ConcurrentHashMap<>(Map.of("id", esclavoId, "role", "slave", "state", "OFF")));
        }
    }

    public void iniciar() {
        try {
            clienteMqtt = new MqttClient(broker, "ControladorMaestroEsclavo_" + UUID.randomUUID(), new MqttDefaultFilePersistence("/tmp"));
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            System.out.println("Conectando al broker: " + broker);
            clienteMqtt.setCallback(this);
            clienteMqtt.connect(connOpts);
            System.out.println("Conectado.");

            // **CRÍTICO: Nos suscribimos al topic 'info' de TODOS los dispositivos**
            for (String deviceId : estadosDispositivos.keySet()) {
                String topicInfo = String.format("dispositivo/%s/funcion/f1/info", deviceId);
                clienteMqtt.subscribe(topicInfo);
                System.out.println("Suscrito para escuchar estado de: " + topicInfo);
            }
            System.out.println("\nEsperando cambios en el maestro (" + maestroId + ") para replicar en los esclavos: " + esclavoIds);

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("Conexión perdida: " + cause.getMessage());
    }

    /**
     * Este es el núcleo del controlador. Se ejecuta cada vez que llega un mensaje.
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        
        String payload = new String(message.getPayload());
        
        // 1. Extraer el ID del dispositivo del topic
        String[] topicParts = topic.split("/");
        if (topicParts.length < 4) return;
        String deviceId = topicParts[1];

        // 2. Extraer el nuevo estado del payload
        String newState;
        try {
            newState = new JSONObject(payload).getString("estado");
        } catch (JSONException e) {
            System.err.println("Payload no es JSON válido: " + payload);
            return;
        }

        // 3. Actualizar nuestro mapa de estados interno
        if (estadosDispositivos.containsKey(deviceId)) {
            estadosDispositivos.get(deviceId).put("state", newState);
            System.out.printf("Estado actualizado: Dispositivo %s está ahora %s\n", deviceId, newState);
        }

        // 4. Si el cambio vino del MAESTRO, replicar en los esclavos
        if (deviceId.equals(maestroId)) {
            System.out.println("--> Cambio detectado en el MAESTRO. Replicando...");
            String accion = mapearEstadoAAccion(newState);
            if (accion != null) {
                for (String esclavoId : esclavoIds) {
                    enviarComando(esclavoId, "f1", accion);
                }
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // No es crítico para este controlador
    }

    /**
     * Convierte un estado (ej: "ON") a una acción (ej: "encender").
     */
    private String mapearEstadoAAccion(String estado) {
        switch (estado.toUpperCase()) {
            case "ON": return "encender";
            case "OFF": return "apagar";
            case "BLINK": return "parpadear";
            default: return null;
        }
    }

    /**
     * Hacemos este método PÚBLICO para que el ServidorWeb pueda llamarlo.
     */
    public void enviarComando(String idDispositivo, String idFuncion, String accion) {
        String topicComando = String.format("dispositivo/%s/funcion/%s/comandos", idDispositivo, idFuncion);
        JSONObject payloadComando = new JSONObject();
        try {
            payloadComando.put("accion", accion);
        } catch (JSONException e) {
            // Esto es muy improbable que suceda con estas claves, pero se maneja la excepción.
            e.printStackTrace();
            return; // Salimos del método si falla la creación del JSON
        }
        try {
            System.out.printf("    -> Enviando comando a '%s': %s\n", idDispositivo, payloadComando);
            clienteMqtt.publish(topicComando, new MqttMessage(payloadComando.toString().getBytes()));
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Uso: java ControladorMaestroEsclavo <broker> <maestro> <esclavo1> [esclavo2] ...");
            System.out.println("Uso: java ControladorMaestroEsclavo tcp://tambori.dsic.upv.es:10083 ttmi050 ttmi051 ttmi052 ...");
            return;
        }

        String broker = args[0];
        String maestroId = args[1];
        List<String> esclavoIds = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            esclavoIds.add(args[i]);
        }

        // Creamos la instancia del controlador PRIMERO
        ControladorMaestroEsclavo controlador = new ControladorMaestroEsclavo(broker, maestroId, esclavoIds);

        // AHORA iniciamos el servidor web, pasándole la instancia del controlador y el ID del maestro
        try {
            ServidorWebMaestroEsclavo web = new ServidorWebMaestroEsclavo(
                PUERTO_WEB,
                estadosDispositivos.values(),
                controlador, // Le pasamos la referencia a sí mismo
                maestroId    // Le pasamos el ID del maestro
            );
            web.iniciar();
        } catch (IOException e) {
            System.err.println("Error fatal al iniciar el servidor web.");
            e.printStackTrace();
            return;
        }

        // Finalmente, iniciamos la lógica MQTT del controlador
        controlador.iniciar();
    }
}