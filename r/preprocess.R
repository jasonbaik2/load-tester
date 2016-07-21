adjust <- function(data, offset){
  data$PubTime = data$PubTime + offset
  data$PubAckReceiveTime = data$PubAckReceiveTime + offset
  data$PubRecReceiveTime = data$PubRecReceiveTime + offset
  data$PubRelSendTime = data$PubRelSendTime + offset
  data$PubCompReceiveTime = data$PubCompReceiveTime + offset
  return(data)
}

normalize <- function(data) {
  # Normalize with respect to the smallest pub time (relative time)
  return(adjust(data, -1 * min(data$PubTime)))
}

mergeFileData <- function(files){
  datas = list()
  
  # Find the min pub time millis. This will be treated as the absolute start time (0)
  for (i in seq_along(files)) {
    datas[[i]] <- read.csv(paste(sep="/", reportDir, files[i]))
    
    if (i == 1) {
      pubTimeMillisMin = min(datas[[i]]$PubTimeMillis)
    } else {
      pubTimeMillisMin = min(pubTimeMillisMin, min(datas[[i]]$PubTimeMillis))
    }
  }
  
  # Adjust the times with respect to the min pub time
  for (i in seq_along(datas)) {
    datas[[i]] <- adjust(datas[[i]], (min(datas[[i]]$PubTimeMillis) - pubTimeMillisMin) * 10^6)
  }
  
  for (i in seq_along(datas)) {
    data = normalize(datas[[i]])
    
    if (i == 1) {
      mergedData = data
    } else {
      mergedData = rbind(mergedData, data)
    }
  }
  return(mergedData)
}