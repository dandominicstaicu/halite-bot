package oldFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.*;
import java.util.Map.Entry;

import Direction;
import GameMap;
import InitPackage;
import Location;
import Move;
import Networking;
import Site;

public class StageOneBot {

    private static GameMap gameMap;
    private static int myID;
    private static List<Move> moves = new ArrayList<Move>();
    private static Set<Location> myLocations = new HashSet<>();
    private static PriorityQueue<Location> heap = new PriorityQueue<>(new HeapComparator());

    private static double getScore(Location location) {
        return (1.0 * location.getSite().production) / (1.0 * location.getSite().strength) + 1;
    }

    private static class HeapComparator implements Comparator<Location> {
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

    // checks if location is my neighbour
    private static boolean isNeighbour(Location location) {
        for (Direction dir : Direction.DIRECTIONS) {
            Location newLocation = gameMap.getLocation(location, dir);
            if (newLocation.getSite().owner == myID) {
                return true;
            }
        }
        return false;
    }


    private static List<Entry<Location, Direction>> getConquerors(Location location) {
        List<Entry<Location, Direction>> conquerors = new ArrayList<>();

        for (Direction dir : Direction.DIRECTIONS) {
            Location newLocation = gameMap.getLocation(location, dir);
            if (newLocation.getSite().owner == myID && myLocations.contains(newLocation)) {
                conquerors.add(new AbstractMap.SimpleEntry<>(newLocation, Direction.invertDirection(dir)));
            }
        }
        return conquerors;
    }

    private static void conquer(Location location) {
        List<Entry<Location, Direction>> conquerors = getConquerors(location);
        for (var conqueror : conquerors) {
            if (conqueror.getKey().getSite().strength >= location.getSite().strength) {
                moves.add(new Move(conqueror.getKey(), conqueror.getValue()));
                myLocations.remove(conqueror.getKey());
            }
        }
    }

    private static class BestMoveTracker {
        public Move bestMove;
        public double shortestDistance;

        public BestMoveTracker() {
            this.bestMove = null;
            this.shortestDistance = Double.MAX_VALUE;
        }
    }

    private static boolean isInnerLoc(int x, int y) {
        Location location = gameMap.getLocation(x, y);
        Site site = location.getSite();

        Location north = gameMap.getLocation(location, Direction.NORTH);
        Location south = gameMap.getLocation(location, Direction.SOUTH);
        Location east = gameMap.getLocation(location, Direction.EAST);
        Location west = gameMap.getLocation(location, Direction.WEST);

        return north.getSite().owner == site.owner
                && south.getSite().owner == site.owner
                && west.getSite().owner == site.owner
                && east.getSite().owner == site.owner;
    }

    private static Location findFarthestBoundary(Location start, Direction direction, int limit) {
        int distance = 0;
        Location current = start;
        while (current.getSite().owner == start.getSite().owner && distance < limit) {
            current = gameMap.getLocation(current, direction);
            distance++;
        }
        return current;
    }

    private static void checkAndUpdateBestMove(Location location, Location loc, Direction dir, BestMoveTracker tracker) {
        double dist = gameMap.getDistance(location, loc);
        if (dist < tracker.shortestDistance) {
            tracker.shortestDistance = dist;
            tracker.bestMove = new Move(loc, dir);
        }
    }

    static void moveInnerTerritory(Location location, int x, int y) {
        Site site = location.getSite();

        if (site.owner == myID) {
            if (!isInnerLoc(x, y)) {
                return;
            }

            // do not move if strenght < 5 * prod wait for it to increase
            if (site.strength < 5 * site.production) {
                Move newMove = new Move(location, Direction.STILL);
                moves.add(newMove);
                myLocations.remove(location);

            } else {
                Location north = findFarthestBoundary(location, Direction.NORTH, gameMap.height / 2);
                Location south = findFarthestBoundary(location, Direction.SOUTH, gameMap.height / 2);
                Location east = findFarthestBoundary(location, Direction.EAST, gameMap.width / 2);
                Location west = findFarthestBoundary(location, Direction.WEST, gameMap.width / 2);

                BestMoveTracker tracker = new BestMoveTracker();

                checkAndUpdateBestMove(location, north, Direction.NORTH, tracker);
                checkAndUpdateBestMove(location, south, Direction.SOUTH, tracker);
                checkAndUpdateBestMove(location, east, Direction.EAST, tracker);
                checkAndUpdateBestMove(location, west, Direction.WEST, tracker);

                moves.add(new Move(location, tracker.bestMove.dir));
                myLocations.remove(location);
            }
        }
    }

    public static void main(String[] args) throws java.io.IOException {

        final InitPackage iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;

        Networking.sendInit("MyJavaBot");

        while (true) {
            moves.clear();
            myLocations.clear();
            heap.clear();

            Networking.updateFrame(gameMap);

            // traverse the map and add neighbours to heap
            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    Location location = gameMap.getLocation(x, y);
                    if (isNeighbour(location) && location.getSite().owner != myID) {
                        heap.add(location);
                    }

                    if (location.getSite().owner == myID) {
                        myLocations.add(location);
                    }

                    moveInnerTerritory(location, x, y);
                }
            }

            while (!heap.isEmpty()) {
                Location location = heap.poll();
                conquer(location);
            }

            // for all weak exteriors
            for (Location location : myLocations) {
                moves.add(new Move(location, Direction.STILL));
            }

            Networking.sendFrame(moves);
        }
    }
}
