# natural-alarm
Java sunrise simulator LED controller using Raspberry Pi and ANAVI Light Controller with MQTT

![](https://github.com/freddieFishCake/resources/blob/master/IMG_8302_small.jpg)

This project requires specific hardware including any Raspberry Pi, including the Pi Zero, RGB LED strips, a suitable power supply and this MQTT based LED controller:  https://www.crowdsupply.com/anavi-technology/light-controller.

The project requires Java 8 or above.

After building the project run it with a command line such as:
```
java -jar target/natural-alarm-mqtt-1.0-SNAPSHOT-jar-with-dependencies --broker tcp://localhost:1883 --topic 1cfcc0e01714d467a44f4bd6b13afe4f/action/rgbled --sunriseInSeconds 1800 --pauseInSeconds 1800
```
The only mandatory parameter is --topic, all others default to the values shown above.

An example of how the hardware could be assembled using an Ikea Holmo floor standing lamp (https://www.ikea.com/gb/en/search/products/?q=holmo). Wrap the self-adhesive RGB LED strips around a suitable tube that fits inside the Holmo lamp.

![Wrap the self-adhesive RGB LED strips around a suitable tube](https://github.com/freddieFishCake/resources/blob/master/IMG_8298_small.jpg)

![Sunrise part #1](https://github.com/freddieFishCake/resources/blob/master/IMG_8307_small.jpg)

![Sunrise part #2](https://github.com/freddieFishCake/resources/blob/master/IMG_8306_small.jpg)

![Sunrise part #4](https://github.com/freddieFishCake/resources/blob/master/IMG_8305_small.jpg)

Example setup steps

Hardware: raspberry pi zero with wifi and header, plus anavid led pHat

Software: Raspbian Stretch light - prepare the SD card and add empty ssh file and populated wpa-supplicant.conf to root directory.

Then, adapted from: https://github.com/AnaviTechnology/anavid

First boot, connect via ssh and use sudo-raspi-config to set
hostname: rpi-zero-natural-alarm-1
enable IC2

```
sudo apt-get update
sudo apt-get upgrade -y
```

Install mosquitto MQTT & clients
```
sudo apt-get install mosquitto  mosquitto-clients
```
Configure mosquitto, 
allow_anonymous true

```
sudo systemctl enable mosquitto.service
sudo systemctl start mosquitto.service
```

Test mosquitto using the clients:

Install wiringpi, piGPIO and OpenSSL:
```
sudo apt-get install -y wiringpi pigpio libssl-dev
```

Install git:
```
sudo apt-get install git
```

Install Paho (library for MQTT clients):
```
cd ~
git clone https://github.com/eclipse/paho.mqtt.c.git
cd paho.mqtt.c
make
sudo make install
```
The make failed, see https://github.com/eclipse/paho.mqtt.c/issues/555]
so,
```
sudo apt-get install doxygen
make html
```
then cleaned up previously broken installation
```
sudo rm /usr/local/lib/libpaho*
```

and then
```
sudo make install
```
ran ok

Build and install anavid:
```
cd ~
git clone https://github.com/AnaviTechnology/anavid.git
cd anavid
make
sudo make install
```

Retrieve device ID:
```
anavid -m
->
Device ID: 5fbbaaa9bbbd4ad2409a09966145df57
```

Troubleshooting:
found this
```
tail /var/log/syslog
Oct 16 17:04:45 rpi-zero-natural-alarm-1 anavid[583]: Device ID: 5fbbaaa9bbbd4ad2409a09966145df57
Oct 16 17:04:45 rpi-zero-natural-alarm-1 anavid[583]: ERROR: Cannot open '/etc/anavilight.ini'. Loading default configrations...
Oct 16 17:04:45 rpi-zero-natural-alarm-1 anavid[583]: ===CONFIGURATIONS===
Oct 16 17:04:45 rpi-zero-natural-alarm-1 anavid[583]: MQTT address: tcp://hassbian.local:1883
Oct 16 17:04:45 rpi-zero-natural-alarm-1 anavid[583]: MQTT client ID: ANAVI-Light-pHAT
Oct 16 17:04:45 rpi-zero-natural-alarm-1 anavid[583]: ====================
Oct 16 17:04:45 rpi-zero-natural-alarm-1 anavid[583]: ERROR -1: Failed to connect to MQTT broker.
Oct 16 17:04:45 rpi-zero-natural-alarm-1 systemd[1]: anavi.service: Main process exited, code=exited, status=1/FAILURE
Oct 16 17:04:45 rpi-zero-natural-alarm-1 systemd[1]: anavi.service: Unit entered failed state.
Oct 16 17:04:45 rpi-zero-natural-alarm-1 systemd[1]: anavi.service: Failed with result 'exit-code'.
```
so we need to change the address for MQTT. Found an example here https://github.com/AnaviTechnology/anavid/blob/master/config/anavilightphat.ini

[mqtt]
address=tcp://hassbian.local:1883
clientId=ANAVI-Light-pHAT
username=
password=

and copied to /etc/anavilight.ini

then stop and restart the anavi daemon
```
sudo systemctl stop anavi
sudo systemctl start anavi
```
this time we got:
Oct 16 17:17:59 rpi-zero-natural-alarm-1 anavid[821]: Unable to open I2C device: No such file or directory

ah, haven't enabled IC2 in raspi-config!

Next, install java to the pi
```
sudo apt-get install openjdk-8-jre-headless 
```
create a file natural-alarm.sh containing the topic / deviceId we obtained from the anavid -m command:

```
# run natural-alarm-mqtt on localhost
java -jar /home/pi/natural-alarm-mqtt.jar -b tcp://localhost:1883 -t 5fbbaaa9bbbd4ad2409a09966145df57/action/rgbled
```

```
chmod +x natural-alarm.sh
```
Then use e.g. crontab to run natural-alarm.sh each day at the desired time, different at weekends, for example to start at 07:00 on weekdays and 09:00 at the weekend use crontab settings like:
```
# m h  dom mon dow   command
0 7 * * 1-5 /home/pi/natural-alarm.sh
0 9 * * 6-7 /home/pi/natural-alarm.sh
```

