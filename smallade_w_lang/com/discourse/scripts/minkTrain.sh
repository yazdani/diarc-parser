java -Xmx2048M -cp weka.jar:com/discourse/mink:. com/discourse/mink/MinkParser -trainingfile $1 -mode LEARN -defaultfeatures -labels -lookahead #-graphtype trees #-printActions #-debug
