plotLatencyDistribution <- function(latencies, main, by = -1){
  # Converted to milli seconds
  latenciesSeconds = latencies / 10^9
  maxLatency = max(latenciesSeconds)
  minLatency = min(latenciesSeconds)
  middleLatency = minLatency + (maxLatency - minLatency) / 2
  
  if (by == -1) {
    by = (ceiling(maxLatency) - floor(minLatency)) / 20
  }
  
  breaks = seq(from = floor(minLatency), to = ceiling(maxLatency), by = by)
  latencyMarks = seq(from = floor(minLatency), to = ceiling(maxLatency), by = by)
  
  h = hist(latenciesSeconds, breaks = breaks, plot = FALSE)
  h$density = h$counts / sum(h$counts) * 100
  plot(h, freq=FALSE, xlab = "Latency (Seconds)", xaxt = 'n', ylab = "%", main = main, xlim=c(min(latencyMarks),max(latencyMarks)), ylim=c(0,100))
  text(h$mids, h$density, h$counts, adj = c(.5, -.5), col = "blue", cex=0.5)
  text(x=middleLatency, y=100, labels=paste("Max:", round(max(latenciesSeconds), digits = 3), "s"), cex=1, adj=c(0,0.5))
  text(x=middleLatency, y=90, labels=paste("99.9%:", round(quantile(latenciesSeconds, probs = c(0.999)), digits = 3)[1], "s"), cex=1, adj=c(0,0.5))
  text(x=middleLatency, y=80, labels=paste("Mean:", round(mean(latenciesSeconds), digits = 3), "s"), cex=1, adj=c(0,0.5))
  axis(1, at=latencyMarks, labels=format(latencyMarks, scientific=FALSE))
}