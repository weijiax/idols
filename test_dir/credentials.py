###Start credentials.py setup###

import tweepy


consumer_key="tHpGLDL8IxN45G8go0FdJTrYM"
consumer_secret="MPtvmrncCMeLibOTgkxYMYHMWaK786kRwAFBtwSKW6NOfzrk0M"

access_token="2267758213-YJKE4ilH0jcysHv0g1ER7cRHmJvQ1mCgTCXgi08"
access_token_secret="CMxL4sDdP5zQyIuxjULb2aUVyaCxtvV1HyquIapeWuXbF"


auth = tweepy.OAuthHandler(consumer_key, consumer_secret)
auth.set_access_token(access_token, access_token_secret)

api = tweepy.API(auth)
