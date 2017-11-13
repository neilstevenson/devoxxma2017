#!/bin/bash
cd /Applications/kafka_2.11-1.0.0/bin

TMPFILE_IN=/tmp/`basename ${0}`.$$.in
TMPFILE_OUT=/tmp/`basename ${0}`.$$.out
ZOOKEEPER=localhost:2181

# See what is registered
echo ls / > $TMPFILE_IN
echo ls /discovery >> $TMPFILE_IN
echo ls /discovery/hazelcast >> $TMPFILE_IN
echo ls /discovery/hazelcast/dev >> $TMPFILE_IN
cat $TMPFILE_IN
bash ./zookeeper-shell.sh $ZOOKEEPER < $TMPFILE_IN > $TMPFILE_OUT
cat $TMPFILE_OUT

# Nodes list is last line, format "[ something, something2, something3 ]
CSV=`cat $TMPFILE_OUT | grep '^\[' | egrep -v 'zookeeper|hazelcast|dev'`

echo ""
if [ `echo $CSV | wc -l` -eq 0 ]
then
 echo No Hazelcast paths registered
else
 echo Hazelcast paths $CSV
 if [ "$CSV" != "[]" ]
 then
  for HAZELCAST_SERVER in `echo $CSV | tr '[' ' ' | tr ']' ' ' | tr ',' ' '`
  do
   echo ""
   echo get /discovery/hazelcast/dev/$HAZELCAST_SERVER > $TMPFILE_IN
   echo =================================================================
   cat $TMPFILE_IN
   echo =================================================================
   cat $TMPFILE_IN | ./zookeeper-shell.sh localhost:2181 
  done
 fi
fi

rm $TMPFILE_IN $TMPFILE_OUT 2> /dev/null
