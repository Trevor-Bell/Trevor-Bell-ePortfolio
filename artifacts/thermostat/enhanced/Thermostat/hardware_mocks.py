#
# hardware_mocks.py
# 
# Fake stand-ins for the Raspberry-Pi-only hardware
# libraries (adafruit_ahtx0, serial, digitalio, gpiozero, etc.), used
# only in unit tests. These let the state machine, sensor, display,
# and serial logic be tested on any machine, without real hardware or
# the Pi-only packages installed.
#

##
## MockSensor - Stands in for the adafruit_ahtx0.AHTx0 sensor object.
##
## Set .temperature / .relative_humidity directly in a test to control
## what a read returns. Set .should_fail = True to simulate a hardware
## read failure and confirm error handling works. temperature and
## relative_humidity are implemented as properties, rather than plain
## attributes, specifically so that setting .should_fail causes the
## next read to raise, matching how a real disconnected sensor
## would fail at the moment of access, not at construction time.
##
class MockSensor:
    ##
    ## Class initialization method to setup the mock sensor with
    ## default readings.
    ##
    def __init__(self, temperature=22.0, relative_humidity=45.0):
        self.temperature = temperature
        self.relative_humidity = relative_humidity
        self.should_fail = False

    ##
    ## temperature - Property getter. Raises a simulated OSError if
    ## should_fail is set; otherwise returns the stored value.
    ##
    @property
    def temperature(self):
        if self.should_fail:
            raise OSError("Simulated sensor read failure")
        return self._temperature

    ##
    ## temperature - Property setter. Lets a test write
    ## mock_sensor.temperature = value exactly as if it were a plain
    ## attribute.
    ##
    @temperature.setter
    def temperature(self, value):
        self._temperature = value

    ##
    ## relative_humidity - Property getter. Same should_fail behavior
    ## as temperature, above.
    ##
    @property
    def relative_humidity(self):
        if self.should_fail:
            raise OSError("Simulated sensor read failure")
        return self._relative_humidity

    ##
    ## relative_humidity - Property setter.
    ##
    @relative_humidity.setter
    def relative_humidity(self, value):
        self._relative_humidity = value

    ## End class MockSensor definition

##
## MockSerial - Stands in for a pyserial Serial object.
##
## Every write() call is recorded in .writes so a test can assert on
## exactly what was sent. Set .should_fail = True to simulate a
## dropped serial connection.
##
class MockSerial:
    ##
    ## Class initialization method to setup the mock connection.
    ##
    def __init__(self):
        self.writes = []
        self.should_fail = False

    ##
    ## write - Records the data that would have been sent, unless
    ## should_fail is set, in which case it raises a simulated OSError
    ## instead.
    ##
    def write(self, data):
        if self.should_fail:
            raise OSError("Simulated serial write failure")
        self.writes.append(data)

    ## End class MockSerial definition

##
## MockDisplay - Stands in for the ManagedDisplay class itself (not
## the low-level LCD library) so DisplayManager's callers can be
## tested without a real LCD. Records the last message shown in
## .last_message.
##
class MockDisplay:
    ##
    ## Class initialization method to setup the mock display.
    ##
    def __init__(self):
        self.last_message = None
        self.cleared = False
    
    ##
    ## updateScreen - Records the message that would have been shown
    ##
    def updateScreen(self, message):
        self.last_message = message

    ##
    ## clear - Records that a clear was requested.
    ##
    def clear(self):
        self.cleared = True

    ##
    ## cleanupDisplay - Records that cleanup was requested.
    ##
    def cleanupDisplay(self):
        self.cleared = True

    ## End class MockDisplay definition

##
## MockLED - Stands in for a gpiozero PWMLED. Records which method was
## called last (.on(), .off(), or .pulse()) so a test can assert on
## LED behavior without real hardware.
##
class MockLED:
    ##
    ## Class initialization method to setup the mock LED with no
    ## state set yet.
    ##
    def __init__(self):
        self.state = None  # will be 'on', 'off', or 'pulse'

    ##
    ## on - Records that the LED was turned on solid.
    ##
    def on(self):
        self.state = 'on'

    ##
    ## off - Records that the LED was turned off.
    ##
    def off(self):
        self.state = 'off'

    ##
    ## pulse - Records that the LED was set to pulse (fade in and out).
    ##
    def pulse(self):
        self.state = 'pulse'

    ## End class MockLED definition
