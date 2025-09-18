### Cómo ejecutar todo para que funcione (La Solución)

Necesitas tener **múltiples terminales abiertas**. Cada terminal ejecutará un programa independiente. En tu caso, necesitas **5 terminales**: una para cada dispositivo (1 maestro + 3 esclavos) y una para el controlador.

**Paso 1: Abre 4 terminales para los Dispositivos**

Debes ejecutar el programa `dispositivo.iniciador.DispositivoIniciador` que creamos en los primeros ejercicios. **¡Necesitas una instancia corriendo para cada dispositivo!**

*   **Terminal 1 (Maestro):**
    ```bash
    java ... dispositivo.iniciador.DispositivoIniciador ttmi050 ttmi050.iot.upv.es 8182 tcp://tambori.dsic.upv.es:10083
    ```

*   **Terminal 2 (Esclavo 1):**
    ```bash
    java ... dispositivo.iniciador.DispositivoIniciador ttmi051 ttmi051.iot.upv.es 8183 tcp://tambori.dsic.upv.es:10083
    ```
    *(Nota: he cambiado el puerto REST a 8183 para evitar conflictos si los ejecutas en la misma máquina)*

*   **Terminal 3 (Esclavo 2):**
    ```bash
    java ... dispositivo.iniciador.DispositivoIniciador ttmi052 ttmi052.iot.upv.es 8184 tcp://tambori.dsic.upv.es:10083
    ```

*   **Terminal 4 (Esclavo 3):**
    ```bash
    java ... dispositivo.iniciador.DispositivoIniciador ttmi053 ttmi053.iot.upv.es 8185 tcp://tambori.dsic.upv.es:10083
    ```

**Paso 2: Abre 1 terminal para el Controlador**

Ahora, en una **quinta terminal**, ejecuta el programa del controlador que has creado.

*   **Terminal 5 (Controlador):**
    ```bash
    java ... dispositivo.ControladorMaestroEsclavo tcp://tambori.dsic.upv.es:10083 ttmi050 ttmi051 ttmi052 ttmi053
    ```

**Paso 3: ¡Prueba la Interfaz!**

1.  Abre tu navegador en `http://localhost:8089`.
2.  Deberías ver el dashboard. Inicialmente, todos los dispositivos estarán `OFF` porque es su estado por defecto al arrancar.
3.  **Haz clic en el botón "Set ON"**.

**Ahora ocurrirá la magia:**
1.  Tu navegador enviará la orden al `Controlador`.
2.  Verás en la **Terminal 5 (Controlador)** un log que dice `Enviando comando a 'ttmi050'...`.
3.  Verás en la **Terminal 1 (Maestro)** que ha recibido el comando, cambia a `ON` y publica su nuevo estado.
4.  Verás de nuevo en la **Terminal 5 (Controlador)** que ha recibido el estado `ON` del maestro y ahora está enviando comandos a los esclavos.
5.  Verás en las **Terminales 2, 3 y 4 (Esclavos)** que reciben el comando y cambian su estado a `ON`.
6.  En la interfaz web, verás que todas las luces se actualizan a verde (`ON`).
