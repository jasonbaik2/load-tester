reportDir = "D:/report/n_1_publish_1_1_subscribe_1465178878185"
file = "3178b79f-1aae-4ef8-b9ab-344c3bc6fb13_MQTTReplyingJMSConsumer_JMS_In_Times.csv"
data <- read.csv(paste(sep="/", reportDir, file))

numMsgs = length(data$JMSActiveMQBrokerInTime)

startTime = min(data$JMSActiveMQBrokerInTime)
endTime = max(data$JMSActiveMQBrokerInTime)
numMillis = endTime - startTime

###########################################################
# Plot throughput per Every m Messages
###########################################################
m = 3000
x <- (m + 1):numMsgs
messageMarks = seq(from = 0 , to = numMsgs, by = 10^3)
sortedReceiveTime = sort(data$JMSActiveMQBrokerInTime)

y = m / (sortedReceiveTime[(m + 1):numMsgs] - sortedReceiveTime[1:(numMsgs - m)]) * 10^3
plot(x, y, main="Avg. Throughput (# msg/s)", xaxt = 'n', xlab="Message #", ylab="Messages per Second (# msg/s)", cex= .1)
avgThroughput = numMsgs / numMillis * 10^3
abline(h = avgThroughput, col="red")
text(numMsgs * 0.75, avgThroughput, labels=paste(round(avgThroughput,2), "msg/s"), pos=3)
legend(x=numMsgs * 0.75, y = max(y) * 0.75,legend = c(paste("m =", m), paste("m =", numMsgs)), lwd = 1, col=c("black", "red"))
axis(1, at=messageMarks, labels=format(messageMarks, scientific=FALSE))