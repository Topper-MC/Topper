package me.hsgamer.topper.agent.holder;

import me.hsgamer.topper.agent.core.Agent;
import me.hsgamer.topper.agent.core.DataEntryAgent;
import me.hsgamer.topper.core.DataEntry;
import me.hsgamer.topper.core.DataHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AgentDataHolder<K, V> extends DataHolder<K, V> {
    private final List<Agent> agentList = new ArrayList<>();
    private final List<DataEntryAgent<K, V>> entryAgentList = new ArrayList<>();

    public AgentDataHolder(String name) {
        super(name);
    }

    public void addAgent(Agent agent) {
        agentList.add(agent);
    }

    public void addEntryAgent(DataEntryAgent<K, V> entryAgent) {
        entryAgentList.add(entryAgent);
    }

    public void removeAgent(Agent agent) {
        agentList.remove(agent);
    }

    public void removeEntryAgent(DataEntryAgent<K, V> entryAgent) {
        entryAgentList.remove(entryAgent);
    }

    @Override
    protected final void onCreate(DataEntry<K, V> entry) {
        entryAgentList.forEach(agent -> agent.onCreate(entry));
    }

    @Override
    protected void onUpdate(DataEntry<K, V> entry, V oldValue) {
        entryAgentList.forEach(agent -> agent.onUpdate(entry, oldValue));
    }

    @Override
    protected final void onRemove(DataEntry<K, V> entry) {
        entryAgentList.forEach(agent -> agent.onRemove(entry));
    }

    public final void register() {
        agentList.forEach(Agent::start);
    }

    public final void unregister() {
        Consumer<Consumer<Agent>> reverseRunnable = consumer -> {
            for (int i = agentList.size() - 1; i >= 0; i--) {
                Agent agent = agentList.get(i);
                consumer.accept(agent);
            }
        };

        reverseRunnable.accept(Agent::beforeStop);

        getEntryMap().values().forEach(entry -> entryAgentList.forEach(agent -> agent.onUnregister(entry)));
        clear();

        reverseRunnable.accept(Agent::stop);
    }
}
