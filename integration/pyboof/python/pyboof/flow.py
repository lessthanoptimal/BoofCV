from pyboof.image import *
import pyboof.ip as ip

class GradientFlow:
    def __init__(self, derivX, derivY):
        self.derivX = derivX
        self.derivY = derivY

    def visualize(self):
        buffered_image = gateway.jvm.boofcv.gui.image.VisualizeImageData.colorizeGradient(self.derivX,self.derivY,-1)
        gateway.jvm.boofcv.gui.image.ShowImages.showWindow(buffered_image,"Gradient")

class BoofFlow:
    def __init__(self, image):
        self.image = image

    def convert_to_ndarray(self):
        return boof_to_ndarray(self.image)

    def get_boof_image(self):
        return self.image

    def get_width(self):
        return self.image.getWidth()

    def get_height(self):
        return self.image.getHeight()

    def visualize(self, name="image"):
        gateway.jvm.boofcv.gui.image.ShowImages.showWindow(self.image,name)

    def blur_gaussian(self,sigma=-1.0,radius=1):
        blurred = self.image._createNew(self.image.getWidth(),self.image.getHeight())
        ip.blur_gaussian(self.image,blurred,sigma,radius)
        return BoofFlow(blurred)

    def blur_mean(self,radius=1):
        blurred = self.image._createNew(self.image.getWidth(),self.image.getHeight())
        ip.blur_mean(self.image,blurred,radius)
        return BoofFlow(blurred)

    def blur_median(self,radius=1):
        blurred = self.image._createNew(self.image.getWidth(),self.image.getHeight())
        ip.blur_median(self.image,blurred,radius)
        return BoofFlow(blurred)

    def gradient(self,type="sobel"):
        type_grad = gradient_type(self.image)
        derivX = create_boof_image(self.get_width(),self.get_height(),type_grad)
        derivY = create_boof_image(self.get_width(),self.get_height(),type_grad)
        ip.gradient(self.image,derivX,derivY,type)
        return GradientFlow(derivX,derivY)