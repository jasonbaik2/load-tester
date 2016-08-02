#!/bin/bash

LOG=$(readlink -f "$1")
LOGPATH=$(dirname "$LOG")
cd $LOGPATH

PREFIX=$2
GC_FILE="$PREFIX"_gc.log
FULL_GC_FILE="$PREFIX"_full_gc.log
TRUNCATED_KARAF_FILE="$PREFIX"_karaf_trunc.log

echo "Formatting the GC log file $LOG to $GC_FILE and $FULL_GC_FILE"

#tail -n +4 $LOG > $TRUNCATED_KARAF_FILE
tail -n +0 $LOG > $TRUNCATED_KARAF_FILE
sed -i 's/JBossA-MQ:karaf@root> //' $TRUNCATED_KARAF_FILE

echo time,youngBefore,youngAfter,youngTotal,heapBefore,heapAfter,heapTotal,duration > $GC_FILE
echo time,youngBefore,youngAfter,youngTotal,oldBefore,oldAfter,oldTotal,heapBefore,heapAfter,heapTotal,permBefore,permAfter,permTotal,duration > $FULL_GC_FILE

awk '/.*\[GC.*/ {print}' $TRUNCATED_KARAF_FILE >> $GC_FILE
awk '/.*\[Full GC.*/ {print}' $TRUNCATED_KARAF_FILE >> $FULL_GC_FILE

sed -i 's/^\(.*\): \[GC.*PSYoungGen: \(.*\)K->\(.*\)K(\(.*\)K).* \(.*\)K->\(.*\)K(\(.*\)K), \(.*\) secs.*Times.*/\1,\2,\3,\4,\5,\6,\7,\8/' $GC_FILE
sed -i 's/^\(.*\): \[Full GC.*PSYoungGen: \(.*\)K->\(.*\)K(\(.*\)K).*ParOldGen: /\1,\2,\3,\4,/' $FULL_GC_FILE
sed -i 's/\(.*\)K->\(.*\)K(\(.*\)K)\] \(.*\)K->\(.*\)K(\(.*\)K) \[PSPermGen:/\1,\2,\3,\4,\5,\6,/' $FULL_GC_FILE
sed -i 's/ \(.*\)K->\(.*\)K(\(.*\)K)\], \(.*\) secs\] \[Times.*/\1,\2,\3,\4/' $FULL_GC_FILE