#!/bin/sh
# Don't use! :)
if [ $1 = "gnu" ]; then
	for i in `find . -name \*`;do cat $i |sed s/javax_comm/javax_comm/g > tmp;mv tmp $i;done
	for i in `find . -name \*`;do cat $i |sed s/javax\.comm/gnu\.io/g > tmp;mv tmp $i;done
elif [ $1 = "javax" ]; then
	for i in `find . -name \*`;do cat $i |sed s/javax_comm/javax_comm/g > tmp;mv tmp $i;done
	for i in `find . -name \*`;do cat $i |sed s/gnu\.io/javax\.comm/g > tmp;mv tmp $i;done
fi
