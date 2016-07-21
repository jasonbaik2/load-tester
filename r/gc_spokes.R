scriptDir = "C:/Users/HCC5fkv/workspace/load-tester/r"
setwd(scriptDir)

source("gc.R")

attach(mtcars)
layout(matrix(c(1,2,3,4,5,6,7,8), 4, 2, byrow = TRUE), widths=c(3,2))
par(mar=c(2,4,2,1))
par(oma = c(0,0,3,0))

gcData <- read.csv(paste(sep="/", reportDir, "spoke1/gc.log"))
fullGcData <- read.csv(paste(sep="/", reportDir, "spoke1/full_gc.log"))
gcData <- subset(gcData, gcData$time >= 0 && gcData$time <= (endTime / 10^9 + setupDuration))
fullGcData <- subset(fullGcData, fullGcData$time <= (endTime / 10^9 + setupDuration))

plotGC(gcData, fullGcData, xlim, 150)
frame()
mtext(text = paste("# Young GC:", length(gcData$time)), line = 0, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("# Full GC:", length(fullGcData$time)), line = -1, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("Max Heap Size:", round(max(c(max(gcData$heapBefore),max(fullGcData$heapBefore)))/1024, digits=2), "MB"), line = -2, at = 0.1, adj = 0, cex=0.8)

gcData <- read.csv(paste(sep="/", reportDir, "spoke2/gc.log"))
fullGcData <- read.csv(paste(sep="/", reportDir, "spoke2/full_gc.log"))
gcData <- subset(gcData, gcData$time >= 0 && gcData$time <= (endTime / 10^9 + setupDuration))
fullGcData <- subset(fullGcData, fullGcData$time <= (endTime / 10^9 + setupDuration))

plotGC(gcData, fullGcData, xlim, 150)
frame()
mtext(text = paste("# Young GC:", length(gcData$time)), line = 0, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("# Full GC:", length(fullGcData$time)), line = -1, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("Max Heap Size:", round(max(c(max(gcData$heapBefore),max(fullGcData$heapBefore)))/1024, digits=2), "MB"), line = -2, at = 0.1, adj = 0, cex=0.8)

gcData <- read.csv(paste(sep="/", reportDir, "spoke3/gc.log"))
fullGcData <- read.csv(paste(sep="/", reportDir, "spoke3/full_gc.log"))
gcData <- subset(gcData, gcData$time >= 0 && gcData$time <= (endTime / 10^9 + setupDuration))
fullGcData <- subset(fullGcData, fullGcData$time <= (endTime / 10^9 + setupDuration))

plotGC(gcData, fullGcData, xlim, 150)
frame()
mtext(text = paste("# Young GC:", length(gcData$time)), line = 0, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("# Full GC:", length(fullGcData$time)), line = -1, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("Max Heap Size:", round(max(c(max(gcData$heapBefore),max(fullGcData$heapBefore)))/1024, digits=2), "MB"), line = -2, at = 0.1, adj = 0, cex=0.8)

gcData <- read.csv(paste(sep="/", reportDir, "spoke4/gc.log"))
fullGcData <- read.csv(paste(sep="/", reportDir, "spoke4/full_gc.log"))
gcData <- subset(gcData, gcData$time >= 0 && gcData$time <= (endTime / 10^9 + setupDuration))
fullGcData <- subset(fullGcData, fullGcData$time <= (endTime / 10^9 + setupDuration))

plotGC(gcData, fullGcData, xlim, 335)
frame()
mtext(text = paste("# Young GC:", length(gcData$time)), line = 0, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("# Full GC:", length(fullGcData$time)), line = -1, at = 0.1, adj = 0, cex=0.8)
mtext(text = paste("Max Heap Size:", round(max(c(max(gcData$heapBefore),max(fullGcData$heapBefore)))/1024, digits=2), "MB"), line = -2, at = 0.1, adj = 0, cex=0.8)
