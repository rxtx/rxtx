#-------------------------------------------------------------------------
#   rxtx is a native interface to serial ports in java.
#   Copyright 1997-2000 by Trent Jarvi trentjarvi@yahoo.com.
#
#   This library is free software; you can redistribute it and/or
#   modify it under the terms of the GNU Library General Public
#   License as published by the Free Software Foundation; either
#   version 2 of the License, or (at your option) any later version.
#
#   This library is distributed in the hope that it will be useful,
#   but WITHOUT ANY WARRANTY; without even the implied warranty of
#   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
#   Library General Public License for more details.
#
#   You should have received a copy of the GNU Library General Public
#   License along with this library; if not, write to the Free
#   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
#-------------------------------------------------------------------------
srcdir = @srcdir@
VPATH = @srcdir@

WJI= -I @WINDOWS_JAVA_INCLUDE@ -I @WINDOWS_JAVA_INCLUDE@/win32 
AUTOMAKE_OPTIONS = foreign
TOP=@TOP@
JPATH=@JPATH@
JCL_PATH=@JCL_PATH@
INCLUDES= -I $(TOP) -I $(TOP)/$(target_alias) -I . -I $(JAVAINCLUDE) -I $(JAVANATINC)
NSJAVA=1
THREADS_FLAG=@THREAD_FLAG@
JAVAC=$(JPATH)/bin/javac $(JCLASSPATH):$(COMM_JAR_PATH):$(JCL_JAR_PATH) -d $(TOP)/ -O 
JAVAH=$(JPATH)/bin/javah $(JAVA_H_CLASSPATH) -d $(TOP)/$(target_alias) -jni
JAVA=$(JPATH)/bin/java $(JCLASSPATH)
JAR=$(JPATH)/bin/jar 
JAVADOC=$(JPATH)/bin/javadoc $(JCLASSPATH)
LIBTOOL = $(SHELL) $(TOP)/libtool
LINK = $(LIBTOOL) --mode=link $(CC) $(CFLAGS) $(LDFLAGS) -version-info 1:0:1
SERIAL_HEADER = $(TOP)/$(target_alias)/javax_comm_RXTXPort.h $(TOP)/$(target_alias)/javax_comm_RXTXCommDriver.h $(TOP)/$(target_alias)/javax_comm_CommPortIdentifier.h
PARALLEL_HEADER = $(TOP)/$(target_alias)/javax_comm_LPRPort.h
I2C_HEADER = $(TOP)/$(target_alias)/javax_comm_I2CPort.h 
Raw_HEADER = $(TOP)/$(target_alias)/javax_comm_RawPort.h 
RS485_HEADER = $(TOP)/$(target_alias)/javax_comm_RS485Port.h
CLEANFILES = $(TOP)/*.class $(TOP)/javax/comm/*.class $(TOP)/jcl.jar
#LTLIBRARIES =     $(lib_LTLIBRARIES)
#BUILT_SOURCES = $(TOP)/$(target_alias)/libSerial.la
SUFFIXES = .java .class
SUFFIXES = .class .h

install: all comm.jar  install-lib 

clean-generic:
	rm -rf $(TOP)/$(target_alias) $(TOP)/javax 
	-test -z "$(CLEANFILES)" || rm -f $(CLEANFILES)
	@echo -e "\n\nMake distclean will remove the remaining files\n\n"
install-lib:
	( cd $(TOP)/$(target_alias) ; \
	if [ -f libSerial.la ];then \
	$(LIBTOOL) --mode=install $(INSTALL_PROGRAM) libSerial.la $(RXTX_PATH);\
	fi; \
	if [ -f libParallel.la ];then \
	$(LIBTOOL) --mode=install $(INSTALL_PROGRAM) libParallel.la $(RXTX_PATH);\
	fi; \
	if [ -f libI2C.la ];then \
	$(LIBTOOL) --mode=install $(INSTALL_PROGRAM) libI2C.la $(RXTX_PATH);\
	fi; \
	if [ -f libRaw.la ];then \
	$(LIBTOOL) --mode=install $(INSTALL_PROGRAM) libRaw.la $(RXTX_PATH);\
	fi; \
	if [ -f libRS485.la ];then \
	$(LIBTOOL) --mode=install $(INSTALL_PROGRAM) libRS485.la $(RXTX_PATH);\
	fi; \
	)
	$(INSTALL_PROGRAM) comm.jar $(JPATH)/$(JCL_PATH)/
run: $(TOP)/test.class all jcl
	(cd $(TOP)/;chmod 755 *.class;\
	LD_LIBRARY_PATH="$(target_alias)";\
	$(JAVA) test)

all: @IDENTIFIERS@ @TARGETLIB@
	@echo -e "\n\nIf you have javax.comm.* and java.comm.* in your CLASSPATH, type make jcl to"
	@echo -e "build the javacomm20-x86.tar.Z supporting classes.\n"
	@echo "Note: CLASSPATH was grabbed during the configure stage.  Modification of the"
	@echo "environment variable will not change the behavior of make without rerunning" 
	@echo  -e "configure or editing the Makefile. \n"

jcl:  $(TOP)/javax/comm/RXTXCommDriver.class $(TOP)/javax/comm/RXTXPort.class $(TOP)/javax/comm/RXTXPort.class @IDENTIFIERS@
	$(JAR) cf jcl.jar javax/comm/*
	@echo "make install will install RxTx"

comm:  all
	$(JAR) cf comm.jar javax/comm/*
	@echo "make install will install RxTx"

$(TOP)/javax/comm/RXTXCommDriver.class $(TOP)/javax/comm/RXTXPort.class \
		$(TOP)/javax/comm/LPRPort.class \
		: $(srcdir)/src/RXTXCommDriver.java \
		$(srcdir)/src/LPRPort.java \
		$(srcdir)/src/RXTXPort.java 
# handle java quirks here.
	$(JAVAH_FIX) \
	$(JAVAC) $(srcdir)/src/RXTXPort.java $(srcdir)/src/LPRPort.java $(srcdir)/src/RXTXCommDriver.java

$(TOP)/$(target_alias)/libParallel.la: $(TOP)/$(target_alias)/ParallelImp.lo
	( cd $(TOP)/$(target_alias); \
	$(LINK) -o libParallel.la -rpath $(RXTX_PATH) $(TOP)/$(target_alias)/ParallelImp.lo;)

$(TOP)/$(target_alias)/ParallelImp.lo: $(srcdir)/src/ParallelImp.c \
                                  $(PARALLEL_HEADER) \
				  $(TOP)/Makefile
	(cd $(TOP)/$(target_alias); \
	$(LIBTOOL) --mode=compile $(CC) $(INCLUDES) $(CPPFLAGS) $(CFLAGS) @VERBOSE_IOEXCEPTIONS@ -c ../$(srcdir)/src/ParallelImp.c -o $(TOP)/$(target_alias)/ParallelImp.lo; )

$(TOP)/$(target_alias)/libSerial.la: $(TOP)/$(target_alias)/SerialImp.lo
	( cd $(TOP)/$(target_alias); \
	$(LINK) -o libSerial.la -rpath $(RXTX_PATH) $(TOP)/$(target_alias)/SerialImp.lo; )

$(TOP)/$(target_alias)/SerialImp.lo: $(srcdir)/src/SerialImp.c \
                                  $(SERIAL_HEADER) \
				  $(TOP)/Makefile
	(cd $(TOP)/$(target_alias); \
	$(LIBTOOL) --mode=compile $(CC) $(INCLUDES) $(CPPFLAGS) $(CFLAGS) @VERBOSE_IOEXCEPTIONS@ -c ../$(srcdir)/src/SerialImp.c -o $(TOP)/$(target_alias)/SerialImp.lo; )

$(TOP)/$(target_alias)/libI2C.la: $(TOP)/$(target_alias)/I2CImp.lo
	( cd $(TOP)/$(target_alias); \
	$(LINK) -o libI2C.la -rpath $(RXTX_PATH) $(TOP)/$(target_alias)/I2CImp.lo; )


$(TOP)/$(target_alias)/I2CImp.lo: $(srcdir)/src/I2CImp.c \
                                  $(I2C_HEADER) \
				  $(TOP)/Makefile
	(cd $(TOP)/$(target_alias); \
	$(LIBTOOL) --mode=compile $(CC) $(INCLUDES) $(CPPFLAGS) $(CFLAGS) @VERBOSE_IOEXCEPTIONS@ -c ../$(srcdir)/src/I2CImp.c -o $(TOP)/$(target_alias)/I2CImp.lo; )

$(TOP)/$(target_alias)/libRS485.la: $(TOP)/$(target_alias)/RS485Imp.lo
	( cd $(TOP)/$(target_alias); \
	$(LINK) -o libRS485.la -rpath $(RXTX_PATH) $(TOP)/$(target_alias)/RS485Imp.lo; )

$(TOP)/$(target_alias)/libRaw.la: $(TOP)/$(target_alias)/RawImp.lo
	( cd $(TOP)/$(target_alias); \
	$(LINK) -o libRaw.la -rpath $(RXTX_PATH) $(TOP)/$(target_alias)/RawImp.lo; )

$(TOP)/$(target_alias)/RawImp.lo: $(srcdir)/src/RawImp.c \
                                  $(Raw_HEADER) \
				  $(TOP)/Makefile
	(cd $(TOP)/$(target_alias); \
	$(LIBTOOL) --mode=compile $(CC) $(INCLUDES) $(CPPFLAGS) $(CFLAGS) @VERBOSE_IOEXCEPTIONS@ -c ../$(srcdir)/src/RawImp.c -o $(TOP)/$(target_alias)/RawImp.lo; )

$(TOP)/$(target_alias)/RS485Imp.lo: $(srcdir)/src/RS485Imp.c \
                                  $(RS485_HEADER) \
				  $(TOP)/Makefile
	(cd $(TOP)/$(target_alias); \
	$(LIBTOOL) --mode=compile $(CC) $(INCLUDES) $(CPPFLAGS) $(CFLAGS) @VERBOSE_IOEXCEPTIONS@ -c ../$(srcdir)/src/RS485Imp.c -o $(TOP)/$(target_alias)/RS485Imp.lo;)

$(TOP)/javax/comm/ParallelPort.class $(TOP)/javax/comm/ParallelPortEvent.class $(TOP)/javax/comm/ParallelPortEventListener.class: $(srcdir)/src/ParallelPort.java $(srcdir)/src/ParallelPortEvent.java $(srcdir)/src/ParallelPortEventListener.java
	$(JAVAC) $(srcdir)/src/ParallelPort.java $(srcdir)/src/ParallelPortEvent.java $(srcdir)/src/ParallelPortEventListener.java

PARALLEL: $(TOP)/javax/comm/ParallelPort.class $(TOP)/javax/comm/ParallelPortEvent.class $(TOP)/javax/comm/ParallelPortEventListener.class

$(TOP)/javax/comm/SerialPort.class $(TOP)/javax/comm/SerialPortEvent.class $(TOP)/javax/comm/SerialPortEventListener.class: $(srcdir)/src/SerialPort.java $(srcdir)/src/SerialPortEvent.java $(srcdir)/src/SerialPortEventListener.java
	$(JAVAC) $(srcdir)/src/SerialPort.java $(srcdir)/src/SerialPortEvent.java $(srcdir)/src/SerialPortEventListener.java

RS232: $(TOP)/javax/comm/SerialPort.class $(TOP)/javax/comm/SerialPortEvent.class $(TOP)/javax/comm/SerialPortEventListener.class

$(TOP)/javax/comm/RS485Port.class $(TOP)/javax/comm/RS485PortEvent.class $(TOP)/javax/comm/RS485PortEventListener.class: $(srcdir)/src/RS485Port.java $(srcdir)/src/RS485PortEvent.java $(srcdir)/src/RS485PortEventListener.java 
	$(JAVAC) $(srcdir)/src/RS485Port.java $(srcdir)/src/RS485PortEvent.java $(srcdir)/src/RS485PortEventListener.java

RS485: $(TOP)/javax/comm/RS485Port.class $(TOP)/javax/comm/RS485PortEvent.class $(TOP)/javax/comm/RS485PortEventListener.class

$(TOP)/javax/comm/I2CPort.class $(TOP)/javax/comm/I2CPortEvent.class $(TOP)/javax/comm/I2CPortEventListener.class: $(srcdir)/src/I2CPort.java $(srcdir)/src/I2CPortEvent.java $(srcdir)/src/I2CPortEventListener.java 
	$(JAVAC) $(srcdir)/src/I2CPort.java $(srcdir)/src/I2CPortEvent.java $(srcdir)/src/I2CPortEventListener.java

I2C: $(TOP)/javax/comm/I2CPort.class $(TOP)/javax/comm/I2CPortEvent.class $(TOP)/javax/comm/I2CPortEventListener.class 

$(TOP)/javax/comm/RawPort.class $(TOP)/javax/comm/RawPortEvent.class $(TOP)/javax/comm/RawPortEventListener.class: $(srcdir)/src/RawPort.java $(srcdir)/src/RawPortEvent.java $(srcdir)/src/RawPortEventListener.java 
	$(JAVAC) $(srcdir)/src/RawPort.java $(srcdir)/src/RawPortEvent.java $(srcdir)/src/RawPortEventListener.java

Raw: $(TOP)/javax/comm/RawPort.class $(TOP)/javax/comm/RawPortEvent.class $(TOP)/javax/comm/RawPortEventListener.class 


$(TOP)/javax/comm/PortInUseException.class $(TOP)/javax/comm/CommPortEnumerator.class $(TOP)/javax/comm/CommPortIdentifier.class $(TOP)/javax/comm/CommPort.class $(TOP)/javax/comm/CommDriver.class $(TOP)/CommPortOwnershipListener.class $(TOP)/NoSuchPortException.class: $(srcdir)/src/CommPortEnumerator.java $(srcdir)/src/CommPortIdentifier.java $(srcdir)/src/CommPort.java $(srcdir)/src/CommDriver.java $(srcdir)/src/CommPortOwnershipListener.java $(srcdir)/src/PortInUseException.java $(srcdir)/src/NoSuchPortException.java
	$(JAVAC) $(srcdir)/src/CommPortEnumerator.java $(srcdir)/src/CommPortIdentifier.java $(srcdir)/src/CommPort.java $(srcdir)/src/CommDriver.java $(srcdir)/src/CommPortOwnershipListener.java $(srcdir)/src/PortInUseException.java $(srcdir)/src/NoSuchPortException.java

IDENT: $(TOP)/javax/comm/CommPortEnumerator.class $(TOP)/javax/comm/CommPortIdentifier.class

$(TOP)/%.class $(TOP)/javax/comm/%.class: $(srcdir)/src/%.java 
	$(JAVAC) $< 

$(I2C_HEADER) $(Raw_HEADER) $(RS485_HEADER) $(SERIAL_HEADER) $(PARALLEL_HEADER): $(TOP)/javax/comm/RXTXPort.class $(TOP)/javax/comm/CommPortIdentifier.class
	$(JAVAH) javax.comm.`echo $(@F) | sed s#\.h##|sed s#javax_comm_##`
	touch $(TOP)/$(target_alias)/$(@F)

borg: $(TOP)/javax/comm/RXTXCommDriver.class $(TOP)/javax/comm/RXTXPort.class $(TOP)/javax/comm/LPRPort.class $(SERIAL_HEADER)
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(TOP)/$(target_alias) -I . $(WJI) -I ../src/ -Wall -c ../src/SerialImp.c -o $(TOP)/$(target_alias)/SerialImp.o
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(TOP)/$(target_alias) -I . $(WJI) -I ../src/ -Wall -c ../src/termios.c -o $(TOP)/$(target_alias)/termios.o
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(TOP)/$(target_alias) -I . $(WJI) -I ../src/ -Wall -c ../src/fixup.c -o $(TOP)/$(target_alias)/fixup.o
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(TOP)/$(target_alias) -I . $(WJI) -I ../src/ -Wall -c ../src/init.cc -o $(TOP)/$(target_alias)/init.o
	ld --base-file $(target_alias)/Serial.base --dll -o $(target_alias)/Serial.dll $(target_alias)/termios.o $(target_alias)/SerialImp.o $(target_alias)/fixup.o $(target_alias)/init.o -luser32 -lgdi32 -lcomdlg32 -lkernel32 -ladvapi32 -lmingw32 -lmoldname -lcrtdll -e _dll_entry@12 --base-file $(target_alias)/Serial.base
	 echo EXPORTS >$(target_alias)/Serial.def;for i in `nm i386-mingw32/Serial.dll | grep "T _Java"|cut -b 13-`;do echo -n $$i|sed s#@.*##;echo "="$$i;done >> $(target_alias)/Serial.def
	dlltool -b$(target_alias) --as=as --dllname $(target_alias)/Serial.dll --def $(target_alias)/Serial.def --base-file $(target_alias)/Serial.base --output-exp $(target_alias)/Serial.exp
	ld --base-file $(target_alias)/Serial.base $(target_alias)/Serial.exp -dll -o $(target_alias)/Serial.dll $(target_alias)/termios.o $(target_alias)/SerialImp.o $(target_alias)/fixup.o $(target_alias)/init.o -luser32 -lgdi32 -lcomdlg32 -lkernel32 -ladvapi32 -lmingw32 -lmoldname -lcrtdll -e _dll_entry@12 --base-file $(target_alias)/Serial.base
	dlltool -b$(target_alias) --as=as --dllname $(target_alias)/Serial.dll --def $(target_alias)/Serial.def --base-file $(target_alias)/Serial.base --output-exp $(target_alias)/Serial.exp
	ld $(target_alias)/Serial.exp -dll -o $(target_alias)/Serial.dll $(target_alias)/termios.o $(target_alias)/SerialImp.o $(target_alias)/fixup.o $(target_alias)/init.o -luser32 -lgdi32 -lcomdlg32 -lkernel32 -ladvapi32 -lmingw32 -lmoldname -lcrtdll -e _dll_entry@12 --base-file $(target_alias)/Serial.base

docs:
	mkdir javax;
	cd javax;ln -s $(srcdir)/src io
	cd ..
	rm -rf javax

# END
