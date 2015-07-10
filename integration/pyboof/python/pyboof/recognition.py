from pyboof.common import *
from pyboof.image import *
from pyboof.geo import *
from py4j import java_gateway


class Config(JavaWrapper):
    def __init__(self, java_ConfigPolygonDetector):
        self.set_java_object(java_ConfigPolygonDetector)

    def get_property(self, name):
        return java_gateway.get_field(self.java_obj,name)

    def set_property(self, name, value):
        return java_gateway.set_field(self.java_obj,name, value)

class ConfigPolygonDetector(Config):
    def __init__(self, java_ConfigPolygonDetector):
        Config.__init__(self,java_ConfigPolygonDetector)

class ConfigFiducialImage(Config):
    def __init__(self, obj=None):
        if obj is None:
            config = gateway.jvm.boofcv.factory.fiducial.ConfigFiducialImage()
        else:
            config = obj
        Config.__init__(self,config)

    def get_polygon_detector(self):
        return ConfigPolygonDetector(self.java_obj.getSquareDetector())


class FiducialFactory:
    def __init__(self, dtype ):
        self.boof_image_type =  dtype_to_Class_SingleBand(dtype)

    def squareRobust(self, config, binary_radius ):
        if isinstance(config,ConfigFiducialImage):
            java_detector = gateway.jvm.boofcv.factory.fiducial.FactoryFiducial.squareImageRobust(config.java_obj,binary_radius,self.boof_image_type)
            return FiducialImageDetector(java_detector)
        else:
            raise RuntimeError("Need to add square binary")

    def squareFast(self, config, threshold ):
        if isinstance(config,ConfigFiducialImage):
            java_detector = gateway.jvm.boofcv.factory.fiducial.FactoryFiducial.squareImageFast(config.java_obj,threshold,self.boof_image_type)
            return FiducialImageDetector(java_detector)
        else:
            raise RuntimeError("Need to add square binary")


class FiducialDetector(JavaWrapper):
    """
    Detects fiducials and estimates their ID and 3D pose
    Wrapper around BoofCV class of the same name
    """

    def __init__(self, java_FiducialDetector):
        self.set_java_object(java_FiducialDetector)

    def detect(self, image ):
        self.java_obj.detect(image)

    def setIntrinsic(self, intrinsic ):
        java_intrinsic = intrinsic.convert_boof()
        self.java_obj.setIntrinsic(java_intrinsic)

    def totalFound(self):
        return self.java_obj.totalFound()

    def getFiducialToCamera(self, which ):
        fid_to_cam = Se3_F64()
        self.java_obj.getFiducialToCamera(which,fid_to_cam.get_java_object())
        return fid_to_cam

    def getId(self, which):
        self.java_obj.getId(which)

    def getWidth(self, which ):
        self.java_obj.getWidth(which)

    def getInputType(self):
        return ImageType(self.java_obj.getInputType())

class FiducialImageDetector(FiducialDetector):

    def addPattern(self, image, side_length, threshold=100.0):
        self.java_obj.addPattern(image,threshold,side_length)

class ConfigCirculant(Config):
    def __init__(self, obj=None):
        if obj is None:
            config = gateway.jvm.boofcv.abst.tracker.ConfigCirculantTracker()
        else:
            config = obj
        Config.__init__(self,config)

class ConfigTld(Config):
    def __init__(self, obj=None):
        if obj is None:
            config = gateway.jvm.boofcv.abst.tracker.ConfigTld()
        else:
            config = obj
        Config.__init__(self,config)

class FactoryTrackerObjectQuad:
    def __init__(self, dtype ):
        self.boof_image_type =  dtype_to_Class_SingleBand(dtype)

    def circulant(self, config=None ):
        java_tracker = gateway.jvm.boofcv.factory.tracker.FactoryTrackerObjectQuad.circulant(config,self.boof_image_type)
        return TrackerObjectQuad(java_tracker)

    def tld(self, config=None ):
        java_tracker = gateway.jvm.boofcv.factory.tracker.FactoryTrackerObjectQuad.tld(config,self.boof_image_type)
        return TrackerObjectQuad(java_tracker)

class TrackerObjectQuad(JavaWrapper):
    """
    High level object tracker.  Takes in a quadrilateral for the initial location of the target then proceeds to
    update it for each new image in the sequence
    """
    def __init__(self, java_TrackerObjectQuad):
        self.set_java_object(java_TrackerObjectQuad)

    def initialize(self, image , location ):
        """
        Initialize the tracker by specifying the location of the target inside the image
        :param image: BoofCV image
        :param location: Quadrilateral2D
           Specifies the location inside the quadrilateral
        :return: bool
           True if initialization was successful or False if it failed
        """
        boof_quad = location.convert_to_boof()
        return self.java_obj.initialize( image , boof_quad )

    def process(self, image, location ):
        """
        Initialize the tracker by specifying the location of the target inside the image
        :param image: BoofCV image
        :param location: Quadrilateral2D
           (output) Will contain the new location of the tracked object
        :return: bool
           True if tracking was successful or False if it failed
        """
        boof_quad = location.convert_to_boof()
        success = self.java_obj.process( image , boof_quad )
        if success:
            location.set(boof_quad)
        return success

    def getImageType(self):
        return ImageType(self.java_obj.getImageType())