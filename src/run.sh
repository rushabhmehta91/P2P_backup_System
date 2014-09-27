#! /bin/bash

if [ "$1" -eq "$1" ] 2>/dev/null
then
	nohup java -jar bootstrap.jar $1 >stdout  &
	echo "Bootstrap server started ar port $1"
	sleep 3
else
	echo "ERROR: Enter port number."
	echo "Usage: sh run.sh <PORT NUMBER>"
	exit 1
fi

