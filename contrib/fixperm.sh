#!/bin/sh

# A script to fix permissions for lock files on Mac OS X
# Contributed by Dmitry Markman <dimitry.markman@verizon.net>
# Fri Aug 23 15:46:46 MDT 2002

curruser=`sudo id -p | grep 'login' | sed 's/login.//'`

if [ ! -d /var/spool/uucp ]
then
sudo mkdir /var/spool/uucp
fi

sudo chgrp uucp /var/spool/uucp
sudo chmod 775 /var/spool/uucp
if [ ! `sudo niutil -readprop / /groups/uucp users | grep $curruser > 
/dev/null` ]
then
  sudo niutil -mergeprop / /groups/uucp users $curruser
fi

