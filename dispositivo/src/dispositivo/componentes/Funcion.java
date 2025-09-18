package dispositivo.componentes;

import dispositivo.interfaces.FuncionStatus;
import dispositivo.interfaces.IFuncion;
import dispositivo.interfaces.IFuncionPublisher;
import dispositivo.utils.MySimpleLogger;

public class Funcion implements IFuncion {
	
	protected String id = null;
	protected boolean habilitada = true; // Ejercicio 4
	protected FuncionStatus initialStatus = null;
	protected FuncionStatus status = null;
	protected IFuncionPublisher publisher = null; // Ejercicio 9
	
	private String loggerId = null;
	
	public static Funcion build(String id) {
		return new Funcion(id, FuncionStatus.OFF);
	}
	
	public static Funcion build(String id, FuncionStatus initialStatus) {
		return new Funcion(id, initialStatus);
	}

	protected Funcion(String id, FuncionStatus initialStatus) {
		this.id = id;
		this.initialStatus = initialStatus;
		this.loggerId = "Funcion " + id;
	}
	
	
	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public void setPublisher(IFuncionPublisher publisher) {
		this.publisher = publisher;
	}
		
	@Override
	public IFuncion encender() {
		if ( !this.estaHabilitada() ) {
			MySimpleLogger.warn(this.loggerId, "Funcion deshabilitada, no se puede modificar");
			return this;
		}
		MySimpleLogger.info(this.loggerId, "==> Encender");
		this.setStatus(FuncionStatus.ON);
		this.publishStatusChange(); // Ejercicio 9
		return this;
	}

	@Override
	public IFuncion apagar() {
		if ( !this.estaHabilitada() ) {
			MySimpleLogger.warn(this.loggerId, "Funcion deshabilitada, no se puede modificar");
			return this;
		}
		MySimpleLogger.info(this.loggerId, "==> Apagar");
		this.setStatus(FuncionStatus.OFF);
		this.publishStatusChange(); // Ejercicio 9
		return this;
	}

	@Override
	public IFuncion parpadear() {
		if ( !this.estaHabilitada() ) {
			MySimpleLogger.warn(this.loggerId, "Funcion deshabilitada, no se puede modificar");
			return this;
		}
		MySimpleLogger.info(this.loggerId, "==> Parpadear");
		this.setStatus(FuncionStatus.BLINK);
		this.publishStatusChange(); // Ejercicio 9
		return this;
	}
	
	protected IFuncion _putIntoInitialStatus() {
		switch (this.initialStatus) {
		case ON:
			this.encender();
			break;
		case OFF:
			this.apagar();
			break;
		case BLINK:
			this.parpadear();
			break;

		default:
			break;
		}
		
		return this;

	}

	@Override
	public FuncionStatus getStatus() {
		return this.status;
	}
	
	protected IFuncion setStatus(FuncionStatus status) {
		this.status = status;
		return this;
	}
	
	@Override
	public IFuncion iniciar() {
		this._putIntoInitialStatus();
		return this;
	}
	
	@Override
	public IFuncion detener() {
		return this;
	}
	
	// TO-DO: Ejercicio 4
	@Override
	public boolean estaHabilitada() {
		return this.habilitada;
	}
	
	@Override
	public void habilitar() {
		this.habilitada = true;
	}

	@Override
	public void deshabilitar() {
		this.habilitada = false;
	}

	protected void publishStatusChange() {
		// TO-DO: Ejercicio 9 - Implementar notificaciones 'push'
		if (this.publisher != null) {
			MySimpleLogger.info(this.loggerId, "Notificando cambio estado de función " + this.id);
			this.publisher.publish(this);
		}
	}
}