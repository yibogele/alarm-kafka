#!/bin/bash
dir=`dirname $PWD`
echo $dir
LIB=.
for jar in $dir/lib/*.jar
   do
     LIB=$LIB:$jar
   done

LIB=$LIB:$dir/config/
echo $LIB

########## home
#JDWP=-Xdebug -Xrunjdwp:transport=dt_socket,address=9998,server=y,suspend=n
#VM_OPTS="-Xms1g -Xmx1g -XX:+HeapDumpOnOutOfMemoryError"
#GATEWAY_OPTS="-Dname=$dir -Dbase_path=$dir -Ddatadownthorough=xhw_data_down_thorough2 -Ddatadownthoroughtpartionnum=3 -Ddevicedataqueue=device_data_topic -Ddevicefaultqueue=device_fault_topic -Ddeviceeventqueue=device_event_topic -Ddatareportpartionnum=3 -Deventreportpartionnum=3 -Dfaultreportpartionnum=3 -Dsound.gateway.path=$dir/ -Dzkurl=192.168.2.226:2181"
#java -server $VM_OPTS $GATEWAY_OPTS -classpath $LIB: com.sound.cloud.gateway.StartUp

########## ali
#JDWP=-Xdebug -Xrunjdwp:transport=dt_socket,address=9998,server=y,suspend=n
JVM_OPTS="-Xms256m -Xmx1g -XX:+HeapDumpOnOutOfMemoryError"
GATEWAY_OPTS="-Dalarm.gateway.path=$dir/"
JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=8087 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
java -server $JVM_OPTS $GATEWAY_OPTS -classpath $LIB: com.marscloud.alarm.kafka.KafkaClient



