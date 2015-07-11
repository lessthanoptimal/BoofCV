import cv2
import pyboof as pb
import numpy as np
import time

cap = cv2.VideoCapture(0)

refPt=[(0,0),(0,0)]
state = 0
quad = pb.Quadrilateral2D()
window_name = "Frame"

cv2.namedWindow(window_name)

def click_rect(event, x, y, flags, param):
    #
    global refPt, state

    # Click and drag a rectangle
    if event == cv2.EVENT_LBUTTONDOWN:
        refPt = [(x, y),(x,y)]
        state = 1
    elif event == 0:
        refPt[1] = (x,y)
    elif event == cv2.EVENT_LBUTTONUP:
        state = 2

def pts_to_quad():
    global refPt, quad
    quad.a.set((refPt[0][0],refPt[0][1]))
    quad.b.set((refPt[1][0],refPt[0][1]))
    quad.c.set((refPt[1][0],refPt[1][1]))
    quad.d.set((refPt[0][0],refPt[1][1]))

cv2.setMouseCallback(window_name, click_rect)

tracker = pb.FactoryTrackerObjectQuad(np.uint8).circulant()
# tracker = pb.FactoryTrackerObjectQuad(np.uint8).tld(pb.ConfigTld(False))

# initialize data structures
ret, frame = cap.read()
if not ret:
    print "Failed to read frame"
    exit(-1)
image_input = tracker.getImageType().create_boof_image(frame.shape[1], frame.shape[0])

while True:
    # Capture frame-by-frame
    ret, frame = cap.read()

    time0 = int(round(time.time() * 1000))
    # Convert it into a boofcv image
    boof_color = pb.ndarray_to_boof(frame)
    time1 = int(round(time.time() * 1000))
    # Convert it into the image type required by the tracker
    pb.convert_boof_image(boof_color,image_input)
    time2 = int(round(time.time() * 1000))

    print("time py to boof: "+str(time1-time0)+" boof to boof "+str(time2-time1))

    if state == 1:
        cv2.rectangle(frame, refPt[0], refPt[1], (0, 255, 0), 2)
    elif state == 2:
        pts_to_quad()
        if not tracker.initialize(image_input,quad):
            print "Initialization failed!"
            state = 0
        else:
            print "Success"
            state = 3
    elif state == 3:
        time0 = int(round(time.time() * 1000))
        if not tracker.process(image_input,quad):
            print "Tracking failed!"
        else:
            lines =  np.array(quad.get_tuple_tuple())
            cv2.polylines(frame,np.int32([lines]),True,(255, 0, 0))
        time1 = int(round(time.time() * 1000))
        print("     tracking:   "+str(time1-time0))

    # Display the resulting frame
    cv2.imshow(window_name,frame)
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

# When everything done, release the capture
cap.release()
cv2.destroyAllWindows()