#!/bin/bash
#Author: Jack Harris
#Date Created: Oct 10, 2010
#When validating that a some code runs a bit of code runs as intended
#we must address a 3 of things
#1. That the Code Actually Halt
#2. That the Code Doesn't Error
#3. That the Code Returns Expected Results.
#
#Notes: In the general case, item one is provably impossible, however in the
#specific case, with the code finish within a bounded #amount of time is quite
#easy. Simply run a timer in parallel.  Therefore, this script handles that for
#you and returns an exit value of 99 if time is the program does not halt in
#the specified time.

usage(){
	echo Parameters:
	echo "-s Name -- Script to run"
	echo "-t Number -- This is the max runtime allowed in seconds to complete the test"
	echo "Optional -e Name -- File containing the expected results"
	echo "Optional -o Name -- Output file to test"
}
RUNTIME_ALLOWED=0
EXPECTED_FILE=""
OUTPUT_FILE=""
RUN_SCRIPT=""

if [[ $# != 8  && $# != 4 ]]; then
	echo "Wrong number of parameters"
	usage
	exit 1
fi

lastType=""
for p in $@; do
	if [ "$lastType" == "" ]; then
  	if [[ $p == "-t" ||  $p == "-e" || $p == "-o" || $p == "-s" ]]; then
			lastType=$p;
		else
			echo "Unknown parameter type $p"
			usage
			exit 1
		fi
  else
		if [ $lastType == "-t" ]; then
			RUNTIME_ALLOWED=$(($p + 0)) #make it a number
		elif [ $lastType == "-e" ]; then
			EXPECTED_FILE=$p
		elif [ $lastType == "-o" ]; then
			OUTPUT_FILE=$p
		elif [ $lastType == "-s" ]; then
			RUN_SCRIPT=$p
		fi
		lastType=""
	fi
done;
if [[ "$RUN_SCRIPT" == "" || "$RUNTIME_ALLOWED" == "" ]]; then
	echo Run Script and Run Time must be specied: -s, -t
	usage
	exit 1
elif [ $# -gt 4 ]; then
	if [[ "$OUTPUT_FILE" ==  "" || "$EXPECTED_FILE" == "" ]]; then 
		echo "If checking validity of output, both the output file and the expected file need to be specified: -o, -e"
		usage
		exit 1
	fi
fi
echo "Running: $RUN_SCRIPT"
echo "For a max of $RUNTIME_ALLOWED seconds"
if [ "$EXPECTED_FILE" != "" ]; then
	echo "Evaluated File: $EXPECTED_FILE"
	echo "Output File: $OUTPUT_FILE"
fi

EXIT_CODE_FILE_NAME=exitcode_$RANDOM

bash -i -c  "$RUN_SCRIPT; echo -e \$? > $EXIT_CODE_FILE_NAME" &
RUN_PID=$!
sleep 1 
start=$(date +%s)
PROCESS_DONE=false
TIMED_OUT=false
while [[ $PROCESS_DONE == false && $TIMED_OUT == false ]]; do 
	if [[ $(( $(date +%s) - $start )) -gt $RUNTIME_ALLOWED  ]]; then
		echo "Process Timed Out"
		TIMED_OUT=true
	elif [ "" == "$(ps |pcregrep -i "^\s*$RUN_PID " | grep -v grep)" ]; then
		echo "Process Complete"
		PROCESS_DONE=true
	else
		sleep 1;
	fi
done

EXIT_CODE=-1
if [ $TIMED_OUT == true ]; then	
  echo "----------------------------------------"
	if [ "$(uname)" == "Darwin" ]; then
		SPAWNED=$(ps -l |awk -v RUN_PID="$RUN_PID"  '{ if ( $3 == RUN_PID ) { print $2 }  }')
	else 
		SPAWNED=$(pgrep -P $RUN_PID)
	fi
	KILL_LIST="-$RUN_PID"
	for child in $SPAWNED; do
		kill -TERM -$child
	done
	kill -TERM -$RUN_PID
	
	EXIT_CODE=99
else 
	sleep 1
	if [ ! -f $EXIT_CODE_FILE_NAME ]; then
		echo "Exit code file not found ($EXIT_CODE_FILE_NAME)";
		EXIT_CODE=98
	else
		EXIT_CODE=$(cat $EXIT_CODE_FILE_NAME)
		rm $EXIT_CODE_FILE_NAME
	fi
fi
if [ "$EXIT_CODE" == "0" ]; then
	if [ "$EXPECTED_FILE" != "" ]; then
		echo "Checking validity of the output"
		difference=$(diff $EXPECTED_FILE $OUTPUT_FILE)
		if [ "$?" != "0" ]; then
			echo "Output file $OUTPUT_FILE does not match $EXPECTED_FILE"
			EXIT_CODE=2
		fi
	fi
fi
echo "$CMD COMPLETE"
echo "Returning Exit Code: $EXIT_CODE"
exit $EXIT_CODE
