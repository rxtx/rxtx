Summary: RXTX 
Name: rxtx
Version: 1.4.1
Release: 1
License: LGPL
Group: Development/Libraries
Source: rxtx-%{PACKAGE_VERSION}.tar.gz
URL: www.rxtx.org
Buildroot: /var/tmp/rxtx-root

%description
%prep
%setup -q

%build
CFLAGS="$RPM_OPT_FLAGS" LDFLAGS=-s ./configure --prefix=/usr
make comm

%install
rm -rf $RPM_BUILD_ROOT
make install

%files
/usr/local/java/jre/lib/ext/comm.jar
/usr/local/java/jre/lib/i386/libSerial.so
/usr/local/java/jre/lib/i386/libParallel.so
/usr/local/java/jre/lib/i386/libI2C.so
/usr/local/java/jre/lib/i386/libRS485.so
/usr/local/java/jre/lib/javax.comm.properties

%clean
rm -rf $RPM_BUILD_ROOT

%changelog
