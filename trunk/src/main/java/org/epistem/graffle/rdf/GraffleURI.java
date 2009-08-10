package org.epistem.graffle.rdf;

/**
 * The URIs used for encoding an Omnigraffle document
 *
 * @author nickmain
 */
public enum GraffleURI {

    og_document,
    og_filePath,
    og_fileName,
    og_createdDate,
    og_creator,
    og_modifiedDate,
    og_modifier,    
    og_author,
    og_keyword,
    og_comments,
    og_language,
    og_organization,
    og_project,  
    og_copyright,
    og_description,
    og_subject,
    og_version,    

    og_note,
    og_text,
    og_item,
    og_value,
    og_solid,
    og_dashed,
    og_stroke,

    og_intersects,
    og_contains,
    
    og_head,
    og_tail,
    og_label,

    og_firstOut,
    og_firstIn,
    og_nextIn,
    og_nextOut,
    og_line,
    
    og_child,
    og_background,
    
    og_sheet,
    
    og_cell,
    og_firstRow,
    og_nextRow,
    og_firstCol,
    og_nextCol,
    og_firstRight,
    og_right,
    og_firstDown,
    og_down,
    og_row,
    og_col,
    og_rowCount,
    og_colCount,
    og_end
    ;
    
    private GraffleURI() {        
        String name       = name();
        int    underscore = name.indexOf( "_" );
        String prefixName = name.substring( 0, underscore );
        
        prefix   = URIPrefix.valueOf( prefixName );
        fragment = name.substring( underscore + 1 );
        uri      = prefix.prefix + fragment;
    }
    
    public final URIPrefix prefix;
    public final String    fragment;
    public final String    uri;    
}
