import os
import numpy as np
from pyboof import gateway

def load_image( path , dtype ):
    file_path = os.path.abspath(path)

    boof_type = dtype_to_single_band_class(dtype)
    found = gateway.jvm.boofcv.io.image.UtilImageIO.loadImage(file_path,boof_type)
    if found is None:
        print file_path
        raise Exception("Can't find image or image format can't be read")
    return found

def ndarray_to_boof( npimg ):
    if npimg is None:
        raise Exception("Input image is None")

    if npimg.dtype == np.uint8:
        b = gateway.jvm.boofcv.struct.image.ImageUInt8()
        b.setData( bytearray(npimg.data) )
    elif npimg.dtype == np.float32:
        b = gateway.jvm.boofcv.struct.image.ImageFloat32()
        b.setData( npimg.data )
    else:
        raise Exception("Image type not supported yet")

    b.setWidth(npimg.shape[1])
    b.setHeight(npimg.shape[0])
    b.setStride(npimg.shape[1])
    return b

def boof_to_ndarray( boof ):
    width = boof.getWidth()
    height = boof.getHeight()
    data = boof.getData()
    nptype = image_type(boof)

    return np.ndarray(shape=(height,width), dtype=nptype, buffer=data)

def gradient_type( boof ):
    dtype = image_type(boof)

    if dtype == np.uint8:
        return np.int16
    elif dtype == np.uint16:
        return np.int32
    elif dtype == np.float32:
        return np.float32
    elif dtype == np.float64:
        return np.float64
    else:
        raise Exception("Unknown type: "+str(dtype))

def create_boof_image( width , height , dtype):
    if dtype == np.uint8:
        return gateway.jvm.boofcv.struct.image.ImageUInt8(width,height)
    elif dtype == np.int8:
        return gateway.jvm.boofcv.struct.image.ImageSInt8(width,height)
    elif dtype == np.uint16:
        return gateway.jvm.boofcv.struct.image.ImageUInt16(width,height)
    elif dtype == np.int16:
        return gateway.jvm.boofcv.struct.image.ImageSInt16(width,height)
    elif dtype == np.float32:
        return gateway.jvm.boofcv.struct.image.ImageFloat32(width,height)
    elif dtype == np.float64:
        return gateway.jvm.boofcv.struct.image.ImageFloat64(width,height)
    else:
        raise Exception("Only uint8 supported")

def image_type( boof ):
    btype = boof.getDataType()
    if btype.isInteger():
        if btype.getNumBits() == 8:
            if btype.isSigned():
                return np.int8
            else:
                return np.uint8
        elif btype.getNumBits() == 16:
            if btype.isSigned():
                return np.int16
            else:
                return np.uint16
        else:
            raise Exception("Number of bits not supported yet")
    else:
        raise Exception("Float images not supported yet")

def dtype_to_single_band_class( dtype ):
    if dtype == np.uint8:
        image = gateway.jvm.boofcv.struct.image.ImageUInt8()
    elif dtype == np.int8:
        image = gateway.jvm.boofcv.struct.image.ImageSInt8()
    elif dtype == np.uint16:
        image = gateway.jvm.boofcv.struct.image.ImageUInt16()
    elif dtype == np.int16:
        image = gateway.jvm.boofcv.struct.image.ImageSInt16()
    elif dtype == np.float32:
        image = gateway.jvm.boofcv.struct.image.ImageFloat32()
    elif dtype == np.float64:
        image = gateway.jvm.boofcv.struct.image.ImageFloat64()
    else:
        raise Exception("Only uint8 supported")

    # Can't access the field directly because class is a restricted keyword in python
    return image.getClass() # TODO Remove this hack.

def dtype_to_image_type( dtype ):
    java_class = dtype_to_single_band_class(dtype)
    return gateway.jvm.boofcv.struct.image.ImageType.single(java_class)

def show_in_java( boof_image , title="Image"):
    gateway.jvm.boofcv.gui.image.ShowImages.showWindow(boof_image,title)