package org.betonquest.betonquest.modules.logger;

import org.betonquest.betonquest.modules.logger.handler.PlayerPackageReceiverSelector;
import org.betonquest.betonquest.modules.logger.handler.ReceiverSelectorRegistry;
import org.betonquest.betonquest.modules.logger.handler.RecordReceiverSelector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Facade to control what players are seeing which log records that are being logged.
 */
public class PlayerLogWatcher {

    /**
     * Storage for active selectors for ease of removing / changing them.
     */
    private final Map<UUID, Map<String, RecordReceiverSelector>> activeSelectors;

    /**
     * Selector registry that is being controlled by this facade.
     */
    private final ReceiverSelectorRegistry selectorRegistry;

    /**
     * Create a facade for the given {@link ReceiverSelectorRegistry}. Please be aware that the facade assumes that it
     * has full and sole control over the registry and that the registry is provided empty.
     *
     * @param selectorRegistry registry to be controlled
     */
    public PlayerLogWatcher(final ReceiverSelectorRegistry selectorRegistry) {
        activeSelectors = new HashMap<>();
        this.selectorRegistry = selectorRegistry;
    }

    /**
     * Check whether at least one filter was registered for the given player.
     *
     * @param subject the player to check
     * @return true if at least one filter is currently registered for the player; false otherwise
     */
    public boolean hasActiveFilters(final UUID subject) {
        return !getSelectorsFor(subject).isEmpty();
    }

    /**
     * Get all active patterns, regardless of their associated level, that are registered for the given player.
     *
     * @param subject the player to search for
     * @return all filters registered for the player
     */
    public Set<String> getActivePatterns(final UUID subject) {
        return getSelectorsFor(subject).keySet();
    }

    /**
     * Check whether the given pattern is active for the given player, regardless of the level.
     *
     * @param subject the player to check
     * @param packagePattern the pattern to check
     * @return true if the pattern is active for the player; false otherwise
     */
    public boolean isActivePattern(final UUID subject, final String packagePattern) {
        return getSelectorsFor(subject).containsKey(packagePattern);
    }

    /**
     * Register a filter. A filter consists of the subject player, the package pattern and a minimum required logging
     * level. The player is represented by its UUID. The filter is a package string that may end with an asterisk '*';
     * in the case that it does end with an asterisk the rest of the string will be matched as prefix, otherwise
     * the pattern will be matched exact. A pure asterisk (or empty prefix) will match any package.
     * If a filter was previously set for the subject player and package pattern combination then just the level will be
     * overwritten to the given (new) level.
     *
     * @param subject the player that the filter is for
     * @param packagePattern the package pattern that should be filtered for
     * @param minimumLevel the minimum logging level to filter for
     */
    public void addFilter(final UUID subject, final String packagePattern, final Level minimumLevel) {
        final PlayerPackageReceiverSelector newSelector = new PlayerPackageReceiverSelector(Set.of(subject), minimumLevel, packagePattern);
        selectorRegistry.addSelector(newSelector);
        final RecordReceiverSelector oldSelector = getSelectorsFor(subject).put(packagePattern, newSelector);
        unregisterSelector(oldSelector);
    }

    /**
     * Remove a filter. An equal UUID and pattern needs to be used for removal, no pattern matching will be done.
     * If there was no filter for the given combination it will be silently ignored.
     *
     * @param subject the player to remove the filter from
     * @param packagePattern the package pattern to be removed
     */
    public void removeFilter(final UUID subject, final String packagePattern) {
        final RecordReceiverSelector oldSelector = getSelectorsFor(subject).remove(packagePattern);
        unregisterSelector(oldSelector);
    }

    private void unregisterSelector(final RecordReceiverSelector oldSelector) {
        if (oldSelector != null) {
            selectorRegistry.removeSelector(oldSelector);
        }
    }

    private Map<String, RecordReceiverSelector> getSelectorsFor(final UUID subject) {
        return activeSelectors.computeIfAbsent(subject, uuid -> new HashMap<>());
    }
}
