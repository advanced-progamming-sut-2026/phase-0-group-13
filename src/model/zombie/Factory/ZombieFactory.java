package model.zombie.Factory;

import model.enums.ZombieType;
import model.zombie.BaseZombie;

public class ZombieFactory {
    private ZombieFactory() {
    }

    public static BaseZombie createZombie(
            ZombieType Type
    ) {

        BaseZombie zombie = switch (Type) {
            case NORMAL -> null;
            case CONEHEAD -> null;
            case BUCKETHEAD -> null;
            case GARGANTUAR -> null;
            case JALAPENO -> null;
            case SQUASH -> null;
            case WALL_NUT -> null;
        };
        // اینجا هایی که نال گذاشتیم باید  در واقع از پکیج دیتا ، بریم اون چیز مربوطه رو برداریم



        return zombie;
    }
}
