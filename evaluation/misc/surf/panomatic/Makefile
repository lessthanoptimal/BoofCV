# Indicates compilator to use
CC      = g++

DIR = /home/pja/projects/thirdparty/panomatic-0.9.4/libsurf

# Specifies compilator options
CFLAGS  = -O3 -Wall `pkg-config --cflags opencv` -I$(DIR) -D LINUX
LDFLAGS = 
LDLIBS  = `pkg-config --libs opencv`

# Files extensions .cpp, .o
SUFFIXES = .cpp .o 
.SUFFIXES: $(SUFFIXES) .

# Name of the main program
DESC  = describe_pan


# Object files .o necessary to build the main program
FOO  = common_panomatic.o $(DIR)/libsurf.a 

all: $(DESC)

# Compilation and link
$(DESC): describe_panomatic.o common_panomatic.o
	$(CC) $(LDFLAGS) -o $(DESC)  describe_panomatic.o $(FOO) $(LDLIBS)


%.o: %.cpp
	$(CC) $(CFLAGS) -c $< -o $@

clean:
	-rm -f $(DESC) 
	-rm -f *.o
