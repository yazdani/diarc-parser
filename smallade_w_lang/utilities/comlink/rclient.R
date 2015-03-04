#####################################################################
#
# SWAGES 1.1
#
# (c) by Matthias Scheutz <mscheutz@nd.edu>
#
# R Client
#
# Last modified: 06-18-07
#
#####################################################################

# Socket used for communication with server
comm_sock = FALSE

# by default the program runs in client mode, to stop this, pass
# either a command line argument, or call the simulation with a
# particular function
clientID = FALSE

# Number of attempts to connect to the server before giving up
connection_attempts = 50

# Waiting before a connection is attempted in sec.
waitforconnection = 10

# Number of attempts to receive a message
receive_attempts = 3

# default for communciation with the server
# this will be overwritten by experiment setup file
servername = "127.0.0.1" 

# "The port on the server used for connections"
serverport = 10009 

# for recording the output of the R analysis
saveSTATSFILE = FALSE
statsout = FALSE

# this will hold result files from model runs, etc. that the client might need
resultfiles = FALSE

# for logging standard output is connected to logger file, otherwise to /dev/null
logging = FALSE

# a seed for repeating an analysis that depends on random numbers
ranseed = 0

#for debugging: print out lots of comments
verbose = FALSE

# message types
PREQ = 0
SINT = 1 
SCNT = 2
SPAR = 3
CPAR = 4
DONE = 5
SSAV = 6
CCYC = 7
PCYC = 8
PSAV = 9
SPLT = 10
ALLD = 11;
UCID = 12;

OK = "1"
NOTOK = "-1"

# this dummy function needs to be overwritten with code during simulation setup
# (e.g., the actr6 code defines a run function that will override it)
run <- function(stats) {
  print("These are the parameters passed to 'run': ")
  print(stats)
}

# INCOMPLETE
# parses the parameters that the R client can deal with
parseparameters <- function (params) {
  for (n in 1:length(params)) {
    p <- params[[n]]
    key <- p[[1]]
    val <- p[[2]]
    if (key == "initfile") {
      if (verbose) print(paste("INITFILE:", val))
      source(val)
    }
    else if (key == "compilehere") {
      if (verbose) print(paste("EVAL:", val))
      eval(parse(text = val))
    }
    else if (key == "statsfile") {
      if (verbose) print(paste("STATSFILE:", val))
      saveSTATSFILE <<- val
    }
    else if (key == "resultfiles") {
      if (verbose) print(paste("RESULTSFILES:", val))
      resultfiles <<- val
    }

# NEED TO IMPLEMENT THIS RIGHT STILL
    else if (key == "ranseed") {
      if (verbose) print(paste("RANSEED:", val))
      ranseed <<- val
    }
    else if (key == "setglobal") {
      # check if the argument is a string, then it needs to be quoted seperately...
      if (is.character(p[[3]])) {
        eval(parse(text=paste(val,"<<- \"",p[[3]],"\"")))
      }
      else {
        eval(parse(text=paste(val,"<<- ",p[[3]])))
      }
    }
    # ignore unknown keys
    else {
      if (verbose) print(paste("Unknown key:", key))
    }
  }
}

# can separate two messages, will not work for more than two, but that
# case should never happen
last = ""
read_line <- function () {
  message = ""
  if (last != "") {
    #print(paste("LAST:",last))
    message <- last
    last <<- ""
  }
  else {
    nonewline = TRUE
    while (nonewline) {
      while (message == "") {
        message <- read.socket(comm_sock)
      }
      m = strsplit(message, "\n")[[1]]
      # check if there are was a newline in it, there was none if the strings are the same...
      if (length(m) > 1) {
        # there was a difference, so check if the newline was only at the end of the first string
        # then the second message was incomplete and we need to read more
        if (message == paste(m[1],"\n",m[2],sep="")) {
          # there were two strings, need to figure out if there was a newline at the end of the second
          # otherwise we have to finish reading until we get one
          # this will not work if the new read creates another \n...  SHOULD NEVER HAPPEN given the protocol
          last <<- paste(m[2],read_line(),sep="")        
        }
        # 2nd message was complete, so just set last to the 2nd message
        else {
          last <<- m[2]
        }          
        # return in any case the first part of the message with the \n
        message <- m[1]
      }
      # if there was no \n in the message, it is incomplete, need to read more
      else if (paste(m,"\n",sep="") != message) {
        message <- paste(m,read_line(),sep="")
      }
      # if the message was not OK or NOTOK, then we still need to read until we hit the \n
      #else if ((m != OK) && (m != NOTOK)) {
      #  message <- paste(message,read_line(),sep="")                
      #}
      # only one message and the message was complete
      else {
        message <- m
      }
      # we are done, return the message
      nonewline <- FALSE
    }
  }
  return(message)
}

# sends a messages
send_message <- function (senderID, mtype, data) {
  write.socket(comm_sock, paste("(\"", senderID, "\" ", mtype, " ", data,")\n", sep = ""))
  message <- read_line()
#  print(paste("GOT MESSAGE BACK...",message))
  if (message == OK) {
    return(TRUE)
  }
  else {
    return(FALSE)
  }
}

# receives a message
receive_message <- function () {
  message <- read_line()
  if (message == "") {
    close.socket(comm_sock)
    FALSE
  }
  else {
    write.socket(comm_sock, OK)
    write.socket(comm_sock, "\n")
    return(eval(parse(text=message)))
  }
}

# waits for a message of a particular type
wait_for_message <- function (typeofmessage) {
  message <- read_line()
  while (message != "") {
    message <- eval(parse(text = message))
    if (message[[2]] == typeofmessage) {
      write.socket(comm_sock, OK)
      write.socket(comm_sock, "\n")
      return(message)
    }
    else {
      message <- read_line()
    }
  }
  FALSE
}

# keeps sending a message and re-opening the socket on failure
keep_sending_message <- function (mID, mtype, mdata) {
  for (n in 1:connection_attempts) {
    if (send_message(mID, mtype, mdata))
      return(TRUE)
    if (verbose) print(paste("SHOULD NEVER GET HERE...", comm_sock))
    close.socket(comm_sock)
    comm_sock <<- make.socket(servername, port = serverport)
    system(paste("sleep", 100 * waitforconnection))
  }
  FALSE  
}

# dummy reset function to reset R
reset <- function () {
}

# the main entry point called by SWAGES
runclient <- function (params) {
  if (is.list(params)) {
    servername <- params[[1]]
    serverport <- params[[2]]
    clientID <- params[[3]]
  }
  
  # NOTE: R already writes the output to a file in command mode...
  if (logging) {
    zz <- file("/tmp/rclientout", open = "wt")
    sink(zz)
    sink(zz, type = "message")
  }

  while (TRUE) {
    simparametermessage = FALSE
    while (!simparametermessage) {
      # print(servername)
      # print(serverport)
      comm_sock <<- make.socket(servername, port = serverport)
      on.exit(close.socket(comm_sock))
      # print("Sending PREQ")
      keep_sending_message(clientID, PREQ, FALSE)
      # print("waiting for SPAR")
      simparametermessage <- wait_for_message(SPAR)
      # print("received SPAR...")
      # print(paste("SIMPARAM:",simparametermessage))

      if (length(simparametermessage) > 1) {
        parseparameters(simparametermessage[[3]][[1]])
        break
      }
    }

    # open the stats file if supplied
    if (is.character(saveSTATSFILE)) statsout <- file(saveSTATSFILE, open = "wt")
    
    if (verbose) print("Analysis started")
    run(statsout)
    if (verbose) print("Analysis finished")

    # if the statsfile was open, close it and relay the information
    if (is.character(saveSTATSFILE)) {
      close(statsout)
      keep_sending_message(clientID, DONE, paste("(",0," ",file.info(saveSTATSFILE)$size,")"))
    }
    else {
      keep_sending_message(clientID, DONE, paste("(",0," ",0,")"))
    }
    
    clientID <- (wait_for_message(UCID))[[3]]
    # if we did not get a new ID, then finish
    if (is.list(clientID)) {
      # close the socket
      close.socket(comm_sock)
      quit(save="no")
    }
    # otherwise the VM will be re-used, so reset the simulation
    else {
      reset()
    }    
  }
}
