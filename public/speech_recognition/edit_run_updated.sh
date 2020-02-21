#!/bin/sh

ml python3 cuda/9.0 cudnn/7.0

if [[ ! "$PYTHONPATH" =~ (^|:)"/work/03076/rhuang/maverick2/.local/lib/python3.6/site-packages"(|/)(:|$) ]]; then


  export PYTHONPATH="/work/03076/rhuang/maverick2/.local/lib/python3.6/site-packages:$PYTHONPATH"

fi

while [ -f "/tmp/myscript.lock" ]; do
  sleep `awk -v min=10 -v max=20 'BEGIN{srand(); print int(min+rand()*(max-min+1))}'`  
done  
touch /tmp/myscript.lock
FILENAME=mby005976_mono_16khz.WAV
FILEPATH="/home/idols/idols-1.0-SNAPSHOT/public/audio_data_new/mby005976_mono_16khz.WAV"
FILESIZE=$(stat -c%s "$FILEPATH")
echo -e "File Name: $FILENAME \nFile Size: $FILESIZE" > /home/idols/idols-1.0-SNAPSHOT/public/speech_recognition/logFiles/mby005976_mono_16khz.WAV.log
/work/03076/rhuang/maverick2/.local/bin/deepspeech --model /work/03076/rhuang/maverick2/speech2/models/output_graph.pbmm --alphabet /work/03076/rhuang/maverick2/speech2/models/alphabet.txt --lm /work/03076/rhuang/maverick2/speech2/models/lm.binary --trie models/trie --audio /home/idols/idols-1.0-SNAPSHOT/public/audio_data_new/mby005976_mono_16khz.WAV > /home/idols/idols-1.0-SNAPSHOT/public/speech_recognition/txtFiles/mby005976_mono_16khz.WAV.txt 2> /home/idols/idols-1.0-SNAPSHOT/public/speech_recognition/errFiles/mby005976_mono_16khz.WAV.err
errFile="/home/idols/idols-1.0-SNAPSHOT/public/speech_recognition/errFiles/mby005976_mono_16khz.WAV.err"
time=$( tail -n 1 "$errFile" )
echo "$time" >> /home/idols/idols-1.0-SNAPSHOT/public/speech_recognition/logFiles/mby005976_mono_16khz.WAV.log
resultfile="/home/idols/idols-1.0-SNAPSHOT/public/speech_recognition/txtFiles/mby005976_mono_16khz.WAV.txt"
while read -r line; do
	echo "$line"
done < "$resultfile"
logfile="/home/idols/idols-1.0-SNAPSHOT/public/speech_recognition/logFiles/mby005976_mono_16khz.WAV.log"
while read -r line; do
	echo "$line"
done < "$logfile"
rm -rf /tmp/myscript.lock
