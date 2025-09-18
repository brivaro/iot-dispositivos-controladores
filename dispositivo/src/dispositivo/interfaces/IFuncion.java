package dispositivo.interfaces;

public interface IFuncion {
	
	public String getId();
	
	public IFuncion iniciar();
	public IFuncion detener();
	
	public IFuncion encender();
	public IFuncion apagar();
	public IFuncion parpadear();
	
	public FuncionStatus getStatus();

	// TO-DO: Ejercicio 4
	public void habilitar();
	public void deshabilitar();
	public boolean estaHabilitada();
	
	// Ejercicio 9
	public void setPublisher(IFuncionPublisher publisher);

}