Summary: RXTX 
Name: rxtx
Version: 1.5
Release: 2
License: LGPL
Group: Development/Libraries
Source: rxtx-%{PACKAGE_VERSION}.tar.gz
URL: www.rxtx.org
Buildroot: /var/tmp/rxtx-root

%description
rxtx is an full implementation of java commapi which aims to support RS232
IEEE 1284, RS485, I2C and RawIO.  This is a developers release. 
%prep
%setup -q

%build
export THREADS_FLAG=native
export CLASSPATH=.:/usr/local/java/lib/jcl.jar:/usr/local/java/lib/comm.jar:/usr/local/java/lib/BlackBox.jar
CFLAGS="$RPM_OPT_FLAGS" LDFLAGS=-s ./configure --prefix=/usr
pwd
make comm

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT/usr/local/java/jre/lib/ext $RPM_BUILD_ROOT/usr/local/java/jre/lib/i386
for n in libI2C.so libRS485.so libSerial.so libParallel.so libRaw.so; do
    install -m 755 -s `uname -m`-pc-linux-gnu/.libs/$n $RPM_BUILD_ROOT/usr/local/java/jre/lib/i386/$n
done
install -m 755 -s comm.jar $RPM_BUILD_ROOT/usr/local/java/jre/lib/ext
install -m 755 -s comm.jar $RPM_BUILD_ROOT/usr/local/java/jre/lib/ext
echo "Driver=gnu.io.RXTXCommDriver" > $RPM_BUILD_ROOT/usr/local/java/jre/lib/javax.comm.properties
%files
%defattr(-,root,root)
%doc AUTHORS ChangeLog README RMISecurityManager.html COPYING INSTALL PORTING TODO
/usr/local/java/jre/lib/ext/comm.jar
/usr/local/java/jre/lib/i386/libSerial.so
/usr/local/java/jre/lib/i386/libParallel.so
/usr/local/java/jre/lib/i386/libI2C.so
/usr/local/java/jre/lib/i386/libRS485.so
/usr/local/java/jre/lib/i386/libRaw.so
/usr/local/java/jre/lib/javax.comm.properties


%clean
rm -rf $RPM_BUILD_ROOT

%changelog
