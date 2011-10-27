# Indicates compilator to use
CC      = g++

DIR = /home/pja/projects/thirdparty/opensurf/OpenSURFcpp

# Specifies compilator options
CFLAGS  = -O3 -Wall `pkg-config --cflags opencv` -I$(DIR)/src -D LINUX
LDFLAGS = 
LDLIBS  = `pkg-config --libs opencv`

# Files extensions .cpp, .o
SUFFIXES = .cpp .o 
.SUFFIXES: $(SUFFIXES) .

# Name of the main program
PROG  = desc_surf
RUNTIME = runtime

# Object files .o necessary to build the main program
OBJS  = describe_opensurf.o
ROBJS = runtime_opensurf.o
FOO  = $(DIR)/fasthessian.o $(DIR)/integral.o $(DIR)/surf.o $(DIR)/utils.o $(DIR)/ipoint.o

all: $(PROG) $(RUNTIME)

# Compilation and link
$(PROG): $(OBJS)
	$(CC) $(LDFLAGS) -o $(PROG) $(FOO) $(OBJS) $(LDLIBS)

$(RUNTIME): $(ROBJS)
	$(CC) $(LDFLAGS) -o $(RUNTIME) $(FOO) $(ROBJS) $(LDLIBS)

%.o: %.cpp
	$(CC) $(CFLAGS) -c $< -o $@

clean:
	-rm -f $(PROG)
	-rm -f $(RUNTIME)
	-rm -f *.o
