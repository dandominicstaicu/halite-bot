SOURCE_FOLDER=Java
MY_BOT=MyBot
BOT_1=bots/DBotv4_linux_x64
BOT_2=bots/starkbot_linux_x64
DIM ?= "30 30"


build:
	javac $(SOURCE_FOLDER)/MyBot.java
	javac $(SOURCE_FOLDER)/RandomBot.java


clean:
	rm -rf *.class *.log *.hlt *.replay

# make fight-random DIM="30 30"
fight-random:
	./halite -d $(DIM) -n 1 -s 42 "java $(MY_BOT)" "java RandomBot"

fight-1:
	./halite -d $(DIM) -n 1 -s 42 "java $(MY_BOT)" $(BOT_1)

fight-2:
	./halite -d $(DIM) -n 1 -s 42 "java $(MY_BOT)" $(BOT_2)

