module dispositivo {
	requires jdk.httpserver;
	requires java.json;
	requires org.eclipse.paho.client.mqttv3;
	requires org.restlet;
	exports dispositivo.interfaces;
	exports dispositivo.componentes;
	exports dispositivo.api.mqtt;
	exports dispositivo.api.rest;
}

