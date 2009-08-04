package org.epistem.graffle.rdf;

/**
 * The well known uri prefixes
 *
 * @author nickmain
 */
public enum URIPrefix {

    rdf  ( "http://www.w3.org/1999/02/22-rdf-syntax-ns#" ),
    rdfs ( "http://www.w3.org/2000/01/rdf-schema#" ),
    owl2 ( "http://www.w3.org/2002/07/owl#" ),
    xsd  ( "http://www.w3.org/2001/XMLSchema#" ),
    epi  ( "http://www.epistem.org/doc/" ),
    og   ( "http://www.epistem.org/og/20090801#" );
    
    /** The URI prefix */
    public final String prefix;
    
    private URIPrefix( String prefix ) {
        this.prefix = prefix;
    }
}
