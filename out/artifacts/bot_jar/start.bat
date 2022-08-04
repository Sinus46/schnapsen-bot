@echo OFF
set /p TOKEN=Please enter the token you want the bot to run in: 

java -jar bot.jar %TOKEN%
PAUSE