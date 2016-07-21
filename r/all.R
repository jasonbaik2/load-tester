scriptDir = "C:/Users/HCC5fkv/workspace/load-tester/r"
setwd(scriptDir)

source("preprocess.R")
source("latency_distribution.R")
source("throughput.R")
source("gc.R")

###############################################################################
# Files

# # Cyclic
# reportDir = "D:/report/n_1_publish_1_1_subscribe_cyclic_2016_07_02_13_55_52"
# setupDuration = 150 # Time b/w the 1st manually triggered full GC and the 1st published message

# # Cyclic SSL
# reportDir = "D:/report/n_1_publish_1_1_subscribe_cyclic_ssl_2016_07_02_18_53_07"
# setupDuration = 230 # Time b/w the 1st manually triggered full GC and the 1st published message

# # Random
# reportDir = "D:/report/n_1_publish_1_1_subscribe_random_2016_07_02_16_44_14"
# setupDuration = 154 # Time b/w the 1st manually triggered full GC and the 1st published message

# # Random SSL
# reportDir = "D:/report/n_1_publish_1_1_subscribe_random_ssl_2016_07_03_08_22_15"
# setupDuration = 242 # Time b/w the 1st manually triggered full GC and the 1st published message

# # Burst
# reportDir = "D:/report/n_1_publish_1_1_subscribe_burst_2016_07_02_17_31_38"
# setupDuration = 152 # Time b/w the 1st manually triggered full GC and the 1st published message

# Burst SSL
reportDir = "D:/report/n_1_publish_1_1_subscribe_burst_ssl_2016_07_03_10_03_34"
setupDuration = 230 # Time b/w the 1st manually triggered full GC and the 1st published message

# # Network 1 Spoke
# reportDir = "D:/report/n_1_publish_1_1_subscribe_burst_networked_1_spoke_2016_07_03_11_19_58"
# setupDuration = 343 # Time b/w the 1st manually triggered full GC and the 1st published message

# Network 2 Spoke
# reportDir = "D:/report/n_1_publish_1_1_subscribe_burst_networked_2_spoke_2016_07_04_07_25_32"
# setupDuration = 125 # Time b/w the 1st manually triggered full GC and the 1st published message

# # Network 3 Spoke
# reportDir = "D:/report/n_1_publish_1_1_subscribe_burst_networked_3_spoke_2016_07_04_08_07_09"
# setupDuration = 104 # Time b/w the 1st manually triggered full GC and the 1st published message

# Network 4 Spoke
# reportDir = "D:/report/n_1_publish_1_1_subscribe_burst_networked_4_spoke_2016_07_04_08_50_44"
# setupDuration = 40 # Time b/w the 1st manually triggered full GC and the 1st published message

# # Network Complete - 2 Spoke
# reportDir = "D:/report/n_1_publish_1_1_subscribe_burst_networked_complete_2_spoke_2016_07_06_11_08_46"
# setupDuration = 103 # Time b/w the 1st manually triggered full GC and the 1st published message

# # Network Complete - 3 Spoke - 1
# reportDir = "D:/report/n_1_publish_1_1_subscribe_burst_networked_complete_3_spoke_2016_07_05_23_57_19"
# setupDuration = 67 # Time b/w the 1st manually triggered full GC and the 1st published message

# # Network Complete - 3 Spoke - 2
# reportDir = "D:/report/n_1_publish_1_1_subscribe_burst_networked_complete_3_spoke_2016_07_06_12_51_12"
# setupDuration = 66 # Time b/w the 1st manually triggered full GC and the 1st published message

# # Network Complete - 4 Spoke
# reportDir = "D:/report/n_1_publish_1_1_subscribe_burst_networked_complete_4_spoke_2016_07_05_16_34_10"
# setupDuration = 65 # Time b/w the 1st manually triggered full GC and the 1st published message


sendIds = c("send1", "send2", "send3", "send4")
receiveId = "receive1"

sendFiles = vector(length = length(sendIds))

for (i in seq_along(sendIds)) {
  sendFiles[i] = paste(sep="/", sendIds[i], "RoundRobinMQTTPublisher_MQTT_Flight_Data.csv")
}

brokerFile = paste(sep="/", receiveId, "MQTTReplyingJMSConsumer_JMS_In_Times.csv")
receiveFiles = paste(sep="/", receiveId, "SynchronousMQTTReplyingJMSConsumer_MQTT_Flight_Data.csv")

###############################################################################

sendData <- mergeFileData(sendFiles)
sendData <- merge(sendData, read.csv(paste(sep="/", reportDir, brokerFile)), by="MessageId")
sendData$JMSActiveMQBrokerInTime = (sendData$JMSActiveMQBrokerInTime - min(sendData$PubTimeMillis)) * 10^6

receiveData <- mergeFileData(receiveFiles)
allData <- mergeFileData(c(sendFiles, receiveFiles))

period = 1
startTime = 0
pubTimeMinMillis = min(allData$PubTimeMillis)

pubTimeMinNano = pubTimeMinMillis * 10^6
pubCompTimeMaxNano = max(allData$PubTimeMillis * 10^6 + allData$PubCompReceiveTime - allData$PubTime)

endTime = pubCompTimeMaxNano - pubTimeMinNano
durationNanos = endTime - startTime
duration = round(durationNanos / 10^9, digits=1)

x = seq(from = -setupDuration, to = ceiling(endTime / 10^9) + 1, by = period)
xMarks = seq(from = -setupDuration, to = ceiling(endTime / 10^9) + 1, by = ceiling(endTime / 10^9) / 20)
xMarks = sort(c(xMarks,0))


# Zoom
xlim = c(min(x),max(x))
# xlim = c(1444,1600)
duration = xlim[2] - xlim[1]
sendDataSubset = subset(sendData, xlim[1] * 10^9 < sendData$PubTime & sendData$PubTime < xlim[2] * 10^9)
receiveDataSubset = subset(receiveData, xlim[1] * 10^9 < receiveData$PubTime & receiveData$PubTime < xlim[2] * 10^9)

# Plot
attach(mtcars)
# layout(matrix(c(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16), 8, 2, byrow = TRUE), widths=c(3,2))
layout(matrix(c(1,2,3,4,5,6,7,8,9,10,11,12,13,14), 7, 2, byrow = TRUE), widths=c(3,2))
# layout(matrix(c(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24), 12, 2, byrow = TRUE), widths=c(3,2))
par(mar=c(2,4,2,1))
par(oma = c(0,0,3,0))

plotThroughput(sendData$PubTime, "Pub Rate (msg/s)", xlim)
frame()
mtext(text = paste("# Connections:", 20000), line = 0, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("# Published Messages:", length(sendDataSubset$PubTime)), line = -1, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("QoS:", 2), line = -2, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("Max Latency:", round(max(sendDataSubset$PubCompReceiveTime - sendDataSubset$PubTime) / 10^9, digits = 3), "s"), line = -3, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("99.9% Latency:", round(quantile(sendDataSubset$PubCompReceiveTime - sendDataSubset$PubTime, probs = c(0.999)) / 10^9, digits = 3)[1], "s"), line = -4, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("Mean Latency:", round(mean(sendDataSubset$PubCompReceiveTime - sendDataSubset$PubTime)/10^9, digits = 3), "s"), line = -5, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("Max Queue-In Time:", round(max(sendDataSubset$JMSActiveMQBrokerInTime - sendDataSubset$PubTime) / 10^9, digits = 3), "s"), line = -6, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("99.9% Queue-In Time:", round(quantile(sendDataSubset$JMSActiveMQBrokerInTime - sendDataSubset$PubTime, probs = c(0.999)) / 10^9, digits = 3)[1], "s"), line = -7, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("Mean Queue-In Time:", round(mean(sendDataSubset$JMSActiveMQBrokerInTime - sendDataSubset$PubTime)/10^9, digits = 3), "s"), line = -8, at = 0.1, adj = 0, cex=0.8)

plotThroughput(sendData$JMSActiveMQBrokerInTime, "Queue In Rate (msg/s)", xlim)
plotLatencyDistribution(sendDataSubset$JMSActiveMQBrokerInTime - sendDataSubset$PubTime, main = "Latency Distribution (%)")
plotThroughput(sendData$PubRecReceiveTime, "Pub Rec Rate (msg/s)", xlim)
plotLatencyDistribution(sendDataSubset$PubRecReceiveTime - sendDataSubset$PubTime, main = "Latency Distribution (%)")
plotThroughput(sendData$PubCompReceiveTime, "Pub Comp Rate (msg/s)", xlim)
plotLatencyDistribution(sendDataSubset$PubCompReceiveTime - sendDataSubset$PubTime, main = "Latency Distribution (%)")

plotThroughput(receiveData$PubTime, "Reply Pub Rate (msg/s)", xlim)
frame()
mtext(text = paste("# Connections:", 20000), line = 0, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("# Received Messages:", length(receiveDataSubset$PubTime)), line = -1, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("QoS:", 2), line = -2, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("Max Latency:", round(max(receiveDataSubset$PubCompReceiveTime - receiveDataSubset$PubTime) / 10^9, digits = 3), "s"), line = -3, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("99.9% Latency:", round(quantile(receiveDataSubset$PubCompReceiveTime - receiveDataSubset$PubTime, probs = c(0.999)) / 10^9, digits = 3)[1], "s"), line = -4, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("Mean Latency:", round(mean(receiveDataSubset$PubCompReceiveTime - receiveDataSubset$PubTime)/10^9, digits = 3), "s"), line = -5, at = 0.1, adj = 0, cex=0.8)
plotThroughput(receiveData$PubRecReceiveTime, "Reply Pub Rec Rate (msg/s)", xlim)
plotLatencyDistribution(receiveDataSubset$PubRecReceiveTime - receiveDataSubset$PubTime, main = "Latency Distribution (%)")
plotThroughput(receiveData$PubCompReceiveTime, "Reply Comp Rate (msg/s)", xlim)
plotLatencyDistribution(receiveDataSubset$PubCompReceiveTime - receiveDataSubset$PubTime, main = "Latency Distribution (%)")

# gcData <- read.csv(paste(sep="/", reportDir, "hub/gc.log"))
# fullGcData <- read.csv(paste(sep="/", reportDir, "hub/full_gc.log"))
# 
# # The 1st full GC is triggered manually to mark the start of the test. Normalize with respect to that
# gcData$time = gcData$time - min(fullGcData$time)
# fullGcData$time = fullGcData$time - min(fullGcData$time)
# gcData <- subset(gcData, gcData$time >= 0 && gcData$time <= (endTime / 10^9 + setupDuration))
# fullGcData <- subset(fullGcData, fullGcData$time <= (endTime / 10^9 + setupDuration))
# 
# plotGC(gcData, fullGcData, xlim, setupDuration)
# frame()
# mtext(text = paste("# Young GC:", length(gcData$time)), line = 0, at = 0.1, adj = 0, cex=0.8)
# mtext(text = paste("# Full GC:", length(fullGcData$time)-1), line = -1, at = 0.1, adj = 0, cex=0.8)
# mtext(text = paste("Max Heap Size:", round(max(c(max(gcData$heapBefore),max(fullGcData$heapBefore)))/1024, digits=2), "MB"), line = -2, at = 0.1, adj = 0, cex=0.8)

mtext("N-to-1 Pub, 1-to-1 Sub", outer = TRUE, cex = 1, line=1)