JAVA=java
HALITE=./halite
DIM ?= 40 40
MY_BOT=BotV2
BOT_1=bots/DBotv4_linux_x64
BOT_2=bots/starkbot_linux_x64
BROWSER=google-chrome
FILE ?= ""

.PHONY: all build clean move_classes run

all: build move_classes

build:
	$(MAKE) -C Java build

move_classes:
	mv Java/*.class .

clean:
	$(MAKE) -C Java clean
	rm -rf *.class *.log *.hlt *.replay  replays halite

run:
	python3 run.py --cmd "java BotV1" --round 1

round2:
	python3 run.py --cmd "java BotV2" --round 2
