# natural-alarm
Java sunrise simulator LED controller using Raspberry Pi and ANAVI Light Controller with MQTT

This project requires specific hardware including any Raspberry Pi, including the Pi Zero, RGB LED strips, a suitable power supply and this MQTT based LED controller:  https://www.crowdsupply.com/anavi-technology/light-controller.

The project requires Java 8 or above.

After building the project run it with a command line such as:
java -jar target/natural-alarm-mqtt-1.0-SNAPSHOT-jar-with-dependencies --broker tcp://localhost:1883 --topic 1cfcc0e01714d467a44f4bd6b13afe4f/action/rgbled --sunriseInSeconds 1800 --pauseInSeconds 1800
The only mandatory parameter is --topic, all other default to the values shown above.

