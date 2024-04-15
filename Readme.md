# Halite Bot
## Team Chess.com

Java Bot for the [Halite](https://github.com/HaliteChallenge/Halite) competition designed by team "Chess.com" for the Algorithms Design course (2nd year);
This bot fights for control of a 2D grid. The bot with the most territory at the end wins.

### Credits
#### Carauleanu Valentin Gabriel 321CA
#### Sampetru Mario 321CA
#### Staicu Dan-Dominic 321CA

## Bot overview

### Stage One

In this stage, the bot has to cover entirely map without fighting with any other bots.
The key strategy here is treating the front lines of the owned territory differently from the inner one.
The front line locations are kept in a PriorityQueue and all the owned locations in a Set.
For the PriorityQueue it was implemented a comparator in order to create a max heap.


For the **Inner Territory** the strategy is to move the tile only if the strength is at least the value of 5 * production in order to be worth it.
If a tile is ready to be moved, the bot checks which is the closest direction in order to find the best move and move into that way.


For the **Front Lines**, that 



All the other locations that are still in the owned locations set have to be weak exteriors that should stand still to get their strength higher.
