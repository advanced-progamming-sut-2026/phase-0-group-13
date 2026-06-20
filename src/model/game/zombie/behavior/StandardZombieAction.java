package model.game.zombie.behavior;

import model.game.Board;
import model.game.plant.Plant;
import model.game.zombie.Zombie;

public class StandardZombieAction implements ZombieAction {
    private double eatingDamage;

    public StandardZombieAction(double eatingDamage) {
        this.eatingDamage = eatingDamage;
    }

    @Override
    public void execute(Zombie zombie, Board board, int currentTick) {
        Plant targetPlant = board.getPlantAt(zombie.getRow(), zombie.getX());

        if (targetPlant != null && !targetPlant.isDead()) {
            zombie.setEating(true);

            if (currentTick % 10 == 0) {
                targetPlant.takeDamage((int) eatingDamage);
            }
        } else {
            zombie.setEating(false);
            zombie.move();
        }
    }
}