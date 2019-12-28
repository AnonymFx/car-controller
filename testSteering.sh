#!/usr/bin/env zsh
current=0
while true; do
	current_actual=$((current % 201 - 100))
	echo $current_actual
	command="m0s$current_actual\n"
	echo Sending $command
	echo -n "$command" | nc -w0 -4u 151.217.233.162 3001
	current=$((current + 5))
	sleep 0.15
done
