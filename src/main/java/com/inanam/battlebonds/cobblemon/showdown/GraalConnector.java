package com.inanam.battlebonds.cobblemon.showdown;

import com.cobblemon.mod.common.battles.runner.ShowdownService;
import com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService;
import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.cobblemon.mod.relocations.graalvm.polyglot.Value;

import java.util.UUID;

public class GraalConnector {

    /**
     * Obtiene el Context de GraalVM desde la instancia de GraalShowdownService.
     */
    public static Context getGraalContext(ShowdownService service) {
        if (service instanceof GraalShowdownService graalService) {
            return graalService.context;
        }
        return null;
    }

    /**
     * Obtiene la instancia JavaScript (Value) de la batalla en Showdown por su UUID.
     */
    public static Value getJsBattle(ShowdownService service, UUID battleId) {
        Context context = getGraalContext(service);
        if (context == null) {
            return null;
        }

        String script = String.format(
                "(function() {" +
                        "   if (typeof battles !== 'undefined' && battles['%s']) return battles['%s'];" +
                        "   if (typeof Sim !== 'undefined' && Sim.battles && Sim.battles['%s']) return Sim.battles['%s'];" +
                        "   return null;" +
                        "})()",
                battleId.toString(), battleId.toString(), battleId.toString(), battleId.toString()
        );

        Value result = context.eval("js", script);
        return (result != null && !result.isNull()) ? result : null;
    }
}