#
# tests/test_state_machine.py
# 
# Unit tests for ThermostatStateMachine,
# using the mocked hardware from hardware_mocks.py so these run
# without a Raspberry Pi or any real sensor/display/serial/LED
# hardware attached.
#

import unittest

from hardware_mocks import MockSensor, MockSerial, MockDisplay, MockLED
from sensor_interface import SensorInterface
from display_manager import DisplayManager
from serial_reporter import SerialReporter
from state_machine import ThermostatStateMachine

##
## build_thermostat - Helper method used to build a
## ThermostatStateMachine wired entirely to mock hardware, so each
## test isn't repeating the same five-line construction. Returns the
## state machine plus every mock object, so a test can both drive the
## state machine and inspect what its mocks recorded.
##
def build_thermostat(temperature=70.0):
    # Helper: builds a ThermostatStateMachine wired entirely to mocks.
    mock_sensor = MockSensor(temperature=temperature)
    mock_serial = MockSerial()
    mock_display = MockDisplay()

    sensor = SensorInterface(sensor=mock_sensor)
    display = DisplayManager(screen=mock_display)
    serial_reporter = SerialReporter(connection=mock_serial)
    red_light = MockLED()
    blue_light = MockLED()

    tsm = ThermostatStateMachine(sensor, display, serial_reporter, red_light, blue_light)
    return tsm, mock_sensor, mock_serial, mock_display, red_light, blue_light

##
## TestStateTransitions - Verifies the off/heat/cool state cycle
## behaves correctly and that the off state turns both LEDs off.
##
class TestStateTransitions(unittest.TestCase):
    ##
    ## test_starts_in_off_state - A freshly built state machine should
    ## always start in the off state.
    ##
    def test_starts_in_off_state(self):
        tsm, *_ = build_thermostat()
        self.assertEqual(tsm.current_state.id, "off")

    ##
    ## test_cycle_off_to_heat_to_cool_to_off - Three presses of the
    ## state button should visit heat, then cool, then return to off.
    ##
    def test_cycle_off_to_heat_to_cool_to_off(self):
        tsm, *_ = build_thermostat()
        tsm.process_temp_state_button()
        self.assertEqual(tsm.current_state.id, "heat")
        tsm.process_temp_state_button()
        self.assertEqual(tsm.current_state.id, "cool")
        tsm.process_temp_state_button()
        self.assertEqual(tsm.current_state.id, "off")

    ##
    ## test_off_state_turns_off_both_lights - Cycling all the way back
    ## to off should leave both LEDs off, regardless of what state
    ## they were in immediately before.
    ##
    def test_off_state_turns_off_both_lights(self):
        tsm, _, _, _, red_light, blue_light = build_thermostat()
        tsm.process_temp_state_button()  # -> heat
        tsm.process_temp_state_button()  # -> cool
        tsm.process_temp_state_button()  # -> off
        self.assertEqual(red_light.state, 'off')
        self.assertEqual(blue_light.state, 'off')

    ## End class TestStateTransitions definition

##
## TestSetPointBounds - Verifies the setpoint increment/decrement
## handlers respect config.SETPOINT_MIN and config.SETPOINT_MAX.
##
class TestSetPointBounds(unittest.TestCase):
    ##
    ## test_increment_stops_at_max - Incrementing past the configured
    ## maximum should have no effect.
    ##
    def test_increment_stops_at_max(self):
        tsm, *_ = build_thermostat()
        tsm.set_point = config_max = 90  # matches config.SETPOINT_MAX
        tsm.process_temp_inc_button()
        self.assertEqual(tsm.set_point, config_max)

    ##
    ## test_decrement_stops_at_min - Decrementing past the configured
    ## minimum should have no effect.
    ##
    def test_decrement_stops_at_min(self):
        tsm, *_ = build_thermostat()
        tsm.set_point = config_min = 50  # matches config.SETPOINT_MIN
        tsm.process_temp_dec_button()
        self.assertEqual(tsm.set_point, config_min)

    ##
    ## test_increment_within_bounds_succeeds - A normal increment,
    ## nowhere near either bound, should still work as expected.
    ##
    def test_increment_within_bounds_succeeds(self):
        tsm, *_ = build_thermostat()
        tsm.set_point = 72
        tsm.process_temp_inc_button()
        self.assertEqual(tsm.set_point, 73)

    ## End class TestSetPointBounds definition

##
## TestSensorFailureHandling - Verifies the sensor-read error handling: 
## a failed read should fall back to the last known good value,
## never raise out of update_lights(), and return None (rather than raising) 
## if no reading has ever succeeded.
##
class TestSensorFailureHandling(unittest.TestCase):
    ##
    ## test_sensor_failure_falls_back_to_last_known_reading - After
    ## one successful read, a subsequent failed read should return the
    ## cached value instead of propagating the error.
    ##
    def test_sensor_failure_falls_back_to_last_known_reading(self):
        tsm, mock_sensor, *_ = build_thermostat(temperature=68.0)
        first_reading = tsm.get_current_fahrenheit()  # succeeds, caches value

        mock_sensor.should_fail = True
        second_reading = tsm.get_current_fahrenheit()  # fails, should reuse cache

        self.assertEqual(first_reading, second_reading)

    ##
    ## test_sensor_failure_does_not_raise - A sensor failure during
    ## update_lights() should never raise out to the caller.
    ##
    def test_sensor_failure_does_not_raise(self):
        tsm, mock_sensor, *_ = build_thermostat()
        mock_sensor.should_fail = True
        try:
            tsm.update_lights()
        except Exception as e:
            self.fail(f"update_lights() raised unexpectedly on sensor failure: {e}")

    ##
    ## test_no_reading_ever_succeeded_returns_none - Edge case: if the
    ## sensor fails on the very first read, there is no cached value
    ## to fall back to, so get_current_fahrenheit() should return None
    ## rather than raising.
    ##
    def test_no_reading_ever_succeeded_returns_none(self):
        mock_sensor = MockSensor()
        mock_sensor.should_fail = True  # fails from the very first read
        sensor = SensorInterface(sensor=mock_sensor)
        display = DisplayManager(screen=MockDisplay())
        serial_reporter = SerialReporter(connection=MockSerial())
        tsm = ThermostatStateMachine(sensor, display, serial_reporter, MockLED(), MockLED())

        self.assertIsNone(tsm.get_current_fahrenheit())

    ## End class TestSensorFailureHandling definition

if __name__ == "__main__":
    unittest.main()
