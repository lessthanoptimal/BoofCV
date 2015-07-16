from pyboof.common import *
from pyboof import gateway

class Border:
    SKIP=0,
    EXTENDED=1
    NORMALIZED=2
    REFLECT=3
    WRAP=4
    VALUE=5

def border_to_java( border ):
    if border == Border.SKIP:
        return gateway.jvm.boofcv.core.image.border.BorderType.valueOf("SKIP")
    elif border == Border.EXTENDED:
        return gateway.jvm.boofcv.core.image.border.BorderType.valueOf("EXTENDED")
    elif border == Border.NORMALIZED:
        return gateway.jvm.boofcv.core.image.border.BorderType.valueOf("NORMALIZED")
    elif border == Border.REFLECT:
        return gateway.jvm.boofcv.core.image.border.BorderType.valueOf("REFLECT")
    elif border == Border.WRAP:
        return gateway.jvm.boofcv.core.image.border.BorderType.valueOf("WRAP")
    elif border == Border.VALUE:
        return gateway.jvm.boofcv.core.image.border.BorderType.valueOf("VALUE")


def blur_gaussian(input,output,sigma=-1.0,radius=1):
    gateway.jvm.boofcv.alg.filter.blur.BlurImageOps.gaussian(input,output,sigma,radius,None)

def blur_mean(input,output,radius=1):
    gateway.jvm.boofcv.alg.filter.blur.BlurImageOps.mean(input,output,radius,None)

def blur_median(input,output,radius=1):
    gateway.jvm.boofcv.alg.filter.blur.BlurImageOps.median(input,output,radius,None)

class GradientType:
    SOBEL="sobel"
    PREWITT="prewitt"
    THREE="three"
    TWO0="two0"
    TWO1="two1"

def gradient(input, derivX , derivY, type=GradientType.SOBEL, border=Border.EXTENDED):
    java_border = border_to_java(border)
    java_DerivativeOps = gateway.jvm.boofcv.alg.filter.derivative.GImageDerivativeOps
    java_DerivativeType = gateway.jvm.boofcv.alg.filter.derivative.DerivativeType
    if type is GradientType.SOBEL:
        java_DerivativeOps.gradient(java_DerivativeType.SOBEL,input,derivX,derivY,java_border)
    elif type is GradientType.PREWITT:
        java_DerivativeOps.gradient(java_DerivativeType.PREWITT,input,derivX,derivY,java_border)
    elif type is GradientType.THREE:
        java_DerivativeOps.gradient(java_DerivativeType.THREE,input,derivX,derivY,java_border)
    elif type is GradientType.TWO0:
        java_DerivativeOps.gradient(java_DerivativeType.TWO_0,input,derivX,derivY,java_border)
    elif type is GradientType.TWO1:
        java_DerivativeOps.gradient(java_DerivativeType.TWO_1,input,derivX,derivY,java_border)
    else:
        raise RuntimeError("Unknown gradient type "+type)

class ImageDistort(JavaWrapper):
    """
    Applies a distortion to a BoofCV image.
    Wrapper around BoofCV ImageDistort class
    """

    def __init__(self, boof_ImageDistort):
        self.set_java_object(boof_ImageDistort)

    def apply(self, imageA , imageB ):
        self.java_obj.apply(imageA,imageB)