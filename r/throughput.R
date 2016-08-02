plotThroughput <- function(data, main, x, xlim, xMarks, avgPeriod){
  numMsgs = length(data)
  y = vector(length = length(x))
  yMarks = seq(from = 0 , to = 1000, by = 100)
  
  for (nanos in data) {
    index = ceiling(nanos / 10^9 / avgPeriod) + 1 / avgPeriod
    y[index] = y[index] + 1
  }
  
  for (i in seq_along(y)) {
    y[i+1] = y[i+1] + y[i]
  }
  
  lower = y[1:(length(y)-1)]
  upper = y[2:length(y)]
  y = (upper - lower) / avgPeriod
  
  if (length(x) > length(y)) {
    backPadding = vector(length = length(x) - length(y))  
    y = c(y, backPadding)
  } else {
    y = y[1:length(x)]
  }

  plot(x, y, main=main, axes=FALSE, xlim = xlim, ylim = c(0, max(yMarks)), xlab="Second (s)", ylab="msg/s", pch = 1, type = "l", cex = 0.1)
  duration = round((max(data) - min(data)) / 10^9, digits=1)
  avgThroughput = numMsgs / (max(data) - min(data)) * 10^9
  abline(h = avgThroughput, col="red", cex = 0.1)
  text(max(x) * 0.75, avgThroughput, labels=paste("mean =", round(avgThroughput,2), "msg/s"), pos=3)
  legend("topright",legend = c(paste("avgPeriod =", avgPeriod, "s"), paste(sep="", "avgPeriod =", duration, "s")), lwd = 1, col=c("black", "red"), cex = 0.5)
  axis(1, at=xMarks, labels=format(xMarks, scientific=FALSE, digits = 0))
  axis(2, at=yMarks, labels=format(yMarks, scientific=FALSE, digits = 0))
}
