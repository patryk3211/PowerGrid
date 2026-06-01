# Current Meter
![Something smells toasty](block:powergrid:current_gauge)

The protocol for interacting with a current meter supports both read and write operations.
Any use of this protocol for violence voids warranty.

When writing to the serial interface of a current meter, both negative and positive values are allowed, this values moves the comma around from the reading.
When `0` it returns in unit Ampere, when `-3` it returns in mA.
Use any extreme values at your own risk, any values lower than -3 are not supported and *WILL* void your warranty.

When reading from the serial interface of a current meter, the value scaled to the right unit based on the configuration.

*We are not responsible for any injury caused by misreadings. This protocol is for eduction use only. Use at your own risk.*