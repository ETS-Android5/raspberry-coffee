#!/usr/bin/env python3
from imageai.Detection import ObjectDetection
import matplotlib.pyplot as plt
import matplotlib.image as mpimg
import os
import sys

input_image = "image.jpg"
if len(sys.argv) > 1:
    input_image = sys.argv[1]
    # for i in range(len(sys.argv)):
    #     print("Arg[{}] {}".format(i, sys.argv[i]))
    # sys.exit(0)

execution_path = os.getcwd()

detector = ObjectDetection()
detector.setModelTypeAsRetinaNet()
detector.setModelPath(os.path.join(execution_path, "resnet50_coco_best_v2.0.1.h5"))
detector.loadModel()
detections = detector.detectObjectsFromImage(input_image=os.path.join(execution_path, input_image),
                                             output_image_path=os.path.join(execution_path, "imagenew.jpg"))

print('Detections:')
for eachObject in detections:
    print(eachObject["name"], " : ", eachObject["percentage_probability"])

plt.imshow(mpimg.imread("imagenew.jpg"))
# plt.show(block=False)
plt.show()
