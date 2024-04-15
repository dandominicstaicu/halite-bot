import java.util.*;

public class StageTwoStrategy extends GameStrategy {
    // owned locations on the map ordered by strength -> stronger pieces will be moved first
    private PriorityQueue<Location> ownedLocations;
    // territories around the map to be processed
    private PriorityQueue<Territory> strategicTerritories;
    // map between a location and how much power was added to it by a previous move
    private Map<Location, Double> movePlan;
    // key = location, value = score of the location based on the neighbours and the location itself
    private Map<Location, Double> locationScoreMap; 

    // class used to keep track of territories and how they are scored
    public class Territory implements Comparable<Territory> {
        public double score;
        public Location location;
        // all owned frontier locations have friendlyDistance = 1
        // the deeper the friendly location, the higher the distance
        public int friendlyDistance;

        public Territory(Location location) {
            this.location = location;
            this.score = 0;
            this.friendlyDistance = 0;
        }

        @Override
        public int compareTo(Territory o) {
            return Double.compare(this.score, o.score);
        }
    }

    // class used to keep track of possible moves
    public class MoveCandidate implements Comparable<MoveCandidate> {
        // location on game map for the future move
        public Location location;
        // direction towards the location
        public Direction direction;
        // score of the location
        public double score;

        public MoveCandidate(Location location, Direction dir) {
            this.location =  location;
            this.direction = dir;
            this.score = 0;
        }

        @Override
        public int compareTo(MoveCandidate o) {
            if (this.score == o.score) {
                // brek the ties randomly
                return new Random().nextInt() - new Random().nextInt();
            } else {
                return Double.compare(this.score, o.score);
            }
        }
    }

    private static final double INFINITY = 1.0 * Integer.MAX_VALUE;
    private static final int MAX_HALITE = 255;
    private static final double SCORE_DISTRIBUTION = 0.5;

    @Override
    public List<Move> computeBestMoves(GameContext gameContext) {
        gameMap = gameContext.gameMap;
        myID = gameContext.myID;

        // set up
        moves = new ArrayList<>();
        ownedLocations = new PriorityQueue<Location>((a,b) -> -Double.compare(a.getSite().strength, b.getSite().strength));
        locationScoreMap = new HashMap<>();
        strategicTerritories = new PriorityQueue<Territory>();
        movePlan = new HashMap<>();
        

        initialize();
        computeScores();

        // move all owned locations starting from stronger to weaker
        while (!ownedLocations.isEmpty()) {
            Location location = ownedLocations.poll();
            Direction moveDir = assignMove(location);
            Location target = gameMap.getLocation(location, moveDir);
            // we save our move in the move location plan
            movePlan.put(target, movePlan.getOrDefault(target, 0.0) + location.getSite().strength);
        }
        return moves;
    }


    // to be refactored for readability maybe
    private void computeScores() {
        do {
            // take the highest score territory from the heap
            Territory territory = strategicTerritories.poll();

            if (!locationScoreMap.containsKey(territory.location)) {
                // using the friendly distance allows the bot to move its pieces to the outside of the
                // controlled territories
                locationScoreMap.put(territory.location, territory.score + territory.friendlyDistance);
                
                // analyze all neighbours
                for (Location neighbour : getNeighbours(territory.location)) {
                    int neighId = neighbour.getSite().owner;
        
                    switch (Ownership.findOwnership(neighId, myID)) {
                        case FRIENDLY:
                            Territory friendlyTerritory = new Territory(neighbour);

                            friendlyTerritory.score = territory.score + (territory.friendlyDistance + 1);
                            friendlyTerritory.friendlyDistance = territory.friendlyDistance + 1;
                            strategicTerritories.add(friendlyTerritory);
                            break;

                        case ENEMY:
                            Territory enemyTerritory = new Territory(neighbour);

                            enemyTerritory.score = INFINITY;
                            enemyTerritory.friendlyDistance =  territory.friendlyDistance;
                            strategicTerritories.add(enemyTerritory);
                            break;

                        case NEUTRAL:
                            Territory neutralTerritory = new Territory(neighbour);
                            // better code

                            neutralTerritory.score = neighbour.getSite().production == 0 ?
                                                        INFINITY : weightedScore(neighbour, territory.score);
                            neutralTerritory.friendlyDistance = territory.friendlyDistance;
                            strategicTerritories.add(neutralTerritory);
                            break;

                        default:
                            throw new IllegalArgumentException("Invalid ownership");
                    }
                }
            }
        } while (locationScoreMap.size() < gameMap.width * gameMap.height);
    }

    // we modify the score of a location based on the score of its neighbour
    private double weightedScore(Location loc, double neighScore) {
        return (1 - SCORE_DISTRIBUTION) * getScore(loc) + SCORE_DISTRIBUTION * neighScore;
    }

    // computes the score of a location
    private double getScore(Location location) {
        if (location.getSite().production == 0) {
            return INFINITY;
        } else {
            return 1.0 * location.getSite().strength / location.getSite().production + 1;
        }
    }

    // all owned locations are added to a set, and all others to the heap for further processing
    private void initialize() {
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                if (gameMap.getLocation(x, y).getSite().owner == myID) {
                    ownedLocations.add(gameMap.getLocation(x, y));
                } else {
                    Territory territory = new Territory(gameMap.getLocation(x, y));
                    territory.score = getScore(territory.location);
                    strategicTerritories.add(territory);
                }
            }
            
        }
    }

    private Direction assignMove(Location myLocation) {
        // if location's strength + prudction + strength assigned to it by other moves
        // is over the limit, we must move to not waste halite
        boolean isMoveNeeded = (myLocation.getSite().strength + myLocation.getSite().production
                                    + movePlan.getOrDefault(myLocation, 0.0) > MAX_HALITE);

        // find all possible moves and their score
        List<MoveCandidate> moveCandidates = findCandidates(myLocation);
        // sort by score to find the best move
        Collections.sort(moveCandidates);
        MoveCandidate bestMove = moveCandidates.get(0);

        // if best move is INFINITY, it means all possible moves are bad choices
        if (bestMove.score == INFINITY) {
            if (isMoveNeeded) {
                // to be improved. do not go random
                moves.add(new Move(myLocation, Direction.randomDirection()));
                return Direction.randomDirection();
            } else {
                // stay still
                moves.add(new Move(myLocation, Direction.STILL));
                return Direction.STILL;
            }
        }

        // if we need to move, we move to the best direction
        if (isMoveNeeded) {
            moves.add(new Move(myLocation, bestMove.direction));
            return bestMove.direction;
        }

        // if no moves are needed and for another piece it was beneficial to
        // move to this location, we stay STILL
        if (!isMoveNeeded && isPartOfThePlan(myLocation)) {
            moves.add(new Move(myLocation, Direction.STILL));
            return Direction.STILL;
        }

        // we do not neceasrrily need to move, however attack if possible
        Site mySite = myLocation.getSite();
        Site bestMoveSite = bestMove.location.getSite();

        // if our best option is an opponent attack if it can be conquered
        if (isAttackOpportunity(bestMoveSite, mySite)) {
            moves.add(new Move(myLocation, bestMove.direction));
            return bestMove.direction;
        }

        // otherwise, move if strong enough
        if (isStrongEnoughToMove(mySite)) {
            moves.add(new Move(myLocation, bestMove.direction));
            return bestMove.direction;
        }

        // if no condition for moving is satisfied, stay STILL
        moves.add(new Move(myLocation, Direction.STILL));
        return Direction.STILL;
    }

    private List<MoveCandidate> findCandidates(Location myLocation) {
        List<MoveCandidate> moveCandidates = new ArrayList<>();
        
        // check al neighbours
        for (Direction dir : Direction.CARDINALS) {
            Location neighbour = gameMap.getLocation(myLocation, dir);
            MoveCandidate moveCandidate = new MoveCandidate(neighbour, dir);

            // AVOID COLLISIONS and waste of halite
            if (movePlan.getOrDefault(neighbour, 0.0) + myLocation.getSite().strength > MAX_HALITE) {
                moveCandidate.score = INFINITY;
            } else {
                moveCandidate.score = locationScoreMap.get(neighbour);
            }

            moveCandidates.add(moveCandidate);
        }

        return moveCandidates;
    }

    private boolean isPartOfThePlan(Location loc) {
        return movePlan.getOrDefault(loc, 0.0) > 0.0;
    }

    private boolean isAttackOpportunity(Site targetSite, Site mySite) {
        return targetSite.owner != myID &&
               (mySite.strength == MAX_HALITE || mySite.strength > targetSite.strength);
    }
    
    private boolean isStrongEnoughToMove(Site mySite) {
        return mySite.strength >= 6 * mySite.production;
    }
}
