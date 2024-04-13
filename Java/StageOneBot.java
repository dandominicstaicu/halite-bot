import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.*;

public class StageOneBot {

    private static GameMap gameMap;
    private static int myID;
    private static List<Move> moves = new ArrayList<Move>();
    private static Set<Location> moveHistory = new HashSet<>();
    private static PriorityQueue<Location> heap = new PriorityQueue<>(new HeapComparator());

    private static double getScore(Location location) {
      return 1.0 * location.getSite().production / location.getSite().strength + 1;
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

    public static void main(String[] args) throws java.io.IOException {

        final InitPackage iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;

        Networking.sendInit("MyJavaBot");
    
            

        while(true) {
            moves.clear();
            moveHistory.clear();
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

            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    // build heap
                }
            }

            while (!heap.isEmpty()) {
                // if can conquer, conquer and add to array of moves and set
            }

            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    // move to closest border, functie
                }
            }



            Networking.sendFrame(moves);
        }
    }
}
