#!/bin/bash

nohup java -jar -Dspring.config.location=application.yaml javadochub.jar &
sleep 2
ps -ef | grep javadochub | grep -v grep
