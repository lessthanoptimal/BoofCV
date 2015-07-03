import os
import numpy as np
import pyboof.common as common
from pyboof import gateway

class Family:
    """
    Enum for the family which the image data type belongs in.

    Equivalent to boofcv.struct.image.ImageType.Family
    """
    SINGLE_BAND = 0,
    MULTI_SPECTRAL = 1
    INTERLEAVED = 2

class ImageType(common.JavaWrapper):
    """
    Description on the image data format.

    Equivalent to boofcv.struct.image.ImageType
    """
    def __init__(self, jImageType):
        self.set_java_object(jImageType)

    def convert_java(self):
        jImageClass = dtype_to_Class_SingleBand(self.dtype)
        if self.family == Family.SINGLE_BAND:
            return gateway.jvm.boofcv.struct.image.ImageType.single(jImageClass)
        elif self.family == Family.MULTI_SPECTRAL:
            return gateway.jvm.boofcv.struct.image.ImageType.ms(self.num_bands,jImageClass)
        elif self.family == Family.INTERLEAVED:
            return gateway.jvm.boofcv.struct.image.ImageType.interleaved(self.num_bands,jImageClass)
        else:
            raise Exception("Unknown family")

    def create_boof_image(self, width, height ):
        return self.convert_java().createImage(width,height)

    def get_family(self):
        return self.java_obj.getFamily().ordinal()

    def get_num_bands(self):
        return self.java_obj.getNumBands()

    def get_dtype(self):
        return JImageDataType_to_dtype(self.java_obj)

def create_ImageType( family, dtype, num_bands = 1 ):
    if family == Family.SINGLE_BAND and num_bands != 1:
        raise Exception("SingleBand images must have only one band")

    jImageClass = dtype_to_Class_SingleBand(dtype)
    if family == Family.SINGLE_BAND:
        jImageType = gateway.jvm.boofcv.struct.image.ImageType.single(jImageClass)
    elif family == Family.MULTI_SPECTRAL:
        jImageType = gateway.jvm.boofcv.struct.image.ImageType.ms(num_bands,jImageClass)
    elif family == Family.INTERLEAVED:
        jImageType = gateway.jvm.boofcv.struct.image.ImageType.interleaved(num_bands,jImageClass)
    else:
        raise Exception("Unknown family")

    return ImageType(jImageType)


def load_single_band( path , dtype ):
    """
    Loads a singe band BoofCV image
    :param path: File path to image
    :param dtype: The data type of the image
    :return:
    """
    file_path = os.path.abspath(path)

    boof_type = dtype_to_Class_SingleBand(dtype)
    found = gateway.jvm.boofcv.io.image.UtilImageIO.loadImage(file_path,boof_type)
    if found is None:
        print file_path
        raise Exception("Can't find image or image format can't be read")
    return found

def load_multi_spectral( path , dtype ):
    file_path = os.path.abspath(path)

    buffered_image =  gateway.jvm.boofcv.io.image.UtilImageIO.loadImage(file_path)
    num_bands = buffered_image.getRaster().getNumBands()
    multi_spectral = create_multi_spectral(buffered_image.getWidth(),buffered_image.getHeight(),num_bands,dtype)
    gateway.jvm.boofcv.io.image.ConvertBufferedImage.convertFrom(buffered_image,multi_spectral,True)

    return multi_spectral

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
    nptype = JImageDataType_to_dtype(boof)

    return np.ndarray(shape=(height,width), dtype=nptype, buffer=data)

def gradient_type( boof ):
    dtype = JImageDataType_to_dtype(boof)

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

def create_single_band( width , height , dtype):
    """
    Creates a single band BoofCV image.

    :param width: Image width
    :param height: Image height
    :param dtype: data type
    :return: New instance of a BoofCV single band image
    """
    if dtype == np.uint8:
        return gateway.jvm.boofcv.struct.image.ImageUInt8(width,height)
    elif dtype == np.int8:
        return gateway.jvm.boofcv.struct.image.ImageSInt8(width,height)
    elif dtype == np.uint16:
        return gateway.jvm.boofcv.struct.image.ImageUInt16(width,height)
    elif dtype == np.int16:
        return gateway.jvm.boofcv.struct.image.ImageSInt16(width,height)
    elif dtype == np.int32:
        return gateway.jvm.boofcv.struct.image.ImageSInt32(width,height)
    elif dtype == np.int64:
        return gateway.jvm.boofcv.struct.image.ImageSInt64(width,height)
    elif dtype == np.float32:
        return gateway.jvm.boofcv.struct.image.ImageFloat32(width,height)
    elif dtype == np.float64:
        return gateway.jvm.boofcv.struct.image.ImageFloat64(width,height)
    else:
        raise Exception("Only uint8 supported")

def create_multi_spectral( width , height , num_bands , dtype):
    """
    Creates a MultiSpectral BoofCV image.

    :param width: Image width
    :param height: Image height
    :param num_bands: Number of bands in the image
    :param dtype: data type
    :return: New instance of a BoofCV single band image
    """

    jImageClass = dtype_to_Class_SingleBand(dtype)

    return gateway.jvm.boofcv.struct.image.MultiSpectral(jImageClass,width,height,num_bands)

def get_dtype( boof_image ):
    """
    Given a BoofCV image return the dtype which matches the storage format of its pixels
    :param boof_image: A BoofCV Java image
    :return: The NumPy dtype
    """
    return JImageDataType_to_dtype(boof_image.getImageType().getDataType())

def JImageDataType_to_dtype( ImageDataType ):
    """
    Given an instance of the BoofCV ImageType object return the dtype which represents its
    internal data
    :param ImageType:
    :return:
    """
    if ImageDataType.isInteger():
        if ImageDataType.getNumBits() == 8:
            if ImageDataType.isSigned():
                return np.int8
            else:
                return np.uint8
        elif ImageDataType.getNumBits() == 16:
            if ImageDataType.isSigned():
                return np.int16
            else:
                return np.uint16
        elif ImageDataType.getNumBits() == 32:
            if ImageDataType.isSigned():
                return np.int32
            else:
                raise Exception("Unsigned 32bit data isn't supported")
        elif ImageDataType.getNumBits() == 64:
            if ImageDataType.isSigned():
                return np.int64
            else:
                raise Exception("Unsigned 64bit data isn't supported")
        else:
            raise Exception("Number of bits not supported for ints. "+str(ImageDataType.getNumBits()))
    else:
       if ImageDataType.getNumBits() == 32:
           return np.float32
       elif ImageDataType.getNumBits() == 64:
           return np.float64
       else:
           raise Exception("Number of bits not supported for floats. "+str(ImageDataType.getNumBits()))

def dtype_to_ImageDataType( dtype ):
    if dtype == np.uint8:
        return gateway.jvm.boofcv.struct.image.ImageDataType.U8
    elif dtype == np.int8:
        return gateway.jvm.boofcv.struct.image.ImageDataType.S8
    elif dtype == np.uint16:
        return gateway.jvm.boofcv.struct.image.ImageDataType.U16
    elif dtype == np.int16:
        return gateway.jvm.boofcv.struct.image.ImageDataType.S16
    elif dtype == np.int32:
        return gateway.jvm.boofcv.struct.image.ImageDataType.S32
    elif dtype == np.int64:
        return gateway.jvm.boofcv.struct.image.ImageDataType.S64
    elif dtype == np.float32:
        return gateway.jvm.boofcv.struct.image.ImageDataType.F32
    elif dtype == np.float64:
        return gateway.jvm.boofcv.struct.image.ImageDataType.F64
    else:
        raise Exception("No BoofCV equivalent")

def family_to_Java_Family( family ):
    if family == Family.SINGLE_BAND:
        return gateway.jvm.boofcv.struct.image.ImageType.Family.SINGLE_BAND
    elif family == Family.MULTI_SPECTRAL:
        return gateway.jvm.boofcv.struct.image.ImageType.Family.MULTI_SPECTRAL
    elif family == Family.INTERLEAVED:
        return gateway.jvm.boofcv.struct.image.ImageType.Family.INTERLEAVED
    else:
        raise Exception("Unknown family. "+str(family))

def dtype_to_Class_SingleBand( dtype ):
    if dtype == np.uint8:
        image = gateway.jvm.boofcv.struct.image.ImageUInt8()
    elif dtype == np.int8:
        image = gateway.jvm.boofcv.struct.image.ImageSInt8()
    elif dtype == np.uint16:
        image = gateway.jvm.boofcv.struct.image.ImageUInt16()
    elif dtype == np.int16:
        image = gateway.jvm.boofcv.struct.image.ImageSInt16()
    elif dtype == np.int32:
        image = gateway.jvm.boofcv.struct.image.ImageSInt32()
    elif dtype == np.int64:
        image = gateway.jvm.boofcv.struct.image.ImageSInt64()
    elif dtype == np.float32:
        image = gateway.jvm.boofcv.struct.image.ImageFloat32()
    elif dtype == np.float64:
        image = gateway.jvm.boofcv.struct.image.ImageFloat64()
    else:
        raise Exception("No BoofCV equivalent. "+str(dtype))

    # Can't access the field directly because class is a restricted keyword in python
    return image.getClass() # TODO Remove this hack.

def ImageDataType_to_dtype( jdatatype ):
    if jdatatype == gateway.jvm.boofcv.struct.image.ImageDataType.U8:
        return np.uint8
    elif jdatatype == gateway.jvm.boofcv.struct.image.ImageDataType.S8:
        return np.int8
    elif jdatatype == gateway.jvm.boofcv.struct.image.ImageDataType.U16:
        return np.uint16
    elif jdatatype == gateway.jvm.boofcv.struct.image.ImageDataType.S16:
        return np.int16
    elif jdatatype == gateway.jvm.boofcv.struct.image.ImageDataType.S32:
        return np.int32
    elif jdatatype == gateway.jvm.boofcv.struct.image.ImageDataType.S64:
        return np.int64
    elif jdatatype == gateway.jvm.boofcv.struct.image.ImageDataType.F32:
        return np.float32
    elif jdatatype == gateway.jvm.boofcv.struct.image.ImageDataType.F64:
        return np.float64
    else:
        raise Exception("Unknown ImageDataType. "+str(jdatatype))

def ClassSingleBand_to_dtype( jclass ):
    jdatatype = gateway.jvm.boofcv.struct.image.ImageDataType.classToType(jclass)
    return ImageDataType_to_dtype(jdatatype)

def dtype_to_ImageType( dtype ):
    java_class = dtype_to_Class_SingleBand(dtype)
    return gateway.jvm.boofcv.struct.image.ImageType.single(java_class)

def show_in_java( boof_image , title="Image"):
    gateway.jvm.boofcv.gui.image.ShowImages.showWindow(boof_image,title)