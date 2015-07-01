import pyboof.image as image
import pyboof.calib as calib

import numpy as np

data_path = "../../../data/applet/calibration/mono/Sony_DSC-HX5V_Chess/"

# Load the camera parameters
intrinsic = calib.Intrinsic()
intrinsic.load_xml(data_path+"intrinsic.xml")

# Load original image and the undistorted image
original = image.load_image(data_path+"frame08.jpg",np.uint8)
undistorted = image.create_boof_image( original.getWidth() , original.getHeight() , image.image_type(original) )

# Remove distortion and show the reuslts
calib.remove_distortion(original,undistorted,intrinsic)
image.show_in_java(original,"Original")
image.show_in_java(undistorted,"Undistorted")