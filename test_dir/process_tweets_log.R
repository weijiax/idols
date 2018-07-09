
#library(rtweet)
install.packages("maps", repos="https://cran.rstudio.com")
library("jsonlite")
library("maps")
# first args: log file path; second args: image path
args = commandArgs(trailingOnly=TRUE)


json_file <- stream_in(file(args[1]), flatten=F)

#json_file = stream_in(file("~/Dropbox_1/TACC/Twitter/log/tweets.log"), flatten=F)

json_file$bbox_coords = json_file[,"place"][,"bounding_box"][,"coordinates"]
json_file$lng=sapply((sapply(json_file$bbox_coords, "[", 1:4)),mean)
json_file$lat=sapply((sapply(json_file$bbox_coords, "[", 5:8)),mean)

#rt <- lat_lng(json_file )

png(file=args[2])
#png(file = "/Users/rhuang/Documents/Dropbox_1/TACC/idols_nsf/idols/public/images/tweets_map.png", width = 480, height = 320)

## plot state boundaries
par(mar = c(0, 0, 0, 0))
maps::map("state", lwd = 1.4)

## plot lat and lng points onto state map
with(json_file, points(lng, lat, pch = 20, cex = 1.2, col = rgb(0, .3, .7, .75)))

dev.off()
