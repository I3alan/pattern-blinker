# pattern-blinker
Simple JavaFX project.

Generates timing pseudo-code to blink LEDs according to a keystroke pattern.

Records up to 5 seconds of input and has adjustable limits to preview
and generate code only for the desired section of the recording.

The output is stored in a generated .txt file.


# Sample Output File:

Snippet A: Toggling and Wait Statements
```
LED OFF
WAIT 478 ms
LED ON
WAIT 251 ms
```

Snippet B: Corresponding Array { Current State (ON = 1, Off = 0), Delay Until Next }
```
{{0, 478}, {1, 251}}
```

HOW-TO-USE: insert the snippet into a loop and replace the toggling and wait statements by corresponding functions

Example:
```
while (count < 10) {
	for(int i = 0; i < array.length; i++) {
		if array[i][0] == 1 {
			toggle_led_on();
		} else {
			toggle_led_off();
		}
	sleep(array[i][1]);
	}
}
```
