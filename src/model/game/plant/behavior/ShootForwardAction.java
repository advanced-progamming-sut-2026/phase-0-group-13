package model.game.plant.behavior;

import model.game.Board;
import model.game.Projectile;
import model.game.plant.Plant;

public class ShootForwardAction implements PlantAction {
    private int actionInterval;
    private int damage;
    private boolean isSlowing;

    public ShootForwardAction(int actionInterval, int damage, boolean isSlowing) {
        this.actionInterval = actionInterval;
        this.damage = damage;
        this.isSlowing = isSlowing;
    }

    public ShootForwardAction(int actionInterval, int damage) {
        this(actionInterval, damage, false);
    }

    @Override
    public void execute(Plant plant, Board board, int currentTick) {
        if (currentTick - plant.getLastActionTick() >= actionInterval) {

            if (board.hasZombieInRow(plant.getRow(), plant.getCol())) {

                Projectile projectile = new Projectile(damage, 0.5, plant.getCol(), plant.getRow(), isSlowing, false);
                board.addProjectile(projectile);

                plant.setLastActionTick(currentTick);
                System.out.printf("Plant %s fired a projectile at row %d!%n", plant.getName(), plant.getRow());
            }
        }
    }
}