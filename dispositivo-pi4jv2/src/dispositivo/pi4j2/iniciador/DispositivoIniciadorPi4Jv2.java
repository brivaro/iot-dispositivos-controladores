package dispositivo.pi4j2.iniciador;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;

import dispositivo.componentes.Dispositivo;
import dispositivo.componentes.pi4j2.FuncionPi4Jv2;
import dispositivo.interfaces.FuncionStatus;
import dispositivo.interfaces.IDispositivo;

public class DispositivoIniciadorPi4Jv2 {

    public static void main(String[] args) {

        if (args.length < 4) {
            System.out.println("Usage: java -jar dispositivo.jar device deviceIP rest-port mqttBroker");
            System.out.println("Example: java -jar dispositivo.jar ttmi058 ttmi058.iot.upv.es 8182 tcp://tambori.dsic.upv.es:10083");
            return;
        }

        String deviceId = args[0];
        String deviceIP = args[1];
        String port = args[2];
        String mqttBroker = args[3];

        // Configuramos el contexto/plataforma del GPIO de la Raspberry
        Context pi4jContext = Pi4J.newAutoContext();

        // 2. Añadimos un "shutdown hook" para apagar Pi4J de forma segura
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Apagando el contexto Pi4J...");
            pi4jContext.shutdown();
            System.out.println("Contexto Pi4J apagado.");
        }));

        IDispositivo d = Dispositivo.build(deviceId, deviceIP, Integer.valueOf(port), mqttBroker);

        // Añadimos funciones al dispositivo, mapeando a los pines BCM correctos
        // f1 (rojo) -> GPIO_17
        FuncionPi4Jv2 f1 = FuncionPi4Jv2.build("f1", 17, FuncionStatus.OFF, pi4jContext);
        d.addFuncion(f1);
        
        // f2 (amarillo) -> GPIO_27
        FuncionPi4Jv2 f2 = FuncionPi4Jv2.build("f2", 27, FuncionStatus.OFF, pi4jContext);
        d.addFuncion(f2);
        
        // 1. AÑADIR LA FUNCIÓN F3
        // f3 (verde) -> GPIO_22
        FuncionPi4Jv2 f3 = FuncionPi4Jv2.build("f3", 22, FuncionStatus.BLINK, pi4jContext);
        d.addFuncion(f3);

        // Arrancamos el dispositivo
        System.out.println("Iniciando dispositivo físico...");
        d.iniciar();
        
        System.out.println("Dispositivo físico iniciado. Presiona Ctrl+C para salir.");
    }
}