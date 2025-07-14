package me.hsgamer.topper.agent.core;

import me.hsgamer.topper.data.core.DataEntry;
import me.hsgamer.topper.data.core.DataHolder;

import java.util.List;
import java.util.function.Consumer;

public interface AgentHolder<K, V> extends DataHolder<K, V> {
    List<Agent> getAgents();

    List<DataEntryAgent<K, V>> getEntryAgents();

    default void onCreate(DataEntry<K, V> entry) {
        getEntryAgents().forEach(agent -> agent.onCreate(entry));
    }

    default void onRemove(DataEntry<K, V> entry) {
        getEntryAgents().forEach(agent -> agent.onRemove(entry));
    }

    default void onUpdate(DataEntry<K, V> entry, V oldValue, V newValue) {
        getEntryAgents().forEach(agent -> agent.onUpdate(entry, oldValue, newValue));
    }

    default void register() {
        getAgents().forEach(Agent::start);
    }

    default void unregister() {
        Consumer<Consumer<Agent>> reverseRunnable = consumer -> {
            List<Agent> agentList = getAgents();
            for (int i = agentList.size() - 1; i >= 0; i--) {
                Agent agent = agentList.get(i);
                consumer.accept(agent);
            }
        };

        reverseRunnable.accept(Agent::beforeStop);

        List<DataEntryAgent<K, V>> entryAgentList = getEntryAgents();
        getEntryMap().values().forEach(entry -> entryAgentList.forEach(agent -> agent.onUnregister(entry)));
        clear();

        reverseRunnable.accept(Agent::stop);
    }
}
