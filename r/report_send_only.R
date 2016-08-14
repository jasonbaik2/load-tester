report <- function (reportDir, reportFile, sends, receives, threadFiles, gcFiles, fullGcFiles, startTimeEpochMillis, xMin = -.Machine$integer.max, xMax = .Machine$integer.max) {
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
    sendMQTTFlightFiles[i] = paste(sep="/", reportDir, sends[i], "MQTT_Flight_Data.csv")
    connectionFiles[i] = paste(sep="/", reportDir, sends[i], "Connection_Stats.csv")
  }
  
  #########################
  # Merge data files
  #########################
  
  sendData <- mergeMQTTFlightFiles(sendMQTTFlightFiles)
  connectionData <- mergeFiles(connectionFiles, "conn_comp")
  
  #########################
  # Filter out messages that didn't make the queue, didn't get received, or completed
  #########################
  
  sendDataSubsetQueueIn = subset(sendData, sendData$JMSActiveMQBrokerInTime > 0)
  sendDataSubsetRec = subset(sendData, sendData$PubRecReceiveTime > 0)
  sendDataSubsetComp = subset(sendData, sendData$PubCompReceiveTime > 0)
  
  #########################
  # Normalize
  #########################
  
  sendDataSubset = adjust(sendData, -startTimeEpochMillis)
  sendDataSubsetQueueIn = adjust(sendDataSubsetQueueIn, -startTimeEpochMillis)
  sendDataSubsetRec = adjust(sendDataSubsetRec, -startTimeEpochMillis)
  sendDataSubsetComp = adjust(sendDataSubsetComp, -startTimeEpochMillis)
  
  connectionData$conn_init = connectionData$conn_init - startTimeEpochMillis
  connectionData$conn_comp = connectionData$conn_comp - startTimeEpochMillis
  connectionData$sub_comp = connectionData$sub_comp - startTimeEpochMillis
  
  #########################
  # Set plot ranges
  #########################
  
  endTimeNanos = max(sendDataSubset$PubTime, sendDataSubset$PubRecReceiveTime, sendDataSubset$PubRelSendTime, sendDataSubset$PubCompReceiveTime, connectionData$conn_init*10^6, subset(connectionData$conn_comp*10^6, !is.na(connectionData$conn_comp)), subset(connectionData$sub_comp*10^6, !is.na(connectionData$sub_comp)))

  endTime = ceiling(endTimeNanos / 10^9)
  
  avgPeriod = 1
  
  x = seq(from = 0, to = endTime, by = avgPeriod)
  xMarks = seq(from = 0, to = endTime + 1, by = endTime / 20)
  xlim = c(max(min(x), xMin), min(max(x), xMax))
  
  sendDataSubset = subset(sendDataSubset, xlim[1] * 10^9 < sendDataSubset$PubTime & sendDataSubset$PubTime < xlim[2] * 10^9)
  
  #########################
  # Plot Throughput and Latency
  #########################
  
  pubRecLatencies = sendDataSubsetRec$PubRecReceiveTime - sendDataSubsetRec$PubTime
  pubCompLatencies = sendDataSubsetComp$PubCompReceiveTime - sendDataSubsetComp$PubTime
  
  pdf(reportFile, width=8.5, height=14)
  attach(mtcars)
  layout(matrix(c(1,2,3,4,5,6,7,8,9,10,11,12,13,14), 7, 2, byrow = TRUE), widths=c(3,2))
  par(mar=c(2,4,2,2))
  par(oma = c(0,0,3,0))
  par(lwd=0.75)
  
  plotThroughput(sendDataSubset$PubTime, "Pub Rate (msg/s)", x, xlim, xMarks, avgPeriod)
  frame()
  mtext(text = paste("QoS:", 2), line = 0, at = 0.1, adj = 0, cex=0.8)
  mtext(text = paste("# Published:", length(sendDataSubset$PubTime)), line = -1, at = 0.1, adj = 0, cex=0.8)
  mtext(text = paste("# Completed:", length(sendDataSubsetComp$PubTime)), line = -2, at = 0.1, adj = 0, cex=0.8)
  mtext(text = paste("# Consumed:", length(sendDataSubsetQueueIn$PubTime)), line = -3, at = 0.1, adj = 0, cex=0.8)
  
  plotThroughput(sendDataSubsetRec$PubRecReceiveTime, "Pub Rec Rate (msg/s)", x, xlim, xMarks, avgPeriod)
  plotLatencyDistribution(pubRecLatencies, main = "Latency Distribution (%)")
  plotThroughput(sendDataSubsetComp$PubCompReceiveTime, "Pub Comp Rate (msg/s)", x, xlim, xMarks, avgPeriod)
  plotLatencyDistribution(pubCompLatencies, main = "Latency Distribution (%)")
  
  mtext("N-to-1 Pub, 1-to-1 Sub", outer = TRUE, cex = 1, line=1)
  
  attach(mtcars)
  layout(matrix(c(1,2,3,4,5,6,7,8,9,10,11,12,13,14), 7, 2, byrow = TRUE), widths=c(3,2))
  par(mar=c(2,4,2,2))
  par(oma = c(0,0,3,0))
  
  #########################
  # Plot Connections
  #########################

  connectionDataSuccess = subset(connectionData, !is.na(connectionData$conn_comp & connectionData$conn_comp / 10^3 < xlim[2]))

  # Connection count scatter plot
  connectionDataSuccess = connectionDataSuccess[with(connectionDataSuccess, order(conn_comp)),]
  connectionDataSuccess[["count"]] <- vector(length = length(connectionDataSuccess$conn_comp))
  connectionDataSuccess$count[1] = 1

  for (i in seq(from=2, to=length(connectionDataSuccess$conn_comp))) {
    connectionDataSuccess$count[i] = connectionDataSuccess$count[i-1] + 1
  }

  conn_comp_second = connectionDataSuccess$conn_comp / 10^3

  plot(conn_comp_second, format(connectionDataSuccess$count, scientific = FALSE), main="Connection Trends", xaxt = 'n', xlim = xlim, xlab="Second (s)", ylab="# Established Connections", cex = 0.1, pch=3)
  axis(1, at=xMarks, labels=format(xMarks, scientific=FALSE, digits = 0))
  abline(h = max(connectionDataSuccess$count), col="red", cex = 0.1)
  text(xlim[2] * 0.2, max(connectionDataSuccess$count), labels=paste("Completed =", max(connectionDataSuccess$count), "/", length(connectionData$conn_init)), pos=1)

  par(new=TRUE)

  # Connection count histogram every 1/100 of the test duration
  breakpoint = xlim[2]/50
  h = hist(conn_comp_second, breaks=seq(from = 0, to = xlim[2], by = breakpoint), plot = FALSE)
  plot(h, freq=TRUE, main=NULL, xlab=NULL, ylab=NULL, axes=FALSE)
  text(h$mids, h$counts, h$counts, adj = c(.5, -.5), col = "blue", cex=0.5)
  countMarks = seq(from=0, to=ceiling(max(h$counts)), by=500)
  axis(4, at=countMarks, labels=format(countMarks, scientific=FALSE))

  plotLatencyDistribution((connectionDataSuccess$conn_comp - connectionDataSuccess$conn_init) * 10^6, main = "Connection Latencies (s)")

  #########################
  # Plot Subscriptions
  #########################

  subscriptionDataSuccess = subset(connectionDataSuccess, !is.na(connectionDataSuccess$sub_comp & connectionDataSuccess$sub_comp / 10^3 < xlim[2]))

  # Connection count scatter plot
  subscriptionDataSuccess = subscriptionDataSuccess[with(subscriptionDataSuccess, order(sub_comp)),]
  subscriptionDataSuccess[["subCount"]] <- vector(length = length(subscriptionDataSuccess$sub_comp))
  subscriptionDataSuccess$subCount[1] = 1

  for (i in seq(from=2, to=length(subscriptionDataSuccess$sub_comp))) {
    subscriptionDataSuccess$subCount[i] = subscriptionDataSuccess$subCount[i-1] + 1
  }

  sub_comp_second = subscriptionDataSuccess$sub_comp / 10^3

  plot(sub_comp_second, format(subscriptionDataSuccess$subCount, scientific = FALSE), main="Subscription Trends", xaxt = 'n', xlim = xlim, xlab="Second (s)", ylab="# Established Subscriptions", cex = 0.1, pch=3)
  axis(1, at=xMarks, labels=format(xMarks, scientific=FALSE, digits = 0))
  abline(h = max(subscriptionDataSuccess$subCount), col="red", cex = 0.1)
  text(xlim[2] * 0.2, max(subscriptionDataSuccess$subCount), labels=paste("Completed =", max(subscriptionDataSuccess$subCount), "/", length(connectionDataSuccess$conn_comp)), pos=1)

  par(new=TRUE)

  # Histogram every 1/100 of the test duration
  breakpoint = xlim[2]/50
  h = hist(sub_comp_second, breaks=seq(from = 0, to = xlim[2], by = breakpoint), plot = FALSE)
  plot(h, freq=TRUE, main=NULL, xlab=NULL, ylab=NULL, axes=FALSE)
  text(h$mids, h$counts, h$counts, adj = c(.5, -.5), col = "blue", cex=0.5)
  countMarks = seq(from=0, to=ceiling(max(h$counts)), by=500)
  axis(4, at=countMarks, labels=format(countMarks, scientific=FALSE))

  plotLatencyDistribution((subscriptionDataSuccess$sub_comp - subscriptionDataSuccess$conn_comp) * 10^6, main = "Subscription Latencies (s)")

  #########################
  # Plot Thread Count
  #########################
  
  for (i in seq_along(threadFiles)) {
    threadCountData <- read.csv(paste(sep="/", reportDir, "BrokerThreadCountCollector", threadFiles[i]))
    threadCountData$time <- (threadCountData$time - startTimeEpochMillis) / 10^3
    threadCountData <- subset(threadCountData, xlim[1] <= threadCountData$time & threadCountData$time <= xlim[2])
    
    plot(threadCountData$time, threadCountData$threadCount, main = paste("Active Threads -", threadCountData$Broker_ID[1]), ylab = "# Threads", xaxt='n', xlim = xlim, type = "l", cex = 0.1)
    axis(1, at=xMarks, labels=format(xMarks, scientific=FALSE, digits = 0))
    frame()
    mtext(text = paste("Max:", max(threadCountData$threadCount)), line = 0, at = 0.1, adj = 0, cex=0.8)
  }
  
  dev.off()
}

setwd(dir = "C:/Users/HCC5fkv/git_repo/load-tester-github/load-tester/r")
reportDir = "D:/report/tcp_vs_nio_thread_growth_nio_2016_08_13_03_21_54"
startTime = "2016-08-13 03:19:27,360"

sends = c("send1")#, "send2", "send3", "send4")
receives = c()#"receive1","receive2","receive3","receive4")
threadFiles = c("Broker_Thread_Count_Stats_172.31.5.252.csv")
gcFiles = c()#"spoke1_gc.log")#,"spoke2_gc.log","spoke3_gc.log","spoke4_gc.log")
fullGcFiles = c()#"spoke1_full_gc.log")#,"spoke2_full_gc.log","spoke3_full_gc.log","spoke4_full_gc.log")
startTimeEpochMillis = as.numeric(as.POSIXlt(startTime)) * 10^3
xMin = -.Machine$integer.max
xMax = .Machine$integer.max

report(reportDir, paste(sep="/", reportDir, "report.pdf"), sends, receives, threadFiles, gcFiles, fullGcFiles, startTimeEpochMillis, xMin, xMax)