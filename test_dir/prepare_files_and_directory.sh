#!/bin/sh

SOURCE_CODE_DIR=/Users/rhuang/Documents/Dropbox_1/TACC/Twitter/
NEW_DIR=/Users/rhuang/Documents/Dropbox_1/TACC/Twitter_final/
LOG_DIR=$NEW_DIR/log/


# remove old tweets log if exist
rm -rf $LOG_DIR

# make a twitter directory and a log directory under it
mkdir -p $LOG_DIR

#copy python and R script to the $NEW_DIR directory
cp $SOURCE_CODE_DIR/{streaming.py,credentials.py,run_streaming_keywords.sh,process_tweets_log.R} $NEW_DIR
