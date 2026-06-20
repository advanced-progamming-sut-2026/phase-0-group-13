package model.game.zombie.ZombieParts;

public class ZombieTemplate {
    // شناسه‌های پایه
    public int id;
    public String name;
    public String type;             // نوع زامبی (معمولی، زره‌پوش، مینی‌گیم و...)

    // مقادیر عددی برای منطق بازی
    public int baseHp;              // سلامتی پایه جان اصلی زامبی (بدون زره)
    public double baseSpeed;        // سرعت حرکت در هر تیک (به صورت اعشاری مثلا 0.5)

    // مقادیر اختیاری که ممکن است در JSON برخی زامبی‌ها باشد
    public int armorHp;             // جان زره (در صورتی که زامبی زره پیش‌فرض در JSON داشته باشد)
    public String damage;           // میزان آسیبی که به گیاه می‌زند (در صورت تغییر نسبت به حالت عادی)
    public String specialAbilities; // ویژگی‌های خاص (مثل پرتاب Imp یا داشتن مشعل)
}