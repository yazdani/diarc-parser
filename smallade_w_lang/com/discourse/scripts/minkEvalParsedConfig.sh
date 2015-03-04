configfile=$1
goldfile=$2
parsedfile=$3

java -cp com/discourse/jar:com/discourse/jar/weka.jar:. com/discourse/mink/MinkParser -config $configfile -goldfile $goldfile -infile $parsedfile