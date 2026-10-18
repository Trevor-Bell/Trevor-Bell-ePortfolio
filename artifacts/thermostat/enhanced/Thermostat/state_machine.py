#
# state_machine.py
# Core thermostat state machine: off/heat/cool
# states, setpoint control, and the display/reporting logic. Receives
# its sensor, display, serial, and LED dependencies through the
# constructor instead of reaching into module-level globals,
# so it can be tested without real hardware attached.
#

from datetime import datetime
from math import floor
from statemachine import StateMachine, State

import config
from sensor_interface import SensorReadError
from serial_reporter import SerialReportError

##
## ThermostatStateMachine - This is the StateMachine implementation
## class. The purpose of this state machine is to manage the three
## states handled by the thermostat:
##
##  off
##  heat
##  cool
##
class ThermostatStateMachine(StateMachine):
    ##
    ## Define the three states for the machine.
    ##
    ##  off - nothing lit up
    ##  heat - only red LED fading in and out
    ##  cool - only blue LED fading in and out
    ##
    off = State(initial=True)
    heat = State()
    cool = State()

    ##
    ## cycle - event that provides the state machine behavior of
    ## transitioning between the three states of the thermostat
    ##
    cycle = (
        off.to(heat) |
        heat.to(cool) |
        cool.to(off)
    )

    ##
    ## Class initialization method to setup the state machine.
    ##
    def __init__(self, sensor, display, serial_reporter, red_light, blue_light):
        ##
        ## sensor: a SensorInterface instance
        ## display: a DisplayManager instance
        ## serial_reporter: a SerialReporter instance
        ## red_light, blue_light: objects with .on(), .off(), .pulse()
        ## methods (real gpiozero PWMLED, or test doubles)
        ##
        self.sensor = sensor
        self.display = display
        self.serial_reporter = serial_reporter
        self.red_light = red_light
        self.blue_light = blue_light

        ## Default temperature setPoint is set from config.py
        self.set_point = config.DEFAULT_SET_POINT

        ## Continue display output
        self.end_display = False

        ## Most recent successful temperature reading, used as a
        ## fallback if a later sensor read fails.
        self.last_known_fahrenheit = None

        super().__init__()

    ##
    ## on_enter_heat - Action performed when the state machine
    ## transitions into the 'heat' state
    ##
    def on_enter_heat(self):
        self.update_lights()
        if config.DEBUG:
            print("Changing state to heat")

    ##
    ## on_exit_heat - Action performed when the state machine
    ## transitions out of the 'heat' state.
    ##
    def on_exit_heat(self):
        self.update_lights()

    ##
    ## on_enter_cool - Action performed when the state machine
    ## transitions into the 'cool' state
    ##
    def on_enter_cool(self):
        self.update_lights()
        if config.DEBUG:
            print("Changing state to cool")

    ##
    ## on_exit_cool - Action performed when the state machine
    ## transitions out of the 'cool' state.
    ##
    def on_exit_cool(self):
        self.update_lights()

    ##
    ## on_enter_off - Action performed when the state machine
    ## transitions into the 'off' state
    ##
    def on_enter_off(self):
        self.red_light.off()
        self.blue_light.off()
        if config.DEBUG:
            print("Changing state to off")

    ##
    ## process_temp_state_button - Utility method used to send events
    ## to the state machine. This is triggered by the button_pressed
    ## event handler for the first button.
    ##
    def process_temp_state_button(self):
        if config.DEBUG:
            print("Cycling Temperature State")
        self.cycle()

    ##
    ## process_temp_inc_button - Utility method used to update the
    ## setPoint for the temperature. This will increase the setPoint
    ## by a single degree, unless it is already at the configured
    ## maximum. This is triggered by the button_pressed event handler
    ## for the second button.
    ##
    def process_temp_inc_button(self):
        if self.set_point < config.SETPOINT_MAX:
            self.set_point += 1
            if config.DEBUG:
                print(f"Increasing Set Point to {self.set_point}")
        elif config.DEBUG:
            print(f"Set point already at maximum ({config.SETPOINT_MAX})")
        self.update_lights()
    
    ##
    ## process_temp_dec_button - Utility method used to update the
    ## setPoint for the temperature. This will decrease the setPoint
    ## by a single degree, unless it is already at the configured
    ## minimum. This is triggered by the button_pressed event handler
    ## for the third button.
    ##
    def process_temp_dec_button(self):
        if self.set_point > config.SETPOINT_MIN:
            self.set_point -= 1
            if config.DEBUG:
                print(f"Decreasing Set Point to {self.set_point}")
        elif config.DEBUG:
            print(f"Set point already at minimum ({config.SETPOINT_MIN})")
        self.update_lights()

    ##
    ## get_current_fahrenheit - Reads the current temperature through
    ## the injected SensorInterface. On sensor failure, logs the error
    ## and falls back to the last known good reading instead of
    ## letting the exception propagate and crash the caller.
    ##
    def get_current_fahrenheit(self):
        # Reads the current temperature. On sensor failure, logs the error
        # and falls back to the last known good reading instead of crashing.
        try:
            self.last_known_fahrenheit = self.sensor.get_fahrenheit()
        except SensorReadError as e:
            if config.DEBUG:
                print(f"Sensor read failed, using last known value: {e}")
        return self.last_known_fahrenheit

    ##
    ## update_lights - Utility method to update the LED indicators on
    ## the Thermostat
    ##
    def update_lights(self):
        ## Make sure temperatures are comparing in the correct scale
        temp = self.get_current_fahrenheit()
        if temp is None:
            # No reading has ever succeeded yet; nothing to compare against.
            return

        temp = floor(temp)
        self.red_light.off()
        self.blue_light.off()

        ## Verify values for debug purposes
        if config.DEBUG:
            print(f"State: {self.current_state.id}")
            print(f"SetPoint: {self.set_point}")
            print(f"Temp: {temp}")

        # Determine visual identifiers
        if self.current_state.id == "heat":
            if temp < self.set_point:
                self.red_light.pulse()
            else:
                self.red_light.on()
        elif self.current_state.id == "cool":
            if temp > self.set_point:
                self.blue_light.pulse()
            else:
                self.blue_light.on()

    ##
    ## report_status - Sends the current state, temperature, and
    ## setpoint to the TemperatureServer through the injected
    ## SerialReporter. If reporting fails, logs the error instead of
    ## crashing.
    ##
    def report_status(self):
        temp = self.last_known_fahrenheit
        if temp is None:
            return
        try:
            self.serial_reporter.report(self.current_state.id, temp, self.set_point)
        except SerialReportError as e:
            if config.DEBUG:
                print(f"Serial report failed: {e}")

    ##
    ## get_temp_line - Builds the display's second line when showing
    ## the current temperature.
    ##
    def get_temp_line(self):
        temp = self.get_current_fahrenheit()
        if temp is not None:
            return f"Temp: {temp:.1f} F\n"
        return "Sensor error\n"

    ##
    ## get_status_line - Builds the display's second line when showing
    ## the thermostat's state and setpoint.
    ##
    def get_status_line(self):
        return f"{self.current_state.id}: Set {self.set_point} F\n"

    ##
    ## shutdown - Signals the display loop to stop and cleans up the
    ## display's GPIO lines.
    ##
    def shutdown(self):
        self.end_display = True
        self.display.shutdown()

    ## End class ThermostatStateMachine definition