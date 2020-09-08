"""
This demo will fill the screen with white, draw a black box on top
and then print Hello World! in the center of the display

This example is for use on (Linux) computers that are using CPython with
Adafruit Blinka to support CircuitPython libraries. CircuitPython does
not support PIL/pillow (python imaging library)!
"""

import board
import digitalio
from PIL import Image, ImageDraw, ImageFont
import adafruit_ssd1305
from time import sleep

# Define the Reset Pin
oled_reset = digitalio.DigitalInOut(board.D4)

# Change these
# to the right size for your display!
WIDTH = 128
HEIGHT = 64  # Change to 32 if needed
BORDER = 4

# Use for SPI
spi = board.SPI()
oled_cs = digitalio.DigitalInOut(board.D5)
oled_dc = digitalio.DigitalInOut(board.D6)
oled = adafruit_ssd1305.SSD1305_SPI(WIDTH, HEIGHT, spi, oled_dc, oled_reset, oled_cs)

# Use for I2C.
# i2c = board.I2C()
# oled = adafruit_ssd1305.SSD1305_I2C(WIDTH, HEIGHT, i2c, addr=0x3c, reset=oled_reset)

print("Hit Ctrl-C to stop")
# Clear display.
oled.fill(0)
oled.show()

# Create blank image for drawing.
# Make sure to create image with mode '1' for 1-bit color.
image = Image.new("1", (oled.width, oled.height))

# Get drawing object to draw on image.
draw = ImageDraw.Draw(image)

# Draw a white background
draw.rectangle((0, 0, oled.width, oled.height), outline=255, fill=255)

# Draw a smaller inner rectangle
draw.rectangle(
    (BORDER, BORDER, oled.width - BORDER - 1, oled.height - BORDER - 1),
    outline=0,
    fill=0,
)

# Load default font.
# font = ImageFont.load_default()
font_size = 10
font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", font_size)

# Draw Some Text
text = "This will be a"
#(font_width, font_height) = font.getsize(text)
#
# print("For [{}], font width: {}, height: {}".format(text, font_width, font_height))
#
draw.text(
    (BORDER + 2, (1 * font_size)),
    text,
    font=font,
    fill=255,
)
text = "message displayed"
# (font_width, font_height) = font.getsize(text)
#
# print("For [{}], font width: {}, height: {}".format(text, font_width, font_height))
#
draw.text(
    (BORDER + 2, (2 * font_size)),
    text,
    font=font,
    fill=255,
)
text = "on several lines!"
# (font_width, font_height) = font.getsize(text)
#
# print("For [{}], font width: {}, height: {}".format(text, font_width, font_height))
#
draw.text(
    (BORDER + 2, (3 * font_size)),
    text,
    font=font,
    fill=255,
)

# Display image
oled.image(image)
oled.show()

keep_looping = True

while keep_looping:
    try:
        sleep(0.1)
    except KeyboardInterrupt:
        print("\n\t\tUser interrupted, exiting.")
        keep_looping = False

oled.fill(0)
oled.show()
print("Bye!")
