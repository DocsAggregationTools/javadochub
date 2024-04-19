#!/bin/bash

kill $(jps -l | grep javadochub | awk '{print $1}')
sleep 2
ps -ef | grep javadochub | grep -v grep
