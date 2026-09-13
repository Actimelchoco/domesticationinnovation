package test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.*;

final class ClassDamageTests {
    private static int checks;
    private static void check(boolean pass, String message) { if (!pass) throw new AssertionError(message); checks++; }
    static int run(ServerLevel level) throws Exception {
        Class<?> statClass = Class.forName(LevelSystem.class.getName() + "$BaseStatReward");
        var amountMethod = LevelSystem.class.getDeclaredMethod("effectiveBaseStatAmount", TameData.class, statClass);
        var weightMethod = LevelSystem.class.getDeclaredMethod("modifiedBaseStatWeight", TameClass.class, statClass);
        amountMethod.setAccessible(true); weightMethod.setAccessible(true);
        Map<String, Double> expected = Map.of("DAMAGE",50D,"KNOCKBACK",5D,"KNOCKBACK_RESIST",35D,"ARMOR",3.5D,"ARMOR_TOUGHNESS",3.5D,"HP",3D,"SPEED",0D);
        for (Object stat : statClass.getEnumConstants()) {
            check(Math.abs((double) weightMethod.invoke(null, TameClass.STRIKER, stat) - expected.get(((Enum<?>)stat).name())) < 1E-8,
                    "striker stat distribution " + stat);
        }
        for (TameClass cls : List.of(TameClass.DPS, TameClass.STRIKER)) {
            var wolf = EntityType.WOLF.create(level); wolf.setTame(true); wolf.setOwnerUUID(UUID.randomUUID());
            wolf.moveTo(0,-59,0); level.addFreshEntity(wolf);
            var data = TameRegistry.get(wolf.getUUID()); data.tameClass = cls;
            double oldUnit = cls == TameClass.DPS ? 0.8 : 0.5;
            data.bonusDamage = oldUnit * 10 + 1;
            data.savedBonusDamage = oldUnit * 5; data.savedLevel = 6;
            wolf.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4 + data.bonusDamage);
            CompoundTag old = new CompoundTag(); old.putString("rewardCategory","BASE_STAT"); old.putString("rewardId","DAMAGE");
            old.putDouble("rewardAmount",oldUnit); old.putBoolean("active",true); old.putInt("level",2);
            data.levelRewardHistory.add(old);
            CompoundTag fresh = old.copy(); fresh.putDouble("rewardAmount",1); fresh.putBoolean("damageRewardV2",true); fresh.putInt("level",12);
            data.levelRewardHistory.add(fresh);
            Object damage = Arrays.stream(statClass.getEnumConstants()).filter(s -> ((Enum<?>)s).name().equals("DAMAGE")).findFirst().orElseThrow();
            check((double) amountMethod.invoke(null,data,damage) == 1, cls + " new damage rolls grant one");
            check(ClassDamageRepair.repair(wolf,data), cls + " repair applies");
            check(Math.abs(data.bonusDamage - 11) < 1E-8, cls + " converts old bonus without multiplying new rewards");
            check(Math.abs(data.savedBonusDamage - 5) < 1E-8, cls + " saved progress repaired");
            check(Math.abs(wolf.getAttributeValue(Attributes.ATTACK_DAMAGE) - 15) < 1E-8, cls + " live damage preserves species base");
            check(old.getDouble("rewardAmount") == 1, cls + " historical reward repaired");
            check(!ClassDamageRepair.repair(wolf,data) && data.bonusDamage == 11, cls + " repeat repair does not stack");
            check(TameData.fromTag(data.toTag()).classDamageRepaired, cls + " repair marker survives save/load");
            wolf.discard();
        }
        var dispatcher = level.getServer().getCommands().getDispatcher();
        boolean denied = false;
        try { dispatcher.execute("tames admin repairClassDamage", level.getServer().createCommandSourceStack().withPermission(0)); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException expectedError) { denied = true; }
        check(denied, "damage repair requires admin permissions");
        dispatcher.execute("tames admin repairClassDamage", level.getServer().createCommandSourceStack().withPermission(4));
        checks++;
        return checks;
    }
}
