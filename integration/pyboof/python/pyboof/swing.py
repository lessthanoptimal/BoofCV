import math
import py4j.java_gateway as jg
from pyboof import gateway

def show( boof_image , title="Image"):
    gateway.jvm.boofcv.gui.image.ShowImages.showWindow(boof_image,title)

def show_grid( images , columns=-1, title="Image Grid"):
    if type(images) is not tuple or not list:
        images = (images)

    array = gateway.new_array(gateway.jvm.java.awt.image.BufferedImage,len(images))
    for idx,image in enumerate(images):
        if jg.is_instance_of(gateway, image, gateway.jvm.java.awt.image.BufferedImage ):
            array[idx] = image
        else:
            array[idx] = gateway.jvm.boofcv.io.image.ConvertBufferedImage.convertTo(image,None,True)

    # If no grid is specified try to make it square
    if columns <= 0:
        columns = int(math.sqrt(len(images)))
    gateway.jvm.boofcv.gui.image.ShowImages.showGrid(columns,title,array)

def show_list( image_name_pairs , title="Image List"):
    if type(image_name_pairs) is not tuple or not list:
        image_name_pairs = (image_name_pairs)

    names = []
    buffered = []
    for pair in image_name_pairs:
        if jg.is_instance_of(gateway, pair[0], gateway.jvm.java.awt.image.BufferedImage ):
            buffered.append(pair[0])
        else:
            buffered.append( gateway.jvm.boofcv.io.image.ConvertBufferedImage.convertTo(pair[0],None,True) )
        names.append(pair[1])

    panel = gateway.jvm.boofcv.gui.ListDisplayPanel()
    for i in range(len(names)):
        panel.addImage(buffered[i],names[i])
    gateway.jvm.boofcv.gui.image.ShowImages.showWindow(panel,title)

def colorize_gradient( derivX , derivY ):
    return gateway.jvm.boofcv.gui.image.VisualizeImageData.colorizeGradient(derivX,derivY,-1)