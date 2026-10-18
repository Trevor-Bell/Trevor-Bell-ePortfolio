#
# serial_reporter.py
# 
# Wraps serial communication with the TemperatureServer. 
# Isolates the pyserial setup and writes behind an
# interface that can be swapped for a mock in tests, and that fails
# safely instead of crashing the program at startup or mid-run if the
# connection is unavailable.
#

import serial
import config

##
## SerialReportError - Raised when the serial connection fails to
## open, or when a write to it fails.
##
class SerialReportError(Exception):
    pass

##
## SerialReporter - Class intended to manage the serial connection to
## the TemperatureServer and send it periodic status updates.
##
class SerialReporter:
    ##
    ## Class initialization method to setup the serial connection.
    ##
    def __init__(self, connection=None):
        ##
        ## connection: an object with a .write() method (real pyserial
        ## Serial, or a MockSerial in tests). If not provided, opens
        ## the real serial port using settings from config.py.
        ##
        if connection is not None:
            self.connection = connection
        else:
            ##
            ## Opening the connection is wrapped in a try/except so a
            ## missing or unavailable serial device raises
            ## SerialReportError, rather than crashing the whole
            ## program before it even reaches main().
            ##
            try:
                self.connection = serial.Serial(
                    port=config.SERIAL_PORT,
                    baudrate=config.SERIAL_BAUDRATE,
                    parity=serial.PARITY_NONE,      # Disable parity
                    stopbits=serial.STOPBITS_ONE,   # Serial protocol will use one stop bit
                    bytesize=serial.EIGHTBITS,
                    timeout=config.SERIAL_TIMEOUT
                )
            except serial.SerialException as e:
                raise SerialReportError(f"Failed to open serial port: {e}")

    ##
    ## report - Formats and sends a comma-delimited status update
    ## (state, current temperature in Fahrenheit, and setpoint) to the
    ## TemperatureServer over the serial connection.
    ##
    def report(self, state, temperature_f, set_point):
        output = f"{state},{temperature_f:.1f},{set_point}\n"
        try:
            self.connection.write(output.encode())
        except Exception as e:
            raise SerialReportError(f"Failed to write serial report: {e}")
    
    ## End class SerialReporter definition