package com.company;

public class Direction {
    private int dir;

    /* Simple class that handles direction of the robot
    0 means up
    90 means down
    45 means right
    135 means left
     */

    public Direction(int direction){
        this.dir = direction;
    }

    private int TurnLeft(int direction){
        if (direction == 0) {
            direction = 135;
        } else {
            direction -= 45;
        }
        return direction;
    }

    private int TurnRight(int direction){
        direction = (direction + 45) % 180;
        return direction;
    }

    public int WhichAction(int target){
        if (TurnLeft(dir) == target){
            dir = TurnLeft(dir);
            return -1;
        } else if (TurnRight(dir) == target){
            dir = TurnRight(dir);
            return 1;
        } else if (target == dir){
            return 2;
        }
        dir = TurnRight(dir);
        return 1;
    }

    public int GetDirection(){
        return dir;
    }

}
