#!/bin/bash

#REV: 10 Aug 2010: script to run sensory integration expts
#MAKE SURE TO HAVE SALIENCY MAP RUNNING!
#./runadeserver com/vision/VisionServerImpl -cammode -8 -imgFilename com/sensory_integration/si_vid8/si_index.indx -size 320 240 -runproc saliency -hideControls
#-show saliency will make it display window, if you want.

function randomShuffle
{
    typeset -a elements
    typeset length=0
    while read line
    do
        elements[$length]=$line
        length=$(($length + 1))
    done
    typeset firstN=${1:-$length}
    if [ $firstN -gt $length ]
    then
        firstN=$length
    fi
    for ((i=0; $i < $firstN; i++))
    do
        randPos=$(($RANDOM % ($length - $i) ))
        printf "%s\n" "${elements[$randPos]}"
        elements[$randPos]=${elements[$length - $i - 1]}
    done
}

#assuming its not already there...
APPEND="LOLz"
mkdir "si_results"$APPEND

output_filename="si_data"
ofolder="si_results"$APPEND"/"$output_filename
delay=60
trialnum=0
liquidnum=1 #liquid number 1 works...0 too
wordfilename=com/sensory_integration/green_tight.wav

while ((liquidnum < 2)); do
    #liquidfn="liquids/INTLIQ"$liquidnum".liq"
    liquidfn="liquids/WORD2LIQ.liq"
    echo $liquidfn
    trialnum=1000 #64 is -800delay
    while ((trialnum < 2000)); do
	delay=48 #44*20=880 (1600-880=720)   64*20=1280 (-720 min)  60 = 60*20 = 1200 which is 2000-1200= -800ms delay
	arrayiter=0
	stringthing=""
	while ((delay<112)); do #116 #136 is +680delay, which means we need at least 680+500 to be safe...=1200 seems fine
	    #fill the array
	    stringthing=$stringthing" "$delay
	    #echo $stringthing
	    let delay+=2
	    

	#generate 50 to 140 at intervals of 2, numbers randomly, append them to each delay, and then sort...iterate thru
#	while ((delay < 130)); do
#	    ./runadeserver com/sensory_integration/si_experiments_ServerImpl -delay $delay -wordfile $wordfilename -load $liquidfn -output $ofolder $liquidnum $trialnum
#	    let delay+=2
   	done
	blah=$(printf "%s\n" $stringthing | randomShuffle)
	#echo $blah
	arr=$(echo $blah | tr " " "\n")
	
	for delay in $arr
	do
	    echo "offset: $delay"
	    ./runadeserver com/sensory_integration/si_experiments_ServerImpl -delay $delay -wordfile $wordfilename -load $liquidfn -output $ofolder $liquidnum $trialnum
	done
	    
	
	
	#now, iterate through, doing delays in that order...
	
	echo "done trialnum="$trialnum
	let trialnum++
    done
    echo "done liquidnum="$liquidnum
    let liquidnum++
done
