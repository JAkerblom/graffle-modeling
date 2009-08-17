package org.epistem.diagram.model.component.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A graph of components built from a diagram model
 *
 * @author nickmain
 */
public class WorkflowGraph {

    private Collection<WorkflowComponent> entryComponents = new HashSet<WorkflowComponent>();
    private Collection<WorkflowComponent> terminalComponents = new HashSet<WorkflowComponent>();
    
    /**
     * Get the entry components - those that do not have any incoming edges.
     */
    public Iterator<WorkflowComponent> entryComponents() {
        return entryComponents.iterator();
    }
    
    /**
     * Get the terminal components - those that do not have any outgoing edges.
     */
    public Iterator<WorkflowComponent> terminalComponents() {
        return terminalComponents.iterator();
    }
    
}
