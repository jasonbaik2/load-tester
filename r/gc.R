plotGC <- function(gcData, fullGcData, xlim, offset = 0){
  ylim=c(0, 8500)
  yMarks=seq(from = 0, to = max(ylim), by = 1000)

  x <- (gcData$time - offset)
  
  plot(x, gcData$youngBefore/1024, col="red", xlim=xlim, ylim=ylim, ylab="", axes=FALSE, pch = 19, cex=0.5)
  par(new=T)
  plot(x, gcData$youngAfter/1024, col="green", xlim=xlim, ylim=ylim, ylab="", axes=FALSE, pch = 19, cex=0.5)
  par(new=T)
  plot(x, gcData$heapBefore/1024, col="blue", xlim=xlim, ylim=ylim, ylab="", axes=FALSE, pch = 19, cex=0.5)
  par(new=T)
  plot(x, gcData$heapAfter/1024, col="yellow", xlim=xlim, ylim=ylim, ylab="", axes=FALSE, pch = 19, cex=0.5)
  par(new=T)
  
  x <- (fullGcData$time - offset)
  
  plot(x, fullGcData$heapBefore/1024, col="black", xlim=xlim, ylim=ylim, ylab="", axes=FALSE, pch = 19, cex=0.5)
  par(new=T)
  plot(x, fullGcData$heapAfter/1024, col="magenta", xlim=xlim, ylim=ylim, xlab="Second (s)", ylab="Megabyte (MB)", axes=FALSE, pch = 19, cex=0.5)
  
  axis(1, at=xMarks, labels=format(xMarks, scientific=FALSE, digits = 0))
  axis(2, at=yMarks, labels=format(yMarks, scientific=FALSE, digits = 0))
  legend("topright",legend = c("Young Before", "Young After", "Heap Before", "Heap After", "Full GC Before", "Full GC After"), pch = 19, col=c("red", "green", "blue", "yellow", "black", "magenta"), cex = 0.5)
}