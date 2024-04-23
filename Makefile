JAVA=java
HALITE=./halite
DIM ?= 40 40
MY_BOT=BotV2
BOT_1=bots/DBotv4_linux_x64
BOT_2=bots/starkbot_linux_x64
BROWSER=google-chrome
FILE ?= ""

.PHONY: all build clean move_classes fight-random fight-1 fight-2 vis custom

all: build move_classes

build:
	$(MAKE) -C Java build

move_classes:
	mv Java/*.class .

clean:
	$(MAKE) -C Java clean
	rm -f *.class *.log *.hlt *.replay

fight-random:
	$(HALITE) -d "$(DIM)" -n 1 -s 42 "$(JAVA) $(MY_BOT)" "$(JAVA) RandomBot"

fight-1:
	$(HALITE) -d "$(DIM)" -n 1 -s 42 "$(JAVA) $(MY_BOT)" "$(BOT_1)"

fight-2:
	$(HALITE) -d "$(DIM)" -n 1 -s 42 "$(JAVA) $(MY_BOT)" "$(BOT_2)"

fight-bots:
	$(HALITE) -d "$(DIM)" -n 1 -s 42 "$(BOT_1)" "$(BOT_2)"

 

# shows the latest reply file
vis:
	@FILE=$$(ls -t *.hlt | head -1); \
	if [ -z "$$FILE" ]; then \
		echo "No .hlt file found."; \
	else \
		echo "Using file: $$FILE"; \
		python3 vis.py $(BROWSER) $$FILE; \
	fi

# shows a specific replay file
custom:
	@echo "enter file name:"
	@read FILE; \
	python3 vis.py $(BROWSER) $$FILE


round1:
	python3 run.py --cmd "java BotV1" --round 1

round2:
	python3 run.py --cmd "java BotV2" --round 2
