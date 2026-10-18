#
# config.py 
# 
# Centralized hardware and application configuration for
# the thermostat.
#

##
## LCD display pin configuration
##
## board is only importable on the Raspberry Pi (or another Blinka-
## supported board), so this is wrapped in a try/except.
##
try:
	import board
	LCD_RS_PIN = board.D17
	LCD_EN_PIN = board.D27
	LCD_D4_PIN = board.D5
	LCD_D5_PIN = board.D6
	LCD_D6_PIN = board.D13
	LCD_D7_PIN = board.D26
except ImportError:
	# board is only available on the Pi; these constants are unused
    # when running on a dev machine without it.
    LCD_RS_PIN = LCD_EN_PIN = LCD_D4_PIN = LCD_D5_PIN = LCD_D6_PIN = LCD_D7_PIN = None

## Modify this if for a different sized character LCD
LCD_COLUMNS = 16
LCD_ROWS = 2

##
## LED pin configuration
##
RED_LED_PIN = 18
BLUE_LED_PIN = 23

##
## Button pin configuration
##
GREEN_BUTTON_PIN = 24   # cycles thermostat state
RED_BUTTON_PIN = 25     # increases setpoint
BLUE_BUTTON_PIN = 12    # decreases setpoint

##
## Serial connection configuration
##
SERIAL_PORT = '/dev/ttyS0'
SERIAL_BAUDRATE = 115200
SERIAL_TIMEOUT = 1  # seconds
SERIAL_REPORT_INTERVAL = 30  # seconds between status reports

##
## Thermostat behavior configuration
##
DEFAULT_SET_POINT = 72  # degrees Fahrenheit
SETPOINT_MIN = 50       # degrees Fahrenheit
SETPOINT_MAX = 90       # degrees Fahrenheit

##
## DEBUG flag
##
## Boolean value to indicate whether or not to print
## status messages on the console of the program.
##
DEBUG = True
