package model.game;

import model.game.plant.Plant;
import model.game.zombie.Zombie;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Board {
    private int rows;
    private int columns;
    private Tile[][] tiles; // این داستان که افکت هارو چجوری باید اضافه بکنیم و ایده ای ندارم

    private List<Zombie> zombies;
    private List<Plant> plants;
    private List<Sun> suns;
    private List<Projectile> projectiles;
    private List<Lawnmower> lawnmowers;

    private GameState gameState;
    private int lastSunDropTick;
    private Random random;

    public Board(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        this.gameState = new GameState();
        this.zombies = new ArrayList<>();
        this.plants = new ArrayList<>();
        this.suns = new ArrayList<>();
        this.projectiles = new ArrayList<>();
        this.lawnmowers = new ArrayList<>();
        this.lastSunDropTick = 0;
        this.random = new Random();
        initialize();
    }

    public void initialize() {
        tiles = new Tile[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                tiles[i][j] = new Tile();
            }
            // چمن زن و بزاریم
            lawnmowers.add(new Lawnmower(i));
        }
    }


    // همه چی و اپدیت میکنیم
    public void updateAll(int currentTick) {
        gameState.update(null, null);

        // تو اینجا این افکت های رو تایل ( الگوی استراتژی ) مثل یخ یا قبر اینارو ، اپدیت میکنیم ولی ناکامله
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                tiles[i][j].getEffect().tick();
            }
        }
        handleSkySunDrop(currentTick);

        for (Plant plant : plants) {
            plant.update(currentTick, this);
        }

        for (Zombie zombie : zombies) {
            zombie.update(currentTick, this);
            checkZombiePlantCollisions(zombie, currentTick);
        }

        handleProjectiles();
        handleLawnmowers();
        cleanupEntities();
    }

    private void handleSkySunDrop(int currentTick) {
        double t = currentTick / 10.0;
        double secondsInterval = Math.max(6 + 0.05 * t, 12);
        int ticksInterval = (int) (secondsInterval * 10);

        if (currentTick - lastSunDropTick >= ticksInterval) {
            lastSunDropTick = currentTick;

            int targetRow = random.nextInt(rows);
            int targetCol = random.nextInt(columns);

            int roll = random.nextInt(100);
            model.enums.SunType type;
            int amount;

            if (roll < 80) {
                type = model.enums.SunType.PEASHOOTER;
                amount = 25;
            } else if (roll < 95) {
                type = model.enums.SunType.REPEATER;
                amount = 100;
            } else {
                type = model.enums.SunType.SUNFLOWER;
                amount = 0;
            }

            Sun newSun = new Sun(amount, 150, type);
            newSun.changinCordinate(targetCol, targetRow);
            suns.add(newSun);

            System.out.printf("New sun dropping at position (%d, %d)%n", targetCol, targetRow);
        }
    }

    private void checkZombiePlantCollisions(Zombie zombie, int currentTick) {
        Plant targetPlant = getPlantAt(zombie.getRow(), zombie.getX());
        if (targetPlant != null && !targetPlant.isDead()) {
            zombie.setEating(true);
            // اعمال دمیج
            if (currentTick % 10 == 0) {
                targetPlant.takeDamage(10);
                if (targetPlant.isDead()) {
                    System.out.printf("Plant %s at (%d, %d) is destroyed.%n",
                            targetPlant.getName(), targetPlant.getCol(), targetPlant.getRow());
                }
            }
        } else {
            zombie.setEating(false);
            zombie.move();
        }
    }

    private void handleProjectiles() {
        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile p = iterator.next();
            p.move();

            boolean hitRegistered = false;
            for (Zombie zombie : zombies) {
                if (zombie.getRow() == p.getYCoordinate() && Math.abs(zombie.getX() - p.getXCoordinate()) < 0.5) {
                    p.hitZombie(zombie);
                    hitRegistered = true;
                    break;
                }
            }

            if (hitRegistered || p.getXCoordinate() > columns) {
                iterator.remove();
            }
        }
    }

    private void handleLawnmowers() {
        for (Lawnmower mower : lawnmowers) {
            if (!mower.isActive()) {
                // بررسی شرط باخت بازی: ورود زامبی به خانه پس از مصرف شدن چمن‌زن
                for (Zombie z : zombies) {
                    if (z.getRow() == mower.getRow() && z.getX() <= -0.5) {
                        System.out.println("The zombie ate your brain; LOSER!!!");
                        return;
                    }
                }
                continue;
            }

            for (Zombie z : zombies) {
                if (z.getRow() == mower.getRow() && z.getX() <= 0.0) {
                    mower.setActive(false); // ماشین چمن‌زن یک‌بار مصرف عه
                    triggerLawnmowerRow(mower.getRow());
                    break;
                }
            }
        }
    }

    private void triggerLawnmowerRow(int row) {
        System.out.printf("The lawn mower in the row %d is triggered and killed these zombies:%n", row);
        for (Zombie z : zombies) {
            if (z.getRow() == row) {
                z.takeDamage(10000, true); // چون 021 عه پوینت کار
                System.out.println("- " + z.getName());
            }
        }
    }

    private void cleanupEntities() {
        plants.removeIf(Plant::isDead);
        zombies.removeIf(Zombie::isDead);
        suns.removeIf(Sun::isExpired);
    }

    public Plant getPlantAt(int row, double x) {
        for (Plant p : plants) {
            if (p.getRow() == row && Math.abs(p.getCol() - x) < 0.5) {
                return p;
            }
        }
        return null;
    }

    public void addProjectile(Projectile p) {
        projectiles.add(p);
    }

    public void addSun(Sun s) {
        suns.add(s);
    }

    public void placePlant(Plant p) {
        plants.add(p);
    }

    public void spawnZombie(Zombie z) {
        zombies.add(z);
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public GameState getGameState() {
        return gameState;
    }

    public boolean hasZombieInRow(int row, double plantX) {
        for (Zombie zombie : zombies) {
            if (zombie.getRow() == row && !zombie.isDead() && zombie.getX() >= plantX) {
                return true;
            }
        }
        return false;
    }

    // برای دسترسی اکشن انفجار به لیست زامبی‌ها
    public List<Zombie> getZombies() {
        return zombies;
    }

    public Plant getPlantAhead(int row, double currentX, double distanceAhead) {
        for (Plant p : plants) {
            if (p.getRow() == row) {
                if (p.getCol() <= currentX && p.getCol() >= (currentX - distanceAhead)) {
                    return p; // گیاه پیدا شد
                }
            }
        }
        return null;
    }

}