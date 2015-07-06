import pyboof.image as image
import pyboof.calib as calib
import pyboof.recognition as recognition

import numpy as np

data_path = "../../../data/applet/fiducial/image/examples/"

# Load the camera parameters
intrinsic = calib.Intrinsic()
intrinsic.load_xml(data_path+"intrinsic.xml")

config = recognition.ConfigFiducialImage()

print "Configuring detector"
detector = recognition.FiducialFactory( np.uint8 ).squareRobust(config,6)
detector.setIntrinsic(intrinsic)
detector.addPattern(image.load_single_band(data_path+"../patterns/chicken.png",np.uint8),4.0)
detector.addPattern(image.load_single_band(data_path+"../patterns/yu.png",np.uint8),4.0)

print "Detecting image"
detector.detect(image.load_single_band(data_path+"image01.jpg",np.uint8))

print "Number Found = "+str(detector.totalFound())

for i in range(detector.totalFound()):
    print "=========== Found "+str(i)
    fid_to_cam = detector.getFiducialToCamera(i)
    # print fid_to_cam
    print "Rotation"
    print "  "+str(fid_to_cam.get_rotation())
    print "Translation"
    print "  "+str(fid_to_cam.get_translation())

    # TODO Render results