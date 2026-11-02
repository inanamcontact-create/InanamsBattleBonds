
package com.inanam.battlebonds.event;

import com.inanam.battlebonds.InanamsBattlebonds;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.inanam.battlebonds.intervention.ItemEffectRegistry;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(
        modid = InanamsBattlebonds.MODID,
        bus = EventBusSubscriber.Bus.GAME
)
public class InamInteractionHandler {

    private static final Map<UUID, Long> LAST_INTERACTION =
            new ConcurrentHashMap<>();

    private static final long INTERACTION_DELAY = 500;

    @SubscribeEvent
    public static void onPokemonInteract(
            PlayerInteractEvent.EntityInteractSpecific event) {

        if (!(event.getTarget() instanceof PokemonEntity)) {
            return;
        }

        var player = event.getEntity();
        var item = event.getItemStack();

        if (item.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        UUID playerId = player.getUUID();

        Long lastInteraction = LAST_INTERACTION.get(playerId);

        if (lastInteraction != null &&
                now - lastInteraction < INTERACTION_DELAY) {
            return;
        }

        LAST_INTERACTION.put(playerId, now);

        String nombreJugador = player.getName().getString();
        String nombreItem = item.getHoverName().getString();

        PokemonEntity pokemon = (PokemonEntity) event.getTarget();

        if (pokemon.getBattleId() == null) {
            return;
        }

        PokemonBattle battle =
                BattleRegistry.getBattle(pokemon.getBattleId());

        var pokemonName = pokemon.getPokemon().getDisplayName(false).getString();

        if (battle == null) {
            return;
        }

        String efectoAplicado = ItemEffectRegistry.applyEffect(battle, pokemon, item);

        battle.broadcastChatMessage(Component.literal(
                "...!"
        ));
        battle.broadcastChatMessage(Component.translatable(
                efectoAplicado,
                nombreJugador,
                nombreItem,
                pokemonName
        ));
    }
}
