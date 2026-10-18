# Enhancement One: Software Design and Engineering

[Home](index.html) | [Code Review](code-review.html) | [Software Engineering](software-engineering.html) | [Algorithms](algorithms.html) | [Databases](databases.html)

## The artifact

A thermostat application originally built for **CS 350: Emerging Systems Architectures and Technologies**, running on a Raspberry Pi. The original `Thermostat.py`, created in Module Seven of that course, implements a three-state (off/heat/cool) thermostat using a state machine, an AHT20 temperature and humidity sensor, a 16x2 LCD display, three physical buttons, two status LEDs, and a serial connection that reports status to a separate temperature server every 30 seconds.

## Original vs. enhanced

- [Original code: Thermostat.py](artifacts/thermostat/original/Thermostat.py)
- [Enhanced code: modular version](artifacts/thermostat/enhanced/Thermostat)

| | Original | Enhanced |
|---|---|---|
| Structure | Single file with no separation between hardware access, display logic, and state management | `config.py`, `sensor_interface.py`, `display_manager.py`, `serial_reporter.py`, `state_machine.py`, and `main.py` |
| Dependencies | `TemperatureMachine` reached into global variables | Dependencies passed through the constructor (dependency injection) |
| Configuration | GPIO pins and serial settings hardcoded inline | Centralized in `config.py` |
| Error handling | None on sensor reads or serial writes; a serial failure crashed the program at startup | Sensor and serial classes catch hardware failures and fall back to a last-known-good value or degrade instead of crashing |
| Testing | None | `hardware_mocks.py` and a nine-test unit suite covering state transitions, setpoint bounds checking, and sensor-failure handling, runnable without a Raspberry Pi |

## Key code

```python
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
```

```python
##
## SensorInterface - Class intended to manage reads from the AHT20
## temperature/humidity sensor.
##
class SensorInterface:
    ##
    ## Class initialization method to setup the sensor.
    ##
    def __init__(self, sensor=None):
        ##
        ## sensor: an object with .temperature and .relative_humidity
        ## attributes (real adafruit_ahtx0.AHTx0, or a MockSensor in
        ## tests). If not provided, sets up the real I2C sensor.
        ##
        if sensor is not None:
            self.sensor = sensor
        else:
            ##
            ## board and adafruit_ahtx0 are only imported here, inside
            ## the else branch, rather than at the top of the file.
            ## Both are Raspberry-Pi-only libraries; importing them
            ## here means this file can still be imported (and
            ## SensorInterface tested with a mock) on a machine that
            ## doesn't have them installed.
            ##
            import board
            import adafruit_ahtx0
            i2c = board.I2C()
            self.sensor = adafruit_ahtx0.AHTx0(i2c)

    ##
    ## get_fahrenheit - Get the temperature in Fahrenheit. Wrapped in
    ## a try/except so a hardware read failure raises
    ## SensorReadError instead of an unhandled exception.
    ##
    def get_fahrenheit(self):
        try:
            celsius = self.sensor.temperature
            return (celsius * 9 / 5) + 32
        except Exception as e:
            raise SensorReadError(f"Failed to read temperature: {e}")

    ##
    ## get_celsius - Get the temperature in Celsius
    ##
    def get_celsius(self):
        try:
            return self.sensor.temperature
        except Exception as e:
            raise SensorReadError(f"Failed to read temperature: {e}")

    ##
    ## get_humidity - Get the Relative Humidity
    ##
    def get_humidity(self):
        try:
            return self.sensor.relative_humidity
        except Exception as e:
            raise SensorReadError(f"Failed to read humidity: {e}")

    ## End class SensorInterface definition
```

## Narrative

### Why this artifact

I selected this artifact for the Software Design and Engineering category because, while it worked correctly, its original structure had clear, fixable weaknesses that gave me an opportunity to demonstrate software engineering rather than superficial cleanup. The code review of the original code identified several concrete issues: the entire application lived in a single file with no separation between hardware access, display logic, and state management; hardware configuration (GPIO pins, serial settings) was hardcoded inline; and, most importantly, both the sensor read and the serial write calls had no error handling at all. A failure in either would silently crash a background thread or, in the serial connection's case, crash the entire program before it even started.

To address these issues, I refactored the single file into focused modules. `config.py` centralizes all hardware and behavioral configuration. `sensor_interface.py`, `display_manager.py`, and `serial_reporter.py` each wrap one piece of hardware behind a small interface with proper error handling. `state_machine.py` contains the core off/heat/cool logic, now receiving its dependencies through its constructor instead of reaching into global variables. `main.py` wires everything together and owns the timing loop. I also added `hardware_mocks.py` and a nine-test unit test suite covering state transitions, setpoint bounds checking, and sensor-failure handling.

This work showcases object-oriented decomposition, dependency injection as a testability pattern, and defensive programming. The sensor and serial classes now catch hardware failures and either fall back to a last-known-good value or degrade rather than crashing, which I verified by running the refactored code on my actual Raspberry Pi and confirming it started and ran identically to the original.

### What I learned and the challenges I faced

The most instructive challenges were a handful of small bugs that turned into useful lessons about cross-platform development.

- **A hard-to-trace import error.** A leftover trailing period from an early comment-to-code edit caused a `SyntaxError` in `display_manager.py`. Visual Studio's Test Explorer reported "no tests found" without surfacing the underlying import error. Running pytest's collection step directly from the command line is what revealed the real cause.
- **A case-sensitivity trap.** My `config.py` file had been saved with a capital "C" despite every import statement referencing it in lowercase. This worked on my Windows machine, since Windows' filesystem is case-insensitive, but would have failed immediately on a case-sensitive filesystem like Linux, which is a real risk for a Raspberry Pi project.
- **Test discovery differences.** pytest and unittest's discovery mechanisms aren't identical. `unittest discover` required a `tests/__init__.py` file to recognize my tests folder as a package, while pytest didn't need one, and my tests weren't visible in Visual Studio's Test Explorer until I understood that distinction.

On the hardware side, once I moved the refactored code onto the Pi, I first re-validated the original `Thermostat.py` to establish a working baseline before testing my changes. In doing so I found and fixed an unrelated wiring issue: the green button's ground connection was one breadboard row off. With that resolved, I deployed the refactored version and confirmed it ran on real hardware with identical behavior to the original: same display output, same button responses, same LED behavior, and a clean serial connection with no errors.

### Outcomes met

- [x] **Outcome 3:** Design and evaluate computing solutions while managing trade-offs. This shows up in the trade-off I made around scope. In my code review, I didn't flag the LED objects as their own defensive-programming concern, so I deliberately left them as plain constructor arguments to `ThermostatStateMachine` rather than wrapping them in their own interface.
- [x] **Outcome 4:** Use well-founded and innovative techniques, skills, and tools. This is demonstrated most directly by the unit test suite: building `hardware_mocks.py` and structuring every hardware-facing class around dependency injection produced a test suite that runs and passes without any Raspberry Pi attached.

[Download the narrative (.docx)](narratives/CS_499_Milestone_Two.docx)
