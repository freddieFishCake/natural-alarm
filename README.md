# natural-alarm
Java sunrise simulator LED controller using Raspberry Pi and ANAVI Light Controller with MQTT

![](https://github.com/freddieFishCake/natural-alarm/blob/master/IMG_8302_small.jpg)

This project requires specific hardware including any Raspberry Pi, including the Pi Zero, RGB LED strips, a suitable power supply and this MQTT based LED controller:  https://www.crowdsupply.com/anavi-technology/light-controller.

The project requires Java 8 or above.

After building the project run it with a command line such as:
```
java -jar target/natural-alarm-mqtt-1.0-SNAPSHOT-jar-with-dependencies --broker tcp://localhost:1883 --topic 1cfcc0e01714d467a44f4bd6b13afe4f/action/rgbled --sunriseInSeconds 1800 --pauseInSeconds 1800
```
The only mandatory parameter is --topic, all others default to the values shown above.

An example of how the hardware could be assembled using an Ikea Holmo floor standing lamp (https://www.ikea.com/gb/en/search/products/?q=holmo).

![Wrap the self-adhesive RGB LED strips around a suitable tube](https://github.com/freddieFishCake/natural-alarm/blob/master/IMG_8298_small.jpg)

![](https://github.com/freddieFishCake/natural-alarm/blob/master/IMG_8305_small.jpg)

![](https://github.com/freddieFishCake/natural-alarm/blob/master/IMG_8306_small.jpg)

![](https://github.com/freddieFishCake/natural-alarm/blob/master/IMG_8307_small.jpg)
