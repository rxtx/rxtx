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
JDIR=$(TOP)/javax/comm
JPATH=@JPATH@
INCLUDES= -I $(TOP) -I $(DEST) -I . -I $(JAVAINCLUDE) -I $(JAVANATINC)
NSJAVA=1
THREADS_FLAG=@THREAD_FLAG@
####################### tools
JAVAC=javac $(JCLASSPATH):$(COMM_JAR_PATH):$(JCL_JAR_PATH) -d $(TOP)/ -O 
JAVAH=javah $(JAVA_H_CLASSPATH) -d $(DEST) -jni
JAVA=java $(JCLASSPATH)
JAR=jar 
JAVADOC=javadoc $(JCLASSPATH)
LIBTOOLC=$(SHELL) $(TOP)/libtool --mode=compile $(CC) $(INCLUDES) $(CPPFLAGS) $(CFLAGS) $(VERBOSE_IOEXCEPTIONS) -c
LIBTOOL_INST=$(SHELL) $(TOP)/libtool --mode=install $(INSTALL_PROGRAM) 
LINK = $(SHELL) $(TOP)/libtool --mode=link $(CC) $(CFLAGS) $(LDFLAGS) -version-info 1:0:1
####################### dirs
SPATH = $(srcdir)/src
DEST=$(TOP)/$(target_alias)
LIBS=$(TARGETLIB)
####################### rules
CLASSES = $(patsubst $(SPATH)/%.java,$(TOP)/javax/comm/%.class,$(wildcard $(SPATH)/*.java))
javafiles = $(wildcard $(SPATH)/*.java)
javahfiles = $(patsubst $(SPATH)/%.java,%,$(wildcard $(SPATH)/*.java))
#######################  

#  compile src/SerialImp.c and simular files into libraries.
$(target_alias)/lib%.la: $(SPATH)/%Imp.c 
	@(cd $(DEST); \
	$(LIBTOOLC) ../$(SPATH)/$*Imp.c -o $(DEST)/$*Imp.lo; \
	$(LINK) -o lib$*.la -rpath $(RXTX_DEST) $(DEST)/$*Imp.lo)

#  compile src/RXTXPort.java and simular files into classes
#  then create the .jar file
$(CLASSES): $(javafiles) $(TOP)/Makefile
	$(JAVAC) $(javafiles)
	@$(JAVAH) `(for i in $(javahfiles);do \
		if grep -q native.*\( $(SPATH)/$$i.java; then \
			echo javax.comm.$$i; \
		fi; \
	done)`
	$(JAR) cf comm.jar javax/comm/*

#  install libSerial.so into the proper directory and copy comm.jar to its
#  proper location
install: all
	@$(LIBTOOL_INST) \
	`for i in $(LIBS);do \
		if [ -f $$i ];then \
		echo $$i; \
		fi; \
	done` $(RXTX_DEST) 
	$(INSTALL_PROGRAM) comm.jar $(JPATH)/

clean-generic:
	rm -rf $(DEST) $(TOP)/javax 
	-test -z "$(CLEANFILES)" || rm -f $(CLEANFILES)

all: $(CLASSES) $(LIBS)
	@echo "make install will install RxTx"

#  create the javadoc files.
docs: $(CLASSES)
	$(JAVADOC) $(javafiles)

################  WIN32 CrossCompiling from here down #######################

borg: $(CLASSES)
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(DEST) -I . $(WJI) -I $(SPATH) -Wall -c $(SPATH)/SerialImp.c -o $(TOP)/$(target_alias)/SerialImp.o
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(DEST) -I . $(WJI) -I $(SPATH) -Wall -c $(SPATH)/termios.c -o $(TOP)/$(target_alias)/termios.o
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(DES%) -I . $(WJI) -I $(SPATH) -Wall -c $(SPATH)/fixup.c -o $(TOP)/$(target_alias)/fixup.o
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(DEST) -I . $(WJI) -I $(SPATH)  -Wall -c $(SPATH)/init.cc -o $(DEST)/init.o
	ld --base-file $(target_alias)/Serial.base --dll -o $(target_alias)/Serial.dll $(target_alias)/termios.o $(target_alias)/SerialImp.o $(target_alias)/fixup.o $(target_alias)/init.o -luser32 -lgdi32 -lcomdlg32 -lkernel32 -ladvapi32 -lmingw32 -lmoldname -lcrtdll -e _dll_entry@12 --base-file $(target_alias)/Serial.base
	 echo EXPORTS >$(target_alias)/Serial.def;for i in `nm i386-mingw32/Serial.dll | grep "T _Java"|cut -b 13-`;do echo -n $$i|sed s#@.*##;echo "="$$i;done >> $(target_alias)/Serial.def
	dlltool -b$(target_alias) --as=as --dllname $(target_alias)/Serial.dll --def $(target_alias)/Serial.def --base-file $(target_alias)/Serial.base --output-exp $(target_alias)/Serial.exp
	ld --base-file $(target_alias)/Serial.base $(target_alias)/Serial.exp -dll -o $(target_alias)/Serial.dll $(target_alias)/termios.o $(target_alias)/SerialImp.o $(target_alias)/fixup.o $(target_alias)/init.o -luser32 -lgdi32 -lcomdlg32 -lkernel32 -ladvapi32 -lmingw32 -lmoldname -lcrtdll -e _dll_entry@12 --base-file $(target_alias)/Serial.base
	dlltool -b$(target_alias) --as=as --dllname $(target_alias)/Serial.dll --def $(target_alias)/Serial.def --base-file $(target_alias)/Serial.base --output-exp $(target_alias)/Serial.exp
	ld $(target_alias)/Serial.exp -dll -o $(target_alias)/Serial.dll $(target_alias)/termios.o $(target_alias)/SerialImp.o $(target_alias)/fixup.o $(target_alias)/init.o -luser32 -lgdi32 -lcomdlg32 -lkernel32 -ladvapi32 -lmingw32 -lmoldname -lcrtdll -e _dll_entry@12 --base-file $(target_alias)/Serial.base

# END
