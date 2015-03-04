:name findDoor
:desc ?mover looks for a door at a given heading ?heading
:var ?mover actor 
:var ?heading coordinate 
:var !headingFrom coordinate 
:var !headingTo coordinate 
# Here's where the events start
getHeading ?mover !headingFrom
printText "headingFrom: !headingFrom"
getHeadingTo ?mover !headingFrom ?heading !headingTo
printText "headingTo: !headingTo"
if
  gt !headingTo 15
then
  turnRel ?mover !headingTo
else
  printText "Not worth turning"
endif
startMove ?mover
while
  not
    getNearestDoor ?mover
  endnot
do
  printText "Still looking for a door"
endwhile
stop ?mover

:name findDoor2
:desc ?mover looks for a door at a given heading ?heading
:var ?mover actor 
:var ?heading coordinate 
:var !headingFrom coordinate 
:var !headingTo coordinate 
# Here's where the events start
getHeading ?mover !headingFrom
printText "headingFrom: !headingFrom"
getHeadingTo ?mover !headingFrom ?heading !headingTo
printText "headingTo: !headingTo"
if
  gt !headingTo 15
then
  turnRel ?mover !headingTo
else
  printText "Not worth turning"
endif
startMove ?mover
while
  not
    getNearestDoor ?mover
  endnot
do
  printText "Still looking for a door"
endwhile
stop ?mover
