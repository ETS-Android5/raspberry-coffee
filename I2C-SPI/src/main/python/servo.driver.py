#
# Drive a continuous or standard servo
# Ground (black or brown) on pin #6
# 5V (red or orange) on pin #2
# Signal (white or yellow) on pin #3
#

import RPi.GPIO as GPIO
from time import sleep

servo_pin = 3  # Physical pin. (3: SDA)


def set_angle(angle):
    duty = angle / 18 + 2
    GPIO.output(servo_pin, True)
    pwm.ChangeDutyCycle(duty)
    sleep(1)
    GPIO.output(servo_pin, False)
    pwm.ChangeDutyCycle(0)


GPIO.setmode(GPIO.BOARD)
GPIO.setup(servo_pin, GPIO.OUT)

pwm = GPIO.PWM(servo_pin, 50)
pwm.start(0)

set_angle(90)
pwm.stop()
GPIO.cleanup()
