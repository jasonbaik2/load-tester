report <- function (reportDir, reportFile, sends, receives, gcFiles, fullGcFiles, startTimeEpochMillis, xMin = -.Machine$integer.max, xMax = .Machine$integer.max) {
  source("preprocess.R")
  source("latency_distribution.R")
  source("throughput.R")
  source("gc.R")

  #########################
  # Read data files
  #########################
  
  sendMQTTFlightFiles = vector(length = length(sends))
  connectionFiles = vector(length = length(sends))
  
  for (i in seq_along(sends)) {
    sendMQTTFlightFiles[i] = paste(sep="/", reportDir, sends[i], "RoundRobinMQTTPublisher_MQTT_Flight_Data.csv")
    connectionFiles[i] = paste(sep="/", reportDir, sends[i], "Connection_Establishment_Times.csv")
  }
  
  receiveMQTTFlightFiles = vector(length = length(receives))
  brokerJMSInTimeFiles = vector(length = length(receives))
  
  for (i in seq_along(receives)) {
    receiveMQTTFlightFiles[i] = paste(sep="/", reportDir, receives[i], "SynchronousMQTTReplyingJMSConsumer_MQTT_Flight_Data.csv")
    brokerJMSInTimeFiles[i] = paste(sep="/", reportDir, receives[i], "MQTTReplyingJMSConsumer_JMS_In_Times.csv")
  }
  
  #########################
  # Merge data files
  #########################
  
  sendData <- mergeMQTTFlightFiles(sendMQTTFlightFiles)
  brokerJMSInTimeData <- mergeFiles(brokerJMSInTimeFiles, "JMSActiveMQBrokerInTime", startTimeEpochMillis)
  brokerJMSInTimeData$JMSActiveMQBrokerInTime = brokerJMSInTimeData$JMSActiveMQBrokerInTime * 10^6
  
  sendData <- merge(sendData, brokerJMSInTimeData, by="MessageId", all.x=TRUE, incomparables = -1)
  receiveData <- mergeMQTTFlightFiles(receiveMQTTFlightFiles)
  
  connectionData <- mergeFiles(connectionFiles, "Connection_Establishment_Times", startTimeEpochMillis)
  connectionData$Connection_Establishment_Times <- connectionData$Connection_Establishment_Times / 10^3
  
  #########################
  # Filter out messages that didn't make the queue, didn't get received, or completed
  #########################
  
  sendDataSubsetQueueIn = subset(sendData, sendData$JMSActiveMQBrokerInTime > 0)
  sendDataSubsetRec = subset(sendData, sendData$PubRecReceiveTime > 0)
  sendDataSubsetComp = subset(sendData, sendData$PubCompReceiveTime > 0)
  
  receiveDataSubsetRec = subset(receiveData, receiveData$PubRecReceiveTime > 0)
  receiveDataSubsetComp = subset(receiveData, receiveData$PubCompReceiveTime > 0)
  
  #########################
  # Normalize
  #########################
  
  sendDataSubset = adjust(sendData, -startTimeEpochMillis)
  sendDataSubsetQueueIn = adjust(sendDataSubsetQueueIn, -startTimeEpochMillis)
  sendDataSubsetRec = adjust(sendDataSubsetRec, -startTimeEpochMillis)
  sendDataSubsetComp = adjust(sendDataSubsetComp, -startTimeEpochMillis)

  receiveDataSubset = adjust(receiveData, -startTimeEpochMillis)
  receiveDataSubsetRec = adjust(receiveDataSubsetRec, -startTimeEpochMillis)
  receiveDataSubsetComp = adjust(receiveDataSubsetComp, -startTimeEpochMillis)
  
  #########################
  # Set plot ranges
  #########################

  endTimeNanos = max(sendDataSubset$PubTime, sendDataSubset$PubRecReceiveTime, sendDataSubset$PubRelSendTime, sendDataSubset$PubCompReceiveTime, receiveDataSubset$PubTime, receiveDataSubset$PubRecReceiveTime, receiveDataSubset$PubRelSendTime, receiveDataSubset$PubCompReceiveTime)
  endTime = ceiling(endTimeNanos / 10^9)

  avgPeriod = 1
  
  x = seq(from = 0, to = endTime, by = avgPeriod)
  xMarks = seq(from = 0, to = endTime + 1, by = endTime / 20)
  xlim = c(max(min(x), xMin), min(max(x), xMax))

  sendDataSubset = subset(sendDataSubset, xlim[1] * 10^9 < sendDataSubset$PubTime & sendDataSubset$PubTime < xlim[2] * 10^9)
  
  #########################
  # Plot Throughput and Latency
  #########################
  
  queueInLatencies = sendDataSubsetQueueIn$JMSActiveMQBrokerInTime - sendDataSubsetQueueIn$PubTime
  pubRecLatencies = sendDataSubsetRec$PubRecReceiveTime - sendDataSubsetRec$PubTime
  pubCompLatencies = sendDataSubsetComp$PubCompReceiveTime - sendDataSubsetComp$PubTime
  
  replyPubRecLatencies = receiveDataSubsetRec$PubRecReceiveTime - receiveDataSubsetRec$PubTime
  replyPubCompLatencies = receiveDataSubsetComp$PubCompReceiveTime - receiveDataSubsetComp$PubTime
  
  pdf(reportFile, width=8.5, height=14)
  attach(mtcars)
  layout(matrix(c(1,2,3,4,5,6,7,8,9,10,11,12,13,14), 7, 2, byrow = TRUE), widths=c(3,2))
  par(mar=c(2,4,2,1))
  par(oma = c(0,0,3,0))
  par(lwd=0.75)
  
  plotThroughput(sendDataSubset$PubTime, "Pub Rate (msg/s)", x, xlim, xMarks, avgPeriod)
  frame()
  mtext(text = paste("QoS:", 2), line = 0, at = 0.1, adj = 0, cex=0.8)
  mtext(text = paste("# Published:", length(sendDataSubset$PubTime)), line = -1, at = 0.1, adj = 0, cex=0.8)
  mtext(text = paste("# Completed:", length(sendDataSubsetComp$PubTime)), line = -2, at = 0.1, adj = 0, cex=0.8)
  mtext(text = paste("# Consumed:", length(sendDataSubsetQueueIn$PubTime)), line = -3, at = 0.1, adj = 0, cex=0.8)
  
  plotThroughput(sendDataSubsetQueueIn$JMSActiveMQBrokerInTime, "Queue In Rate (msg/s)", x, xlim, xMarks, avgPeriod)
  plotLatencyDistribution(queueInLatencies, main = "Latency Distribution (%)")
  plotThroughput(sendDataSubsetRec$PubRecReceiveTime, "Pub Rec Rate (msg/s)", x, xlim, xMarks, avgPeriod)
  plotLatencyDistribution(pubRecLatencies, main = "Latency Distribution (%)")
  plotThroughput(sendDataSubsetComp$PubCompReceiveTime, "Pub Comp Rate (msg/s)", x, xlim, xMarks, avgPeriod)
  plotLatencyDistribution(pubCompLatencies, main = "Latency Distribution (%)")
  
  plotThroughput(receiveDataSubset$PubTime, "Reply Pub Rate (msg/s)", x, xlim, xMarks, avgPeriod)
  frame()
  mtext(text = paste("QoS:", 2), line = 0, at = 0.1, adj = 0, cex=0.8)
  mtext(text = paste("# Published:", length(receiveDataSubset$PubTime)), line = -1, at = 0.1, adj = 0, cex=0.8)
  mtext(text = paste("# Completed:", length(receiveDataSubsetComp$PubTime)), line = -2, at = 0.1, adj = 0, cex=0.8)
  
  plotThroughput(receiveDataSubsetRec$PubRecReceiveTime, "Reply Pub Rec Rate (msg/s)", x, xlim, xMarks, avgPeriod)
  plotLatencyDistribution(replyPubRecLatencies, main = "Latency Distribution (%)")
  plotThroughput(receiveDataSubsetComp$PubCompReceiveTime, "Reply Comp Rate (msg/s)", x, xlim, xMarks, avgPeriod)
  plotLatencyDistribution(replyPubCompLatencies, main = "Latency Distribution (%)")

  mtext("N-to-1 Pub, 1-to-1 Sub", outer = TRUE, cex = 1, line=1)

  attach(mtcars)
  layout(matrix(c(1,2,3,4,5,6,7,8,9,10), 5, 2, byrow = TRUE), widths=c(3,2))
  par(mar=c(2,4,2,1))
  par(oma = c(0,0,3,0))

  #########################
  # Plot Connections
  #########################
  
  # Connection count scatter plot
  connectionData$Connection_Establishment_Times = sort(connectionData$Connection_Establishment_Times)
  connectionData[["count"]] <- vector(length = length(connectionData))
  
  for (i in seq(from=2, to=length(connectionData$Connection_Establishment_Times))) {
    connectionData$count[i] = connectionData$count[i-1] + 1
  }
  
  plot(connectionData$Connection_Establishment_Times, format(connectionData$count, scientific = FALSE), main="Connection Trends", xaxt = 'n', xlim = xlim, xlab="Second (s)", ylab="# Established Connections", cex = 0.1, pch=3)
  axis(1, at=xMarks, labels=format(xMarks, scientific=FALSE, digits = 0))
  
  par(new=TRUE)
  
  # Connection count histogram every 1/100 of the test duration
  breakpoint = max(x)/50
  h = hist(connectionData$Connection_Establishment_Times, breaks=seq(from = 0, to = max(x), by = breakpoint), plot = FALSE)
  plot(h, freq=TRUE, main=NULL, xlab=NULL, ylab=NULL, axes=FALSE)
  text(h$mids, h$counts, h$counts, adj = c(.5, -.5), col = "blue", cex=0.5)
  countMarks = seq(from=0, to=ceiling(max(h$counts)), by=500)
  axis(4, at=countMarks, labels=format(countMarks, scientific=FALSE))
  
  frame()
  mtext(text = paste("Max # Connections:", max(connectionData$count)), line = 0, at = 0.1, adj = 0, cex=0.8)

  #########################
  # Plot GC
  #########################

  for (i in seq_along(gcFiles)) {
    gcData <- read.csv(paste(sep="/", reportDir, gcFiles[i]))
    gcData$time <- (as.numeric(as.POSIXlt(strptime(gcData$time, format="%Y-%m-%dT%H:%M:%OS%z"))) * 1000 - startTimeEpochMillis) / 1000
    gcData <- subset(gcData, xlim[1] * 10^3 <= gcData$time & gcData$time <= xlim[2] * 10^3)
    
    fullGcData <- read.csv(paste(sep="/", reportDir, fullGcFiles[i]))
    fullGcData$time <-  (as.numeric(as.POSIXlt(strptime(fullGcData$time, format="%Y-%m-%dT%H:%M:%OS%z"))) * 1000 - startTimeEpochMillis) / 1000
    fullGcData <- subset(fullGcData, xlim[1] * 10^3 <= fullGcData$time & fullGcData$time <= xlim[2] * 10^3)
    
    plotGC(gcData, fullGcData, xlim, xMarks)
    frame()
    mtext(text = paste("# Young GC:", length(gcData$time)), line = 0, at = 0.1, adj = 0, cex=0.8)
    mtext(text = paste("# Full GC:", length(fullGcData$time)), line = -1, at = 0.1, adj = 0, cex=0.8)
    mtext(text = paste("Max Heap Size:", round(max(c(max(gcData$heapBefore),max(fullGcData$heapBefore)))/1024, digits=2), "MB"), line = -2, at = 0.1, adj = 0, cex=0.8)
  }
  
  dev.off()
}

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

# # Burst SSL
# reportDir = "D:/report/n_1_publish_1_1_subscribe_burst_ssl_2016_07_03_10_03_34"
# setupDuration = 230 # Time b/w the 1st manually triggered full GC and the 1st published message

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

# # Network Complete Random - 4 Spoke
# reportDir = "D:/report/n_1_publish_1_1_subscribe_random_networked_2016_07_27_01_05_56"
# setupDuration = 65

# # Max Connections
# reportDir = "D:/report/n_1_publish_1_1_subscribe_max_connections_2016_07_28_14_51_38"

# Max connections 40000-50
reportDir = "D:/report/n_1_publish_1_1_subscribe_max_connections_2016_08_01_23_41_51"
startTime = "2016-08-01 22:57:39,676"

setwd(dir = "C:/Users/HCC5fkv/git_repo/load-tester-github/load-tester/r")

sends = c("send1", "send2", "send3", "send4")
receives = c("receive1","receive2","receive3","receive4")
gcFiles = c("spoke1_gc.log")#,"spoke2_gc.log","spoke3_gc.log","spoke4_gc.log")
fullGcFiles = c("spoke1_full_gc.log")#,"spoke2_full_gc.log","spoke3_full_gc.log","spoke4_full_gc.log")
startTimeEpochMillis = as.numeric(as.POSIXlt(startTime)) * 10^3

report(reportDir, paste(sep="/", reportDir, "report.pdf"), sends, receives, gcFiles, fullGcFiles, startTimeEpochMillis)