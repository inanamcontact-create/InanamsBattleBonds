package com.inanam.battlebonds.cobblemon.showdown;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.battles.runner.ShowdownService;
import kotlin.Unit;

import java.util.function.Consumer;

public class ShowdownThreadExecutor {

    /**
     * Encola una acción para ser ejecutada en el ShowdownThread nativo de Cobblemon.
     * Cero modificaciones al mod base y 100% seguro para GraalVM.
     */
    public static void execute(Consumer<ShowdownService> action) {
        Cobblemon.INSTANCE.getShowdownThread().queue(service -> {
            try {
                action.accept(service);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Unit.INSTANCE; // Retorno requerido para lambdas de Kotlin que devuelven Unit
        });
    }
}
