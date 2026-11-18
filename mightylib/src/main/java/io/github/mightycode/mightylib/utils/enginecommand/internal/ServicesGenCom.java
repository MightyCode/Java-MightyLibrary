package io.github.mightycode.mightylib.utils.enginecommand.internal;

import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.registry.Registrations;
import io.github.mightycode.mightylib.services.RunningService;
import io.github.mightycode.mightylib.services.ServiceDeclaration;
import io.github.mightycode.mightylib.services.ServiceKey;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;

// The services of the engine in their order of creation, and the factories of the scene and input managers
public final class ServicesGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "services";

    public ServicesGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "The services of the engine (order, contract, implementation, plugin, dependencies) and the managers");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.executes("Which plugin provides each service, and which one was replaced or fell back",
                context -> list(Views.engine()));
    }

    private static CommandResult list(Engine engine) {
        JSONArray services = new JSONArray();
        StringBuilder message = new StringBuilder();
        int order = 0;
        for (RunningService running : engine.services()) {
            ServiceDeclaration<?> declaration = running.declaration();
            String implementation = running.service().getClass().getName();

            JSONObject json = new JSONObject();
            json.put("order", ++order);
            json.put("key", declaration.key().toString());
            json.put("contract", declaration.key().type().getName());
            json.put("implementation", implementation);
            json.put("plugin", declaration.plugin());
            json.put("replaces", declaration.replaces() == null ? JSONObject.NULL : declaration.replaces());
            json.put("requires", keys(declaration.requiredKeys()));
            json.put("after", keys(declaration.afterKeys()));
            json.put("hasFallback", declaration.fallbackFactory() != null);
            json.put("fallbackUsed", running.isFallback());
            json.put("overlayLayer", running.service().overlayLayer());
            services.put(json);

            message.append("\n").append(order).append(". ").append(declaration.key()).append(": ")
                    .append(running.service().getClass().getSimpleName())
                    .append(" (").append(declaration.key().type().getSimpleName()).append("), plugin ")
                    .append(declaration.plugin())
                    .append(declaration.replaces() == null ? "" : " replacing " + declaration.replaces())
                    .append(running.isFallback() ? ", FALLBACK" : "")
                    .append(declaration.requiredKeys().isEmpty() ? "" : ", requires " + declaration.requiredKeys())
                    .append(declaration.afterKeys().isEmpty() ? "" : ", after " + declaration.afterKeys());
        }

        Registrations registrations = engine.registrations();
        JSONObject managers = new JSONObject();
        String sceneManager = engine.sceneManager().getClass().getName();
        String inputManager = engine.targetWindow().getInputManager().getClass().getName();
        managers.put("sceneManager", new JSONObject().put("implementation", sceneManager)
                .put("plugin", registrations.sceneManagerFactoryPlugin()));
        managers.put("inputManager", new JSONObject().put("implementation", inputManager)
                .put("plugin", registrations.inputManagerFactoryPlugin()));
        message.append("\nScene manager: ").append(engine.sceneManager().getClass().getSimpleName())
                .append(" (plugin ").append(registrations.sceneManagerFactoryPlugin()).append(")")
                .append("\nInput manager: ").append(engine.targetWindow().getInputManager().getClass().getSimpleName())
                .append(" (plugin ").append(registrations.inputManagerFactoryPlugin()).append(")");

        JSONObject data = new JSONObject();
        data.put("count", services.length());
        data.put("services", services);
        data.put("managers", managers);
        return CommandResult.ok(services.length() + " service(s)" + message, data);
    }

    private static JSONArray keys(Collection<ServiceKey<?>> keys) {
        JSONArray array = new JSONArray();
        for (ServiceKey<?> key : keys)
            array.put(key.toString());
        return array;
    }
}
