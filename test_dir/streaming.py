"""
Using Twitter stream API, capture and write out all the tweets containing the terms occurring in the keywords list"
"""
from tweepy.streaming import StreamListener
from tweepy import OAuthHandler
from tweepy import Stream
from credentials import *
from time import time,ctime
import simplejson
import sys
import re
from pprint import pprint as pp
from datetime import datetime
import calendar
import logging
import logging.handlers

class StdOutListener(StreamListener):
   
    def __init__(self):
        StreamListener.__init__(self)
        debuglog.debug("Starting")

    def on_data(self, data):
        data = data.rstrip()
        tweetlog.info(data)
        dataJson = simplejson.loads(data)
        volumelog.debug(simplejson.dumps(dataJson['text']))
        return True

    def on_error(self, status):
        debuglog.error(status)



if __name__ == '__main__':
    tweetlog = logging.getLogger('tweets')
    tweetlog.setLevel(logging.INFO)
    handler = logging.handlers.WatchedFileHandler("log/tweets.log")
    handler.setFormatter(logging.Formatter('%(message)s'))
    tweetlog.addHandler(handler)

    debuglog = logging.getLogger('debug')
    debuglog.setLevel(logging.DEBUG)
    handler = logging.FileHandler('log/debug.log')
    handler.setFormatter(logging.Formatter("%(created)f\t%(message)s"))
    debuglog.addHandler(handler)

    volumelog = logging.getLogger('volume')
    volumelog.setLevel(logging.DEBUG)
    handler = logging.FileHandler('log/volume.log')
    handler.setFormatter(logging.Formatter("%(created)f\t%(message)s"))
    volumelog.addHandler(handler)

    l = StdOutListener()
    mystream = Stream(auth, l,timeout=15)

    ####Insert your search terms of interest in this list as strings separated by commas...Examples for USPS provided below###
    #keywords = ["realdonaldtrump", "cnn"]
    keywords = [xxxxx]
    try:
        mystream.filter(track=keywords, languages=['en'])
    except Exception as ex:
        debuglog.exception(ex)
    finally:
        logging.shutdown()
