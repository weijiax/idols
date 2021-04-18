#!/bin/sh

ml python3 cuda/9.0 cudnn/7.0

if [[ ! "$PYTHONPATH" =~ (^|:)"/work/03076/rhuang/maverick2/.local/lib/python3.6/site-packages"(|/)(:|$) ]]; then


  export PYTHONPATH="/work/03076/rhuang/maverick2/.local/lib/python3.6/site-packages:$PYTHONPATH"

fi

while [ -f "/tmp/myscript.lock" ]; do
  sleep `awk -v min=10 -v max=20 'BEGIN{srand(); print int(min+rand()*(max-min+1))}'`  
done  
touch /tmp/myscript.lock
FILENAME=$1
FILEPATH="/home/idols/idols-1.0-SNAPSHOT/public/audio_data_new/$1"
FILESIZE=$(stat -c%s "$FILEPATH")
echo -e "File Name: $FILENAME \nFile Size: $FILESIZE" > /home/idols/idols-1.0-SNAPSHOT/public/speech_recognition/logFiles/$1.log
/work/03076/rhuang/maverick2/.local/bin/deepspeech --model /work/03076/rhuang/maverick2/speech2/models/output_graph.pbmm --alphabet /work/03076/rhuang/maverick2/speech2/models/alphabet.txt --lm /work/03076/rhuang/maverick2/speech2/models/lm.binary --trie models/trie --audio /home/idols/idols-1.0-SNAPSHOT/public/audio_data_new/$1 > /home/idols/idols-1.0-SNAPSHOT/public/speech_recognition/txtFiles/$1.txt 2> /home/idols/idols-1.0-SNAPSHOT/public/speech_recognition/errFiles/$1.err
errFile="/home/idols/idols-1.0-SNAPSHOT/public/speech_recognition/errFiles/$1.err"
time=$( tail -n 1 "$errFile" )
echo "$time" >> /home/idols/idols-1.0-SNAPSHOT/public/speech_recognition/logFiles/$1.log
resultfile="/home/idols/idols-1.0-SNAPSHOT/public/speech_recognition/txtFiles/$1.txt"
while read -r line; do
	echo "$line"
done < "$resultfile"
logfile="/home/idols/idols-1.0-SNAPSHOT/public/speech_recognition/logFiles/$1.log"
while read -r line; do
	echo "$line"
done < "$logfile"
rm -rf /tmp/myscript.lock
