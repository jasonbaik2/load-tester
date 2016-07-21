#!/bin/bash

tail -n +4 karaf.log > karaf_trunc.log
sed -i 's/JBossA-MQ:karaf@root> //' karaf_trunc.log

echo time,youngBefore,youngAfter,youngTotal,heapBefore,heapAfter,heapTotal,duration > gc.log
echo time,youngBefore,youngAfter,youngTotal,oldBefore,oldAfter,oldTotal,heapBefore,heapAfter,heapTotal,permBefore,permAfter,permTotal,duration > full_gc.log

awk '/.*\[GC.*/ {print}' karaf_trunc.log >> gc.log
awk '/.*\[Full GC.*/ {print}' karaf_trunc.log >> full_gc.log

sed -i 's/^\(.*\):.*\[GC.*PSYoungGen: \(.*\)K->\(.*\)K(\(.*\)K).* \(.*\)K->\(.*\)K(\(.*\)K), \(.*\) secs.*Times.*/\1,\2,\3,\4,\5,\6,\7,\8/' gc.log
sed -i 's/^\(.*\):.*\[Full GC.*PSYoungGen: \(.*\)K->\(.*\)K(\(.*\)K).*ParOldGen: /\1,\2,\3,\4,/' full_gc.log
sed -i 's/\(.*\)K->\(.*\)K(\(.*\)K)\] \(.*\)K->\(.*\)K(\(.*\)K) \[PSPermGen:/\1,\2,\3,\4,\5,\6,/' full_gc.log
sed -i 's/ \(.*\)K->\(.*\)K(\(.*\)K)\], \(.*\) secs\] \[Times.*/\1,\2,\3,\4/' full_gc.log