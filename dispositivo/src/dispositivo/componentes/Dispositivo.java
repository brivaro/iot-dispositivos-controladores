package dispositivo.componentes;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import dispositivo.api.iot.infraestructure.Dispositivo_RegistradorMQTT;
import dispositivo.api.mqtt.Dispositivo_APIMQTT;
import dispositivo.api.mqtt.FuncionPublisher_APIMQTT;
import dispositivo.api.rest.Dispositivo_APIREST;
import dispositivo.interfaces.IDispositivo;
import dispositivo.interfaces.IFuncion;
import dispositivo.interfaces.IFuncionPublisher;

public class Dispositivo implements IDispositivo {
	
	protected String deviceId = null;
	protected boolean habilitado = true; // Ejercicio 4
	protected Map<String, IFuncion> functions = null;
	protected Dispositivo_RegistradorMQTT registrador = null;
	protected Dispositivo_APIMQTT apiFuncionesMQTT = null;
	protected Dispositivo_APIREST apiFuncionesREST = null;
	protected IFuncionPublisher publisher = null; // Ejercicio 9
	
	
	public static Dispositivo build(String deviceId, String ip, String mqttBrokerURL) {
		Dispositivo dispositivo = new Dispositivo(deviceId);
		dispositivo.registrador = Dispositivo_RegistradorMQTT.build(deviceId, ip, mqttBrokerURL);
		dispositivo.apiFuncionesMQTT = Dispositivo_APIMQTT.build(dispositivo, mqttBrokerURL);
		dispositivo.apiFuncionesREST = Dispositivo_APIREST.build(dispositivo);
		dispositivo.publisher = new FuncionPublisher_APIMQTT(mqttBrokerURL, deviceId); // Ejercicio 9
		return dispositivo;
	}

	public static Dispositivo build(String deviceId, String ip, int port, String mqttBrokerURL) {
		Dispositivo dispositivo = new Dispositivo(deviceId);
		dispositivo.registrador = Dispositivo_RegistradorMQTT.build(deviceId, ip, mqttBrokerURL);
		dispositivo.apiFuncionesMQTT = Dispositivo_APIMQTT.build(dispositivo, mqttBrokerURL);
		dispositivo.apiFuncionesREST = Dispositivo_APIREST.build(dispositivo, port);
		dispositivo.publisher = new FuncionPublisher_APIMQTT(mqttBrokerURL, deviceId); // Ejercicio 9
		return dispositivo;
	}

	protected Dispositivo(String deviceId) {
		this.deviceId = deviceId;
	}
	
	@Override
	public String getId() {
		return this.deviceId;
	}

	protected Map<String, IFuncion> getFunctions() {
		return this.functions;
	}
	
	protected void setFunctions(Map<String, IFuncion> fs) {
		this.functions = fs;
	}
	
	@Override
	public Collection<IFuncion> getFunciones() {
		if ( this.getFunctions() == null )
			return null;
		return this.getFunctions().values();
	}
	
	
	@Override
	public IDispositivo addFuncion(IFuncion f) {
		if ( this.getFunctions() == null )
			this.setFunctions(new HashMap<String, IFuncion>());
		this.getFunctions().put(f.getId(), f);
		f.setPublisher(this.publisher); // Ejercicio 9
		return this;
	}
	
	
	@Override
	public IFuncion getFuncion(String funcionId) {
		if ( this.getFunctions() == null )
			return null;
		return this.getFunctions().get(funcionId);
	}
	
		
	@Override
	public IDispositivo iniciar() {
		this.publisher.connect(); // Ejercicio 9

		for(IFuncion f : this.getFunciones()) {
			f.iniciar();
		}

		this.registrador.registrar();
		this.apiFuncionesMQTT.iniciar();
		this.apiFuncionesREST.iniciar();
		return this;
	}

	@Override
	public IDispositivo detener() {
		this.registrador.desregistrar();
		this.apiFuncionesMQTT.detener();
		this.apiFuncionesREST.detener();
		this.publisher.disconnect(); // Ejercicio 9
		for(IFuncion f : this.getFunciones()) {
			f.detener();
		}
		return this;
	}

	// TO-DO: Ejercicio 4
	@Override
	public void habilitar() {
		this.habilitado = true;
		for(IFuncion f : this.getFunciones()) {
			f.habilitar();
		}
	}

	@Override
	public void deshabilitar() {
		this.habilitado = false;
		for(IFuncion f : this.getFunciones()) {
			f.deshabilitar();
		}
	}

	@Override
	public boolean estaHabilitado() {
		return this.habilitado;
	}
}