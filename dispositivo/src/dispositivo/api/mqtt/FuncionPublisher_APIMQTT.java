package dispositivo.api.mqtt;

import java.util.UUID;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONException;
import org.json.JSONObject;

import dispositivo.interfaces.Configuracion;
import dispositivo.interfaces.IFuncion;
import dispositivo.interfaces.IFuncionPublisher;
import dispositivo.utils.MySimpleLogger;

public class FuncionPublisher_APIMQTT implements IFuncionPublisher {

    private MqttClient myClient;
    private String mqttBroker;
    private String dispositivoId;
    private String loggerId;

    public FuncionPublisher_APIMQTT(String mqttBroker, String dispositivoId) {
        this.mqttBroker = mqttBroker;
        this.dispositivoId = dispositivoId;
        this.loggerId = dispositivoId + "-apiMQTT-pub";
    }

    @Override
    public void connect() {
        String clientID = this.dispositivoId + UUID.randomUUID().toString() + ".publisher";
        MqttConnectOptions connOpt = new MqttConnectOptions();
        connOpt.setCleanSession(true);
        connOpt.setKeepAliveInterval(30);

        try {
            MqttDefaultFilePersistence persistence = new MqttDefaultFilePersistence("/tmp");
            myClient = new MqttClient(this.mqttBroker, clientID, persistence);
            myClient.connect(connOpt);
            MySimpleLogger.info(this.loggerId, "Conectado al broker " + this.mqttBroker);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() {
        try {
            if (myClient != null && myClient.isConnected()) {
                myClient.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void publish(IFuncion funcion) {
        if (myClient == null || !myClient.isConnected()) {
            MySimpleLogger.warn(loggerId, "No se puede publicar, cliente no conectado.");
            return;
        }

        String topicStr = Configuracion.TOPIC_BASE + "dispositivo/" + this.dispositivoId + "/funcion/" + funcion.getId() + "/info";
        MqttTopic topic = myClient.getTopic(topicStr);

        JSONObject payload = new JSONObject();
        try {
            payload.put("id", funcion.getId());
            payload.put("estado", funcion.getStatus().name());
        } catch (JSONException e) {
            // Esto es muy improbable que suceda con estas claves, pero se maneja la excepción.
            e.printStackTrace();
            return; // Salimos del método si falla la creación del JSON
        }

        MqttMessage message = new MqttMessage(payload.toString().getBytes());
        message.setQos(0);
        message.setRetained(false);

        try {
            MySimpleLogger.info(loggerId, "Publicando en " + topicStr + " -> " + payload.toString());
            topic.publish(message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}