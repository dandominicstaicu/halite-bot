#!/usr/bin/env python3

import hlt
from hlt import NORTH, EAST, SOUTH, WEST, STILL, Move, Square
import heapq
import random
import time
from collections import defaultdict
from itertools import chain
import logging
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--name',type=str, nargs='?', default='roibotV8a',help='Name of this version of the bot.')
parser.add_argument('--alpha', type=float, nargs='?', default=0.25, help='Alpha to use in exponential smoothing / discounting of ROI over distance.  Default is 0.25')  #Default was 0.1
parser.add_argument('--potential_degradation_step', type=float, nargs='?', default=0.5, help='Times friendly_distance ** 2 is potential degradation.  Default is 0.5')  #Default was 1.0
parser.add_argument('--enemy_ROI', type=float, nargs='?', default=-1.0, help='Amount of ROI to include for each enemy adjacent to an empty square.  Default is -1.0')
parser.add_argument('--hold_until', type=int, nargs='?', default=5, help='Hold square STILL until strength >= args.hold_until * production.  Default is 5.')
args = parser.parse_args()
logging.basicConfig(filename=args.name+'.log', level=logging.DEBUG, format='%(asctime)s - %(levelname)s - %(message)s')
logging.debug(str(args))

myID, game_map = hlt.get_init()
if game_map.width == 20 or game_map.height == 20 or game_map.starting_player_count >= 4:
    args.hold_until = 6
    args.potential_degradation_step = 0.4
    assert args.hold_until == 6
    assert args.potential_degradation_step == 0.4
hlt.send_init(args.name)


class MoveCandidate:
    def __init__(self, potential, direction, target):
        self.potential = potential
        self.direction = direction
        self.target = target

    def __lt__(self, other):
        # First compare by potential, then use the random factor to break ties
        if self.potential == other.potential:
            return random.random() < random.random()
        return self.potential < other.potential

def assign_move(square):
    candidates = []

    # Evaluate potential moves to each neighbor
    for direction, neighbor in enumerate(game_map.neighbors(square)):
        # Calculate potential for overflow
        if destinations[neighbor] + square.strength > 255:
            potential = float('inf')  # Prevent overflow by assigning high potential
        else:
            potential = pf_map[neighbor]  # Normal potential calculation

        # Create a candidate move
        candidate = MoveCandidate(potential, direction, neighbor)
        candidates.append(candidate)

    # Choose the best candidate based on potential and random tie-breaking
    best_candidate = min(candidates) if candidates else None

    # Evaluate the staying condition
    staying_is_bad = (square.strength + square.production + destinations.get(square, 0)) > 255

    # Decision making process
    if best_candidate and best_candidate.potential == float('inf'):
        if staying_is_bad:
            # All moves are bad, choose least bad move including staying still
            return Move(square, random.choice([NORTH, EAST, SOUTH, WEST, STILL]))
        else:
            # Staying is not bad, just stay
            return Move(square, STILL)

    if not staying_is_bad and any(Move(neighbor, best_candidate.direction) in moves for neighbor in game_map.neighbors(square)):
        return Move(square, STILL)

    if not staying_is_bad and destinations[square] > 0:
        return Move(square, STILL)

    if best_candidate:
        if staying_is_bad:
            return Move(square, best_candidate.direction)
        if best_candidate.target.owner != myID and (square.strength == 255 or square.strength > best_candidate.target.strength):
            return Move(square, best_candidate.direction)
        elif square.strength >= square.production * args.hold_until:
            return Move(square, best_candidate.direction)

    return Move(square, STILL)

def initial_potential(square):
    # if empty, correlate with utility as an attacking square (if there are neighboring enemies)
    if square.owner == square.strength == 0:
        return 0
    elif square.production == 0:
        return float('inf')
    else:
        return square.strength / square.production

turn = 0
while True:
    start_time = time.time()
    turn += 1
    moves = []
    game_map.get_frame()

    # scores for all locations
    # a class vector -> min heap
    frontier = [(initial_potential(square), random.random(), initial_potential(square), 0, square) for square in game_map if square.owner != myID]
    # keeps track of where each location should ideally move to : Location -> score
    pf_map = dict()
    heapq.heapify(frontier)
    while len(pf_map) < game_map.width * game_map.height:
        # class: _ , _ , square_potential, friendly_distance (initally 0), square (location)
        _, _, square_potential, friendly_distance, square = heapq.heappop(frontier)
        if square not in pf_map:
            # update the potential map with the new score
            pf_map[square] = square_potential + 1 * friendly_distance ** 2

            for neighbor in game_map.neighbors(square):
                # if the neighbor is not mine, update the heap with the new score based on the neighbor
                if neighbor.owner != myID:
                    # if neutral and non zero production, compute potential as percentages between neighbour and inital square
                    neighbor_potential  = (1 - 0.5) * square_potential + 0.5 * neighbor.strength / neighbor.production if neighbor.production and neighbor.owner == 0 else float('inf')
                    heapq.heappush(frontier, (neighbor_potential, random.random(), neighbor_potential, friendly_distance, neighbor))
                else:
                    # lower score if the neighbor is mine
                    neighbor_potential  = square_potential + 1 * (friendly_distance + 1) ** 2
                    heapq.heappush(frontier, (neighbor_potential, random.random(), square_potential, friendly_distance + 1, neighbor))
    moves = set() #list()
    # dict of Location -> direction
    destinations = defaultdict(int)
    # MY LOCATIONS sorted by strength
    for square in sorted((square for square in game_map if square.owner == myID and square.strength > 0), key=lambda x: x.strength, reverse=True):
        move = assign_move(square)
        moves.add(move)
        target = game_map.get_target(square, move.direction)
        destinations[target] += square.strength
    hlt.send_frame(moves)
    
    logging.debug(str(turn) + ' :: ' + str(int(1000 * (time.time() - start_time))))
