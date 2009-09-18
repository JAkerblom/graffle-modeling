package org.epistem.diagram.model;

/**
 * Implemented by graphics that can connect to other graphics
 *
 * @author nickmain
 */
public interface Connector {

    public Graphic getHead();
    public Graphic getTail();

    public boolean isSolid();
}
