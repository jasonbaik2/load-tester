adjust <- function(data, offsetMillis){
  data$PubTimeMillis = data$PubTimeMillis + offsetMillis
  data$PubAckReceiveTime = data$PubTimeMillis * 10^6 - data$PubTime + data$PubAckReceiveTime
  data$PubRecReceiveTime = data$PubTimeMillis * 10^6 - data$PubTime + data$PubRecReceiveTime 
  data$PubRelSendTime = data$PubTimeMillis * 10^6 - data$PubTime + data$PubRelSendTime
  data$PubCompReceiveTime = data$PubTimeMillis * 10^6 - data$PubTime + data$PubCompReceiveTime
  data$PubTime = data$PubTimeMillis * 10^6
  return(data)
}

mergeMQTTFlightFiles <- function(files){
  datas = list()
  
  for (i in seq_along(files)) {
    # Adjust the times with respect to the start time
    datas[[i]] <- read.csv(files[i])
  }
  
  for (i in seq_along(datas)) {
    data = datas[[i]]
    
    if (i == 1) {
      mergedData = data
    } else {
      mergedData = rbind(mergedData, data)
    }
  }
  return(mergedData)
}

mergeFiles <- function(files, column, startTimeEpochMillis){
  datas = list()
  
  for (i in seq_along(files)) {
    datas[[i]] = read.csv(files[i])
    
    # Adjust the times with respect to the start time
    datas[[i]][column] = datas[[i]][column] - startTimeEpochMillis
    
    if (i == 1) {
      mergedData = datas[[i]]
    } else {
      mergedData = rbind(mergedData, datas[[i]])
    }
  }
  return(mergedData)
}