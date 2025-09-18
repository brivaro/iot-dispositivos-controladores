package dispositivo.interfaces;

public interface IFuncionPublisher {
    void connect();
    void disconnect();
    void publish(IFuncion funcion);
}