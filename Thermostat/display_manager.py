#
# display_manager.py
# 
# Wraps LCD display output. ManagedDisplay handles the low-level 
# GPIO/LCD library setup DisplayManager sits above it as the class 
# the rest of the application actually talks to, 
# and accepts a pre-built display object for testing.
#

import config

##
## ManagedDisplay - Class intended to manage the 16x2 Display 
##
class ManagedDisplay:
    ##
    ## Class Initialization method to setup the display
    ##
    def __init__(self):
        ##
        ## digitalio and adafruit_character_lcd are only imported here,
        ## inside __init__, rather than at the top of the file. Both are
        ## Raspberry-Pi-only libraries; importing them here means this
        ## file can still be imported (and DisplayManager tested with a
        ## mock) on a machine that doesn't have them installed, as long
        ## as a real ManagedDisplay is never actually constructed there.
        ##
        import digitalio
        import adafruit_character_lcd.character_lcd as characterlcd

        ##
        ## Setup the six GPIO lines to communicate with the display.
        ## This uses the digitalio class to handle digital
        ## outputs on the GPIO lines. There is also a similar
        ## class for analog IO.
        ##
        self.lcd_rs = digitalio.DigitalInOut(config.LCD_RS_PIN)
        self.lcd_en = digitalio.DigitalInOut(config.LCD_EN_PIN)
        self.lcd_d4 = digitalio.DigitalInOut(config.LCD_D4_PIN)
        self.lcd_d5 = digitalio.DigitalInOut(config.LCD_D5_PIN)
        self.lcd_d6 = digitalio.DigitalInOut(config.LCD_D6_PIN)
        self.lcd_d7 = digitalio.DigitalInOut(config.LCD_D7_PIN)

        self.lcd_columns = config.LCD_COLUMNS
        self.lcd_rows = config.LCD_ROWS

        # Initialise the lcd class
        self.lcd = characterlcd.Character_LCD_Mono(
            self.lcd_rs, self.lcd_en,
            self.lcd_d4, self.lcd_d5, self.lcd_d6, self.lcd_d7,
            self.lcd_columns, self.lcd_rows
        )

        # Wipe LCD screen before start
        self.lcd.clear()

    ##
    ## cleanupDisplay - Method used to cleanup the digitalIO lines that
    ## are used to run the display
    ##
    def cleanupDisplay(self):
        # Clear the LCD
        self.lcd.clear()
        self.lcd_rs.deinit()
        self.lcd_en.deinit()
        self.lcd_d4.deinit()
        self.lcd_d5.deinit()
        self.lcd_d6.deinit()
        self.lcd_d7.deinit()

    ##
    ## clear - Convenience method used to clear the display
    ##
    def clear(self):
        self.lcd.clear()

    ##
    ## updateScreen - Convenience method used to update the message
    ##
    def updateScreen(self, message):
        self.lcd.clear()
        self.lcd.message = message

    ## End class ManagedDisplay definition

##
## DisplayManager - Sits above ManagedDisplay and is the class the
## rest of the application (the state machine, main.py) actually
## talks to. Accepting the underlying screen object as a constructor
## argument is what allows a MockDisplay to be substituted in tests,
## instead of this class always reaching for real hardware.
##
class DisplayManager:
    ##
    ## Class Initialization method. If no screen object is provided,
    ## sets up a real ManagedDisplay (i.e. real hardware); otherwise
    ## uses whatever was passed in (e.g. a MockDisplay in tests).
    ##
    def __init__(self, screen=None):
        self.screen = screen if screen is not None else ManagedDisplay()

    ##
    ## show - Convenience method used to display a two-line message
    ##
    def show(self, line1, line2):
        self.screen.updateScreen(line1 + line2)
    
    ##
    ## clear - Convenience method used to clear the display
    ##
    def clear(self):
        self.screen.clear()

    ##
    ## shutdown - Cleans up the underlying display's GPIO lines
    ##
    def shutdown(self):
        self.screen.cleanupDisplay()

    ## End class DisplayManager definition
