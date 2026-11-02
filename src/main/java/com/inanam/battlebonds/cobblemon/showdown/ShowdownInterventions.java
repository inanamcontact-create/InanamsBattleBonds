package com.inanam.battlebonds.cobblemon.showdown;

import com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService;
import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.cobblemon.mod.relocations.graalvm.polyglot.Value;

import java.util.UUID;

public class ShowdownInterventions {

    /**
     * Ubica dinámicamente el Pokémon activo en side.active (soporta 1v1 y 2v2).
     */
    private static Value getActivePokemon(Value playerSide, String targetIdentifier) {
        Value activeArray = playerSide.getMember("active");
        if (activeArray == null || !activeArray.hasArrayElements() || activeArray.getArraySize() == 0) {
            return null;
        }

        if (activeArray.getArraySize() == 1 || targetIdentifier == null || targetIdentifier.isEmpty()) {
            return activeArray.getArrayElement(0);
        }

        String targetLower = targetIdentifier.toLowerCase();
        for (long i = 0; i < activeArray.getArraySize(); i++) {
            Value pokemon = activeArray.getArrayElement(i);
            if (pokemon == null || pokemon.isNull()) continue;

            Value fainted = pokemon.getMember("fainted");
            if (fainted != null && !fainted.isNull() && fainted.asBoolean()) continue;

            Value nameVal = pokemon.getMember("name");
            Value speciesVal = pokemon.getMember("species");

            String name = (nameVal != null && !nameVal.isNull()) ? nameVal.asString().toLowerCase() : "";
            String species = (speciesVal != null && !speciesVal.isNull() && speciesVal.hasMember("name"))
                    ? speciesVal.getMember("name").asString().toLowerCase() : "";

            if (name.equals(targetLower) || species.equals(targetLower) || name.contains(targetLower)) {
                return pokemon;
            }
        }

        return activeArray.getArrayElement(0);
    }

    /**
     * 1. Inflige daño o modifica el HP y notifica a Cobblemon mediante sendFromShowdown.
     */
    public static void applyDamageAndModifyHp(UUID battleId, boolean isPlayer1, String targetIdentifier, int damageAmount, Integer targetHp) {
        ShowdownThreadExecutor.execute(service -> {
            Value jsBattle = GraalConnector.getJsBattle(service, battleId);
            if (jsBattle == null) return;

            Context context = GraalConnector.getGraalContext(service);
            if (context == null) return;

            String side = isPlayer1 ? "p1" : "p2";
            Value playerSide = jsBattle.getMember(side);
            if (playerSide == null || playerSide.isNull()) return;

            Value pokemon = getActivePokemon(playerSide, targetIdentifier);
            if (pokemon == null || pokemon.isNull()) return;

            // Script JS que aplica los cambios y extrae el nuevo protocolo de battle.log
            Value scriptFunc = context.eval("js",
                    "(function(battle, pokemon, damage, targetHp) {" +
                            "   if (!battle || !pokemon) return null;" +
                            "   let logStart = battle.log ? battle.log.length : 0;" +
                            "   if (damage > 0) {" +
                            "       battle.damage(damage, pokemon);" +
                            "   }" +
                            "   if (targetHp !== null && targetHp !== undefined) {" +
                            "       pokemon.sethp(targetHp);" +
                            "       battle.add('-sethp', pokemon, pokemon.getHealth());" +
                            "   }" +
                            "   if (pokemon.hp <= 0 && !pokemon.fainted) {" +
                            "       pokemon.faint();" +
                            "   }" +
                            "   if (battle.log && battle.log.length > logStart) {" +
                            "       let newLines = battle.log.slice(logStart);" +
                            "       return 'update\\n' + newLines.join('\\n');" +
                            "   }" +
                            "   return null;" +
                            "})"
            );

            Value result = scriptFunc.execute(jsBattle, pokemon, damageAmount, targetHp);

            // Transmitir las nuevas líneas del protocolo directamente al receptor de Cobblemon
            if (result != null && result.isString() && service instanceof GraalShowdownService graalService) {
                graalService.sendFromShowdown(battleId.toString(), result.asString());
            }
        });
    }

    /**
     * 2. Aplica o remueve un efecto volátil (Mofa) y notifica a Cobblemon.
     */
    public static void manageVolatileEffect(UUID battleId, boolean isPlayer1, String targetIdentifier, String effectName, int turns) {
        ShowdownThreadExecutor.execute(service -> {
            Value jsBattle = GraalConnector.getJsBattle(service, battleId);
            if (jsBattle == null) return;

            Context context = GraalConnector.getGraalContext(service);
            if (context == null) return;

            String side = isPlayer1 ? "p1" : "p2";
            Value playerSide = jsBattle.getMember(side);
            if (playerSide == null || playerSide.isNull()) return;

            Value pokemon = getActivePokemon(playerSide, targetIdentifier);
            if (pokemon == null || pokemon.isNull()) return;

            Value scriptFunc = context.eval("js",
                    "(function(battle, pokemon, effectName, turns) {" +
                            "   if (!battle || !pokemon) return null;" +
                            "   let logStart = battle.log ? battle.log.length : 0;" +
                            "   if (turns <= 0) {" +
                            "       pokemon.removeVolatile(effectName);" +
                            "       battle.add('-end', pokemon, 'move: ' + effectName);" +
                            "   } else {" +
                            "       pokemon.addVolatile(effectName);" +
                            "       if (pokemon.volatiles && pokemon.volatiles[effectName]) {" +
                            "           pokemon.volatiles[effectName].duration = turns;" +
                            "       }" +
                            "       battle.add('-start', pokemon, 'move: ' + effectName);" +
                            "   }" +
                            "   if (battle.log && battle.log.length > logStart) {" +
                            "       let newLines = battle.log.slice(logStart);" +
                            "       return 'update\\n' + newLines.join('\\n');" +
                            "   }" +
                            "   return null;" +
                            "})"
            );

            Value result = scriptFunc.execute(jsBattle, pokemon, effectName, turns);

            if (result != null && result.isString() && service instanceof GraalShowdownService graalService) {
                graalService.sendFromShowdown(battleId.toString(), result.asString());
            }
        });
    }

    /**
     * 3. Aplica un incremento o decremento de estadísticas y notifica a Cobblemon.
     */
    public static void modifyStatBoost(UUID battleId, boolean isPlayer1, String targetIdentifier, String stat, int amount) {
        ShowdownThreadExecutor.execute(service -> {
            Value jsBattle = GraalConnector.getJsBattle(service, battleId);
            if (jsBattle == null || amount == 0) return;

            Context context = GraalConnector.getGraalContext(service);
            if (context == null) return;

            String side = isPlayer1 ? "p1" : "p2";
            Value playerSide = jsBattle.getMember(side);
            if (playerSide == null || playerSide.isNull()) return;

            Value pokemon = getActivePokemon(playerSide, targetIdentifier);
            if (pokemon == null || pokemon.isNull()) return;

            Value scriptFunc = context.eval("js",
                    "(function(battle, pokemon, stat, amount) {" +
                            "   if (!battle || !pokemon) return null;" +
                            "   let logStart = battle.log ? battle.log.length : 0;" +
                            "   let boost = {};" +
                            "   boost[stat] = amount;" +
                            "   pokemon.boostBy(boost);" +
                            "   if (battle.log && battle.log.length > logStart) {" +
                            "       let newLines = battle.log.slice(logStart);" +
                            "       return 'update\\n' + newLines.join('\\n');" +
                            "   }" +
                            "   return null;" +
                            "})"
            );

            Value result = scriptFunc.execute(jsBattle, pokemon, stat, amount);

            if (result != null && result.isString() && service instanceof GraalShowdownService graalService) {
                graalService.sendFromShowdown(battleId.toString(), result.asString());
            }
        });
    }
}