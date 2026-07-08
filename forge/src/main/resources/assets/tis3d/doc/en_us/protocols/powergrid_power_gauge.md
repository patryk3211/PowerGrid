# Power Meter
![Its in watts](block:powergrid:power_gauge)

The protocol for interacting with a power meter supports both read and write operations.
*Any conversion to non-SI units such as but not limited to horsepower, BTU/h and foot-pound per minute will be reported.*

When writing to the serial interface of a power meter, both negative and positive values are allowed, this values moves the comma around from the reading.
When `0` it returns in unit Watts, when `-3` it returns in mW.
Use any extreme values at your own risk, any values lower than -3 are not supported and *WILL* void your warranty.

When reading from the serial interface of a power meter, the value scaled to the right unit based on the configuration.

*We are not responsible for any injury caused by misreadings. This protocol is for eduction use only. Use at your own risk.*