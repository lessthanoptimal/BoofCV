import cv2
import pyboof.ip
import pyboof.image

# Can load an image using OpenCV then convert it into BoofCV
ndarray_img = cv2.imread('t_top_noborder.jpg',0)

boof0 = pyboof.image.ndarray_to_boof(ndarray_img)

# Can also use BoofCV to load the image directly