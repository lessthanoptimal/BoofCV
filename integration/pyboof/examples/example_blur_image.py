import pyboof.ip as ip
import pyboof.image as image
import pyboof.swing as swing

import numpy as np

original = image.load_single_band('../../../data/applet/outdoors01.jpg',np.uint8)

gaussian = original.createSameShape() # useful function which creates a new image of the
mean = original.createSameShape()     # same type and shape as the original

# Apply different types of blur to the image
ip.blur_gaussian(original,gaussian,radius=3)
ip.blur_mean(original,mean,radius=3)

# display the results in a single window as a list
image_list = [(original,"original"),(gaussian,"gaussian"),(mean,"mean")]
swing.show_list(image_list,title="Outputs")