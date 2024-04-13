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
