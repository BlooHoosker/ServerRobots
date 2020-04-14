package com.company;

import javafx.util.Pair;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class NavigationAI {

    Queue<Pair<Integer, Integer>> destinationTiles;
    Pair<Integer, Integer> currentPosition;
    Pair<Integer, Integer> destination;
    Direction direction;

    /* Returns signal:
    0 reached destination
    1 turn right
    -1 turn left
    2 move
    */

    public NavigationAI(Pair<Integer, Integer> position) {
        this.currentPosition = position;
        direction = new Direction(0);
        destination = new Pair<>(2,2);;
        destinationTiles = new LinkedList<>();
        destinationTiles.add(new Pair<>(2, 2));
        destinationTiles.add(new Pair<>(1, 2));
        destinationTiles.add(new Pair<>(0, 2));
        destinationTiles.add(new Pair<>(-1, 2));
        destinationTiles.add(new Pair<>(-2, 2));
        destinationTiles.add(new Pair<>(-2, 1));
        destinationTiles.add(new Pair<>(-1, 1));
        destinationTiles.add(new Pair<>(0, 1));
        destinationTiles.add(new Pair<>(1, 1));
        destinationTiles.add(new Pair<>(2, 1));
        destinationTiles.add(new Pair<>(2, 0));
        destinationTiles.add(new Pair<>(1, 0));
        destinationTiles.add(new Pair<>(0, 0));
        destinationTiles.add(new Pair<>(-1, 0));
        destinationTiles.add(new Pair<>(-2, 0));
        destinationTiles.add(new Pair<>(-2, -1));
        destinationTiles.add(new Pair<>(-1, -1));
        destinationTiles.add(new Pair<>(0, -1));
        destinationTiles.add(new Pair<>(1, -1));
        destinationTiles.add(new Pair<>(2, -1));
        destinationTiles.add(new Pair<>(2, -2));
        destinationTiles.add(new Pair<>(1, -2));
        destinationTiles.add(new Pair<>(0, -2));
        destinationTiles.add(new Pair<>(-1, -2));
        destinationTiles.add(new Pair<>(-2, -2));
    }

    // Sets initial direction
    public void SetDirection(Pair<Integer, Integer> origin, Pair<Integer, Integer> target){
        direction = GetDirection(origin,target);
    }

    // Gets direction from two positions next to each other
    private Direction GetDirection(Pair<Integer, Integer> origin, Pair<Integer, Integer> target){
        Pair<Integer, Integer> result = new Pair<>(target.getKey() - origin.getKey(), target.getValue() - origin.getValue());
        if (result.getKey() > 0){
           return new Direction(45);
        } else if (result.getKey() < 0){
            return new Direction(135);
        } else if (result.getValue() > 0){
            return new Direction(0);
        } else if (result.getValue() < 0){
            return new Direction(90);
        }
        return new Direction(-1);
    }

    // Returns move signal based on AIs position and target destination
    private int MoveTo(Pair<Integer, Integer> target){

        Pair<Integer, Integer>directionX = new Pair<>(target.getKey(), currentPosition.getValue());
        Pair<Integer, Integer>directionY = new Pair<>(currentPosition.getKey(), target.getValue());
        boolean reachedX = false;

        // Check if we reached destination
        if ( currentPosition.getValue().compareTo(target.getValue()) == 0 && currentPosition.getKey().compareTo(target.getKey()) == 0 ){
            return 0;
        }

        if ( GetDirection(currentPosition, directionX).GetDirection() == -1 ){
            reachedX = true;
        }

        int state;
        if (reachedX){
            // Generates turn command for Y direction after we reached X
            state = direction.WhichAction(GetDirection(currentPosition, directionY).GetDirection());
        } else {
            // Generates turn command if we didnt reach same X yet
            state = direction.WhichAction(GetDirection(currentPosition, directionX).GetDirection());
        }

        return state;
    }

    // Public method to be used for normal moves
    public int Move(Pair<Integer, Integer> position){
        currentPosition = position;
        return MoveTo(destination);
    }

    // Public method used for when the robot is in search mode
    public int Search(Pair<Integer, Integer> position){

        currentPosition = position;
        if (destinationTiles.isEmpty()){
            return 3;
        }

        destination = destinationTiles.element();
        int state = MoveTo(destination);

        if (state == 0){
            destinationTiles.remove();
        }
        return state;
    }

}
