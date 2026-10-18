#
# sensor_interface.py
# 
# Wraps the AHT20 temperature/humidity sensor.
# Isolates all direct hardware access behind a small interface so the
# sensor can be swapped for a mock in tests, and so a failed read
# doesn't crash the caller.
#

##
## SensorReadError - Raised when a temperature or humidity read fails.
##
class SensorReadError(Exception):
    pass

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
