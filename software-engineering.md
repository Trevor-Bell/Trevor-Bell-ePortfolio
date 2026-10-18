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
# Paste the ThermostatStateMachine constructor showing dependency injection.
```

```python
# Paste the SensorInterface read method showing the last-known-good fallback.
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
