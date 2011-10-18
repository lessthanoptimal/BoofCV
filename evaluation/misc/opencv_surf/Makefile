SRC = describe_opencv.cpp
TARG = describe_opencv

CC = g++

CPPFLAGS = -g -Wall -Wno-unused-function `pkg-config --cflags opencv`

LDFLAGS = `pkg-config --libs opencv`

OBJ = $(SRC:.cpp=.o)

all: $(TARG)

clean: rm -f *~ *.o $(TARG)
