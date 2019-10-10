package net.htb.naturalalarm;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinPwmOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.wiringpi.Gpio;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jonathan Lister
 */
public class Main {

    // State variables to indicate if we are currently executing a sunrise or a sunset
    private static boolean sunrise = false;
    private static boolean sunset = false;
    private boolean stopAdjusting = false;
    private boolean buttonDown = false;

    // create gpio controller
    final GpioController gpio = GpioFactory.getInstance();
    // provision gpio pin #02 as an input pin with its internal pull down resistor enabled
    final GpioPinDigitalInput myButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02,
            PinPullResistance.PULL_DOWN);
    
    
    // Raspberry Pi models A+, B+, 2B, 3B also support hardware PWM pins: GPIO_23, GPIO_24, GPIO_26
    GpioPinPwmOutput redPwm; // = gpio.provisionPwmOutputPin(RaspiPin.GPIO_23);
    GpioPinPwmOutput greenPwm; // = gpio.provisionPwmOutputPin(RaspiPin.GPIO_24);
    GpioPinPwmOutput bluePwm; // = gpio.provisionPwmOutputPin(RaspiPin.GPIO_26);
    
    // changed range to match the soft pwm
    private static final int PWM_RANGE = 200; // 1024;
    
    private static final Logger log = Logger.getLogger("Main");
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        Main m = new Main();
        m.start();
    }
    
    private void start() throws InterruptedException {
        initialiseHardware();
        demoTheLight();
        while (true) {
            // Check if it is time to start the alarm
            if (timeToWakeUp()) {
                startSunrise();
            }
            try {            
                Thread.sleep(1000L);
            } catch (InterruptedException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void initialiseHardware() {        
        // you can optionally use these wiringPi methods to further customize the PWM generator
        // see: http://wiringpi.com/reference/raspberry-pi-specifics
        // mode is mark:space or balanced        
        Gpio.pwmSetMode(Gpio.PWM_MODE_MS);
        // default range is 1024
        Gpio.pwmSetRange(PWM_RANGE);
        // divisor for pwm clock
        Gpio.pwmSetClock(1);        

        // Add a button debounce of 1 second
        myButton.setDebounce(1000);
        // Add a button state change listener        
        myButton.addListener((GpioPinListenerDigital) (GpioPinDigitalStateChangeEvent event) -> {
            log.info("Button event");
            // flip-flop becasue we get two events per button press
            buttonDown = !buttonDown;
            if (buttonDown) {
                log.info("Button down");
                // If we are in a sunrise cycle, interrupt it and fade to black
                if (sunrise) {
                    sunrise = false;
                    stopAdjusting = true;
                    //fadeToBlack();
                }
                // If we are in a sunset cycle, just turn the lights off
                if (sunset) {
                    sunset = false;
                    stopAdjusting = true;
                    black();
                }
                // if we are doing nothing, start a sunset cycle
                if (!sunrise && !sunset) {
                    startSunset();
                }
            }
        });
    }
    
    private boolean timeToWakeUp() {
        boolean retVal = false;
        LocalDateTime now = LocalDateTime.now();
        log.fine("Local date " + now);
        DayOfWeek day = DayOfWeek.of(now.get(ChronoField.DAY_OF_WEEK ));
        int secondOfDay = now.get(ChronoField.SECOND_OF_DAY);
        switch (day) {
            case MONDAY:
            case TUESDAY:
            case WEDNESDAY:
            case THURSDAY:
            case FRIDAY: {
                // 7 am
                int limit = 60 * 60 * 7;
                // 60s window
                if (secondOfDay >= limit  && secondOfDay < limit + 60) {
                    retVal = true;
                }
                break;
            }    
            case SATURDAY:
            case SUNDAY: {
                int limit = 60 * 60 * 9;
                if (secondOfDay >= limit  && secondOfDay < limit + 60) {
                    retVal = true;
                }
                break;
            }                    
        }        
        return retVal;
    }

    
    private void startSunrise() {
        log.info("Start sunrise...");
        sunrise = true;
        sunset = false;
        // gradually bring the sun up
        // by increasing brightness once a second over 30 minutes
        // 30m x 60s -> 1800s
        int steps = 1800;
        float step = (float) PWM_RANGE / (float) steps;
        
        // start with more red and then gradually shift to yellow
        LedControl redControl = ImmutableLedControl
                .builder()
                .start(0)
                .end(PWM_RANGE)
                .step(step)
                .build();
        LedControl greenControl = ImmutableLedControl
                .builder()
                .start(-PWM_RANGE / 4)
                .end(PWM_RANGE)
                .step(step)
                .build();
        // no blue until we are half-way through
        LedControl blueControl = ImmutableLedControl
                .builder()
                .start(-PWM_RANGE / 2)
                .end(PWM_RANGE / 2)
                .step(step)
                .build();
        ChangeRequest changeRequest = ImmutableChangeRequest
                .builder()
                .red(redControl)
                .green(greenControl)
                .blue(blueControl)
                .steps(steps)
                .millisBetweenSteps(1000)
                .build();
        // adjust the light
        adjustTheLight(changeRequest);
        sunrise = false;
        // Turn the light off in case someone forgets ..
        fadeToBlack();
    }

    private void demoTheLight() throws InterruptedException {
        //tryAllPins();
        // two hardware pwm channels
        greenPwm = gpio.provisionPwmOutputPin(RaspiPin.GPIO_23);
        redPwm = gpio.provisionPwmOutputPin(RaspiPin.GPIO_26);
        redPwm.setPwmRange(PWM_RANGE);
        // use software for the third pwm with default value zero
        bluePwm = gpio.provisionSoftPwmOutputPin(RaspiPin.GPIO_27, 0);  
        bluePwm.setPwmRange(PWM_RANGE);
        // the default range for a soft pwn is 100, so adjust to the same range as other channels
        // flashes if you use a large value like 1024...
        bluePwm.setPwmRange(PWM_RANGE);
        
        black();
        Thread.sleep(2000);
        red();
        Thread.sleep(2000);
        green();
        Thread.sleep(2000);
        blue();
        Thread.sleep(2000);
        white();
        Thread.sleep(2000);
        balancedWhite();
        Thread.sleep(2000);
        black();
        Thread.sleep(2000);
        // bring up r then g then b to full brightness over 5 seconds each
        int steps = 50;
        int millisBetweenSteps = 100;
        float step = (float) PWM_RANGE / (float) steps;
        LedControl increasing = ImmutableLedControl
                        .builder()
                        .end(PWM_RANGE)
                        .step(step)
                        .build();
        LedControl decreasing = ImmutableLedControl
                        .builder()
                        .start(PWM_RANGE)
                        .step(step)
                        .build();
        ChangeRequest changeRequest = ImmutableChangeRequest
                .builder()
                .red(increasing)
                .steps(steps)
                .millisBetweenSteps(millisBetweenSteps)
                .build();
        adjustTheLight(changeRequest);     
        changeRequest = ImmutableChangeRequest
                .builder()
                .red(decreasing)
                .steps(steps)
                .millisBetweenSteps(millisBetweenSteps)
                .build();
        adjustTheLight(changeRequest);     
        black();        

        changeRequest = ImmutableChangeRequest
                .builder()
                .green(increasing)
                .steps(steps)
                .millisBetweenSteps(millisBetweenSteps)
                .build();
        adjustTheLight(changeRequest);     
        changeRequest = ImmutableChangeRequest
                .builder()
                .green(decreasing)
                .steps(steps)
                .millisBetweenSteps(millisBetweenSteps)
                .build();
        adjustTheLight(changeRequest);     
        black();        

        changeRequest = ImmutableChangeRequest
                .builder()
                .blue(increasing)
                .steps(steps)
                .millisBetweenSteps(millisBetweenSteps)
                .build();
        adjustTheLight(changeRequest);     
        changeRequest = ImmutableChangeRequest
                .builder()
                .blue(decreasing)
                .steps(steps)
                .millisBetweenSteps(millisBetweenSteps)
                .build();
        adjustTheLight(changeRequest);     
        black();        
        
/*        log.info("Step is " + step);
        LedControl redControl = ImmutableLedControl
                .builder()
                .start(0)
                .end(PWM_RANGE)
                .step(step)
                .build();
        LedControl greenControl = ImmutableLedControl
                .builder()
                .start(-PWM_RANGE / 3)
                .end(PWM_RANGE)
                .step(step * 3)
                .build();
        LedControl blueControl = ImmutableLedControl
                .builder()
                .start((-PWM_RANGE /3) * 2)
                .end(PWM_RANGE)
                .step(step *3 / 3)
                .build();
        ChangeRequest changeRequest = ImmutableChangeRequest
                .builder()
                .red(redControl)
                .green(greenControl)
                .blue(blueControl)
                .steps(steps)
                .millisBetweenSteps(millisBetweenSteps)
                .build();
        // adjust the light
        // TODO pass in a function
        adjustTheLight(changeRequest);     
        black();        
*/
    }
    

    // hmm stateToMonitor might need to be a callback function..
    private void adjustTheLight(ChangeRequest changeRequest) {
        LedControl redControl = changeRequest.getRed();
        LedControl greenControl = changeRequest.getGreen();
        LedControl blueControl = changeRequest.getBlue();
        float redValue = redControl.getStart();
        float greenValue = greenControl.getStart();
        float blueValue = blueControl.getStart();
        stopAdjusting = false;
        for (int step = 0; step < changeRequest.getSteps(); step++) {
            log.info("Step: " + step + " of " + changeRequest.getSteps() + " R: " + redValue + " G: " + greenValue + " B: " + blueValue);
            setLevel(redPwm, redValue);
            setLevel(greenPwm, greenValue);
            setLevel(bluePwm, blueValue);
            redValue = adjustValue(redValue, redControl);
            greenValue = adjustValue(greenValue, greenControl);
            blueValue = adjustValue(blueValue, blueControl);
            try {
                Thread.sleep(changeRequest.getMillisBetweenSteps());
            } catch (InterruptedException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }      
            // check if a button has been pressed
            if (stopAdjusting) {
                break;
            }            
        }                
    }   

    private void setLevel(GpioPinPwmOutput pwmOutput, float level) {
        if (level > 0) {
            pwmOutput.setPwm(Math.round(level));
        } else {
            pwmOutput.setPwm(0);
        }
    }
    
    private float adjustValue(float oldValue, LedControl ledControl) {
        float newValue = oldValue;
        if (ledControl.isBrighteinng()) {
            //log.info("Brightening, old " + oldValue + " increment " + ledControl.getStep());
            newValue = oldValue + ledControl.getStep();
            if (newValue > ledControl.getEnd()) {
                newValue = ledControl.getEnd();
            }
        }
        if (ledControl.isDimming()) {
            //log.info("Dimming, old " + oldValue + " increment " + ledControl.getStep());
            newValue = oldValue - ledControl.getStep();
            if (newValue < ledControl.getEnd()) {
                newValue = ledControl.getEnd();
            }
        }
        return newValue;
    }    
    
    private void startSunset() {
        sunset = true;
        sunrise = false;
        // 5 minutes is 5 * 60 = 300 seconds
        int steps = 300;
        int millisBetweenSteps = 1000;
        int startLevel = PWM_RANGE / 2; // half brightness
        float step = (float) startLevel / (float) steps;
        log.info("Step is " + step);
        LedControl redControl = ImmutableLedControl
                .builder()
                .start(startLevel)
                .end(0)
                .step(step)
                .build();
        LedControl greenControl = ImmutableLedControl
                .builder()
                .start((int) Math.round(startLevel * 0.85))
                .end(0)
                .step(step * 1.5f)
                .build();
        LedControl blueControl = ImmutableLedControl
                .builder()
                .start((int)Math.round(startLevel * 0.5))
                .end(0)
                .step(step * 2.0f)
                .build();
        ChangeRequest changeRequest = ImmutableChangeRequest
                .builder()
                .red(redControl)
                .green(greenControl)
                .blue(blueControl)
                .steps(steps)
                .millisBetweenSteps(millisBetweenSteps)                
                .build();
        // adjust the light
        adjustTheLight(changeRequest);     
        black();                
        sunset = false;
    }

    private void fadeToBlack() {
        // gradually fade to black within a minute or so, starting from current levels
        int steps = 60;
        int millisBetweenSteps = 200;
        int maxLevel = Math.max(redPwm.getPwm(), greenPwm.getPwm());
        maxLevel = Math.max(maxLevel, bluePwm.getPwm());
        float step = (float) maxLevel / (float) steps;
        log.info("Step is " + step);
        LedControl redControl = ImmutableLedControl
                .builder()
                .start(redPwm.getPwm())
                .end(0)
                .step(step)
                .build();
        LedControl greenControl = ImmutableLedControl
                .builder()
                .start(greenPwm.getPwm())
                .end(0)
                .step(step)
                .build();
        LedControl blueControl = ImmutableLedControl
                .builder()
                .start(bluePwm.getPwm())
                .end(0)
                .step(step)
                .build();
        ChangeRequest changeRequest = ImmutableChangeRequest
                .builder()
                .red(redControl)
                .green(greenControl)
                .blue(blueControl)
                .steps(steps)
                .millisBetweenSteps(millisBetweenSteps)
                .build();
        // adjust the light
        adjustTheLight(changeRequest);     
        black();
    }    

    private void black() {
        // quickly fade to black
        log.info("Black");
        redPwm.setPwm(0);
        greenPwm.setPwm(0);
        bluePwm.setPwm(0);
    }    
    
    private void blue() {
        log.info("Blue");
        redPwm.setPwm(0);
        greenPwm.setPwm(0);
        bluePwm.setPwm(PWM_RANGE);        
    }
    
    private void green() {
        log.info("Green");
        redPwm.setPwm(0);
        greenPwm.setPwm(PWM_RANGE);
        bluePwm.setPwm(0);        
    }

    private void red() {
        log.info("Red");
        redPwm.setPwm(PWM_RANGE);
        greenPwm.setPwm(0);
        bluePwm.setPwm(0);        
    }
    
    private void white() {
        log.info("White");
        redPwm.setPwm(PWM_RANGE);
        greenPwm.setPwm(PWM_RANGE);
        bluePwm.setPwm(PWM_RANGE);        
    }

    private void balancedWhite() {
        log.info("Balanced white");
        redPwm.setPwm(PWM_RANGE);
        greenPwm.setPwm(PWM_RANGE);
        bluePwm.setPwm(Math.round(PWM_RANGE * 0.7f));        
    }
    
}
