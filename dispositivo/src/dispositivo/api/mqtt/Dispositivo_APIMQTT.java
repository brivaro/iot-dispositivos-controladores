package dispositivo.api.mqtt;

import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONException;
import org.json.JSONObject;

import dispositivo.interfaces.Configuracion;
import dispositivo.interfaces.IDispositivo;
import dispositivo.interfaces.IFuncion;
import dispositivo.utils.MySimpleLogger;

public class Dispositivo_APIMQTT implements MqttCallback {

	protected MqttClient myClient;
	protected MqttConnectOptions connOpt;
	protected String clientId = null;
	
	protected IDispositivo dispositivo;
	protected String mqttBroker = null;
	protected String mqttUser = null;
	protected String mqttPassword = null;
	
	private String loggerId = null;
	
	public static Dispositivo_APIMQTT build(IDispositivo dispositivo, String brokerURL) {
		Dispositivo_APIMQTT api = new Dispositivo_APIMQTT(dispositivo);
		api.setBroker(brokerURL);
		return api;
	}
	
	protected Dispositivo_APIMQTT(IDispositivo dev) {
		this.dispositivo = dev;
		this.loggerId = dev.getId() + "-apiMQTT";
	}
	
	protected void setBroker(String mqttBrokerURL) {
		this.mqttBroker = mqttBrokerURL;
	}
	
	protected void setBrokerCredentials(String user, String password) {
		this.mqttUser = user;
		this.mqttPassword = password;
	}
	
	@Override
	public void connectionLost(Throwable t) {
		MySimpleLogger.debug(this.loggerId, "Connection lost!");
		// code to reconnect to the broker would go here if desired
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		//System.out.println("Pub complete" + new String(token.getMessage().getPayload()));
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		
		String payload = new String(message.getPayload());
		
		System.out.println("-------------------------------------------------");
		System.out.println("| Topic:" + topic);
		System.out.println("| Message: " + payload);
		System.out.println("-------------------------------------------------");
		
		
		// TO-DO: Ejercicio 7 - Codificar mensajes en JSON
		String accion = "";
		try {
			JSONObject jsonPayload = new JSONObject(payload);
			accion = jsonPayload.getString("accion");
		} catch (JSONException e) {
			MySimpleLogger.warn(this.loggerId, "Mensaje MQTT no es un JSON válido: " + payload);
			return;
		}

		// TO-DO: Ejercicio 8 - Extender API para habilitar/deshabilitar dispositivo (y sus funciones)
		if (topic.equals(this.calculateDeviceCommandTopic())) {
			if (accion.equalsIgnoreCase("habilitar")) {
				this.dispositivo.habilitar();
			} else if (accion.equalsIgnoreCase("deshabilitar")) {
				this.dispositivo.deshabilitar();
			} else {
				MySimpleLogger.warn(this.loggerId, "Acción de dispositivo '" + accion + "' no reconocida.");
			}
			return;
		}
		
		String[] topicNiveles = topic.split("/");
		String funcionId = topicNiveles[topicNiveles.length-2];
		
		IFuncion f = this.dispositivo.getFuncion(funcionId);
		if ( f == null ) {
			MySimpleLogger.warn(this.loggerId, "No encontrada funcion " + funcionId);
			return;
		}

		if ( accion.equalsIgnoreCase("encender") ) {
			f.encender();
		} else if ( accion.equalsIgnoreCase("apagar") ) {
			f.apagar();
		} else if ( accion.equalsIgnoreCase("parpadear") ) {
			f.parpadear();
		} else {
			MySimpleLogger.warn(this.loggerId, "Acción '" + accion + "' no reconocida. Sólo admitidas: encender, apagar o parpadear");
		}
	}

	/**
	 * 
	 * runClient
	 * The main functionality of this simple example.
	 * Create a MQTT client, connect to broker, pub/sub, disconnect.
	 * 
	 */
	public void connect() {
		// setup MQTT Client
		String clientID = this.dispositivo.getId() + UUID.randomUUID().toString() + ".subscriber";
		connOpt = new MqttConnectOptions();
		
		connOpt.setCleanSession(true);
		connOpt.setKeepAliveInterval(30);
		if ( this.mqttUser != null ) {
			connOpt.setUserName(this.mqttUser);
			connOpt.setPassword(this.mqttPassword.toCharArray());
		}
		
		// Connect to Broker
		try {
			MqttDefaultFilePersistence persistence = null;
			try {
				persistence = new MqttDefaultFilePersistence("/tmp");
			} catch (Exception e) {
			}
			if ( persistence != null )
				myClient = new MqttClient(this.mqttBroker, clientID, persistence);
			else
				myClient = new MqttClient(this.mqttBroker, clientID);


			myClient.setCallback(this);
			myClient.connect(connOpt);
		} catch (MqttException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		MySimpleLogger.info(this.loggerId, "Conectado al broker " + this.mqttBroker);

	}
	
	
	public void disconnect() {
		
		// disconnect
		try {
			// wait to ensure subscribed messages are delivered
			if (myClient != null && myClient.isConnected()) {
				myClient.disconnect();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		
	}

	
	protected void subscribe(String myTopic) {
		
		// subscribe to topic
		try {
			int subQoS = 0;
			myClient.subscribe(myTopic, subQoS);
			MySimpleLogger.info(this.loggerId, "Suscrito al topic " + myTopic);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	

	protected void unsubscribe(String myTopic) {
		
		// unsubscribe to topic
		try {
			myClient.unsubscribe(myTopic);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void iniciar() {

		if ( this.myClient == null || !this.myClient.isConnected() )
			this.connect();
		
		if ( this.dispositivo == null )
			return;
		
		// Suscripción al topic de comandos del dispositivo
		this.subscribe(this.calculateDeviceCommandTopic());

		for(IFuncion f : this.dispositivo.getFunciones())
			this.subscribe(this.calculateCommandTopic(f));
	}
	
	
	
	public void detener() {
		// To-Do
		if (this.dispositivo != null) {
			this.unsubscribe(this.calculateDeviceCommandTopic());
			for(IFuncion f : this.dispositivo.getFunciones()) {
				this.unsubscribe(calculateCommandTopic(f));
			}
		}
		this.disconnect();
	}

	protected String calculateDeviceCommandTopic() {
		return Configuracion.TOPIC_BASE + "dispositivo/" + dispositivo.getId() + "/comandos";
	}
	
	protected String calculateCommandTopic(IFuncion f) {
		return Configuracion.TOPIC_BASE + "dispositivo/" + dispositivo.getId() + "/funcion/" + f.getId() + "/comandos";
	}
	
	protected String calculateInfoTopic(IFuncion f) {
		return Configuracion.TOPIC_BASE + "dispositivo/" + dispositivo.getId() + "/funcion/" + f.getId() + "/info";
	}
	
}