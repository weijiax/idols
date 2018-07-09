#!/bin/sh

# uncomment two lines below when on Wrangler
# export PYTHONPATH=/home/03076/rhuang/.local/lib/python2.7/site-packages
# module load python


# replace keyword as the first argument
sed "s/xxxxx/$1/g" streaming.py > streaming_keywords.py
nohup python streaming_keywords.py &> out  &
pid=$!
echo $pid
# kill streaming after duration of the second argument
sleep $2
kill $pid

# sleep 2
# IMAGE_PATH=/Users/rhuang/Documents/Dropbox_1/TACC/idols_nsf/idols/public/images/tweets_map.png
# Rscript process_tweets_log.R ./log/tweets.log $IMAGE_PATH
