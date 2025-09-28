#!/bin/bash

if [ "$#" -ne 4 ]; then
  echo "Error: Se requieren 4 argumentos."
  echo "Uso: $0 <ID_DISPOSITIVO> <IP_DISPOSITIVO> <PUERTO_REST> <BROKER_MQTT>"
  exit 1
fi

DEVICE_ID=$1
DEVICE_IP=$2
REST_PORT=$3
MQTT_BROKER=$4

echo "Lanzando dispositivo IoT con la siguiente configuración:"
echo " - ID: $DEVICE_ID"
echo " - IP: $DEVICE_IP"
echo " - Puerto: $REST_PORT"
echo " - Broker: $MQTT_BROKER"
echo " - Para detener, presiona Ctrl+C"

# Ejecutamos el JAR con sudo para tener acceso a los pines GPIO
sudo java -jar dispositivo-pi.jar "$DEVICE_ID" "$DEVICE_IP" "$REST_PORT" "$MQTT_BROKER"