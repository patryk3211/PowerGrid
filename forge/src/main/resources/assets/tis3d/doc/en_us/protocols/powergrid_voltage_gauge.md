# Voltage Meter
![If number high stay away!](block:powergrid:voltage_gauge)

The protocol for interacting with a voltage meter supports both read and write operations.
Any use of this protocol for enjoyment voids warranty.

When writing to the serial interface of a voltage meter, both negative and positive values are allowed, this values moves the comma around from the reading.
When `0` it returns in unit Volts, when `-3` it returns in mV.
Use any extreme values at your own risk, any values lower than -3 are not supported and *WILL& void your warranty.

When reading from the serial interface of a voltage meter, the value scaled to the right unit based on the configuration.

*We are not responsible for any injury caused by misreadings. This protocol is for eduction use only. Use at your own risk.*