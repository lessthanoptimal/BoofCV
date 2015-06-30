import pyboof.ip as ip
import pyboof.image as image

import numpy as np

original = image.load_image('../../../data/applet/outdoors01.jpg',np.uint8)
gaussian = image.create_boof_image( original.getWidth() , original.getHeight() , np.uint8 )
mean = image.create_boof_image( original.getWidth() , original.getHeight() , np.uint8 )

ip.blur_gaussian(original,gaussian,radius=3)
ip.blur_mean(original,mean,radius=3)

image.show_in_java(original,"Original")
image.show_in_java(gaussian,"Gaussian")
image.show_in_java(mean,"Mean")