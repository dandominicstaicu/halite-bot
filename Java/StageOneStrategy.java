import java.util.*;
import java.util.Map.Entry;

public class StageOneStrategy extends GameStrategy {
    // all locations owned by me
    private Set<Location> ownedLocations;
    // all locations that are on the front line ie near owned locations
    private PriorityQueue<Location> frontLine;

    // keeps track of the best move to a location and the distance to it, used in moveInnerTerritory
    private  class BestMoveTracker {
        public Move bestMove;
        public double shortestDistance;

        public BestMoveTracker() {
            this.bestMove = null;
            this.shortestDistance = Double.MAX_VALUE;
        }

        public void updateMove(Location dest, Location loc, Direction direct) {
            double dist = gameMap.getDistance(dest, loc);
            if (dist < shortestDistance) {
                shortestDistance = dist;
                bestMove = new Move(loc, direct);
            }
        }
    }

    // comparator for front line locations based on the score (max heap)
    private class frontLineLocationComparator implements Comparator<Location> {
        private double getScore(Location location) {
            return (1.0 * location.getSite().production) / (1.0 * location.getSite().strength) + 1;
        }

        @Override
        public int compare(Location a, Location b) {
            double scoreA = 0;
            double scoreB = 0;

            for (Direction dir : Direction.DIRECTIONS) {
                Location newLocation = gameMap.getLocation(a, dir);
                scoreA += getScore(newLocation);
            }

            for (Direction dir : Direction.DIRECTIONS) {
                Location newLocation = gameMap.getLocation(b, dir);
                scoreB += getScore(newLocation);
            }

            return Double.compare(scoreA, scoreB);
        }
    }

    private  List<Entry<Location, Direction>> getConquerors(Location location) {
        List<Entry<Location, Direction>> conquerors = new ArrayList<>();

        for (Direction dir : Direction.DIRECTIONS) {
            Location newLocation = gameMap.getLocation(location, dir);
            if (newLocation.getSite().owner == myID && ownedLocations.contains(newLocation)) {
                conquerors.add(new AbstractMap.SimpleEntry<>(newLocation, Direction.invertDirection(dir)));
            }
        }
        return conquerors;
    }

    private  void conquer(Location location) {
        List<Entry<Location, Direction>> conquerors = getConquerors(location);
        for (var conqueror : conquerors) {
            if (conqueror.getKey().getSite().strength >= location.getSite().strength) {
                moves.add(new Move(conqueror.getKey(), conqueror.getValue()));
                ownedLocations.remove(conqueror.getKey());
            }
        }
    }

     void moveInnerTerritory(Location location, int x, int y) {
        Site site = location.getSite();

        if (site.owner == myID) {
            if (!isInnerLoc(x, y)) {
                return;
            }

            // do not move if strenght < 5 * prod wait for it to increase
            if (site.strength < 5 * site.production) {
                Move newMove = new Move(location, Direction.STILL);
                moves.add(newMove);
                ownedLocations.remove(location);
                return;

            }

            BestMoveTracker tracker = new BestMoveTracker();

            for (Direction dir : Direction.getEastWestDirections()) {
                Location destination = findFarthestBoundary(location, dir, gameMap.width / 2);
                tracker.updateMove(location, destination, dir);
            }

            for (Direction dir : Direction.getNorthSouthDirections()) {
                Location destination = findFarthestBoundary(location, dir, gameMap.height / 2);
                tracker.updateMove(location, destination, dir);
            }

            moves.add(new Move(location, tracker.bestMove.dir));
            ownedLocations.remove(location);
        }
    }


    @Override
    public List<Move> computeBestMoves(GameContext gameContext) {
        gameMap = gameContext.gameMap;
        myID = gameContext.myID;
        moves = new ArrayList<>();
        ownedLocations = new HashSet<>();
        frontLine = new PriorityQueue<>(new frontLineLocationComparator());

        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Location location = gameMap.getLocation(x, y);
                if (isNeighbour(location) && location.getSite().owner != myID) {
                    frontLine.add(location);
                }

                if (location.getSite().owner == myID) {
                    ownedLocations.add(location);
                }

                moveInnerTerritory(location, x, y);
            }
        }

        while (!frontLine.isEmpty()) {
            Location location = frontLine.poll();
            conquer(location);
        }

        // for all weak exteriors
        for (Location location : ownedLocations) {
            moves.add(new Move(location, Direction.STILL));
        }

        return moves;

    }
    
}
