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

AUTOMAKE_OPTIONS = foreign
INCLUDES= -I $(TOP) -I $(DEST) -I . $(JAVAINCLUDE) $(JAVANATINC)
####################### vars for rxtx-1.4->1.5 transition
JARTARGET=comm.jar
CLASSDIR=javax.comm
CLASSTOP=javax
CLASSDEST=javax/comm
####################### tools
LIBTOOLC=$(TOP)/libtool --mode=compile $(CC) $(INCLUDES) $(CPPFLAGS) $(CFLAGS) $(VERBOSE_IOEXCEPTIONS) -c
LIBTOOL_INST=$(TOP)/libtool --mode=install $(INSTALL_PROGRAM)
LINK =$(TOP)/libtool --mode=link $(CC) $(CFLAGS) $(LDFLAGS) -version-info 1:0:1
####################### dirs
SPATH = $(srcdir)/src
DEST=$(target_alias)
####################### rules
CLASSES = $(patsubst $(SPATH)/%.java,$(TOP)/$(CLASSDEST)/%.class,$(wildcard $(SPATH)/*.java))
javafiles = $(wildcard $(SPATH)/*.java)
javahfiles = $(patsubst $(SPATH)/%.java,%,$(wildcard $(SPATH)/*.java))
#######################
CLEANFILES = $(TOP)/*.class $(TOP)/$(CLASSDEST)/*.class $(TOP)/$(JARTARGET)

#  compile src/SerialImp.c and simular files into libraries.
$(target_alias)/lib%.la: $(SPATH)/%Imp.@EXTENSION@ $(CLASSES)
	@( \
	$(LIBTOOL_FIX) \
	cd $(DEST); \
	$(LIBTOOLC) ../$(SPATH)/$*Imp.@EXTENSION@ -o $(TOP)/$(DEST)/$*Imp.lo; \
	$(LINK) -o lib$*.la -rpath $(RXTX_PATH) $(TOP)/$(DEST)/$*Imp.lo)

#  compile src/RXTXPort.java and simular files into classes
#  then create the .jar file
$(CLASSES): $(javafiles) $(TOP)/Makefile
	$(JAVAH_FIX)
	$(JAVAC) $(javafiles)
	@$(JAVAH) `(for i in $(javahfiles);do \
		if grep -q native.*\( $(SPATH)/$$i.java; then \
			echo $(CLASSDIR).$$i; \
		fi; \
	done)`
	$(JAR) cf $(JARTARGET) $(CLASSTOP)/*

#  install libSerial.so into the proper directory and copy $(JARTARGET) to its
#  proper location
install: all
	@$(LIBTOOL_INST) \
	`for i in $(TARGETLIB);do \
		if [ -f $$i ];then \
			echo $$i; \
		fi; \
	done` $(RXTX_PATH)
	$(INSTALL_PROGRAM) $(JARTARGET) $(JHOME)/

clean-generic:
	rm -rf $(DEST) $(TOP)/$(CLASSTOP)
	-test -z "$(CLEANFILES)" || rm -f $(CLEANFILES)

all: $(TARGETLIB)

#  create the javadoc files.
docs: $(CLASSES)
	$(JAVADOC) $(javafiles)

################  WIN32 CrossCompiling from here down #######################

$(DEST)/Serial.dll: $(CLASSES) $(TOP)/Makefile
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(DEST) -I . $(WINDOWS_JAVA_INCLUDE) -I $(SPATH) -Wall -c $(SPATH)/SerialImp.c -o $(DEST)/SerialImp.o
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(DEST) -I . $(WINDOWS_JAVA_INCLUDE) -I $(SPATH) -Wall -c $(SPATH)/termios.c -o $(DEST)/termios.o
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(DEST) -I . $(WINDOWS_JAVA_INCLUDE) -I $(SPATH) -Wall -c $(SPATH)/fixup.c -o $(DEST)/fixup.o
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(DEST) -I . $(WINDOWS_JAVA_INCLUDE) -I $(SPATH) -Wall -c $(SPATH)/init.cc -o $(DEST)/init.o
	ld --base-file $(target_alias)/Serial.base --dll -o $(target_alias)/Serial.dll $(target_alias)/termios.o $(target_alias)/SerialImp.o $(target_alias)/fixup.o $(target_alias)/init.o -luser32 -lgdi32 -lcomdlg32 -lkernel32 -ladvapi32 -lmingw32 -lmoldname -lcrtdll -e _dll_entry@12 --base-file $(target_alias)/Serial.base
	 echo EXPORTS >$(target_alias)/Serial.def;for i in `nm i386-mingw32/Serial.dll | grep "T _Java"|cut -b 13-`;do echo -n $$i|sed s#@.*##;echo "="$$i;done >> $(target_alias)/Serial.def
	dlltool -b$(target_alias) --as=as --dllname $(target_alias)/Serial.dll --def $(target_alias)/Serial.def --base-file $(target_alias)/Serial.base --output-exp $(target_alias)/Serial.exp
	ld --base-file $(target_alias)/Serial.base $(target_alias)/Serial.exp -dll -o $(target_alias)/Serial.dll $(target_alias)/termios.o $(target_alias)/SerialImp.o $(target_alias)/fixup.o $(target_alias)/init.o -luser32 -lgdi32 -lcomdlg32 -lkernel32 -ladvapi32 -lmingw32 -lmoldname -lcrtdll -e _dll_entry@12 --base-file $(target_alias)/Serial.base
	dlltool -b$(target_alias) --as=as --dllname $(target_alias)/Serial.dll --def $(target_alias)/Serial.def --base-file $(target_alias)/Serial.base --output-exp $(target_alias)/Serial.exp
	ld $(target_alias)/Serial.exp -dll -o $(target_alias)/Serial.dll $(target_alias)/termios.o $(target_alias)/SerialImp.o $(target_alias)/fixup.o $(target_alias)/init.o -luser32 -lgdi32 -lcomdlg32 -lkernel32 -ladvapi32 -lmingw32 -lmoldname -lcrtdll -e _dll_entry@12 --base-file $(target_alias)/Serial.base

# END
