#!/bin/sh

NEW_DIR=/work/03076/rhuang/wrangler/Twitter_stream/
LOG_DIR=$NEW_DIR/log
IMAGE_PATH=$NEW_DIR/tweets_map_$USER.png

cd $NEW_DIR; source run_streaming_keywords.sh \"@WhiteHouse\",\"@realdonaldtrump\" 60

#uncomment below when on Wrangler
#module load Rstats 

cd $NEW_DIR; Rscript process_tweets_log.R $LOG_DIR/tweets.log $IMAGE_PATH
