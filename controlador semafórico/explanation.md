### Cómo ejecutar todo para que funcione

Necesitas tener **múltiples terminales abiertas**. Cada terminal ejecutará un programa independiente. En tu caso, necesitas **3 terminales**: una para cada dispositivo (2 dispositivos + 1 controlador) y una para el controlador.

**Paso 1: Abre 4 terminales para los Dispositivos**

Debes ejecutar el programa `dispositivo.iniciador.DispositivoIniciador` que creamos en los primeros ejercicios. **¡Necesitas una instancia corriendo para cada dispositivo!**

*   **Terminal 1 (Semaforo Dispositivo 1):**
    ```bash
    java ... dispositivo.iniciador.DispositivoIniciador ttmi050 ttmi050.iot.upv.es 8182 tcp://tambori.dsic.upv.es:10083
    ```

*   **Terminal 2 (Semaforo Dispositivo 2):**
    ```bash
    java ... dispositivo.iniciador.DispositivoIniciador ttmi051 ttmi051.iot.upv.es 8183 tcp://tambori.dsic.upv.es:10083
    ```
    *(Nota: he cambiado el puerto REST a 8183 para evitar conflictos si los ejecutas en la misma máquina)*

**Paso 2: Abre 1 terminal para el Controlador**

Ahora, en una **terminal**, ejecuta el programa del controlador que has creado.

*  **Terminal 3 (Controlador):**
    ```bash
    java ... dispositivo.ControladorSemaforico tcp://tambori.dsic.upv.es:10083 ttmi050 ttmi051
    ```

**Paso 3: ¡Prueba la Interfaz!**

1.  Abre tu navegador en `http://localhost:8089`.
2.  Deberías ver el dashboard con los semáforos.
3.  **Haz clic en el botón "Set ON"**.