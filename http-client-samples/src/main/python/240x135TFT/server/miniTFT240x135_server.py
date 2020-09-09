#!/usr/bin/env python3
#
# Requires:
# ---------
# pip3 install http (already in python3.7, no need to install it)
#
# Provides REST access to the cache, try GET http://localhost:8080/lis3mdl/cache
#
import json
import sys
import threading
import traceback
import time
from http.server import HTTPServer, BaseHTTPRequestHandler
from time import sleep

import digitalio
import busio
import board
from PIL import Image, ImageDraw, ImageFont
import adafruit_rgb_display.st7789 as st7789

# Define the Reset Pin
oled_reset = digitalio.DigitalInOut(board.D4)

sample_data = {  # Used for VIEW, and non-implemented operations. Fallback.
    "1": "First",
    "2": "Second",
    "3": "Third",
    "4": "Fourth"
}
server_port = 8080
REST_DEBUG = False

# Configuration for CS and DC pins (these are FeatherWing defaults on M0/M4):
cs_pin = digitalio.DigitalInOut(board.CE0)
dc_pin = digitalio.DigitalInOut(board.D25)
reset_pin = None

# Config for display baudrate (default max is 24mhz):
BAUDRATE = 64000000

# Setup SPI bus using hardware SPI:
spi = board.SPI()


# Create the ST7789 display:
disp = st7789.ST7789(
    spi,
    cs=cs_pin,
    dc=dc_pin,
    rst=reset_pin,
    baudrate=BAUDRATE,
    width=135,
    height=240,
    x_offset=53,
    y_offset=40,
)

# Create blank image for drawing.
# Make sure to create image with mode 'RGB' for full color.
height = disp.width  # we swap height/width to rotate it to landscape!
width = disp.height
image = Image.new("RGB", (width, height))
rotation = 90
font_size = 24
font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", font_size)


def load_font(size):
    return ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", size)


# Turn on the backlight
backlight = digitalio.DigitalInOut(board.D22)
backlight.switch_to_output()
backlight.value = True

# Get drawing object to draw on image.
draw = ImageDraw.Draw(image)


def cls(width, height, color="#000000"):
    # Draw a black filled box to clear the image.
    draw.rectangle((0, 0, width, height), outline=0, fill=color)


def display(rotation):
    disp.image(image, rotation)


def write_on_screen(draw, text, x, y, font, color):
    draw.text((x, y),
              text,
              font=font,
              fill=color)


PATH_PREFIX = "/miniTFT"


# Defining a HTTP request Handler class
class ServiceHandler(BaseHTTPRequestHandler):
    # sets basic headers for the server
    def _set_headers(self):
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        # reads the length of the Headers
        length = int(self.headers['Content-Length'])
        # reads the contents of the request
        content = self.rfile.read(length)
        temp = str(content).strip('b\'')
        self.end_headers()
        return temp

    # To silence the HTTP logger
    def log_message(self, format, *args):
        if REST_DEBUG:
            print(format % args)
        return

    # GET Method Definition
    def do_GET(self):
        if REST_DEBUG:
            print("GET methods")
        # defining all the headers
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        #
        full_path = self.path
        split = full_path.split('?')
        path = split[0]
        qs = None
        if len(split) > 1:
            qs = split[1]
        # The parameters into a map
        prm_map = {}
        if qs is not None:
            qs_prms = qs.split('&')
            for qs_prm in qs_prms:
                nv_pair = qs_prm.split('=')
                if len(nv_pair) == 2:
                    prm_map[nv_pair[0]] = nv_pair[1]
                else:
                    print("oops, no equal sign in {}".format(qs_prm))

        if path == PATH_PREFIX + "/oplist":
            response = {
                "oplist": [{
                        "path": PATH_PREFIX + "/oplist",
                        "verb": "GET",
                        "description": "Get the available operation list."
                    }, {
                        "path": PATH_PREFIX + "/display",
                        "verb": "POST",
                        "description": "Display some text on screen. Body application/json, like [ { x: x, y: y, text: 'Text' } ]"
                    }, {
                        "path": PATH_PREFIX + "/clean",
                        "verb": "POST",
                        "description": "Clear the screen."
                    }]
            }
            response_content = json.dumps(response).encode()
            self.send_response(200)
            # defining the response headers
            # self.send_header('Content-Type', 'application/json')
            # content_len = len(response_content)
            # self.send_header('Content-Length', content_len)
            # self.end_headers()
            self.wfile.write(response_content)
        else:
            if REST_DEBUG:
                print("GET on {} not managed".format(self.path))
            error = "NOT FOUND!"
            self.send_response(400)
            self.send_header('Content-Type', 'plain/text')
            content_len = len(error)
            self.send_header('Content-Length', content_len)
            self.end_headers()
            self.wfile.write(bytes(error, 'utf-8'))

    # VIEW method definition. Uncommon...
    def do_VIEW(self):
        # dict var. for pretty print
        display = {}
        temp = self._set_headers()
        # check if the key is present in the sample_data dictionary
        if temp in sample_data:
            display[temp] = sample_data[temp]
            # print the keys required from the json file
            self.wfile.write(json.dumps(display).encode())
        else:
            error = "{} Not found in sample_data\n".format(temp)
            self.send_response(404)
            self.send_header('Content-Type', 'plain/text')
            content_len = len(error)
            self.send_header('Content-Length', content_len)
            self.end_headers()
            self.wfile.write(bytes(error, 'utf-8'))

    # POST method definition
    def do_POST(self):
        if REST_DEBUG:
            print("POST request, {}".format(self.path))
        if self.path == PATH_PREFIX + "/display":
            # Get text to display from body (application/json)
            content_len = int(self.headers.get('Content-Length'))
            post_body = self.rfile.read(content_len).decode('utf-8')
            print("POST /display Content: {}".format(post_body))
            # {
            #   "rotation": 90,
            #   "bg-color": "#000000",  // default black
            #   "text": [ { x: x, y: y, text: "Text", size: 24, color: "#FFFFFF" } ]
            # }
            payload = json.loads(post_body)
            print("POST /display JSON Content: {}".format(payload))
            try:
                # Do the job
                bg_color = payload["bg-color"]
                color = bg_color if bg_color is not None else "#000000"
                cls(width, height, color)
                for line in payload["text"]:
                    print("\tLine: {}".format(line))
                    global font_size
                    global font
                    line_font_size = line["size"]
                    if line_font_size is not None:
                        font = load_font(line_font_size)
                        font_size = line_font_size
                    fg_color = line["color"]
                    color = fg_color if fg_color is not None else "#FFFFFF"
                    write_on_screen(draw, line['text'], line['x'], line['y'], font, color)

                json_rotation = payload["rotation"]
                rotation = json_rotation if json_rotation is not None else 90
                display(rotation)
                print("Display finished")

                # Response
                self.send_response(201)
                self.send_header('Content-Type', 'application/json')
                response = {"status": "OK"}
                response_content = json.dumps(response).encode()
                content_len = len(response_content)
                self.send_header('Content-Length', content_len)
                self.end_headers()
                self.wfile.write(response_content)
            except:
                self.send_response(500)
                response = {"status": "Barf"}
                self.wfile.write(json.dumps(response).encode())
        elif self.path == PATH_PREFIX + "/clean":
            # Get text to display from body (application/json)
            content_len = int(self.headers.get('Content-Length'))
            post_body = self.rfile.read(content_len).decode('utf-8')
            print("POST /display Content: {}".format(post_body))
            # {
            #   "rotation": 90,
            #   "bg-color": "#000000"   // default black
            # }
            payload = json.loads(post_body)
            print("POST /display JSON Content: {}".format(payload))
            try:
                bg_color = payload["bg-color"]
                color = bg_color if bg_color is not None else "#000000"
                cls(width, height, color)
                json_rotation = payload["rotation"]
                rotation = json_rotation if json_rotation is not None else 90
                display(rotation)
                # Response
                self.send_response(201)
                self.send_header('Content-Type', 'application/json')
                response = {"status": "OK"}
                response_content = json.dumps(response).encode()
                content_len = len(response_content)
                self.send_header('Content-Length', content_len)
                self.end_headers()
                self.wfile.write(response_content)
            except:
                self.send_response(500)
                response = {"status": "Barf"}
                self.wfile.write(json.dumps(response).encode())
        else:
            if REST_DEBUG:
                print("POST on {} not managed".format(self.path))
            error = "NOT FOUND!"
            self.send_response(404)
            self.send_header('Content-Type', 'plain/text')
            content_len = len(error)
            self.send_header('Content-Length', content_len)
            self.end_headers()
            self.wfile.write(bytes(error, 'utf-8'))

    # self.wfile.write(json.dumps(data[str(index)]).encode())

    # PUT method Definition
    def do_PUT(self):
        if REST_DEBUG:
            print("PUT request, {}".format(self.path))
        if self.path.startswith("/whatever/"):
            self.send_response(201)
            response = {"status": "OK"}
            self.wfile.write(json.dumps(response).encode())
        else:
            if REST_DEBUG:
                print("PUT on {} not managed".format(self.path))
            error = "NOT FOUND!"
            self.send_response(404)
            self.send_header('Content-Type', 'plain/text')
            content_len = len(error)
            self.send_header('Content-Length', content_len)
            self.end_headers()
            self.wfile.write(bytes(error, 'utf-8'))

    # DELETE method definition
    def do_DELETE(self):
        if REST_DEBUG:
            print("DELETE on {} not managed".format(self.path))
        error = "NOT FOUND!"
        self.send_response(400)
        self.send_header('Content-Type', 'plain/text')
        content_len = len(error)
        self.send_header('Content-Length', content_len)
        self.end_headers()
        self.wfile.write(bytes(error, 'utf-8'))


machine_name = "127.0.0.1"
MACHINE_NAME_PRM_PREFIX = "--machine-name:"
PORT_PRM_PREFIX = "--port:"
VERBOSE_PREFIX = "--verbose:"


if len(sys.argv) > 0:  # Script name + X args
    for arg in sys.argv:
        if arg[:len(MACHINE_NAME_PRM_PREFIX)] == MACHINE_NAME_PRM_PREFIX:
            machine_name = arg[len(MACHINE_NAME_PRM_PREFIX):]
        if arg[:len(PORT_PRM_PREFIX)] == PORT_PRM_PREFIX:
            server_port = int(arg[len(PORT_PRM_PREFIX):])
        if arg[:len(VERBOSE_PREFIX)] == VERBOSE_PREFIX:
            REST_DEBUG = (arg[len(VERBOSE_PREFIX):].lower() == "true")

# Server Initialization
port_number = server_port
print("Starting server on port {}".format(port_number))
server = HTTPServer((machine_name, port_number), ServiceHandler)
#
print("Try curl -X GET http://{}:{}{}/oplist".format(machine_name, port_number, PATH_PREFIX))
print("or  curl -v -X VIEW http://{}:{}{} -H \"Content-Length: 1\" -d \"1\"".format(machine_name, port_number, PATH_PREFIX))
#
try:
    server.serve_forever()
except KeyboardInterrupt:
    print("\n\t\tUser interrupted (server.serve), exiting.")

cls(width, height)
print("Done")
