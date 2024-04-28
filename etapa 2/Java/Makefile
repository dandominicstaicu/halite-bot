JAVAC=javac
JAVA=java
SOURCE_FOLDER=.

SOURCES=$(wildcard $(SOURCE_FOLDER)/*.java)
CLASSES=$(SOURCES:$(SOURCE_FOLDER)/%.java=%.class)

.PHONY: all build clean

build: $(CLASSES)

run: build
	$(JAVA) BotV2

%.class: $(SOURCE_FOLDER)/%.java
	$(JAVAC) $<

clean:
	rm -f *.class *.log *.hlt

