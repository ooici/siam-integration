#
# Shell script to facilitate build/start/stop commands associated with
# the SIAM-CI adapter service, in particular for the nightly builds/tests.
# 

PIDFILE=siamci.pid

if [ "$1" = "build" ]; then
	echo "Installing dependencies..."
	(cd etc/maven && ant install)
	rc=$?
	if [ "0" != "$rc" ]; then
		echo "error running ant to install dependencies"
		exit $rc
	fi
	echo "Building..."
	mvn compile
	rc=$?
	if [ "0" != "$rc" ]; then
		echo "error running maven to build the service program"
		exit $rc
	fi

elif [ "$1" = "start" ]; then
	if [ -e $PIDFILE ]; then
		echo "$PIDFILE exists; not starting as the service may be already running"
		exit 1
	fi
	if [ "$2" = "" ]; then
		mvn exec:java -Psiam-ci -Dexec.args=--siam=siam.oceanobservatories.org &
	else
		mvn exec:java -Psiam-ci -Dexec.args=--siam=$2 &
	fi
	rc=$?
	if [ "0" != "$rc" ]; then
		echo "error running maven to launch the service program"
		exit $rc
	fi
	pid=$!
	echo "$pid" > $PIDFILE
	echo "SIAM-CI adapter service started. PID $pid stored in $PIDFILE"

elif [ "$1" = "stop" ]; then
	pid=`cat $PIDFILE`
	rc=$?
	if [ "0" != "$rc" ]; then
		echo "error reading $PIDFILE. Is the service actually running? If it is and you know its PID, you can terminate it by issueing the command 'kill -TERM pid'"
		exit $rc
	fi
	echo "Sending $pid the TERM signal..."
	kill -TERM $pid
	rc=$?
	if [ "0" != "$rc" ]; then
		echo "error killing the service process"
		exit $rc
	fi
	rm $PIDFILE

else
	echo "A sort of controller for the SIAM-CI adapter service"
	echo "Usage:"
	echo "  ./siamci.sh build                         # builds the service"
	echo "  ./siamci.sh start [<siam-host>]           # starts the service"
	echo "  ./siamci.sh stop                          # stops the service"
	echo
	echo "By default, <siam-host> is siam.oceanobservatories.org"
	exit
fi
