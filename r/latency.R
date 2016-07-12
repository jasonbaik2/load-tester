# Read file into a table

#file = readline("Please enter the stat file:")
reportDir = "D:/report/n_1_publish_1_1_subscribe_1465177978297"
file = "D:/report/data.csv"
# params <- read.csv(file, nrows = 1)
# data <- read.csv(file, skip = 2)
data <- read.csv(file)

numMsgs = length(data$PubTime)
messageMarks = seq(from = 0 , to = numMsgs, by = 10^3)

# The times are in nanos
startTime = data$PubTime[1]
endTime = data$PubTime[numMsgs]
numMillis = (endTime - startTime) / 10^6

qos = 2

attach(mtcars)
par(mfrow = c(3,1), oma = c(0,0,10,0))

###########################################################
# Plot latency
# Latency is the time the published message is successfully acknowledged (by PUBACK for QoS = 1, PUBCOMP for QoS = 2) by the broker
# Latency = (PubAckReceiveTime or PubCompReceiveTime) - PubTime
###########################################################
x <- 1:numMsgs

if (qos == 1) {
  receiveTime = data$PubAckReceiveTime
} else {
  receiveTime = data$PubCompReceiveTime
}

latencies = receiveTime - data$PubTime
latenciesUs = latencies / 10^3

avgLatency = mean(latenciesUs)
plot(x, latenciesUs, main="Latency (us)", xaxt = 'n', xlab="Message #", ylab="Latency (us)", ylim = c(0, avgLatency * 2), cex= .1)
axis(1, at=messageMarks, labels=format(messageMarks, scientific=FALSE))