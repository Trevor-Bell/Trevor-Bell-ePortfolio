#
# main.py
# 
# Application entry point. Constructs the SensorInterface,
# DisplayManager, SerialReporter, and ThermostatStateMachine, wires up
# the physical buttons, and owns the timing loop that drives the
# display and periodic serial reporting.
#

from datetime import datetime
from threading import Thread
from time import sleep

from gpiozero import Button, PWMLED

import config
from sensor_interface import SensorInterface
from display_manager import DisplayManager
from serial_reporter import SerialReporter, SerialReportError
from state_machine import ThermostatStateMachine

##
## How often (in seconds) the display alternates between showing the
## temperature and showing the thermostat's state/setpoint.
##
DISPLAY_ALTERNATE_SECONDS = 5

##
## How often (in seconds) update_lights() is refreshed even without a
## button press, so a changing temperature is reflected promptly.
##
LIGHT_REFRESH_SECONDS = 10

##
## build_serial_reporter - Attempts to open the real serial
## connection. If it fails, prints a clear startup message and
## returns None so the program can continue running without serial
## reporting, instead of crashing the whole program.
##
def build_serial_reporter():
    try:
        return SerialReporter()
    except SerialReportError as e:
        print(f"Warning: serial reporting unavailable ({e}). Continuing without it.")
        return None

##
## display_loop - Runs on a background thread. Alternates the display
## between the current temperature and the thermostat's status,
## refreshes the lights periodically, and sends a serial report
## periodically.
##
def display_loop(tsm):
    seconds_elapsed = 0

    while not tsm.end_display:
        ## Setup line 1
        line1 = datetime.now().strftime("%m/%d/%Y %H:%M") + "\n"

        ## Setup line 2 - alternate between temperature and status
        show_status = (seconds_elapsed // DISPLAY_ALTERNATE_SECONDS) % 2 == 1
        line2 = tsm.get_status_line() if show_status else tsm.get_temp_line()

        ## Update Display
        tsm.display.show(line1, line2)

        ## Run the routine to update the lights periodically so
        ## operations stay smooth even without a button press
        if seconds_elapsed % LIGHT_REFRESH_SECONDS == 0:
            tsm.update_lights()

        ## Update server periodically, if serial reporting is available
        if tsm.serial_reporter is not None and seconds_elapsed % config.SERIAL_REPORT_INTERVAL == 0:
            tsm.report_status()

        seconds_elapsed += 1
        sleep(1)

##
## main - Constructs all the hardware-facing objects, wires up the
## buttons, starts the display loop on its own thread, and runs the
## main loop until a keyboard interrupt is received.
##
def main():
    ## Initialize the sensor, display, and serial reporter
    sensor = SensorInterface()
    display = DisplayManager()
    serial_reporter = build_serial_reporter()

    ## The two LEDs, utilizing the pins configured in config.py
    red_light = PWMLED(config.RED_LED_PIN)
    blue_light = PWMLED(config.BLUE_LED_PIN)

    ## Setup the State Machine
    tsm = ThermostatStateMachine(sensor, display, serial_reporter, red_light, blue_light)

    ## Start the display management functionality in a separate
    ## execution thread
    thread = Thread(target=display_loop, args=(tsm,))
    thread.start()

    ## Configure the green button to cycle the thermostat when pressed.
    green_button = Button(config.GREEN_BUTTON_PIN)
    green_button.when_pressed = tsm.process_temp_state_button

    ## Configure the red button to increase the setpoint by a degree.
    red_button = Button(config.RED_BUTTON_PIN)
    red_button.when_pressed = tsm.process_temp_inc_button

    ## Configure the blue button to decrease the setpoint by a degree.
    blue_button = Button(config.BLUE_BUTTON_PIN)
    blue_button.when_pressed = tsm.process_temp_dec_button

    ## Setup loop variable
    repeat = True

    ## Repeat until the user creates a keyboard interrupt (CTRL-C)
    while repeat:
        try:
            sleep(30)
        except KeyboardInterrupt:
            ## Catch the keyboard interrupt (CTRL-C) and exit cleanly.
            ## No need to manually clean up the GPIO pins, the
            ## gpiozero library handles that process.
            print("Cleaning up. Exiting...")
            repeat = False
            tsm.shutdown()
            sleep(1)

if __name__ == "__main__":
    main()
