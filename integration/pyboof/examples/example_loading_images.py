import cv2
import pyboof as pb
import numpy as np

image_path = '../../../data/applet/outdoors01.jpg'

# Can load an image using OpenCV then convert it into BoofCV
ndarray_img = cv2.imread(image_path,0)

boof_cv = pb.ndarray_to_boof(ndarray_img)

# Can also use BoofCV to load the image directly
boof_gray = pb.load_single_band(image_path,np.uint8)
boof_color = pb.load_multi_spectral(image_path,np.uint8)

# Let's display all 3 of them in Java
pb.swing.show(boof_cv,"OpenCV")
pb.swing.show(boof_gray,"Gray Scale")
pb.swing.show(boof_color,"Color")