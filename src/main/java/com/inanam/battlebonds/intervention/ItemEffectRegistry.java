package com.inanam.battlebonds.intervention;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.inanam.battlebonds.cobblemon.showdown.ShowdownInterventions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.UUID;

public class ItemEffectRegistry {

    public static String applyEffect(PokemonBattle battle, PokemonEntity pokemonEntity, ItemStack item) {

        if (battle == null || pokemonEntity == null || item == null || item.isEmpty()) {
            return "none";
        }

        UUID battleId = battle.getBattleId();
        if (battleId == null) {
            return "none";
        }

        // 1. Determinar si el Pokémon objetivo pertenece al Lado 1 (p1) o Lado 2 (p2)
        boolean isPlayer1 = Arrays.stream(battle.getSide1().getActors())
                .distinct()
                .anyMatch(actor -> actor.getPokemonList().stream()
                        .anyMatch(p -> p.getUuid().equals(pokemonEntity.getPokemon().getUuid())));

        String targetIdentifier = pokemonEntity.getPokemon().getDisplayName(false).getString();

        // --- EFECTO 1: DAÑO DIRECTO (Espada de Diamante) ---
        if (item.is(Items.DIAMOND_SWORD)) {
            int damage = 30;

            // Invocación con el identificador del Pokémon objetivo:
            ShowdownInterventions.applyDamageAndModifyHp(battleId, isPlayer1, targetIdentifier, 30, null);

            return "battlebonds.intervention.attack";
        }

        // --- EFECTO 2: TAUNT / MOFA (Pluma) ---
        if (item.is(Items.FEATHER)) {
            // Aplica Mofa (Taunt) por 3 turnos en Showdown
            ShowdownInterventions.manageVolatileEffect(battleId, isPlayer1, targetIdentifier, "taunt", 3);
            return "battlebonds.intervention.interact";
        }

        // --- EFECTO 3: STAT BOOST (+1 Ataque) (Polvo de Glowstone) ---
        if (item.is(Items.COOKED_CHICKEN)) {
            // Incrementa +1 etapa de Ataque en Showdown
            ShowdownInterventions.modifyStatBoost(battleId, isPlayer1, targetIdentifier, "atk", 1);
            return "battlebonds.intervention.consume";
        }

        return "none";
    }
}