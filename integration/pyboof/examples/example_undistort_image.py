import pyboof.image as image
import pyboof.calib as calib
import pyboof.swing as swing

import numpy as np

data_path = "../../../data/applet/calibration/stereo/Bumblebee2_Chess/"

# Load the camera parameters
intrinsic = calib.Intrinsic()
intrinsic.load_xml(data_path+"intrinsicLeft.xml")

# Load original image and the undistorted image
original = image.load_single_band(data_path+"left08.jpg",np.uint8)
undistorted = original.createSameShape()

# Remove distortion and show the results
calib.remove_distortion(original,undistorted,intrinsic)
swing.show_in_java(original,"Original")
swing.show_in_java(undistorted,"Undistorted")

