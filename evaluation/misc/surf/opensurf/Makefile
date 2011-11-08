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
RDESC = runtime_describe
RDET  = runtime_detect
RALL  = runtime_all

# Object files .o necessary to build the main program
FOO  = $(DIR)/fasthessian.o $(DIR)/integral.o $(DIR)/surf.o $(DIR)/utils.o $(DIR)/ipoint.o

all: $(PROG) $(RDESC) $(RDET) $(RALL)

# Compilation and link
$(PROG): describe_opensurf.o
	$(CC) $(LDFLAGS) -o $(PROG) $(FOO) describe_opensurf.o $(LDLIBS)

$(RDESC): runtime_opensurf.o
	$(CC) $(LDFLAGS) -o $(RDESC) $(FOO) runtime_opensurf.o $(LDLIBS)
$(RDET): runtime_detect_opensurf.o
	$(CC) $(LDFLAGS) -o $(RDET) $(FOO) runtime_detect_opensurf.o $(LDLIBS)
$(RALL): runtime_all_opensurf.o
	$(CC) $(LDFLAGS) -o $(RALL) $(FOO) runtime_all_opensurf.o $(LDLIBS)

%.o: %.cpp
	$(CC) $(CFLAGS) -c $< -o $@

clean:
	-rm -f $(PROG) $(RDESC) $(RDET)
	-rm -f *.o
