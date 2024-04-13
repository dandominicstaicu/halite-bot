import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.Map.Entry;

public class StageOneBot {

    private static GameMap gameMap;
    private static int myID;
    private static List<Move> moves = new ArrayList<Move>();
    private static Set<Location> myLocations = new HashSet<>();
    private static PriorityQueue<Location> heap = new PriorityQueue<>(new HeapComparator());


    // debug
    private static String file = "output.txt";

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
        List<Entry<Location, Direction>> conquerors= new ArrayList<>();

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

    private static void findFurthestOuter(Location far, Direction dir, Site site) {
        int dist = 0;

        while (far.getSite().owner == site.owner) {
            if (dir.equals(Direction.NORTH) || dir.equals(Direction.SOUTH)) { // NORTH/SOUTH
                if (++dist < gameMap.height / 2) {
                    far = gameMap.getLocation(far, dir);
                }
            } else { // EAST/WEST
                if (++dist < gameMap.width / 2) {
                    far = gameMap.getLocation(far, dir);
                }
            }
        }
    }

    private static void checkAndUpdateBestMove(Location location, Location loc, Direction dir, BestMoveTracker tracker) {
        double dist = gameMap.getDistance(location, loc);
        if (dist < tracker.shortestDistance) {
            tracker.shortestDistance = dist;
            tracker.bestMove = new Move(loc, dir);
        }
    }

    public static void main(String[] args) throws java.io.IOException {

        final InitPackage iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;

        Networking.sendInit("MyJavaBot");
        
        FileWriter writer = new FileWriter(file, true);
        writer.write("New frame\n");
            

        while(true) {
            moves.clear();
            myLocations.clear();
            heap.clear();

            Networking.updateFrame(gameMap);

            /*
             * parcurgem tot, pt vecini calculam scorul -> heap
             * 
             * 
             * while !heap_empty:
             *  // verificam daca putem sa cucerim direct, daca nu stam
             * 
             * parcurgem inca o data, ce nu a fost mutat (int interior) il facem sa mearga spre exteriorul cel mai apropiat daca are un strenght >= 5 * prod
             * 
             * 
             */

            
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
                }
            }

            // dominic do your magic here. remove them from myLocations after assigning them work


            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    // move to closest border, functie
                    // do not move to a site that already has 255
                    // do not move if strenght < 5 * prod

                    Location location = gameMap.getLocation(x, y);
                    Site site = location.getSite();

                    if (site.owner == myID) {
                        if (!isInnerLoc(x, y)) {
                            continue;
                        }

                        // do not move if strenght < 5 * prod
                        // wait for it to increase
                        if (site.strength < 5 * site.production) {
                            Move newMove = new Move(location, Direction.STILL);
                            moves.add(newMove);
                            myLocations.remove(location);

                        } else {
                            Location north = gameMap.getLocation(location, Direction.NORTH);
                            Location south = gameMap.getLocation(location, Direction.SOUTH);
                            Location east = gameMap.getLocation(location, Direction.EAST);
                            Location west = gameMap.getLocation(location, Direction.WEST);

                            findFurthestOuter(north, Direction.NORTH, site);
                            findFurthestOuter(south, Direction.SOUTH, site);
                            findFurthestOuter(east, Direction.EAST, site);
                            findFurthestOuter(west, Direction.WEST, site);


//
//                            PriorityQueue<Move> farMoves = new
//                                    PriorityQueue<Move>(new Comparator<Move>() {
//                                @Override
//                                public int compare(Move o1, Move o2) {
//                                    return (int) gameMap.getDistance(location, o1.loc) -
//                                            (int) gameMap.getDistance(location, o2.loc);
//                                }
//                            });
//
//                            farMoves.add(new Move(north, Direction.NORTH));
//                            farMoves.add(new Move(south, Direction.SOUTH));
//                            farMoves.add(new Move(east, Direction.EAST));
//                            farMoves.add(new Move(west, Direction.WEST));
//
//                            // Get the best move from the heap.
//                            moves.add(new Move(location, farMoves.peek().dir));


                            BestMoveTracker tracker = new BestMoveTracker();

                            checkAndUpdateBestMove(location, north, Direction.NORTH, tracker);
                            checkAndUpdateBestMove(location, south, Direction.SOUTH, tracker);
                            checkAndUpdateBestMove(location, east, Direction.EAST, tracker);
                            checkAndUpdateBestMove(location, west, Direction.WEST, tracker);

                            moves.add(new Move(location, Direction.NORTH));
                            myLocations.remove(location);
                        }
                    }
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
