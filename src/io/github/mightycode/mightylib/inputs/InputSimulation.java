package io.github.mightycode.mightylib.inputs;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Simulated actions (commands, scripts, later replays), stored then applied when the inputs of the next tick are computed.
 * A simulated press is seen by the whole next tick, exactly like a real one.
 * Main thread only.
 */
public class InputSimulation {
    // Hold until a release is asked
    public static final int UNTIL_RELEASE = -1;

    private static class SimulatedEvent {
        private final int actionId;
        // Number of ticks, UNTIL_RELEASE, or 0 to release
        private final int ticks;

        private SimulatedEvent(int actionId, int ticks) {
            this.actionId = actionId;
            this.ticks = ticks;
        }
    }

    // Stored events, applied at the next inputs update
    private final Queue<SimulatedEvent> pendingEvents;

    // Held actions with their remaining ticks (or UNTIL_RELEASE)
    private final Map<Integer, Integer> heldActions;

    private final Set<Integer> currentState;
    private final Set<Integer> oldState;

    public InputSimulation() {
        pendingEvents = new ArrayDeque<>();
        heldActions = new HashMap<>();
        currentState = new HashSet<>();
        oldState = new HashSet<>();
    }

    // Press the action during a number of ticks (or UNTIL_RELEASE) from the next tick
    public void press(int actionId, int ticks) {
        if (ticks <= 0 && ticks != UNTIL_RELEASE)
            throw new IllegalArgumentException("Number of ticks must be positive or UNTIL_RELEASE");

        pendingEvents.add(new SimulatedEvent(actionId, ticks));
    }

    // Release the action from the next tick
    public void release(int actionId) {
        pendingEvents.add(new SimulatedEvent(actionId, 0));
    }

    // Called when the inputs of the next tick are computed
    void update() {
        oldState.clear();
        oldState.addAll(currentState);

        // Held actions end after their number of ticks
        Iterator<Map.Entry<Integer, Integer>> iterator = heldActions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> held = iterator.next();
            if (held.getValue() == UNTIL_RELEASE)
                continue;

            if (held.getValue() <= 1)
                iterator.remove();
            else
                held.setValue(held.getValue() - 1);
        }

        // New events count from the tick they are applied to
        SimulatedEvent event;
        while ((event = pendingEvents.poll()) != null) {
            if (event.ticks == 0)
                heldActions.remove(event.actionId);
            else
                heldActions.put(event.actionId, event.ticks);
        }

        currentState.clear();
        currentState.addAll(heldActions.keySet());
    }

    boolean getState(int actionId) {
        return currentState.contains(actionId);
    }

    boolean pressed(int actionId) {
        return currentState.contains(actionId) && !oldState.contains(actionId);
    }

    boolean released(int actionId) {
        return !currentState.contains(actionId) && oldState.contains(actionId);
    }

    public boolean isHeld(int actionId) {
        return heldActions.containsKey(actionId);
    }
}
