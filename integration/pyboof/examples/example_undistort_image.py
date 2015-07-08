import pyboof as pb

import numpy as np

data_path = "../../../data/applet/calibration/stereo/Bumblebee2_Chess/"

# Load the camera parameters
intrinsic = pb.Intrinsic()
intrinsic.load_xml(data_path+"intrinsicLeft.xml")

# Load original image and the undistorted image
original = pb.load_single_band(data_path+"left08.jpg",np.uint8)
undistorted = original.createSameShape()

# Remove distortion and show the results
pb.remove_distortion(original,undistorted,intrinsic)
pb.swing.show(original,"Original")
pb.swing.show(undistorted,"Undistorted")

