import cv2
import pyboof.image as image
import pyboof.swing as swing
import numpy as np

image_path = '../../../data/applet/outdoors01.jpg'

# Can load an image using OpenCV then convert it into BoofCV
ndarray_img = cv2.imread(image_path,0)

boof_cv = image.ndarray_to_boof(ndarray_img)

# Can also use BoofCV to load the image directly
boof_gray = image.load_single_band(image_path,np.uint8)
boof_color = image.load_multi_spectral(image_path,np.uint8)

# Let's display all 3 of them in Java
swing.show(boof_cv,"OpenCV")
swing.show(boof_gray,"Gray Scale")
swing.show(boof_color,"Color")