#!/bin/bash

sudo java -jar dispositivo-pi.jar $(hostname) $(hostname).iot.upv.es 8182 tcp://tambori.dsic.upv.es:10083

#sudo java -jar dispositivo-pi.jar $(hostname) $(hostname).iot.upv.es 8182 tcp://ttmi008.iot.upv.es:1883