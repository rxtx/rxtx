#!/bin/sh
# Don't use! :)
# This script will eat itself if its in the same tree as rxtx.  Just put it
# in some other location.
#
# Usage ../ChangePackage target package
# Example ../ChangePackage gnu
#
# This would change all the files from javax to gnu.
#
# Bash was assumed when this script was writting.  With luck it will work
# in other shells.
#
# More sanity checks could be made but just move the script to the
# directory above rxtx-version,  then cd to .../rxtx-version.  Finally
# call ../ChangePackage javax gnu or the reverse.

if [ ! -n "$1" ]; then
	echo "********************************************************";
	echo "*";  
	echo "*  usage ../ChangePackage target package [gnu or javax]";
	echo "*";  
	echo "*  BIG WARNING:";  
	echo "*";  
	echo "*  Running this script while is is in the rxtx tree";
	echo "*  will ruin the script."
	echo "*";  
	echo "*  This script is not failsafe!";
	echo "*";  
	echo "*  more comments in the script";
	echo "********************************************************";
	exit;
fi

switchit( )
{
	for i in `find . -name \* -type f`;do cat $i | \
		sed s/$1/$2/g > tmpfile;mv tmpfile $i;done
}
fixperms( )
{
	chmod 755 acinclude.m4 aclocal.m4 autogen.sh config.guess config.sub \
		configure install-sh ltconfig missing
}

if [ $1 = "gnu" ]; then
	switchit javax\\/comm gnu\\/io;
	switchit javax_comm gnu_io;
	switchit javax\\.comm gnu\\.io;
	switchit CLASSTOP=javax CLASSTOP=gnu;
	# this one will probably create problems
	for i in `find . -name Makefile\*`;do \
		cat $i | sed s/javax/gnu/g > tmpfile;
		mv tmpfile $i;
	done;
elif [ $1 = "javax" ]; then
	echo
	switchit gnu\\/io javax\\/comm;
	switchit gnu_io javax_comm;
	switchit gnu\\.io javax\\.comm;
	# this one will probably create problems
	for i in `find . -name Makefile\*`;do
		cat $i | sed s/gnu/javax/g > tmpfile;
		mv tmpfile $i;
	done;
fi
fixperms;

